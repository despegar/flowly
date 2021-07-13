package flowly.core.workflow

import flowly.core._
import flowly.core.context.ExecutionContextFactory
import flowly.core.repository.model.{Attempts, Session}
import flowly.core.repository.model.Status.{CANCELLED, FINISHED, TO_RETRY}
import flowly.core.repository.{InMemoryRepository, Repository}
import flowly.core.serialization.Serializer
import flowly.core.tasks.ExecutionTask
import flowly.core.tasks.basic.{FinishTask, Task}
import flowly.core.tasks.compose.Retryable
import org.specs2.mutable.Specification


class WorkflowSpec extends Specification {

  case class RetryableError(msg:String) extends RuntimeException(msg) with Retryable {
    override def canBeRetried: Boolean = true
  }


  private def buildWorkflow(serializer: Serializer): Workflow = {
    val thirdTask = FinishTask("OK")
    val secondTask =
      tasks.BlockingRetryCancellableTask("Task2", thirdTask, { ec =>
        if (ec.get(StringKey).contains("fail")) Left(RetryableError("error !"))
        else Right(ec.contains(BooleanKey))
      }, List(BooleanKey, StringKey))

    val firstTask = tasks.ExecutionTask("Task1", secondTask){ case(_, ec) => Right(ec) }

    new Workflow {
      override def initialTask: Task = firstTask
      override val executionContextFactory: ExecutionContextFactory = new ExecutionContextFactory(serializer)
      override val repository: Repository = new InMemoryRepository
    }
  }

  "Workflow" should {
    "throw runtime exception" in new Context {
      val task2 = ExecutionTask("Task2", FinishTask("Task2")) { case (_, ec) => Right(ec) }
      val task1 = tasks.ExecutionTask("Task1", task2) { case (_, ec) => Right(ec) }

      new Workflow {
          def initialTask: Task = task1
          override val executionContextFactory = new ExecutionContextFactory(serializer)
          override val repository = new InMemoryRepository
        } must throwAn[IllegalStateException]
    }

    "Finish OK" in new Context {
      val workflow: Workflow = buildWorkflow(serializer)

      val sessionOrError = for {
        sessionId     <- workflow.init(Param(StringKey, "test"))
        _             <- workflow.execute(sessionId, List.empty)
        _             <- workflow.execute(sessionId, Param(BooleanKey, true))
        session       <- workflow.repository.getById(sessionId)
      } yield session

      private val session: Session = sessionOrError.getOrElse(throw new RuntimeException("Session not found."))

      session.status must_== FINISHED
    }

    "End as CANCELLED" in new Context {
      val workflow: Workflow = buildWorkflow(serializer)

      val sessionOrError = for {
        sessionId     <- workflow.init(Param(StringKey, "test"))
        _             <- workflow.execute(sessionId, List.empty)
        _             <- workflow.cancel(sessionId)
        session       <- workflow.repository.getById(sessionId)
      } yield session

      private val session: Session = sessionOrError.getOrElse(throw new RuntimeException("Session not found."))

      session.status must_== CANCELLED
    }

    "Fail trying to resume a cancelled workflow" in new Context {
      val workflow: Workflow = buildWorkflow(serializer)

      val sessionOrError = for {
        sessionId     <- workflow.init(Param(StringKey, "test"))
        _             <- workflow.execute(sessionId, List.empty)
        _             <- workflow.cancel(sessionId)
        _             <- workflow.execute(sessionId, Param(BooleanKey, true))
        session       <- workflow.repository.getById(sessionId)
      } yield session

      sessionOrError must beAnInstanceOf[Left[SessionCantBeExecuted, Session]]
    }

    "Should not retry after cancel" in new Context {
      val workflow: Workflow = buildWorkflow(serializer)

      val sessionOrError = for {
        sessionId         <- workflow.init(Param(StringKey, "test"))
        _                 <- workflow.execute(sessionId, List.empty)
        _                 =  workflow.execute(sessionId, Param(StringKey, "fail"))
        sessionAfterError <- workflow.repository.getById(sessionId)
        _                 = if (sessionAfterError.status != TO_RETRY) throw new RuntimeException("Invalid status after fail with Retryable error on retryable task")
        _                 <- workflow.cancel(sessionId)
        session           <- workflow.repository.getById(sessionId)
      } yield session

      private val session: Session = sessionOrError.getOrElse(throw new RuntimeException("Session not found."))

      session.status must_== CANCELLED
      private val attempts: Attempts = session.attempts.getOrElse(throw new RuntimeException("empty attempts is not possible."))

      attempts.nextRetry.isEmpty must_== true
      attempts.quantity > 0 must_== true

    }
  }

}

