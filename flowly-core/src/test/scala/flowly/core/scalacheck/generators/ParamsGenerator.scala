package flowly.core.scalacheck.generators

import flowly.core.scalacheck.model.BooleanKey
import flowly.core.{IntKey, Param}
import org.scalacheck.Gen

object ParamsGenerator {

  def getParams: Gen[List[Param]] = for {

    param1 <- intParam()

    param2 <- booleanParam()

    params <- Gen.someOf(List(param1, param2))

  } yield params.toList

  private def intParam(): Gen[Param] = for {

    value <- Gen.choose(Int.MinValue, Int.MaxValue)

  } yield Param(IntKey, value)

  private def booleanParam(): Gen[Param] = for {

    value <- Gen.oneOf(true, false)

  } yield Param(BooleanKey, value)

}
