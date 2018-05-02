package ru.neoflex.microservices.bank

import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}

import scala.util.{Failure, Success}

object Testing {
  implicit val system = akka.actor.ActorSystem.create("test")
  implicit val executionContext = system.dispatcher
  implicit lazy val materializer: ActorMaterializer = ActorMaterializer()

  def main(args: Array[String]): Unit = {
    val src = Source.fromIterator[String](() => {Seq("1", "3", "5", "7").iterator})
    //val f = src.takeWhile(_ != "5", true).runWith(Sink.headOption).value
    src.takeWhile(_ != "5", true).filter(_ == "5").runWith(Sink.lastOption).onComplete{
      case Success(v) =>
        System.out.println(v);
      case Failure(e) =>
        System.out.println(e);
    }



  }

}
