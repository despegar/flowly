package flowly.core.scalacheck.generators

import flowly.core.tasks.basic.Task
import org.scalacheck.Gen

trait TaskGenerator {

  def genNextTask(previousDepth: Int): Gen[Task] = {
    val depth = previousDepth + 1
    Gen.frequency((1 * depth, FinishTaskGenerator.genTask(depth)),
                  (50 / depth, Gen.delay(BlockingTaskGenerator.genTask(depth))),
                  (100 / depth, Gen.delay(ExecutionTaskGenerator.genTask(depth))),
                  (30 / depth, Gen.delay(DisjunctionTaskGenerator.genTask(depth)))
                  //10 / depth, Gen.lzy(CircularSubflowGenerator.genTask(depth)))
                 )
  }

  def getTaskId: Gen[String] = Gen.uuid.flatMap(_.toString)

}
