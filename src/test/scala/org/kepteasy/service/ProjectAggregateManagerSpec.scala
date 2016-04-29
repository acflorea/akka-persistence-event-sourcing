package org.kepteasy.service

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.testkit.TestActorRef
import akka.util.Timeout
import org.kepteasy.domain.ProjectAggregate._
import org.kepteasy.service.{ProjectAggregateManager, AggregateManager}
import org.scalatest.FlatSpec
import org.scalatest.BeforeAndAfterAll
import scala.concurrent.duration._
import scala.concurrent.{Future, Await}
import scala.language.postfixOps

class ProjectAggregateManagerSpec extends FlatSpec with BeforeAndAfterAll {

  import ProjectAggregateManager._

  implicit val actorSystem = ActorSystem("projectAggregateManagerSpec-system")

  implicit val timeout = Timeout(2 seconds)

  implicit val executionContext = actorSystem.dispatcher

  override def afterAll = {
    actorSystem.shutdown
  }

  "ProjectAggregateManager" should "create new child actor when creating new project" in {
    val manager = TestActorRef(ProjectAggregateManager.props, "ProjectAggregateManager-test-actor")

    val initialSize = manager.children.size

    manager ! CreateProject(name = "testName", address = Some("testAddress"))

    val finalSize = manager.children.size

    assert(finalSize == initialSize + 1)
  }

  it should "use existing child actor when updating project data" in {
    val manager = TestActorRef(ProjectAggregateManager.props)

    //create a new project
    val future = (manager ? CreateProject(name = "testName", address = Some("testAddress"))).mapTo[Project]

    val project = Await.result(future, 2 seconds)

    val initialSize = manager.children.size

    //update the project
    val testBag = Bag("bagId", "bagName", Seq(Item("itemId")))

    manager ! ImportBagToProject(project.id, testBag)

    //check children size
    val finalSize = manager.children.size

    assert(finalSize == initialSize)
  }

  it should "kill child actors when max count is exceeded" in {
    val manager = TestActorRef(ProjectAggregateManager.props)

    //create more projects than manager should keep
    implicit val timeout = Timeout(5 seconds)
    val futures = (0 to AggregateManager.maxChildren * 2).foldLeft(Seq[Future[Project]]()) { (futures, counter) =>
      futures :+ (manager ? CreateProject(name = s"testName_$counter", address = Some("testAddress"))).mapTo[Project]
    }

    val future = Future sequence futures
    Await.result(future, 5 seconds)

    val finalSize = manager.children.size

    assert(finalSize <= AggregateManager.maxChildren)
  }

}