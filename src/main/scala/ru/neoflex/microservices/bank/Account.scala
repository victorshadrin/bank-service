package ru.neoflex.microservices.bank

import akka.actor.Props
import akka.persistence.PersistentActor

object Account {

  def props: Props = Props[Account]

  case class OpenAccountEvent(accountName: String)

}

class Account extends PersistentActor {
  import Account._
  import AccountProtocol._


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
