package ru.neoflex.microservices.bank

import akka.actor.{ActorLogging, Props}
import akka.http.scaladsl.model.DateTime
import akka.persistence.PersistentActor
import akka.pattern.ask

import scala.collection.mutable

object AccountAggregator {
  def props: Props = Props[AccountAggregator]
  trait AccountEvent
  case class OpenAccountEvent(openDate: DateTime) extends AccountEvent
  case class CloseAccountEvent(closeDate: DateTime) extends AccountEvent
  case class DepositEvent(changeDate: DateTime, amount: Double) extends AccountEvent
  case class WithdrawEvent(changeDate: DateTime, amount: Double) extends AccountEvent
  case class TransactionLockEvent(changeDate: DateTime, amount: Double, transactionId: Long) extends AccountEvent
  case class TransactionCompleteEvent(changeDate: DateTime, transactionId: Long) extends AccountEvent
  case class TransactionCancelEvent(changeDate: DateTime, transactionId: Long) extends AccountEvent
}

class AccountAggregator extends PersistentActor with ActorLogging {

  import AccountAggregator._
  import AccountProtocol._

  case class AccountState(amount: Double, openDate: DateTime, closeDate: DateTime, lastChangeDate: DateTime,
                          locked: Map[Long, Double]) {
    def isClosed(): Boolean = closeDate != null
    def isOpened(): Boolean = openDate != null
    def freeAmount(): Double = amount - locked.foldLeft(0.0){
      case (total,(id, value)) => total + value
    }
    def knownTransaction(transactionId: Long): Boolean = locked.keys.find(_ == transactionId).isDefined
  }

  var accountState: AccountState = AccountState(0, null, null, null, Map[Long, Double]())

  override def receiveRecover: Receive = {
    case event: AccountEvent if accountState.closeDate == null => {
      updateState(event)
    }
  }

  def updateState(event: AccountEvent): Unit = event match {
    case event: OpenAccountEvent if accountState.closeDate == null => {
      accountState = accountState.copy(openDate = event.openDate,lastChangeDate = event.openDate)
    }
    case event: CloseAccountEvent if accountState.closeDate == null => {
      accountState = accountState.copy(closeDate = event.closeDate, lastChangeDate = event.closeDate)
    }
    case event: DepositEvent if accountState.closeDate == null => {
      accountState = accountState.copy(amount = accountState.amount + event.amount, lastChangeDate = event.changeDate)
    }
    case event: WithdrawEvent if accountState.closeDate == null => {
      accountState = accountState.copy(amount = accountState.amount - event.amount, lastChangeDate = event.changeDate)
    }
    case event: TransactionLockEvent if accountState.closeDate == null => {
      accountState = accountState.copy(locked = accountState.locked + ((event.transactionId, event.amount)), lastChangeDate = event.changeDate)
    }
    case event: TransactionCompleteEvent if accountState.closeDate == null => {
      accountState = accountState.copy(amount = accountState.amount - accountState.locked.get(event.transactionId).get,
        locked = accountState.locked - (event.transactionId), lastChangeDate = event.changeDate)
    }
    case event: TransactionCancelEvent if accountState.closeDate == null => {
      accountState = accountState.copy(locked = accountState.locked - (event.transactionId), lastChangeDate = event.changeDate)
    }
  }

  override def receiveCommand: Receive = {
    case cmd: OpenAccountCommand =>
      if (accountState.isOpened())
        sender ! CommandFailed("Account already opened")
      else
        persist(OpenAccountEvent(DateTime.now)) { event =>
          updateState(event)
          sender ! CommandCompleted()
        }
    case cmd: CloseAccountCommand =>
      if (accountState.isClosed())
        sender ! CommandFailed("Account already closed")
      else
        persist(CloseAccountEvent(DateTime.now)) { event =>
          updateState(event)
          sender ! CommandCompleted()
        }
    case cmd: GetBalanceCommand =>
      sender ! GetBalanceCommandResponse(accountState.amount)
    case cmd: DepositCommand =>
      if (accountState.isClosed())
        sender ! CommandFailed("Account already closed")
      else if (cmd.amount <= 0)
        sender ! CommandFailed("Wrong amount")
      else
        persist(DepositEvent(DateTime.now, cmd.amount)) { event =>
          updateState(event)
          sender ! CommandCompleted()
        }
    case cmd: WithdrawCommand =>
      if (accountState.isClosed())
        sender ! CommandFailed("Account already closed")
      else if (cmd.amount > accountState.freeAmount() )
        sender ! CommandFailed("Wrong amount")
      else
        persist(WithdrawEvent(DateTime.now, cmd.amount)) { event =>
          updateState(event)
          sender ! CommandCompleted()
      }
    case cmd: TransactionLockCommand =>
      if (accountState.isClosed())
        sender ! CommandFailed("Account already closed")
      else if (cmd.amount > accountState.freeAmount() )
        sender ! CommandFailed("Wrong amount")
      else if (accountState.knownTransaction(cmd.transactionId))
        sender ! CommandFailed("Already locked")
      else
        persist(TransactionLockEvent(DateTime.now, cmd.amount, cmd.transactionId)) { event =>
          updateState(event)
          sender ! CommandCompleted()
        }
    case cmd: TransactionCompleteCommand =>
      if (accountState.isClosed())
        sender ! CommandFailed("Account already closed")
      else if (! accountState.knownTransaction(cmd.transactionId))
        sender ! CommandFailed("Unknown transaction")
      else
        persist(TransactionCompleteEvent(DateTime.now, cmd.transactionId)) { event =>
          updateState(event)
          sender ! CommandCompleted()
        }
    case cmd: TransactionCancelCommand =>
      if (accountState.isClosed())
        sender ! CommandFailed("Account already closed")
      else if (! accountState.knownTransaction(cmd.transactionId))
        sender ! CommandFailed("Unknown transaction")
      else
        persist(TransactionCancelEvent(DateTime.now, cmd.transactionId)) { event =>
          updateState(event)
          sender ! CommandCompleted()
        }
  }

  override def persistenceId: String = self.path.name
}
