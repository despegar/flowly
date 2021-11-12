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

import flowly.core.context.ReadableExecutionContext
import flowly.core.repository.model.Session
import flowly.core.repository.model.Session.SessionId
import flowly.core.tasks.basic.Task

// DRAFT

case class ExecutionError(sessionId: SessionId, taskName: String, executionContext: ReadableExecutionContext, cause: Throwable) extends Throwable(cause)

object ExecutionError {
  def apply(session: Session, task: Task, executionContext: ReadableExecutionContext, cause: Throwable): ExecutionError = new ExecutionError(session.sessionId, task.name, executionContext, cause)
}

case class TaskNotFound(taskName: String) extends Throwable

case class SessionNotFound(sessionId: SessionId) extends Throwable
case class RepositoryError(message: String, throwable: Throwable) extends Throwable(message, throwable)

case class SessionCantBeExecuted(sessionId: String) extends Throwable

case class KeyNotFound(key: String) extends Throwable

case class ParamsNotAllowed(params: List[Param]) extends Throwable

case class DisjunctionTaskError(taskName:String) extends Throwable

case class SerializationException(message: String, cause: Throwable) extends Throwable(message, cause)