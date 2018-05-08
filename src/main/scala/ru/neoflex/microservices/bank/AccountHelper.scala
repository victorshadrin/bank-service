package ru.neoflex.microservices.bank

import akka.actor.{ActorNotFound, ActorRef, ActorSystem}
import akka.persistence.query.journal.leveldb.scaladsl.LeveldbReadJournal
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import akka.util.Timeout
import scala.concurrent.{ExecutionContext, Future}

trait AccountHelper {
  def getAccount(accountNumber: String)(implicit system: ActorSystem, readJournal: LeveldbReadJournal,
                                        materializer: ActorMaterializer, executionContext: ExecutionContext,
                                        timeout: Timeout): Future[ActorRef] = {
    val ref = for {
      id <- readJournal.currentPersistenceIds().dropWhile(_ != "Account-" + accountNumber).take(1).runWith(Sink.headOption)
      if (id.isDefined)
      ref <- system.actorSelection("/user/" + id.get).resolveOne().recover[ActorRef] {
        case e: ActorNotFound => {
          system.actorOf(AccountAggregator.props, accountNumber)
        }
      }
    } yield {
      ref
    }
    ref
  }
}

