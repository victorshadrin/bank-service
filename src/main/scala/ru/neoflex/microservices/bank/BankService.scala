package ru.neoflex.microservices.bank

import akka.NotUsed
import akka.actor.{Actor, ActorLogging, ActorNotFound, ActorRef, ActorSystem, Props}
import akka.persistence.query.PersistenceQuery
import akka.persistence.query.journal.leveldb.scaladsl.LeveldbReadJournal
import akka.stream.scaladsl.{Sink, Source}
import akka.pattern.ask
import AccountProtocol.{GetBalanceResponse, _}
import akka.cluster.sharding.ClusterSharding
import akka.stream.ActorMaterializer
import akka.util.Timeout

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}


object BankService {
  def props: Props = Props[BankService]

  case class OpenAccount(accountName: String)
  case class AccountExists(accountName: String)
  case class AccountOpened(accountName: String)

  case class GetBalance(accountName: String)
  case class AccountNotFound(accountName: String)

}

class BankService extends Actor with ActorLogging {

  import BankService._

  implicit def executionContext = context.dispatcher
  implicit def materializer: ActorMaterializer = ActorMaterializer()
  implicit def timeout: Timeout = Timeout(5.seconds)

  lazy val readJournal = PersistenceQuery(context.system).readJournalFor[LeveldbReadJournal](
    LeveldbReadJournal.Identifier)

  def findAccount(accountNumber: String): Future[Option[ActorRef]] = {
      val actorRef = for {
        id <- readJournal.currentPersistenceIds().dropWhile(_ != accountNumber).take(1).runWith(Sink.headOption)
        ref <- context.system.actorSelection("/user/" + id).resolveOne().recover {
            case e: ActorNotFound => {
              if (id.isDefined) {
                context.system.actorOf(AccountAggregator.props, id.get)
              } else {
                null
              }
            }
          }
      } yield {
        if (id.isEmpty) {
          None
        } else {
          Some(ref)
        }
      }
      actorRef
  }

  override def receive: Receive = {
    case cmd: OpenAccount =>
      val origSender = sender()
        readJournal.currentPersistenceIds().dropWhile(_ != cmd.accountName).take(1).runWith(Sink.headOption)
        .onComplete{
          case Success(opt) =>
            if (opt.isEmpty) {
              val account = context.system.actorOf(AccountAggregator.props, cmd.accountName)
              log.info("path: " + account.path)
              (account ? OpenAccountCommand(cmd.accountName)).onComplete( message => {
                origSender ! AccountOpened(cmd.accountName)
              })
            } else {
              opt.foreach(log.info)
              origSender ! AccountExists
            }
          case Failure(e) => {
            log.info("Exception")
            origSender ! Failure(e)
          }
        }
    case GetBalance(accName: String) => {
      log.info("GetBalance: " + accName)
      val _sender = sender()
      findAccount(accName).onComplete{
        case Success(opt : Option[ActorRef]) if opt.isDefined =>
          (opt.get ? GetBalanceCommand()).onComplete({
            case Success(response: GetBalanceResponse) =>
              _sender ! response.balance
          })
        case Failure(e) =>
          log.info("Exception: " + e.getMessage)
          _sender ! e.getMessage
        case default => {
          log.info("Actor not found: " + default)
          _sender ! "Actor not found"
        }

      }
    }
  }
}
