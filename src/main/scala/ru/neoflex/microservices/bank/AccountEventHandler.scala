package ru.neoflex.microservices.bank

import akka.actor.Actor
import slick.jdbc.H2Profile.api._
import java.sql.{Date, Timestamp}

import akka.http.scaladsl.model.DateTime
import ru.neoflex.microservices.bank.BankApp.system

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class AccountEventHandler(implicit val db: Database) extends Actor with AccountStore {

  import AccountEvents._
  implicit val executionContext = system.dispatcher

  implicit def DateTime2Timestamp(value : DateTime) =
    new Timestamp(value.year,value.month, value.day, value.hour, value.minute, value.second, 0);

  override def receive: Receive = {
    case evt: OpenAccountEvent =>
      insert(evt.accountNumber, evt.openDate, null, evt.openDate, 0)
    case evt: CloseAccountEvent =>
      getAccount(evt.accountNumber).onComplete{
        case Success(account) => {
          updateAccount(account.copy(closed = evt.closeDate, changed = evt.closeDate))
        }
        case Failure(ex) =>
          println(ex)
      }
    case evt: DepositEvent =>
      getAccount(evt.accountNumber).onComplete{
        case Success(account) => {
          updateAccount(account.copy(changed = evt.changeDate, amount = account.amount + evt.amount))
        }
        case Failure(ex) =>
          println(ex)
      }
    case evt: WithdrawEvent =>
      getAccount(evt.accountNumber).onComplete{
        case Success(account) => {
          updateAccount(account.copy(changed = evt.changeDate, amount = account.amount - evt.amount))
        }
        case Failure(ex) =>
          println(ex)
      }
  }
}
