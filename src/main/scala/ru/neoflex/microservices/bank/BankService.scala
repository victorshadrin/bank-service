package ru.neoflex.microservices.bank

import akka.NotUsed
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.persistence.query.PersistenceQuery
import akka.persistence.query.journal.leveldb.scaladsl.LeveldbReadJournal
import akka.stream.scaladsl.{Sink, Source}
import akka.pattern.ask
import AccountProtocol._
import akka.stream.ActorMaterializer
import akka.util.Timeout
import scala.concurrent.duration._
import scala.util.{Failure, Success}


object BankService {
  def props: Props = Props[BankService]

  case class OpenAccountCommand(accountName: String)
  case class AccountBalanceCommand(accountName: String)

}

class BankService extends Actor with ActorLogging {

  import BankService._

  implicit def executionContext = context.dispatcher
  implicit def materializer: ActorMaterializer = ActorMaterializer()
  implicit def timeout: Timeout = Timeout(5.seconds)

  override def receive: Receive = {
    case cmd: OpenAccountCommand =>
      val readJournal = PersistenceQuery(context.system).readJournalFor[LeveldbReadJournal](
        LeveldbReadJournal.Identifier)
      readJournal.persistenceIds()
        .dropWhile(_ != cmd.accountName).take(1).runWith(Sink.lastOption)
        .onComplete{
          case Success(opt) =>
            if (opt.isEmpty) {
              val account = context.system.actorOf(Account.props, cmd.accountName)
              (account ? OpenAccountCommand(cmd.accountName)).onComplete( message =>
                sender() ! message
              )
            } else {
              sender() ! OpenAccountFailed(cmd.accountName, "Already exists")
            }
          case Failure(e) =>
            sender() ! OpenAccountFailed(cmd.accountName, e.getMessage)
        }


  }
}
