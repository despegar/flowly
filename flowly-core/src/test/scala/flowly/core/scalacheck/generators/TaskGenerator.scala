package flowly.core.scalacheck.generators

import flowly.core.tasks.basic.Task
import org.scalacheck.Gen

trait TaskGenerator {

  def genNextTask(depth: Int): Gen[Task] = {
    val nextDepth = depth + 1
    Gen.frequency(100 / depth -> ExecutionTaskGenerator.genTask(nextDepth),
      50 / depth -> BlockingTaskGenerator.genTask(nextDepth),
      30 / depth -> DisjunctionTaskGenerator.genTask(nextDepth),
      //10 / depth -> CircularSubflowGenerator.genTask(nextDepth),
      1 * depth -> FinishTaskGenerator.genTask())
  }

}
