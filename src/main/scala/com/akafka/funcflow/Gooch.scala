package com.akafka.funcflow

import akka.NotUsed
import akka.stream.scaladsl.Flow

object Gooch {
  def apply[A, B, Z](func: A => Either[Z, B]) = {
    new Gooch[A, B, Z] {
      override def function(in: A): Out[B] = func(in)
    }
  }
}

// TODO: Variance...
// TODO: Materialized values
// TODO: Other flow stages? mapAsync, mapConcat
trait Gooch[A, B, Z] {
  type In[X] = Either[Z, X]
  type Out[X] = Either[Z, X]

  def function(in: A): Out[B]

  def wrappedFunction(in: In[A]): Out[B] = {
    in match {
      case Left(shortCut) => Left(shortCut)
      case Right(expectedInput) => function(expectedInput)
    }
  }

  val flowStage: Flow[In[A], Out[B], NotUsed] = Flow[In[A]].map(wrappedFunction)

  def via[C](gooch: Gooch[B, C, Z]): Gooch[A, C, Z] = {
    Gooch[A, C, Z] { in: A =>
      this.function(in) match {
        case Left(shortCut) => Left(shortCut)
        case Right(expectedInput) => gooch.function(expectedInput)
      }
    }
  }

  def via(mcSchnoedler: McSchnoedler[B, Z]): McSchnoedler[A, Z] = {
    McSchnoedler[A, Z] { in: A =>
      this.function(in) match {
        case Left(shortCut) => shortCut
        case Right(expectedInput) => mcSchnoedler.function(expectedInput)
      }
    }
  }
}

