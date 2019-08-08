package flowly.core.scalacheck.generators

import flowly.core.{ErrorOr, IntKey}
import flowly.core.context.WritableExecutionContext
import flowly.core.scalacheck.model.{BooleanKey, TestingTask}
import flowly.core.tasks.basic.{ExecutionTask, Task}
import org.scalacheck.Gen

object ExecutionTaskGenerator extends TaskGenerator {

  def genTask(depth: Int): Gen[Task] = {

    println(s"Execution Task build start. Depth $depth")

    for {

      nextTask <- genNextTask(depth)

      performImplementation <- Gen.frequency(8 -> doNothing,
        1 -> setBooleanKey,
        1 -> setIntKey)

      task = new ExecutionTask with TestingTask {

        override protected def perform(sessionId: String, executionContext: WritableExecutionContext): ErrorOr[WritableExecutionContext] = performImplementation.apply(executionContext)

        override val next: Task = nextTask

      }

    } yield task
  }


  private def doNothing: Gen[WritableExecutionContext => ErrorOr[WritableExecutionContext]] = (executionContext: WritableExecutionContext) => Right(executionContext)


  private def setBooleanKey: Gen[WritableExecutionContext => ErrorOr[WritableExecutionContext]] = for {

    value <- Gen.oneOf(true, false)

  } yield (executionContext: WritableExecutionContext) => Right(executionContext.set(BooleanKey, value))


  private def setIntKey: Gen[WritableExecutionContext => ErrorOr[WritableExecutionContext]] = for {

    value <- Gen.choose(Int.MinValue, Int.MaxValue)

  } yield (executionContext: WritableExecutionContext) => Right(executionContext.set(IntKey, value))

}
