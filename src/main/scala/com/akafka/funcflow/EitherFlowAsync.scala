package com.akafka.funcflow

import akka.NotUsed
import akka.stream.scaladsl.Flow

import scala.concurrent.{ExecutionContext, Future}

object EitherFlowAsync {
  def wrapAsync[A, Z, B](parallelism: Int)
    (function: A => Either[Z, B])
    (implicit ec: ExecutionContext): EitherFlowAsync[A, Z, B] = {

    EitherFlowAsync(parallelism)(x => Future(function(x)))
  }
}

final case class EitherFlowAsync[A, Z, B](parallelism: Int)
  (f: A => Future[Either[Z, B]])
  (implicit ec: ExecutionContext) extends FlowFunction[A, Z] {

  override type In = Either[Z, A]
  override type Out = Either[Z, B]
  override type FuncOut = Future[Either[Z, B]]

  override def function(in: A): FuncOut = f(in)

  override def flow: Flow[In, Out, NotUsed] = Flow[In].mapAsync(parallelism) {
    case Left(shortCut) => Future.successful(Left(shortCut))
    case Right(expectedInput) => f(expectedInput)
  }
}
