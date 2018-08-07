package com.akafka.funcflow

import akka.NotUsed
import akka.stream.scaladsl.Flow

trait FlowFunction[A, Z] {
  type In
  type Out
  type FuncOut
  type Connector[C] = Either[Z, C]

  def function(in: A): FuncOut

  // TODO: Use materialized values
  def flow: Flow[In, Out, NotUsed]
}
