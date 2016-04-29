package org.kepteasy.service

import java.net.URLEncoder

import akka.actor.Props
import org.kepteasy.domain.AggregateRoot.{Remove, GetState}
import org.kepteasy.domain.ProjectAggregate
import org.kepteasy.domain.ProjectAggregate.{AddBag, Item, Bag, Initialize}
import org.kepteasy.service.ProjectAggregateManager._

/**
 * Project Aggregate Manager
 */
object ProjectAggregateManager {

  import AggregateManager._

  /** These are a special breed of surrogates DTOs */
  case class CreateProject(name: String, address: Option[String]) extends Command

  case class GetProject(id: String) extends Command

  case class DeleteProject(id: String) extends Command

  case class ImportBagToProject(id: String, bag: Bag) extends Command

  /** */
  def props: Props = Props(new ProjectAggregateManager)

}


class ProjectAggregateManager extends AggregateManager {

  def processCommand = {
    case CreateProject(name, address) =>
      val id = URLEncoder.encode(name, "UTF-8")
      processAggregateCommand(id, Initialize(name, address))

    case ImportBagToProject(id, bag) =>
      processAggregateCommand(id, AddBag(bag))

    case GetProject(id) =>
      processAggregateCommand(id, GetState)

    case DeleteProject(id) =>
      processAggregateCommand(id, Remove)
  }

  override def aggregateProps(id: String) = ProjectAggregate.props(id)
}
