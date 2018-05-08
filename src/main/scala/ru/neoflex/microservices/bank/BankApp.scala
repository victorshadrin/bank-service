package ru.neoflex.microservices.bank

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.util.Timeout
import slick.basic.DatabasePublisher
import slick.jdbc.H2Profile.api._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

object BankApp extends App with CommandRoutes with QueryRoutes {

  implicit lazy val timeout = Timeout(5.seconds)

  implicit val system: ActorSystem = ActorSystem("bankServiceServer")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  def routes: Route = {
    commandRoute ~
    queryRoute
  }

  implicit val db = Database.forConfig("h2database")

  createRepository()

  Http().bindAndHandle(routes, "localhost", 8080)
  println("Server started.")
  Await.result(system.whenTerminated, Duration.Inf)
}
