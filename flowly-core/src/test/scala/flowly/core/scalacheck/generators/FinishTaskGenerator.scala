package flowly.core.scalacheck.generators

import flowly.core.scalacheck.model.{TestingTask, UUID}
import flowly.core.tasks.basic.FinishTask
import org.scalacheck.Gen

object FinishTaskGenerator extends TaskGenerator {

  def genTask(depth: Int): Gen[FinishTask] = {
    println(s"Finish Task build start. Depth $depth")
    new FinishTask(UUID()) with TestingTask
  }

}
