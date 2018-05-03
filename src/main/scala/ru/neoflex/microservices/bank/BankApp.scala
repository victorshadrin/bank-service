package ru.neoflex.microservices.bank

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.{complete, concat, get, pathEnd, pathPrefix}
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.util.Timeout

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

object BankApp extends App with BankRoutes {

  implicit lazy val timeout = Timeout(5.seconds)

  implicit val system: ActorSystem = ActorSystem("bankServiceServer")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  Http().bindAndHandle(route, "localhost", 8080)
  Await.result(system.whenTerminated, Duration.Inf)
}
