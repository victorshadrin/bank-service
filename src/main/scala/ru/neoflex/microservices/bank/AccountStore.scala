package ru.neoflex.microservices.bank

import java.sql.Timestamp

import akka.actor.{Actor, ActorLogging}
import akka.http.scaladsl.model.DateTime
import slick.jdbc.H2Profile.api._
import slick.lifted.ProvenShape

import scala.concurrent.{ExecutionContext, Future}

object AccountStore  {
  case class Account(id: String, changed: Timestamp,closed: Timestamp, opened: Timestamp, amount: Double)
}

trait AccountStore  {
  import AccountStore._

  implicit def db: Database
  implicit def executionContext : ExecutionContext

  class AccountRow(tag: Tag) extends Table[(String, Timestamp,Timestamp,Timestamp, Double)](tag, "ACCOUNTS") {
    def id: Rep[String] = column[String]("ACCOUNT_NUMBER", O.PrimaryKey) // This is the primary key column
    def changed: Rep[Timestamp] = column[Timestamp]("CHANGED")
    def closed: Rep[Timestamp] = column[Timestamp]("CLOSED")
    def opened: Rep[Timestamp] = column[Timestamp]("OPENED")
    def amount: Rep[Double] = column[Double]("AMOUNT")

    def * : ProvenShape[(String, Timestamp,Timestamp,Timestamp, Double)] =  (id, changed,closed, opened, amount)
  }
  val accounts = TableQuery[AccountRow]

  def insert(id: String, changed: Timestamp, closed: Timestamp, opened: Timestamp, amount: Double): Unit = {
    accounts.insertOrUpdate(id, changed, closed, opened, amount)
  }

  def updateAccount(account: Account): Unit = {
    accounts.update(account.id, account.changed, account.closed, account.opened, account.amount)
  }

  def getAccount(id: String) : Future[Account] = {
    db.run(accounts.filter(_.id === id).result.head
      .map[Account]{case (id: String, changed: Timestamp, closed: Timestamp, opened: Timestamp, amount: Double) => Account(id, changed, closed, opened, amount)})
  }

  def getAmount(id: String) : Future[Option[Double]] = {
    db.run(accounts.filter(_.id === id).map(_.amount).result.headOption)
  }

  def getAccounts(): Future[Seq[String]] = {
    db.run(accounts.map(_.id).result)
  }

  def createRepository(): Unit = {
    db.run(DBIO.seq(accounts.schema.create))
  }

}
