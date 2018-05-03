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

  case class OpenAccount(accountName: String)
  case class AccountExists(accountName: String)
  case class AccountOpened(accountName: String)

  case class GetBalance(accountName: String)
  case class AccountNotFound(accountName: String)
  case class GetBalanceResponse(accounrName: String, balance: Double)

}

class BankService extends Actor with ActorLogging {

  import BankService._

  implicit def executionContext = context.dispatcher
  implicit def materializer: ActorMaterializer = ActorMaterializer()
  implicit def timeout: Timeout = Timeout(5.seconds)

  override def receive: Receive = {
    case cmd: OpenAccount =>
      val origSender = sender()
      val readJournal = PersistenceQuery(context.system).readJournalFor[LeveldbReadJournal](
        LeveldbReadJournal.Identifier)
        readJournal.currentPersistenceIds().dropWhile(_ != cmd.accountName).take(1).runWith(Sink.headOption)
        .onComplete{
          case Success(opt) =>
            if (opt.isEmpty) {
              val account = context.system.actorOf(AccountAggregator.props, cmd.accountName)
              (account ? OpenAccountCommand(cmd.accountName)).onComplete( message => {
                origSender ! AccountOpened(cmd.accountName)
              })
            } else {
              origSender ! AccountExists
            }
          case Failure(e) => {
            log.info("Exception")
            origSender ! Failure(e)
          }
        }
    case GetBalance(accName: String) => {
      context.system.actorSelection("")
        }
  }
}
