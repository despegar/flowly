/*
 * Copyright © 2018-2019 the flowly project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package flowly.core

import flowly.core.events.EventListener
import flowly.core.repository.Repository
import flowly.core.repository.model.Session
import flowly.core.repository.model.Session.SessionId
import flowly.core.tasks.basic.Task
import flowly.core.tasks.model._
import flowly.core.context.{ExecutionContextFactory, Key}
import flowly.core.tasks.compose.CancelFlow

import scala.annotation.tailrec


trait Workflow {

  //Validate workflow consistency when constructed
  checkConsistency()

  //TODO: It could be useful to validate that current open sessions in DB are blocked in valid tasks for the current Workflow.

  def initialTask: Task

  def eventListeners: List[EventListener] = Nil

  val executionContextFactory: ExecutionContextFactory

  val repository: Repository

  /**
    * Initialize a new workflow session
    *
    * @param params initial workflow variables
    * @return session id
    */
  def init(params: Param*): ErrorOr[SessionId] = {

    repository.insert(Session(params.toList.toVariables)).map { session =>

      // On Initialization Event
      eventListeners.foreach(_.onInitialization(session.sessionId, session.variables))

      session.sessionId

    }

  }

  /**
    * Execute an instance of [[Workflow]] form its current [[Task]] with the given params
    *
    * @param sessionId session id
    * @param params    new workflow variables
    * @return execution result
    */
  def execute(sessionId: SessionId, params: Param*): ErrorOr[ExecutionResult] = execute(sessionId, params.toList)

  /**
    * Execute an instance of [[Workflow]] form its current [[Task]] with the given params
    *
    * @param sessionId session id
    * @param params    new workflow variables
    * @return execution result
    */
  def execute(sessionId: SessionId, params: List[Param]): ErrorOr[ExecutionResult] = for {

    // Get Session, can be executed?
    session <- repository.getById(sessionId).filterOrElse(_.isExecutable, SessionCantBeExecuted(sessionId))

    // Get Current Task
    currentTask <- currentTask(session)

    // Are params allowed?
    _ <- if(currentTask.accept(params.toKeys)) Right(true) else Left(ParamsNotAllowed(params))

    // Set the session as running
    runningSession <- repository.update(session.resume(currentTask, params))

    // Create Execution Context
    executionContext = executionContextFactory.create(session)

    // On Start or Resume Event
    _ = session.lastExecution.fold(eventListeners.foreach(_.onStart(sessionId, executionContext)) ) { _ =>
      eventListeners.foreach(_.onResume(sessionId, executionContext))
    }

    // Execute from Current Task
    result <- execute(currentTask, runningSession)

  } yield result

  private def currentTask(session: Session): ErrorOr[Task] = {
    val taskName = session.lastExecution.map(_.taskName).getOrElse(initialTask.name)
    tasks.find(_.name == taskName).toRight(TaskNotFound(taskName))
  }

  private def execute(task: Task, session: Session): ErrorOr[ExecutionResult] = {

    // Create Execution Context
    val executionContext = executionContextFactory.create(session)

    def onFailure(cause: Throwable): ErrorOr[ExecutionResult] = Left(ExecutionError(session, task, executionContext, cause))

    // Execute Task
    task.execute(session.sessionId, executionContext) match {

      case Continue(nextTask, resultingExecutionContext) =>

        repository.update(session.continue(nextTask, resultingExecutionContext)).fold(onFailure, { session =>

          // On Continue Event
          eventListeners.foreach(_.onContinue(session.sessionId, resultingExecutionContext, task.name, nextTask.name))

          // Execute next Task
          execute(nextTask, session)

        })

      case SkipAndContinue(nextTask, resultingExecutionContext) =>

        repository.update(session.continue(nextTask, resultingExecutionContext)).fold(onFailure, { session =>

          // On skip and Continue Events
          eventListeners.foreach(l => {
            l.onSkipAndContinue(session.sessionId, resultingExecutionContext, task.name, nextTask.name)
          })

          // Execute next Task
          execute(nextTask, session)

        })

      case Block =>

        repository.update(session.blocked(task)).fold(onFailure, { session =>

          // On Block Event
          eventListeners.foreach(_.onBlock(session.sessionId, executionContext, task.name))

          Right(ExecutionResult(session, executionContext, task))

        })

      case Finish =>

        repository.update(session.finished(task)).fold(onFailure, { session =>

          // On Finish Event
          eventListeners.foreach(_.onFinish(session.sessionId, executionContext, task.name))

          Right(ExecutionResult(session, executionContext, task))

        })

      case Cancel =>
        repository.update(session.cancelled(task)).fold(onFailure, { session =>

          // On Cancel Event
          eventListeners.foreach(_.onCancel(session.sessionId, executionContext, task.name))

          Right(ExecutionResult(session, executionContext, task))
        })

      case ToRetry(cause, attempts) =>

        repository.update(session.toRetry(task, cause, attempts)).fold(onFailure, { session =>

          // On ToRetry Event
          eventListeners.foreach(_.onToRetry(session.sessionId, executionContext, task.name, cause, attempts))

          Left(ExecutionError(session, task, executionContext, cause))

        })

      case OnError(cause) =>

        repository.update(session.onError(task, cause)).fold(onFailure, { session =>

          // On Error Event
          eventListeners.foreach(_.onError(session.sessionId, executionContext, task.name, cause))

          Left(ExecutionError(session, task, executionContext, cause))

        })

    }

  }

  def cancel(sessionId: SessionId): ErrorOr[ExecutionResult] = {
    execute(sessionId, List(Param(CancelFlow, true)))
  }

  /**
    * It returns Variables for a given session id
    *
    * @param sessionId session id
    * @return Variables (read only)
    */
  def variables(sessionId: SessionId): ErrorOr[Variables] = repository.getById(sessionId).map(_.variables)

  def currentAllowedKeys(sessionId: String): ErrorOr[List[Key[_]]] = for {

    session <- repository.getById(sessionId)

    currentTask <- currentTask(session)

  } yield currentTask.allowedKeys

  def getToRetry:ErrorOr[Iterator[SessionId]] = repository.getToRetry

  /**
    * It returns a list of every [[Task]] in this workflow
    *
    * @return list of [[Task]]
    */
  private def tasks: List[Task] = {
    def tasks(currentTask: Task, accum: List[Task]): List[Task] = {
      if(accum.exists(_.id == currentTask.id)) accum
      else currentTask.followedBy.foldRight(currentTask :: accum)(tasks)
    }
    tasks(initialTask, Nil)
  }

  /**
    * Check if there are duplicated tasks
    */
  private def checkConsistency(): Unit = {
    val duplicated = tasks.map(_.name).diff(tasks.map(_.name).distinct)
    if (duplicated.nonEmpty) throw new IllegalStateException(s"There are repeated Tasks: $duplicated. Workflow can't be constructed")
  }

}