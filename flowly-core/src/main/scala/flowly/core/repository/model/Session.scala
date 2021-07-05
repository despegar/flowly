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

package flowly.core.repository.model

import java.time.Instant
import java.util.UUID

import flowly.core.repository.model.Session.{SessionId, Status}
import flowly.core.repository.model.Status._
import flowly.core.tasks.basic.Task
import flowly.core.context.WritableExecutionContext
import flowly.core.{Param, Variables}

case class Session(sessionId: SessionId, variables: Variables, lastExecution: Option[Execution], attempts:Option[Attempts], createdAt: Instant, status: Status, version: Long) {

  def resume(task: Task, params: List[Param]): Session = {
    copy(lastExecution = Option(Execution(task.name)), variables = variables ++ params.toVariables, status = RUNNING, attempts = attempts.map(_.newAttempt()) )
  }

  def continue(task: Task, executionContext: WritableExecutionContext): Session = {
    copy(lastExecution = Option(Execution(task.name)), variables = executionContext.variables, status = RUNNING, attempts = None)
  }

  def blocked(task: Task): Session = {
    copy(lastExecution = Option(Execution(task.name)), status = BLOCKED, attempts = None)
  }

  def finished(task: Task): Session = {
    copy(lastExecution = Option(Execution(task.name)), status = FINISHED, attempts = None)
  }

  def cancelled(task: Task): Session = {
    copy(lastExecution = Option(Execution(task.name, "Flow cancelled by user")), status = CANCELLED, attempts = None)
  }

  def onError(task: Task, throwable: Throwable): Session = {
    copy(lastExecution = Option(Execution(task.name, throwable.getMessage)), status = ERROR, attempts = attempts.map(_.stopRetrying()))
  }

  def toRetry(task: Task, throwable: Throwable, attempts: Attempts): Session = {
    copy(lastExecution = Option(Execution(task.name, throwable.getMessage)), status = TO_RETRY, attempts = Option(attempts))
  }

  def isExecutable: Boolean = status match {
    case RUNNING | FINISHED | CANCELLED => false
    case _ => true
  }

}
object Session {

  type SessionId = String
  type Status    = String

  def apply(id: SessionId, variables: Variables): Session = {
    new Session(id, variables, None, None, Instant.now, CREATED, 0L)
  }
  def apply(variables: Variables): Session = apply(UUID.randomUUID.toString, variables)

}

case class Execution(taskName: String, message:Option[String], at: Instant)
object Execution {
  def apply(taskName: String): Execution = new Execution(taskName, None, Instant.now)
  def apply(taskName: String, message:String): Execution = new Execution(taskName, Option(message), Instant.now)
}

case class Attempts(quantity: Int, firstAttempt: Instant, nextRetry: Option[Instant]) {
  def newAttempt():Attempts = copy(quantity = quantity + 1)
  def stopRetrying(): Attempts = copy(nextRetry = None)
  def withNextRetry(nextRetry: Instant): Attempts = copy(nextRetry = Option(nextRetry))
}
object Attempts {
  def apply(): Attempts = Attempts(quantity = 1, firstAttempt = Instant.now(), None)
}