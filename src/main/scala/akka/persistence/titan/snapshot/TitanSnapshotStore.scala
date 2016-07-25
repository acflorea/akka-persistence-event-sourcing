package akka.persistence.titan.snapshot

import akka.actor.ActorLogging
import akka.persistence.serialization.Snapshot
import akka.persistence.{SelectedSnapshot, SnapshotMetadata, SnapshotSelectionCriteria}
import akka.persistence.snapshot.SnapshotStore
import com.thinkaurelius.titan.core.attribute.Cmp
import org.apache.tinkerpop.gremlin.process.traversal.Order
import com.typesafe.config.Config
import akka.persistence.titan.TitanCommons._
import akka.serialization.{Serialization, SerializationExtension}

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.pickling.Defaults._
import scala.pickling.json._

/**
 * Created by aflorea on 18.07.2016.
 */
class TitanSnapshotStore(cfg: Config) extends SnapshotStore with ActorLogging {

  private lazy val serialization: Serialization = SerializationExtension(context.system)

  val config = new TitanSnapshotStoreConfig(cfg)

  import config._

  override def loadAsync(
                          persistenceId: String,
                          criteria: SnapshotSelectionCriteria
                        ): Future[Option[SelectedSnapshot]] = {

    val snapshotVertex = graph.query()
      .has(PERSISTENCE_ID_KEY, persistenceId)
      .has(TIMESTAMP_KEY, Cmp.LESS_THAN_EQUAL, criteria.maxSequenceNr)
      .has(SEQUENCE_NR_KEY, Cmp.LESS_THAN_EQUAL, criteria.maxSequenceNr)
      .orderBy(TIMESTAMP_KEY, Order.decr)
      .orderBy(SEQUENCE_NR_KEY, Order.decr)
      .vertices().asScala.headOption

    Future {
      snapshotVertex map {
        vertex =>
          val snapshotMetadata = SnapshotMetadata(
            vertex.property[String](PERSISTENCE_ID_KEY).value(),
            vertex.property[Long](SEQUENCE_NR_KEY).value(),
            vertex.property[Long](TIMESTAMP_KEY).value()
          )

          val pickled = vertex.property[String](PAYLOAD_KEY).value()
          JSONPickle(pickled).unpickle[Snapshot]

          // FixMe - load actual object
          SelectedSnapshot(snapshotMetadata, "Test")
      }
    }
  }

  override def saveAsync(
                          metadata: SnapshotMetadata,
                          snapshot: Any
                        ): Future[Unit] = {

    Future {

      val vertex = graph.addVertex()
      // Keys
      vertex.property(TIMESTAMP_KEY, System.currentTimeMillis())
      vertex.property(PERSISTENCE_ID_KEY, metadata.persistenceId)
      vertex.property(SEQUENCE_NR_KEY, metadata.sequenceNr)
      vertex.property(SEQUENCE_NR_KEY, metadata.timestamp)

      val pickle = snapshot.pickle
      vertex.property(PAYLOAD_KEY, pickle.value)
      graph.tx().commit()
      log.debug(s"$snapshot persisted OK!")

    }
  }

  override def deleteAsync(
                            metadata: SnapshotMetadata
                          ): Future[Unit] = {

    Future.successful()
  }

  override def deleteAsync(
                            persistenceId: String, criteria: SnapshotSelectionCriteria
                          ): Future[Unit] = {

    Future.successful()
  }
}

