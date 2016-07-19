package org.kepteasy

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import spray.can._

object Boot extends App {

  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem("seed-actor-system")
  
  implicit val executionContext = system.dispatcher

  // create and start our service actor
  val service = system.actorOf(Props(new ServiceActor), "seed-service")

  // start a new HTTP server on port 9999 with our service actor as the handler
  IO(Http) ! Http.Bind(service, interface = "0.0.0.0", port = 9999)
}