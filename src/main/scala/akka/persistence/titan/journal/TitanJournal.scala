package akka.persistence.titan.journal

import akka.actor.ActorLogging
import akka.persistence.journal.AsyncWriteJournal
import akka.persistence.titan.TitanCommons._
import akka.persistence.{AtomicWrite, PersistentRepr}
import akka.serialization.{Serialization, SerializationExtension}
import com.thinkaurelius.titan.core.attribute.Cmp
import com.typesafe.config.Config
import org.apache.tinkerpop.gremlin.process.traversal.Order

import scala.collection.JavaConverters._
import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.pickling.Defaults._
import scala.pickling.json._
import scala.util.Try


/**
 * Created by aflorea on 18.07.2016.
 */
class TitanJournal(conf: Config) extends AsyncWriteJournal with ActorLogging {

  private lazy val serialization: Serialization = SerializationExtension(context.system)

  val config = new TitanJournalConfig(conf)

  import config._

  override def asyncWriteMessages(
                                   messages: Seq[AtomicWrite]
                                 ): Future[Seq[Try[Unit]]] = {

    def getCCParams(cc: Any) =
      cc match {
        // A class
        case cc: AnyRef => (Map[String, Any]() /: cc.getClass.getDeclaredFields) { (a, f) =>
          f.setAccessible(true)
          a + (f.getName -> (f.get(cc) match {
            case Some(value) => value
            case anythingelse => anythingelse
          }))
        }
        case _ => Map("_raw" -> cc)
      }


    Future {

      for {
        message <- messages
        payload <- message.payload
      } yield
        Try {
          val vertex = graph.addVertex()
          // Keys
          vertex.property(TIMESTAMP_KEY, System.currentTimeMillis())
          vertex.property(PERSISTENCE_ID_KEY, payload.persistenceId)
          vertex.property(SEQUENCE_NR_KEY, payload.sequenceNr)
          // Deleted ?
          vertex.property(DELETED_KEY, payload.deleted)
          // Properties
          getCCParams(payload.payload) map { entry =>
            vertex.property(s"$PAYLOAD_KEY.${entry._1}", entry._2)
          }
          serialization.serialize(payload) map {
            vertex.property(PAYLOAD_KEY, _)
          }
          graph.tx().commit()
          log.debug(s"$payload persisted OK!")
        }

    }
  }

  override def asyncDeleteMessagesTo(
                                      persistenceId: String,
                                      toSequenceNr: Long
                                    ): Future[Unit] = {

    Future.successful()
  }

  override def asyncReplayMessages(
                                    persistenceId: String,
                                    fromSequenceNr: Long,
                                    toSequenceNr: Long,
                                    max: Long
                                  )
                                  (
                                    recoveryCallback: (PersistentRepr) => Unit
                                  ): Future[Unit] = {

    val journalVertices = graph.query()
      .has(PERSISTENCE_ID_KEY, persistenceId)
      .has(SEQUENCE_NR_KEY, Cmp.GREATER_THAN_EQUAL, fromSequenceNr)
      .has(SEQUENCE_NR_KEY, Cmp.LESS_THAN_EQUAL, toSequenceNr)
      .orderBy(TIMESTAMP_KEY, Order.decr)
      .orderBy(SEQUENCE_NR_KEY, Order.decr)
      .vertices().asScala

    Future {
      journalVertices map { vertex =>

        serialization.deserialize[PersistentRepr](
          vertex.property[Array[Byte]](PAYLOAD_KEY).value(),
          Class[PersistentRepr]).get

      } foreach recoveryCallback
    }

  }

  override def asyncReadHighestSequenceNr(
                                           persistenceId: String,
                                           fromSequenceNr: Long
                                         ): Future[Long] = {

    val journalVertex = graph.query()
      .has(PERSISTENCE_ID_KEY, persistenceId)
      .has(SEQUENCE_NR_KEY, Cmp.GREATER_THAN_EQUAL, fromSequenceNr)
      .orderBy(TIMESTAMP_KEY, Order.decr)
      .orderBy(SEQUENCE_NR_KEY, Order.decr)
      .vertices().asScala.headOption

    Future {
      journalVertex match {
        case Some(vertex) => vertex.property[Long](SEQUENCE_NR_KEY).value()
        case _ => 0L
      }
    }

  }

}
