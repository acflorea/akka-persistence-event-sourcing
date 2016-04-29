package org.kepteasy.service

import akka.actor._
import org.kepteasy.domain.UserAggregate.{Initialize, ChangePassword}
import org.kepteasy.domain.{AggregateRoot, UserAggregate}

object UserAggregateManager {

  import AggregateManager._

  case class RegisterUser(name: String, pass: String) extends Command
  case class GetUser(name: String) extends Command
  case class ChangeUserPassword(id: String, pass: String) extends Command

  def props: Props = Props(new UserAggregateManager)
}

class UserAggregateManager extends AggregateManager {

  import AggregateRoot._
  import UserAggregateManager._
  import UserAggregateManager._

  def processCommand = {
    case RegisterUser(name, pass) =>
      val id = "user-" + name
      processAggregateCommand(id, Initialize(pass))
    case GetUser(name) =>
      val id = "user-" + name
      processAggregateCommand(id, GetState)
    case ChangeUserPassword(id, pass) =>
      processAggregateCommand(id, ChangePassword(pass))
  }

  override def aggregateProps(id: String) = UserAggregate.props(id)
}