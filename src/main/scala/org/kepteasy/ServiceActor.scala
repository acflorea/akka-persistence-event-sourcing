package org.kepteasy

import akka.actor._
import org.json4s.DefaultFormats
import org.kepteasy.route.{ProjectRoute, UserRoute}
import org.kepteasy.service.{ProjectAggregateManager, UserAggregateManager}
import spray.http.MediaTypes._

class ServiceActor extends Actor with ActorLogging with ProjectRoute with UserRoute {

  val json4sFormats = DefaultFormats

  implicit def actorRefFactory = context

  override implicit def executionContext = context.dispatcher

  // Actor in charge with all Project Aggregates
  val projectAggregateManager = context.actorOf(ProjectAggregateManager.props)

  // Actor in charge with all User Aggregates
  val userAggregateManager = context.actorOf(UserAggregateManager.props)

  def receive =
    runRoute(
      pathPrefix("api") {
        pathPrefix("v1") {
          respondWithMediaType(`application/json`) {
            projectRoute ~ userRoute
          }
        }
      }
    )

}