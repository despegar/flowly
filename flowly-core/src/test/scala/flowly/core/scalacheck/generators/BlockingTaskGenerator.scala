package flowly.core.scalacheck.generators

import flowly.core.{BooleanKey, IntKey}
import flowly.core.context.{Key, ReadableExecutionContext}
import flowly.core.tasks.basic.{BlockingTask, Task}
import org.scalacheck.Gen

object BlockingTaskGenerator extends TaskGenerator {

  def genTask(depth: Int): Gen[Task] = {

    for {

      nextTask <- genNextTask(depth)

      blockingCondition <- Gen.oneOf(booleanBlockingCondition, intBlockingCondition)

      taskId <- getTaskId

      blockingTask = new BlockingTask {

        override val id: String = taskId

        override protected def condition(executionContext: ReadableExecutionContext): Boolean = blockingCondition(executionContext)

        override val next: Task = nextTask

        override def allowedKeys: List[Key[_]] = List(IntKey, BooleanKey)
      }

    } yield blockingTask

  }

  private val booleanBlockingCondition: Gen[ReadableExecutionContext => Boolean] = {
    c: ReadableExecutionContext => c.get(BooleanKey).exists(v => v)
  }

  private val intBlockingCondition: Gen[ReadableExecutionContext => Boolean] = {
    c: ReadableExecutionContext => c.get(IntKey).exists(v => v >= 0)
  }

}