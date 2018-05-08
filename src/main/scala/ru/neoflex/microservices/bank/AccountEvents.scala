package ru.neoflex.microservices.bank

import akka.http.scaladsl.model.DateTime

object AccountEvents {
  abstract class AccountEvent()
  case class OpenAccountEvent(accountNumber: String, openDate: DateTime) extends AccountEvent
  case class CloseAccountEvent(accountNumber: String, closeDate: DateTime) extends AccountEvent
  case class DepositEvent(accountNumber: String, changeDate: DateTime, amount: Double) extends AccountEvent
  case class WithdrawEvent(accountNumber: String, changeDate: DateTime, amount: Double) extends AccountEvent
  case class TransactionLockEvent(accountNumber: String, changeDate: DateTime, amount: Double, transactionId: Long) extends AccountEvent
  case class TransactionCompleteEvent(accountNumber: String, changeDate: DateTime, transactionId: Long) extends AccountEvent
  case class TransactionCancelEvent(accountNumber: String, changeDate: DateTime, transactionId: Long) extends AccountEvent
}
