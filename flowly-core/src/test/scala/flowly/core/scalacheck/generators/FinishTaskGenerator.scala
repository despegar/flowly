package flowly.core.scalacheck.generators

import flowly.core.scalacheck.model.{TestingTask, UUID}
import flowly.core.tasks.basic.FinishTask
import org.scalacheck.Gen

object FinishTaskGenerator extends TaskGenerator {

  def genTask(): Gen[FinishTask] = {
    new FinishTask(UUID()) with TestingTask
  }

}
