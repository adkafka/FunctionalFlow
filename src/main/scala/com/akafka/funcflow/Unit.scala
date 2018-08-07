package com.akafka.funcflow

import akka.NotUsed
import akka.stream.scaladsl.Flow

/**
  * Represents the final flow function in a chain
 */
object Unit {
  def apply[A, Z](func: A => Z) = {
    new Unit[A, Z] {
      override def function(in: A): Z = func(in)
    }
  }
}

trait Unit[A, Z] extends FlowFunction[A, Z] {
  type In = Either[Z, A]
  type Out = Z

  def function(in: A): Z

  def wrappedFunction(in: In): Z = {
    in match {
      case Left(shortCut) => shortCut
      case Right(expectedInput) => function(expectedInput)
    }
  }

  val flowStage: Flow[In, Z, NotUsed] = Flow[In].map(wrappedFunction)
}
