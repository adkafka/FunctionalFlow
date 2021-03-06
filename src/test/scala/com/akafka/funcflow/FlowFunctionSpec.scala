package com.akafka.funcflow

import org.scalatest.{FreeSpec, Matchers}

class FlowFunctionSpec extends FreeSpec with Matchers {
  "FlowFunction" - {
    "should be able to chain simple functions together" in {
      val dropLessThan5: Int => Either[String, Int] = x => if (x < 5) Left("less than 5") else Right(x)
      val dropLessThan10: Int => Either[String, Int] = x => if (x < 10) Left("less than 10") else Right(x)

      val g =
        EitherFlow(dropLessThan5)
          .andThen(EitherFlow(dropLessThan10))

      g.function(1) shouldBe Left("less than 5")
      g.function(6) shouldBe Left("less than 10")
      g.function(11) shouldBe Right(11)
    }

    "should be able to chain simple functions together ending in an unwrapped object" in {
      val dropLessThan5: Int => Either[String, Int] = x => if (x < 5) Left("less than 5") else Right(x)
      val dropLessThan10: Int => Either[String, Int] = x => if (x < 10) Left("less than 10") else Right(x)
      val finalStage: Int => String = x => s"Input $x made it to the end!"

      val g =
        EitherFlow(dropLessThan5)
          .andThen(EitherFlow(dropLessThan10))
          .andThen(Unit(finalStage))

      g.function(1) shouldBe "less than 5"
      g.function(6) shouldBe "less than 10"
      g.function(11) shouldBe "Input 11 made it to the end!"
    }
  }
}
