package org.kepteasy.route

import akka.pattern.ask
import akka.util.Timeout
import org.kepteasy.domain.ProjectAggregate.Project
import org.kepteasy.domain.AggregateRoot
import AggregateRoot.Removed
import org.kepteasy.service.ProjectAggregateManager.{CreateProject, GetProject}
import org.kepteasy.service.{ProjectAggregateManager, UserAggregateManager}
import UserAggregateManager.RegisterUser
import java.util.UUID
import org.json4s.{DefaultFormats, JObject}
import org.scalatest.{BeforeAndAfterAll, Matchers, FlatSpec}
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps
import spray.http.{BasicHttpCredentials, StatusCodes}
import spray.testkit.ScalatestRouteTest

class ProjectRouteSpec
  extends FlatSpec with ScalatestRouteTest with Matchers with ProjectRoute with BeforeAndAfterAll {

  implicit val json4sFormats = DefaultFormats

  implicit val timeout = Timeout(2.seconds)

  implicit def executionContext = system.dispatcher

  def actorRefFactory = system

  val userAggregateManager = system.actorOf(UserAggregateManager.props)

  val projectAggregateManager = system.actorOf(ProjectAggregateManager.props)

  implicit val routeTestTimeout = RouteTestTimeout(5.seconds)

  val credentials = BasicHttpCredentials("test", "test")

  override def beforeAll: Unit = {
    val userFuture = userAggregateManager ? RegisterUser("test", "test")
    Await.result(userFuture, 5 seconds)
  }

  "ProjectRoute" should "return not found if non-existing project is requested" in {
    Get("/projects/" + UUID.randomUUID().toString) ~> projectRoute ~> check {
      response.status shouldBe StatusCodes.NotFound
    }
  }

  it should "create a project" in {
    val name = "testProjectName01"
    val address = Some("testProjectAddress")
    Post("/projects", Map("name" -> name, "address" -> address)) ~> addCredentials(credentials) ~> projectRoute ~> check {
      response.status shouldBe StatusCodes.Created
      val id = (responseAs[JObject] \ "id").extract[String]
      val project = getProjectFromManager(id)
      project.name shouldEqual name
      project.address shouldEqual address
    }
  }

  it should "return existing project" in {
    val name = "testProjectName02"
    val address = Some("testProjectAddress")
    val project = createProjectInManager(name, address)
    Get(s"/projects/" + project.id) ~> projectRoute ~> check {
      response.status shouldBe StatusCodes.OK
      val responseJson = responseAs[JObject]
      (responseJson \ "name").extract[String] shouldEqual name
      (responseJson \ "address").extract[Option[String]] shouldEqual address
    }
  }

  it should "remove project" in {
    val name = "testProjectName03"
    val address = "testProjectAddress"
    val project = createProjectInManager(name, Some(address))
    Delete("/projects/" + project.id) ~> addCredentials(credentials) ~> projectRoute ~> check {
      response.status shouldBe StatusCodes.NoContent
      val emptyVehicleFuture = (projectAggregateManager ? GetProject(project.id))
      val emptyVehicle = Await.result(emptyVehicleFuture, 2.seconds)
      emptyVehicle shouldBe Removed
    }
  }

  private def getProjectFromManager(id: String) = {
    val projectFuture = (projectAggregateManager ? GetProject(id)).mapTo[Project]
    Await.result(projectFuture, 2.seconds)
  }

  private def createProjectInManager(name: String, address: Option[String]) = {
    val projectFuture = (projectAggregateManager ? CreateProject(name, address)).mapTo[Project]
    Await.result(projectFuture, 2.seconds)
  }

}