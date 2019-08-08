package flowly.core.scalacheck.generators

import flowly.core.tasks.basic.Task
import org.scalacheck.Gen

trait TaskGenerator {

  def genNextTask(previousDepth: Int): Gen[Task] = {
    val depth = previousDepth + 1
    Gen.frequency((1 * depth, FinishTaskGenerator.genTask(depth)),
                  (50 / depth, Gen.lzy(BlockingTaskGenerator.genTask(depth))),
                  (100 / depth, Gen.lzy(ExecutionTaskGenerator.genTask(depth))),
                  (30 / depth, Gen.lzy(DisjunctionTaskGenerator.genTask(depth)))
                  //10 / depth, Gen.lzy(CircularSubflowGenerator.genTask(depth)))
                 )
  }

}
