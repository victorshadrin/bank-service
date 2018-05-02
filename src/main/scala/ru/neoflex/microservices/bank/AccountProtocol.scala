package ru.neoflex.microservices.bank

object AccountProtocol {
  case class OpenAccountCommand(accountName: String)

  case class OpenAccountSuccess(accountName: String)
  case class OpenAccountFailed(accountName: String, reason: String)

  case class GetBalanceCommand(accountName: String)
  case class GetBalanceResponse(accounrName: String, balance: Double)

}
