package com.akafka.funcflow

trait FlowFunction[A, Z] {
  type In
  type Out
}
