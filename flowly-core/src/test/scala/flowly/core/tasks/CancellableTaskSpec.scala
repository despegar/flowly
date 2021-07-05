package flowly.core.tasks

import flowly.core.context.ExecutionContext
import flowly.core.tasks.basic.FinishTask
import flowly.core.tasks.compose.CancelFlow
import flowly.core.tasks.model.{Block, Cancel}
import flowly.core.{BooleanKey, TasksContext, tasks}
import org.specs2.mutable.Specification

class CancellableTaskSpec extends Specification {

  val blockingCancellable = tasks.BlockingCancellableTask("BlockingCancellableTask", FinishTask("OK"), ec => Right(ec.contains(BooleanKey)), List.empty)

  "Cancellable task" should {
    "return Cancel task status after execution" in new TasksContext {
      blockingCancellable.execute("sessionId1", ec) must_== Block
      private val updatedEc: ExecutionContext = ec.set(CancelFlow, true)
      blockingCancellable.execute("sessionId1", updatedEc) must_== Cancel
    }
  }

}
