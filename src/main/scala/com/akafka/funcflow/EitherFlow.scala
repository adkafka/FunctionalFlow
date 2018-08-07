package com.akafka.funcflow

import akka.NotUsed
import akka.stream.scaladsl.{Flow, GraphDSL, Merge, Partition}
import akka.stream.{FlowShape, Graph}

object EitherFlow {
  def apply[A, Z, B](func: A => Either[Z, B]) = {
    new EitherFlow[A, Z, B] {
      override def function(in: A): Out = func(in)
    }
  }
}

// TODO: Variance...
// TODO: Materialized values
// TODO: Other flow stages? mapAsync, mapConcat
// These should be flow stages themselves...
trait EitherFlow[A, Z, B] extends FlowFunction[A, Z] {
  type In = Either[Z, A]
  type Out = Either[Z, B]
  type FuncOut = Out

  def wrappedFunction(in: In): Out = {
    in match {
      case Left(shortCut) => Left(shortCut)
      case Right(expectedInput) => function(expectedInput)
    }
  }

  override def flow: Flow[In, Out, NotUsed] = Flow[In].map(wrappedFunction)

  def map[C](flowFunc: EitherFlow[B, Z, C]): EitherFlow[A, Z, C] = {
    EitherFlow[A, Z, C] { in: A =>
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

  // Note: Ordering IS NOT preserved here! If you need ordering, you must write your own FlowFunction that wraps
  // the behavior you desire. However, for synchronous, ordered flow stages, this will not matter, and this function
  // is safe to use. (TODO: Confirm this)
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

