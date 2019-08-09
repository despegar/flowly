package flowly.core.scalacheck.generators

import flowly.core.tasks.basic.FinishTask
import org.scalacheck.Gen

object FinishTaskGenerator extends TaskGenerator {

  def genTask(depth: Int): Gen[FinishTask] = for {
    taskId <- getTaskId
    task = FinishTask(taskId)
  } yield task

}
