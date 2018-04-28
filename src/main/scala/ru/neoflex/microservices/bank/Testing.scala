package ru.neoflex.microservices.bank

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}

object Testing {
  implicit val system = akka.actor.ActorSystem.create("test")
  implicit lazy val materializer: ActorMaterializer = ActorMaterializer()

  def main(args: Array[String]): Unit = {
    val src = Source.fromIterator[String](() => {Seq("1", "3", "5", "7").iterator})
    val f = src.takeWhile(_ != "5", true).to(Sink.headOption.).run()
    System.out.println(f);
  }

}
