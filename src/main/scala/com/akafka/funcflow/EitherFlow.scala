package com.akafka.funcflow

import akka.NotUsed
import akka.stream.{FlowShape, Graph}
import akka.stream.scaladsl.{Flow, GraphDSL, Merge, Partition}

import scala.concurrent.{ExecutionContext, Future}

object EitherFlow {
  def apply[A, B, Z](func: A => Either[Z, B]) = {
    new EitherFlow[A, B, Z] {
      override def function(in: A): Out = func(in)
    }
  }
}

// TODO: Variance...
// TODO: Materialized values
// TODO: Other flow stages? mapAsync, mapConcat
// These should be flow stages themselves...
trait EitherFlow[A, B, Z] extends FlowFunction[A, Z] {
  type In = Either[Z, A]
  type Out = Either[Z, B]
  type Connector[C] = Either[Z, C]

  def function(in: A): Out

  def wrappedFunction(in: In): Out = {
    in match {
      case Left(shortCut) => Left(shortCut)
      case Right(expectedInput) => function(expectedInput)
    }
  }

  def flow: Flow[In, Out, NotUsed] = Flow[In].map(wrappedFunction)
  // This doesn't work because it renders the entire flow as one flow stage... instead of allowing for a synchronous
  // flow stage to be added to an asynchronous one.
  def flowAsync(parallelsim: Int)
    (implicit ex: ExecutionContext): Flow[In, Out, NotUsed] = Flow[In].mapAsync(parallelsim) {

    case Left(shortCut) => Future.successful(Left(shortCut))
    case Right(expectedInput) => Future(function(expectedInput))
  }

  def map[C](flowFunc: EitherFlow[B, C, Z]): EitherFlow[A, C, Z] = {
    EitherFlow[A, C, Z] { in: A =>
      this.function(in) match {
        case Left(shortCut) => Left(shortCut)
        case Right(expectedInput) => flowFunc.function(expectedInput)
      }
    }
  }

  def map(unit: Unit[B, Z]): Unit[A, Z] = {
    Unit[A, Z] { in: A =>
      this.function(in) match {
        case Left(shortCut) => shortCut
        case Right(expectedInput) => unit.function(expectedInput)
      }
    }
  }

  // TODO: All vias should be like this (same return type?). We should also add some implicit magic so I can Flow.via(Gooch...)
  def via[C](flow: Flow[B, Connector[C], _]): Graph[FlowShape[In, Connector[C]], NotUsed] = {
    GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      val in = b.add(this.flow)
      val partition = b.add(Partition[Connector[B]](2, x => if (x.isLeft) 0 else 1))
      val merge = b.add(Merge[Connector[C]](2))
      val extractShortCut = Flow[Connector[B]].map(x => Left(x.left.get))
      val happyPath = Flow[Connector[B]].map(x => x.right.get).via(flow)

      // TODO: Merging does not preserve order! We need a better solution
      in ~> partition.in
            partition.out(0) ~> extractShortCut ~> merge.in(0)
            partition.out(1) ~> happyPath ~> merge.in(1)

      FlowShape(in.in, merge.out)
    }
  }
  def viaUnit[C](flow: Flow[B, C, _]): Graph[FlowShape[In, Connector[C]], NotUsed] = via(flow.map(Right.apply))
}

