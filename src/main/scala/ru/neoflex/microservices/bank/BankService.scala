package ru.neoflex.microservices.bank

import akka.NotUsed
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.persistence.query.PersistenceQuery
import akka.persistence.query.journal.leveldb.scaladsl.LeveldbReadJournal
import akka.stream.scaladsl.{Sink, Source}


object BankService {
  def props: Props = Props[BankService]

  case class OpenAccountCommand(accountName: String)
}

class BankService extends Actor with ActorLogging {

  import BankService._

  implicit def executionContext = context.dispatcher

  override def receive: Receive = {
    case cmd: OpenAccountCommand =>
      val readJournal = PersistenceQuery(context.system).readJournalFor[LeveldbReadJournal](
        LeveldbReadJournal.Identifier)
      val count = readJournal.persistenceIds().takeWhile(_ != cmd.accountName).to(Sink.last)


  }
}
