package ru.neoflex.microservices.bank

object AccountProtocol {
  case class OpenAccountCommand()
  case class CloseAccountCommand()

  case class CommandCompleted()
  case class CommandFailed(reason: String)

  case class GetBalanceCommand()
  case class GetBalanceCommandResponse(balance: Double)

  case class DepositCommand(amount: Double)
  case class WithdrawCommand(amount: Double)
  case class TransactionLockCommand(transactionId: Long, amount: Double)
  case class TransactionCompleteCommand(transactionId: Long)
  case class TransactionCancelCommand(transactionId: Long)

}
