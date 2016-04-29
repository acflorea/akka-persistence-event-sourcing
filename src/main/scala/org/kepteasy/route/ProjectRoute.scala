package org.kepteasy.route

import akka.actor.ActorRef
import org.kepteasy.domain.ProjectAggregate
import org.kepteasy.domain.ProjectAggregate.{Bag, Item}
import org.kepteasy.route.ProjectRoute.BagData
import org.kepteasy.service.AggregateManager
import org.kepteasy.service.AggregateManager.Command
import org.kepteasy.service.ProjectAggregateManager.{ImportBagToProject, DeleteProject, GetProject, CreateProject}
import spray.httpx.Json4sSupport
import spray.routing.{Route, HttpService}
import spray.routing.authentication.BasicAuth

object ProjectRoute {

  // Bag transport item
  case class BagData(id: String, name: String, items: Seq[Item]) extends Command

}

/**
 * Spray route for Project related operations
 * - it's basically an Authenticated HTTPService with JsonSupport
 */
trait ProjectRoute
  extends HttpService
    with Json4sSupport
    with RequestHandlerCreator
    with UserAuthenticator {

  /** the aggregate manager */
  val projectAggregateManager: ActorRef

  val root = "projects"

  val projectRoute =
    path(root / Segment / "bag") { id =>
      // Add bag to project
      post {
        authenticate(BasicAuth(userAuthenticator _, realm = "secured")) { user =>
          entity(as[BagData]) { bagData =>
            serveUpdate(ImportBagToProject(id, Bag(bagData.id, bagData.name, bagData.items)))
          }
        }
      }
    } ~
      path(root / Segment) { id =>
        // Retrieve project
        get {
          serveGet(GetProject(id))
        } ~
          // Remove project
          delete {
            authenticate(BasicAuth(userAuthenticator _, realm = "secured")) { user =>
              serveDelete(DeleteProject(id))
            }
          }
      } ~
      path(root) {
        // Create project
        authenticate(BasicAuth(userAuthenticator _, realm = "secured")) { user =>
          post {
            entity(as[CreateProject]) { cmd =>
              serveCreate(cmd)
            }
          }
        }
      }

  private def serveCreate(message: AggregateManager.Command): Route =
    ctx => handleRegister[ProjectAggregate.Project](ctx, projectAggregateManager, message)

  private def serveUpdate(message: AggregateManager.Command): Route =
    ctx => handleUpdate[ProjectAggregate.Project](ctx, projectAggregateManager, message)

  private def serveDelete(message: AggregateManager.Command): Route =
    ctx => handleDelete(ctx, projectAggregateManager, message)

  private def serveGet(message: AggregateManager.Command): Route =
    ctx => handleGet[ProjectAggregate.Project](ctx, projectAggregateManager, message)

}
