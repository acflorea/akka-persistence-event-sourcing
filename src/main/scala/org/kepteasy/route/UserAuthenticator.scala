package org.kepteasy.route

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import com.github.t3hnar.bcrypt._
import org.kepteasy.service.UserAggregateManager
import UserAggregateManager.GetUser
import org.kepteasy.domain.UserAggregate.User
import spray.routing.authentication.UserPass

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

trait UserAuthenticator {

  val userAggregateManager: ActorRef

  implicit def executionContext: ExecutionContext

  def userAuthenticator(userPass: Option[UserPass]): Future[Option[User]] =
    userPass match {
      case Some(UserPass(user, pass)) =>
        implicit val timeout = Timeout(2 seconds)
        (userAggregateManager ? GetUser(user)).map(_ match {
          case u: User if pass.isBcrypted(u.pass) =>
            Some(u)
          case _ => None
        })
      case None =>
        Future(None)
    }

}
