package akka.persistence.titan.snapshot

import akka.actor.ActorLogging
import akka.persistence.{SelectedSnapshot, SnapshotMetadata, SnapshotSelectionCriteria}
import akka.persistence.snapshot.SnapshotStore
import com.thinkaurelius.titan.core.attribute.Cmp
import org.apache.tinkerpop.gremlin.process.traversal.Order
import com.typesafe.config.Config
import akka.persistence.titan.TitanCommons._
import scala.collection.JavaConverters._
import scala.concurrent.Future

/**
 * Created by aflorea on 18.07.2016.
 */
class TitanSnapshotStore(cfg: Config) extends SnapshotStore with ActorLogging {

  val config = new TitanSnapshotStoreConfig(cfg)

  import config._

  override def loadAsync(
                          persistenceId: String,
                          criteria: SnapshotSelectionCriteria
                        ): Future[Option[SelectedSnapshot]] = {

    val testOpen = graph.isOpen

    val snapshotVertex = graph.query()
      .has(PERSISTENCE_ID_KEY, persistenceId)
      .has(TIMESTAMP_KEY, Cmp.LESS_THAN_EQUAL, criteria.maxSequenceNr)
      .has(SEQUENCE_NR_KEY, Cmp.LESS_THAN_EQUAL, criteria.maxSequenceNr)
      .orderBy(TIMESTAMP_KEY, Order.decr)
      .orderBy(SEQUENCE_NR_KEY, Order.decr)
      .vertices().asScala.headOption

    Future.successful(None)
  }

  override def saveAsync(
                          metadata: SnapshotMetadata,
                          snapshot: Any
                        ): Future[Unit] = {

    Future.successful()
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

