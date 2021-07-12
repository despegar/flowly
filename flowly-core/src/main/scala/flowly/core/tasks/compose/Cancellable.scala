package flowly.core.tasks.compose

import flowly.core.context.{ExecutionContext, Key}
import flowly.core.tasks.basic.{CancelTask, Task}
import flowly.core.tasks.model.TaskResult

trait Cancellable extends Task {

  abstract override private[flowly] def execute(sessionId: String, executionContext: ExecutionContext): TaskResult = {
    if(executionContext.get(CancelFlow).exists(identity)) followedByAfterCancel.execute(sessionId, executionContext)
    else super.execute(sessionId, executionContext)
  }

  override def internalAllowedKeys: List[Key[_]] = List(CancelFlow) ++ super.internalAllowedKeys

  //Override to add chained tasks to do stuff before cancelling. Then define CancelTask as next task there.
  protected def followedByAfterCancel: Task = CancelTask
}

case object CancelFlow extends Key[Boolean]

