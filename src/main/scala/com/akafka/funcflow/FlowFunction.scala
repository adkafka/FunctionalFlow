package com.akafka.funcflow

import akka.NotUsed
import akka.stream.scaladsl.Flow

// TODO: Variance...
// TODO: Materialized values
// TODO: Other flow stages? mapAsync, mapConcat
trait FlowFunction[A, Z] {
  type In
  type Out
  type FuncOut
  type Connector[C] = Either[Z, C]

  def function(in: A): FuncOut

  // TODO: Make this implicit? So we don't need to call .flow
  def flow: Flow[In, Out, NotUsed]
}
