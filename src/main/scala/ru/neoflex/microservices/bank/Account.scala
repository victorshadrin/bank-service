package ru.neoflex.microservices.bank

import akka.persistence.PersistentActor

object Account {
  case class OpenAccountCommand(accountName: String)

  case class OpenAccountEvent(accountName: String)
}

class Account extends PersistentActor {
  import Account._


  override def receiveRecover: Receive = {
    case event: OpenAccountEvent =>

  }

  override def receiveCommand: Receive = {
    case cmd: OpenAccountCommand =>
      persist(OpenAccountEvent(cmd.accountName)) { event =>
        sender ! event
      }

  }

  override def persistenceId: String = self.path.name
}
