package ru.neoflex.microservices.bank

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ExceptionHandler, Route, StandardRoute}
import spray.json.DefaultJsonProtocol
import akka.pattern.ask
import akka.persistence.query.PersistenceQuery
import akka.persistence.query.journal.leveldb.scaladsl.LeveldbReadJournal
import akka.stream.ActorMaterializer
import akka.util.Timeout

import scala.concurrent.{ExecutionContext, Future}
import akka.stream.scaladsl.Sink

object CommandRoutes {

  case class OpenAccountRequest(accountNumber: String)
  case class CloseAccountRequest(accountNumber: String)
  case class DepositRequest(accountNumber: String, amount: Double)
  case class TransactionLockRequest(accountNumber: String, transactionId: Long, amount: Double)
  case class TransactionCancelRequest(accountNumber: String, transactionId: Long)
  case class TransactionCompleteRequest(accountNumber: String, transactionId: Long)
  case class WithdrawRequest(accountNumber: String, amount: Double)
  case class AccountBalanceResponse(value: Double)

}

trait CommandRoutes extends SprayJsonSupport with DefaultJsonProtocol with AccountHelper {

  import CommandRoutes._
  import StatusCodes._
  import AccountCommands._

  val exceptionHandler = ExceptionHandler {
    case e: NoSuchElementException => {
      complete(HttpResponse(InternalServerError, entity = "Entity not found"))
    }
  }

  implicit def executionContext: ExecutionContext
  implicit def system: ActorSystem
  implicit def timeout: Timeout
  implicit def materializer: ActorMaterializer

  implicit val openAccountRequestFormat = jsonFormat1(OpenAccountRequest)
  implicit val closeAccountRequestFormat = jsonFormat1(CloseAccountRequest)
  implicit val depositRequestFormat = jsonFormat2(DepositRequest)
  implicit val withdrawRequestFormat = jsonFormat2(WithdrawRequest)
  implicit val accountBalanceResponseFormat = jsonFormat1(AccountBalanceResponse)
  implicit val transactionLockRequestFormat = jsonFormat3(TransactionLockRequest)
  implicit val transactionCancelRequestFormat = jsonFormat2(TransactionCancelRequest)
  implicit val transactionCompleteRequestFormat = jsonFormat2(TransactionCompleteRequest)


  implicit lazy val readJournal: LeveldbReadJournal = PersistenceQuery(system).readJournalFor[LeveldbReadJournal](
    LeveldbReadJournal.Identifier)

  def commandRoute: Route = {
    handleExceptions(exceptionHandler) {
      openAccountRequest ~
        closeAccountRequest ~
        withdrawRequest ~
        depositRequest
    }
  }

  def openAccountRequest: Route = {
    path("open") {
      post {
        entity(as[OpenAccountRequest]) { req =>
          val futureOption: Future[Option[String]] = readJournal.currentPersistenceIds().dropWhile(_ != "Account-" + req.accountNumber).take(1).runWith(Sink.headOption)
          onSuccess(futureOption) {
            case Some(str) =>
              complete(OK, "Account exists")
            case None =>
              onSuccess(system.actorOf(AccountAggregator.props, req.accountNumber) ? OpenAccountCommand()) { response =>
                complete(OK, "Account opened")
              }
          }
        }
      }
    }
  }

  def closeAccountRequest: Route = {
    path("close") {
      post {
        entity(as[CloseAccountRequest]) { req =>
          onSuccess(getAccount(req.accountNumber)) { ref =>
            onSuccess(ref ? CloseAccountCommand()) {
              case res: CommandCompleted =>
                complete(StatusCodes.OK)
              case res: CommandFailed =>
                complete(InternalServerError, res.reason)
            }
          }
        }
      }
    }
  }

  def withdrawRequest: Route = {
    path("withdraw") {
      post {
        entity(as[DepositRequest]) { req =>
          onSuccess(getAccount(req.accountNumber)) { ref =>
            onSuccess(ref ? WithdrawCommand(req.amount)) {
              case res: CommandCompleted =>
                complete(StatusCodes.OK)
              case res: CommandFailed =>
                complete(InternalServerError, res.reason)
            }
          }
        }
      }
    }
  }

  def depositRequest: Route = {
    path("deposit") {
      post {
        entity(as[DepositRequest]) { req =>
          onSuccess(getAccount(req.accountNumber)) { ref =>
            onSuccess(ref ? DepositCommand(req.amount)) {
              case res: CommandCompleted =>
                complete(StatusCodes.OK)
              case res: CommandFailed =>
                complete(InternalServerError, res.reason)
            }
          }
        }
      }
    }
  }

  def transactionLockRequest: Route = {
    path("lock") {
      post {
        entity(as[TransactionLockRequest]) { req =>
          onSuccess(getAccount(req.accountNumber)) { ref =>
            onSuccess(ref ? TransactionLockCommand(req.transactionId, req.amount)) {
              case res: CommandCompleted =>
                complete(StatusCodes.OK)
              case res: CommandFailed =>
                complete(InternalServerError, res.reason)
            }
          }
        }
      }
    }
  }

}
