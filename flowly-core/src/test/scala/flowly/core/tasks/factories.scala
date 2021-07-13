package flowly.core.tasks

import flowly.core.ErrorOr
import flowly.core.context.{Key, ReadableExecutionContext, WritableExecutionContext}
import flowly.core.tasks.basic.Task
import flowly.core.tasks.compose.{Cancellable, Retry}
import flowly.core.tasks.strategies.scheduling.{ConstantSchedulingStrategy, SchedulingStrategy}
import flowly.core.tasks.strategies.stopping.{QuantityStoppingStrategy, StoppingStrategy}


object BlockingTask {

  def apply(_name: String, _next: Task, _condition: ReadableExecutionContext => ErrorOr[Boolean], _allowedKeys: List[Key[_]]): basic.BlockingTask = new basic.BlockingTask {

    override def name: String = _name

    val next: Task = _next

    def condition(variables: ReadableExecutionContext): ErrorOr[Boolean] = _condition(variables)

    override protected def customAllowedKeys = Nil

  }

}

object DisjunctionTask {

  def apply(_name: String, _branches: (ReadableExecutionContext => ErrorOr[Boolean], Task)*): basic.DisjunctionTask = new basic.DisjunctionTask {

    override def name: String = _name

    override protected def customAllowedKeys = Nil

    override protected def blockOnNoCondition = false

    def branches: List[(ReadableExecutionContext => ErrorOr[Boolean], Task)] = _branches.toList
  }

  def apply(_id: String, ifTrue: Task, ifFalse: Task, condition: ReadableExecutionContext => ErrorOr[Boolean]): basic.DisjunctionTask = {
    apply(_id, (condition, ifTrue), (_ => Right(true), ifFalse))
  }

}

object BlockingDisjunctionTask {
  def apply(_name: String, _allowedKeys: List[Key[_]], _branches: (ReadableExecutionContext => ErrorOr[Boolean], Task)*): basic.DisjunctionTask = new basic.DisjunctionTask {

    override def name: String = _name

    override protected def customAllowedKeys = Nil

    def branches: List[(ReadableExecutionContext => ErrorOr[Boolean], Task)] = _branches.toList

    override protected def blockOnNoCondition = true

  }
}

object ExecutionTask {

  def apply(_name: String, _next: Task)(_perform: (String, WritableExecutionContext) => ErrorOr[WritableExecutionContext]): basic.ExecutionTask = new basic.ExecutionTask {

    override def name: String = _name

    val next: Task = _next

    def perform(sessionId: String, executionContext: WritableExecutionContext): ErrorOr[WritableExecutionContext] = _perform(sessionId, executionContext)

  }

}

object BlockingRetryCancellableTask {
  def apply(_name: String, _next: Task, _condition: ReadableExecutionContext => ErrorOr[Boolean],
            _allowedKeys: List[Key[_]]): basic.BlockingTask = new basic.BlockingTask with Cancellable with Retry {

    override val next: Task = _next

    override protected def condition(executionContext: ReadableExecutionContext): ErrorOr[Boolean] = _condition.apply(executionContext)

    override protected def customAllowedKeys: List[Key[_]] = _allowedKeys

    override protected def schedulingStrategy: SchedulingStrategy = new ConstantSchedulingStrategy(1)

    override protected def stoppingStrategy: StoppingStrategy = new QuantityStoppingStrategy(50)
  }
}