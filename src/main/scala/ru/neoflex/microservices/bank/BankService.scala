package ru.neoflex.microservices.bank

import akka.NotUsed
import akka.actor.{Actor, ActorLogging, ActorNotFound, ActorRef, ActorSystem, Props}
import akka.persistence.query.PersistenceQuery
import akka.persistence.query.journal.leveldb.scaladsl.LeveldbReadJournal
import akka.stream.scaladsl.{Sink, Source}
import akka.pattern.ask
import akka.cluster.sharding.ClusterSharding
import akka.stream.ActorMaterializer
import akka.util.Timeout
import ru.neoflex.microservices.bank.AccountProtocol.{GetBalanceCommand, GetBalanceCommandResponse, OpenAccountCommand}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}


object BankService {
  def props: Props = Props[BankService]

  case class OpenAccount(accountName: String)
  case class AccountExists(accountName: String)
  case class AccountOpened(accountName: String)

  case class GetBalance(accountName: String)
  case class GetBalanceResponse(value: Double)

  case class AccountNotFound(accountName: String)
  case class BankServiceException(message: String)

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
        if (id.isDefined)
        ref <- context.system.actorSelection("/user/" + id.get).resolveOne().recover[ActorRef] {
            case e: ActorNotFound => {
              context.system.actorOf(AccountAggregator.props, id.get)
            }
          }
      } yield {
        if (id.isEmpty) {
          None
        } else {
          Some(ref)
        }
      }
      actorRef.recover{
        case e : Exception => {
          None
        }
      }
  }

  override def receive: Receive = {
    case cmd: OpenAccount =>
      val origSender = sender()
        findAccount(cmd.accountName).onComplete{
          case Success(opt : Option[ActorRef]) if opt.isDefined =>
            origSender ! AccountExists
          case default => {
            val account = context.system.actorOf(AccountAggregator.props, cmd.accountName)
            (account ? OpenAccountCommand()).onComplete( message => {
              origSender ! AccountOpened(cmd.accountName)
            })
          }
        }
    case GetBalance(accName: String) => {
      val _sender = sender()
      findAccount(accName).onComplete{
        case Failure(exception) =>
          _sender ! BankServiceException(exception.getMessage)
        case Success(None) =>
          _sender ! AccountNotFound(accName)
        case Success(opt : Option[ActorRef])  =>
          (opt.get ? GetBalanceCommand()).onComplete({
            case Success(response: GetBalanceCommandResponse) =>
              _sender ! GetBalanceResponse(response.balance)
            case Failure(exception) =>
              _sender ! BankServiceException(exception.getMessage)
          })

      }
    }
  }
}
