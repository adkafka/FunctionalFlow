package com.akafka.funcflow

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akka.testkit.TestKit
import org.scalatest.{AsyncFreeSpecLike, Matchers}

// TODO: Make two examples. 1) Kafka element processing. 2) Http response rendering.
class FlowFunctionAsyncSpec extends TestKit(ActorSystem("FlowFunctionAsyncSpec")) with AsyncFreeSpecLike with Matchers {
  implicit val mat = ActorMaterializer()
  implicit val ec = system.dispatcher

  "FlowFunction Async" - {
    "yield a valid flow" in {
      val dropLessThan5: Int => Either[String, Int] = x => if (x < 5) Left("less than 5") else Right(x)
      val dropLessThan10: Int => Either[String, Int] = x => if (x < 10) Left("less than 10") else Right(x)
      val finalStage: Int => String = x => s"Input $x made it to the end!"

      val flow =
        EitherFlow(dropLessThan5).flow
          .via(EitherFlowAsync.wrapAsync(5)(dropLessThan10).flow)
          .via(Unit(finalStage).flow)

      Source(List(1, 6, 11))
        .map(Right.apply) // TODO: Get rid of this line
        .via(flow)
        .runWith(Sink.seq)
        .map { result =>
          result shouldBe List("less than 5", "less than 10", "Input 11 made it to the end!")
        }
    }
  }
}
