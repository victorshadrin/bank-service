package ru.neoflex.microservices.bank

import akka.actor.{ActorLogging, Props}
import akka.persistence.PersistentActor
import akka.pattern.ask

object AccountAggregator {

  def props: Props = Props[AccountAggregator]

  case class OpenAccountEvent(accountName: String)

}

class AccountAggregator extends PersistentActor with ActorLogging {
  import AccountAggregator._
  import AccountProtocol._


  override def receiveRecover: Receive = {
    case event: OpenAccountEvent =>

  }

  override def receiveCommand: Receive = {
    case cmd: OpenAccountCommand =>
      log.info("Processing OpenAccountCommand")
      persist(OpenAccountEvent(cmd.accountName)) { event =>
        log.info("After persist")
        sender ! "Command sent"
      }

  }

  override def persistenceId: String = self.path.name
}
