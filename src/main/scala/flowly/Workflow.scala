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

package flowly

import flowly.events.EventHook
import flowly.repository.Repository
import flowly.repository.model.Session
import flowly.repository.model.Session.SessionId
import flowly.tasks._
import flowly.variables.{ReadableVariables, Variables}


trait Workflow {

  def initialTask: Task

  def eventHook: EventHook

  // temp
  val repository = new Repository

  /**
    * Initialize a new workflow session
    *
    * @param params initial workflow variables
    * @return session id
    */
  def init(params: Param*): ErrorOr[String] = {

    repository.createSession(params.toVariables).map { session =>

      // On Initialization Event
      eventHook.onInitialization(session.id, session.variables)

      session.id

    }

  }

  /**
    * Execute an instance of [[Workflow]] form its current [[Task]] with the given params
    *
    * @param sessionId session id
    * @param params    new workflow variables
    * @return execution result
    */
  def execute(sessionId: SessionId, params: Param*): ErrorOr[Result] = {

    for {

      // Get Session, can be executed?
      currentSession <- repository.getSession(sessionId).filterOrElse(_.isExecutable, SessionCantBeExecuted(sessionId))

      // Get Current Task
      currentTask <- {

        val taskId = currentSession.lastExecution.map(_.taskId).getOrElse(initialTask.id)

        tasks.find(_.id == taskId).toRight(TaskNotFound(taskId))

      }

      // On Start Event
      _ = if (currentTask == initialTask) eventHook.onStart(sessionId, currentSession.variables)

      // Merge old and new Variables
      currentVariables = currentSession.variables.merge(params.toVariables)

      // Execute from Current Task
      result <- execute(currentTask, currentSession, currentVariables)

    } yield result

  }

  private def execute(task: Task, session: Session, variables: Variables): ErrorOr[Result] = {

    def onSuccess(currentSession: Session): ErrorOr[Result] = {

      // Execute Task
      task.execute(currentSession.id, variables) match {

        case Continue(next, currentVariables) =>

          // On Execution Event
          eventHook.onExecution(session.id, currentVariables, task.id)

          // Execute next Task
          execute(next, currentSession, currentVariables)

        case Block =>

          repository.saveSession(currentSession.blocked(task)).fold(onFailure, { session =>

            // On Block Event
            eventHook.onBlocked(session.id, session.variables, task.id)

            Right(Result(session, task))

          })

        case Finish =>

          repository.saveSession(currentSession.finished(task)).fold(onFailure, { session =>

            // On Finish Event
            eventHook.onFinish(session.id, session.variables, task.id)

            Right(Result(session, task))

          })

        case OnError(cause) =>

          // TODO: if repo fails I lost original cause
          repository.saveSession(currentSession.onError(task, cause)).fold(onFailure, { session =>

            // On Error Event
            eventHook.onError(session.id, session.variables, task.id, cause)

            Left(ExecutionError(session, task, cause))

          })

      }

    }

    def onFailure(cause: Throwable): ErrorOr[Result] = Left(ExecutionError(session, task, cause))

    // Update Session to Running Status and execute it
    repository.saveSession(session.running(task, variables)).fold(onFailure, onSuccess)

  }

  /**
    * Cancel an instance of Workflow, it can't be executed again
    *
    * @param sessionId session id
    * @return session id
    */
  def cancel(sessionId: SessionId, reason: String): ErrorOr[String] = {

    for {

      session <- repository.getSession(sessionId)

      result <- repository.saveSession(session.cancelled(reason))

      // On Cancellation Event
      _ = eventHook.onCancellation(sessionId, reason, session.variables, session.lastExecution.map(_.taskId))

    } yield result

  }.map(_.id)

  /**
    * It returns Variables for a given session id
    *
    * @param sessionId session id
    * @return Variables (read only)
    */
  def variables(sessionId: SessionId): ErrorOr[ReadableVariables] = repository.getSession(sessionId).map(_.variables)

  /**
    * It returns a list of every [[Task]] in this workflow
    *
    * @return list of [[Task]]
    */
  private def tasks: List[Task] = {
    def tasks(currentTask: Task, accum: List[Task]): List[Task] = {
      if (accum.contains(currentTask)) accum
      else currentTask.followedBy.foldRight(currentTask :: accum)(tasks)
    }

    tasks(initialTask, Nil)
  }

}