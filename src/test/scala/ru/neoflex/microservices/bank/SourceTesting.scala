package ru.neoflex.microservices.bank

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}

import scala.util.{Failure, Success}

object SourceTesting {

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("api")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    val src = Source.fromIterator(() => Seq[String]("1", "33", "44", "2").iterator)
    src.dropWhile(_ != "441").take(1).runWith(Sink.lastOption).onComplete{
      case Success(v) =>
        println(v)
      case Failure(e) =>
        println(e)
    }
    system.terminate()
  }

}
