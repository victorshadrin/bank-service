package ru.neoflex.microservices.bank


import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ExceptionHandler, Route, StandardRoute}
import spray.json.DefaultJsonProtocol

import scala.concurrent.{ExecutionContext, Future}
import slick.jdbc.H2Profile.api._
import slick.lifted.ProvenShape

object QueryRoutes {

}

trait QueryRoutes extends SprayJsonSupport with DefaultJsonProtocol with AccountStore {

  import StatusCodes._

  def queryRoute: Route = {
      getBalanceRequest
  }

  def getBalanceRequest: Route = {
    get {
      pathPrefix("getBalance" / Segment) { account: String =>
        pathEndOrSingleSlash {
          onSuccess(getAmount(account)) {
            case Some(amount) =>
              complete(OK, "" + amount)
            case None =>
              complete(InternalServerError, "Account not found")
          }
        }
      }
    }
  }
}