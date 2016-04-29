package org.kepteasy.domain

import akka.actor.Props
import akka.persistence.SnapshotMetadata
import org.kepteasy.domain.AggregateRoot._
import org.kepteasy.domain.ProjectAggregate.ProjectStatus.ProjectStatus

/**
 * An abstract Project aggregate
 * - A Bag consists of multiple Items along with their quantities and properties
 * - A Bag can be imported into a Project. This leads to actually associating all the Items
 * from that particular Bag to the Project.
 * - The use can decide to "order" the items associated with a certain project,
 * but before placing the order it can adjust the item quantities
 */
object ProjectAggregate {

  import AggregateRoot._

  case class Item(itemId: String,
                  name: String = "",
                  itemType: String = "",
                  itemProperties: Map[String, String] = Map.empty[String, String],
                  initialQuantity: Int = 0,
                  quantity: Int = 0,
                  multiplicationFactor: Int = 1
                 )

  case class Bag(id: String, name: String, items: Seq[Item])

  /**
   * Possible Project status values
   */
  object ProjectStatus extends Enumeration {

    type ProjectStatus = Value
    val inprogress = Value("In Progress")
    val ordered = Value("Ordered")
  }


  /** Aggregate specific state values */
  case class Project(id: String,
                     name: String = "",
                     address: Option[String] = None,
                     bags: Option[Seq[Bag]] = None,
                     connections: Option[Seq[Item]] = None,
                     linkedProjectIds: Option[Seq[String]] = None,
                     status: ProjectStatus = ProjectStatus.inprogress
                    ) extends State

  /** Aggregate specific commands */
  case class Initialize(name: String, address: Option[String]) extends Command

  case class AddBag(bag: Bag) extends Command

  case class UpdateItemQuantity(itemId: String, quantity: Int) extends Command

  case class UpdateItemMultiplicationFactor(
                                             itemId: String,
                                             multiplicationFactor: Int) extends Command

  case object PlaceOrder extends Command

  case class LinkToProject(id: String) extends Command

  /** Aggregate specific events */
  case class ProjectInitialized(name: String, address: Option[String]) extends Event

  case class BagImportedToProject(bag: Bag) extends Event

  case class ItemQuantityUpdated(itemId: String, quantity: Int) extends Event

  case class LinkedToProject(id: String) extends Event

  case class ItemMultiplicationFactorUpdated(
                                              itemId: String,
                                              multiplicationFactor: Int) extends Event

  case object OrderPlaced extends Event

  /** Blow away! */
  case object ProjectRemoved extends Event

  /** Configurable object used to create an Actor */
  def props(id: String): Props = Props(new ProjectAggregate(id))

}


class ProjectAggregate(id: String) extends AggregateRoot {

  import ProjectAggregate._

  override def persistenceId = id

  override def updateState(evt: AggregateRoot.Event): Unit = evt match {
    case ProjectInitialized(name, address) =>
      context.become(created)
      state = Project(id, name, address)

    case BagImportedToProject(bag: Bag) => state match {
      case p: Project =>
        val newBags = p.bags match {
          // add the new bag
          case Some(bs) => bs :+ bag
          // build a new bag sequence
          case None => Seq(bag)
        }
        state = p.copy(bags = Option(newBags))
      case _ => // nada
    }

    case LinkedToProject(projectId: String) => state match {
      case p: Project =>
        val newLinkedProjectIds = p.linkedProjectIds match {
          // add the new connection
          case Some(pIds) => pIds :+ projectId
          // build a new connections sequence
          case None => Seq(projectId)
        }
        state = p.copy(linkedProjectIds = Option(newLinkedProjectIds))
      case _ => // nada
    }

    case ItemQuantityUpdated(itemId, quantity) => state match {
      case p: Project if p.bags.isDefined =>
        val newBags = p.bags.get map { bag =>
          val newItems = bag.items map { item =>
            if (item.itemId == itemId)
              item.copy(quantity = quantity)
            else item
          }
          bag.copy(items = newItems)
        }
        state = p.copy(bags = Some(newBags))
      case _ => // nada
    }

    case ItemMultiplicationFactorUpdated(itemId, multiplicationFactor) => state match {
      case p: Project if p.bags.isDefined =>
        val newBags = p.bags.get map { bag =>
          val newItems = bag.items map { item =>
            if (item.itemId == itemId)
              item.copy(multiplicationFactor = multiplicationFactor)
            else item
          }
          bag.copy(items = newItems)
        }
        state = p.copy(bags = Some(newBags))
      case _ => // nada
    }

    case OrderPlaced => state match {
      case p: Project =>
      case _ => //nada
    }

    case ProjectRemoved =>
      context.become(removed)
      state = Removed
  }

  /** Valid events in an uninitialized state (before creation)
   * - Initialize
   * - GetState
   * - KillAggregate
   * */
  val initial: Receive = {
    case Initialize(name, address) =>
      persist(ProjectInitialized(name, address))(afterEventPersisted)
    case GetState =>
      respond()
    case KillAggregate =>
      context.stop(self)
  }

  /** Valid events in a created state (after creation)
   * - ImportBagToProject
   * - UpdateItemQuantity
   * - UpdateItemMultiplicationFactor
   * - PlaceOrder
   * - Remove
   * - GetState
   * - KilAggregate
   * */
  val created: Receive = {
    case AddBag(bag) =>
      persist(BagImportedToProject(bag))(afterEventPersisted)
    case UpdateItemQuantity(itemId, quantity) =>
      persist(ItemQuantityUpdated(itemId, quantity))(afterEventPersisted)
    case UpdateItemMultiplicationFactor(itemId, multiplicationFactor) =>
      persist(ItemMultiplicationFactorUpdated(itemId, multiplicationFactor))(afterEventPersisted)
    case PlaceOrder =>
      persist(OrderPlaced)(afterEventPersisted)
    case Remove =>
      persist(ProjectRemoved)(afterEventPersisted)
    case GetState =>
      respond()
    case KillAggregate =>
      context.stop(self)
  }

  /** Valid events in a removed state (after deletion)
   * - GetState
   * - KilAggregate
   * */
  val removed: Receive = {
    case GetState =>
      respond()
    case KillAggregate =>
      context.stop(self)
  }

  val receiveCommand: Receive = initial

  override def restoreFromSnapshot(metadata: SnapshotMetadata, state: State) = {
    this.state = state
    state match {
      case Uninitialized => context become initial
      case Removed => context become removed
      case _: Project => context become created
    }
  }


}
