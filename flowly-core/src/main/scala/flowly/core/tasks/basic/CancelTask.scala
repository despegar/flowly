package flowly.core.tasks.basic
import flowly.core.context.{ExecutionContext, Key}
import flowly.core.tasks.model.{Cancel, TaskResult}

case object CancelTask extends Task {
  override private[flowly] def execute(sessionId: String, executionContext: ExecutionContext): TaskResult = Cancel

  override private[flowly] def followedBy: List[Task] = Nil

  override protected def customAllowedKeys: List[Key[_]] = Nil

  override def name: String = "CANCELLED"
}
