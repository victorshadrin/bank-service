package ru.neoflex.microservices.bank

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import ru.neoflex.microservices.bank.BankService.OpenAccountCommand
import spray.json.DefaultJsonProtocol
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object BankRoutes {
  case class OpenAccountRequest(accountNumber: String)

}

trait BankRoutes extends SprayJsonSupport with DefaultJsonProtocol {
  import BankRoutes._

  implicit def executionContext: ExecutionContext
  implicit def system: ActorSystem
  implicit def timeout: Timeout

  implicit val openAccountRequestFormat = jsonFormat1(OpenAccountRequest)

  val bankService: ActorRef = system.actorOf(BankService.props, "bankService")

  def route: Route = {
    openAccountRequest ~
      getBalanceRequest
  }


  def openAccountRequest: Route = {
    path("openAccount") {
      post{
        entity(as[OpenAccountRequest]) { req =>
          onComplete(bankService ? OpenAccountCommand(req.accountNumber)) {
            case Success(response) =>
              complete("OK")
          }
        }
      }
    }
  }

  def getBalanceRequest: Route = {
    get {
      pathPrefix("getBalance" / Segment) { accountNumber =>
        pathEndOrSingleSlash {
          complete("OK")
        }
      }
    }
  }
}
