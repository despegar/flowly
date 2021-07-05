package flowly.core.tasks.compose

import flowly.core.context.{ExecutionContext, Key}
import flowly.core.tasks.basic.Task
import flowly.core.tasks.model.{Cancel, Finish, TaskResult}

trait Cancellable extends Task {

  abstract override private[flowly] def execute(sessionId: String, executionContext: ExecutionContext): TaskResult = {
    if(executionContext.get(CancelFlow).exists(identity)) Cancel
    else super.execute(sessionId, executionContext)
  }

  override def internalAllowedKeys: List[Key[_]] = List(CancelFlow) ++ super.internalAllowedKeys

}

case object CancelFlow extends Key[Boolean]

