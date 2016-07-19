package akka.persistence.titan.snapshot

import akka.actor.ActorLogging
import akka.persistence.{SelectedSnapshot, SnapshotMetadata, SnapshotSelectionCriteria}
import akka.persistence.snapshot.SnapshotStore
import com.typesafe.config.Config

import scala.concurrent.Future

/**
 * Created by aflorea on 18.07.2016.
 */
class TitanSnapshotStore(cfg: Config) extends SnapshotStore with ActorLogging {

  val config = new TitanSnapshotStoreConfig(cfg)

  override def loadAsync(
                          persistenceId: String,
                          criteria: SnapshotSelectionCriteria
                        ): Future[Option[SelectedSnapshot]] = {

    val testOpen = config.graph.isOpen

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

