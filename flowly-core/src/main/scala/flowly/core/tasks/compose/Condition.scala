package flowly.core.tasks.compose

import flowly.core.context.{ExecutionContext, ReadableExecutionContext}
import flowly.core.tasks.basic.{ExecutionTask, Task}
import flowly.core.tasks.model.{OnError, SkipAndContinue, TaskResult}

trait Condition extends Task {
  this: ExecutionTask =>

  protected def condition(executionContext: ReadableExecutionContext):Boolean

  abstract override private[flowly] def execute(sessionId: String, executionContext: ExecutionContext): TaskResult = {
    try {
      if (condition(executionContext)) super.execute(sessionId, executionContext) else SkipAndContinue(next)
    } catch {
      case throwable: Throwable => OnError(throwable)
    }
  }

}