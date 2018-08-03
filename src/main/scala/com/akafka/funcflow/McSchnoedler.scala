package com.akafka.funcflow

import akka.NotUsed
import akka.stream.scaladsl.Flow

object McSchnoedler {
  def apply[A, Z](func: A => Z) = {
    new McSchnoedler[A, Z] {
      override def function(in: A): Z = func(in)
    }
  }
}

trait McSchnoedler[A, Z] {
  type In[X] = Either[Z, X]

  def function(in: A): Z

  def wrappedFunction(in: In[A]): Z = {
    in match {
      case Left(shortCut) => shortCut
      case Right(expectedInput) => function(expectedInput)
    }
  }

  val flowStage: Flow[In[A], Z, NotUsed] = Flow[In[A]].map(wrappedFunction)
}
