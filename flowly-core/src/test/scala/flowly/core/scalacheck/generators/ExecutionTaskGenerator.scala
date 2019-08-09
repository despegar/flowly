package flowly.core.scalacheck.generators

import flowly.core.context.WritableExecutionContext
import flowly.core.tasks.basic.{ExecutionTask, Task}
import flowly.core.{BooleanKey, ErrorOr, IntKey}
import org.scalacheck.{Arbitrary, Gen}

object ExecutionTaskGenerator extends TaskGenerator {

  def genTask(depth: Int): Gen[Task] = {

    for {

      nextTask <- genNextTask(depth)

      performImplementation <- Gen.frequency(8 -> doNothing,
                                                  1 -> setBooleanKey,
                                                  1 -> setIntKey)

      taskId <- getTaskId

      task = new ExecutionTask {

        override val id: String = taskId

        override protected def perform(sessionId: String, executionContext: WritableExecutionContext): ErrorOr[WritableExecutionContext] = performImplementation.apply(executionContext)

        override val next: Task = nextTask

      }

    } yield task

  }


  private def doNothing: Gen[WritableExecutionContext => ErrorOr[WritableExecutionContext]] = (executionContext: WritableExecutionContext) => Right(executionContext)


  private def setBooleanKey: Gen[WritableExecutionContext => ErrorOr[WritableExecutionContext]] = for {

    value <- Arbitrary.arbitrary[Boolean]

  } yield (executionContext: WritableExecutionContext) => Right(executionContext.set(BooleanKey, value))


  private def setIntKey: Gen[WritableExecutionContext => ErrorOr[WritableExecutionContext]] = for {

    value <- Arbitrary.arbitrary[Int]

  } yield (executionContext: WritableExecutionContext) => Right(executionContext.set(IntKey, value))

}
