package ru.neoflex.microservices.bank

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import spray.json.DefaultJsonProtocol
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object BankRoutes {
  case class OpenAccountRequest(accountNumber: String)
  case class AccountBalanceResponse(accountNumber: String, value: Double)
}

trait BankRoutes  extends SprayJsonSupport with DefaultJsonProtocol {
  import BankRoutes._
  import BankService._

  implicit def executionContext: ExecutionContext
  implicit def system: ActorSystem
  implicit def timeout: Timeout

  implicit val openAccountRequestFormat = jsonFormat1(OpenAccountRequest)
  implicit val accountBalanceResponseFormat = jsonFormat2(AccountBalanceResponse)

  lazy val bankService: ActorRef = system.actorOf(BankService.props, "bankService")

  def route: Route = {
    openAccountRequest ~
      getBalanceRequest
  }


  def openAccountRequest: Route = {
    path("openAccount") {
      post{
        entity(as[OpenAccountRequest]) { req =>
          onSuccess(bankService ? OpenAccount(req.accountNumber)) {
            case AccountExists =>
              complete(StatusCodes.OK, "Account exists")
            case AccountOpened(acc: String) =>
              complete(StatusCodes.OK, "Account added")
          }
        }
      }
    }
  }

  def getBalanceRequest: Route = {
    get {
      pathPrefix("getBalance" / Segment) { accountNumber =>
        pathEndOrSingleSlash {
          onSuccess(bankService ? GetBalance(accountNumber)) {
            case res: GetBalanceResponse => {
                complete (AccountBalanceResponse(res.accounrName, res.balance))
            }
            case default =>
              complete(StatusCodes.InternalServerError)
          }
        }
      }
    }
  }
}
