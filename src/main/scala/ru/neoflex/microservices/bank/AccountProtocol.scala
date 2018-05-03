package ru.neoflex.microservices.bank

object AccountProtocol {
  case class OpenAccountCommand(accountName: String)

  case class AccountExists(accountName: String)

  case class CommandCompleted(accountName: String)
  case class CommandFailed(accountName: String, reason: String)

  case class AccountException(message: String)  extends Exception

  case class GetBalanceCommand(accountName: String)
  case class GetBalanceResponse(accounrName: String, balance: Double)

}
