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

package flowly.core.context

import flowly.core.repository.model.{Attempts, Session}
import flowly.core.serialization.Serializer
import flowly.core.tasks.basic.{BlockingTask, DisjunctionTask, ExecutionTask}
import flowly.core.{ErrorOr, KeyNotFound, Variables}

/**
  * Read-only interface of Variables
  */
trait ReadableExecutionContext {

  def get[T: Manifest](key: Key[T]): Option[T]

  def getOrElse[T: Manifest](key: Key[T], orElse: => T): T

  def getOrError[T: Manifest](key: Key[T]):ErrorOr[T]

  def contains(key: Key[_]): Boolean

  def exists[T: Manifest](key: Key[T], f: T => Boolean): Boolean

  def forall[T: Manifest](key: Key[T], f: T => Boolean): Boolean

  private[flowly] def variables: Variables

}

/**
  * Writable interface of Variables
  */
trait WritableExecutionContext extends ReadableExecutionContext {

  def set[T: Manifest](key: Key[T], value: T): WritableExecutionContext

  def unset(key: Key[_]): WritableExecutionContext

}

/**
  * Immutable variables that are pass through the tasks during a workflow execution.
  * It can be used from a [[BlockingTask]], [[DisjunctionTask]] or [[ExecutionTask]] in order to make decisions or
  * it can be modified (through a copy) from an [[ExecutionTask]].
  *
  * After a successful task this context is saved in the repository in order to keep safe any modification.
  *
  */
class ExecutionContext private[flowly](val variables: Variables, val attempts:Option[Attempts], serializer: Serializer) extends ReadableExecutionContext with WritableExecutionContext {

  def get[T: Manifest](key: Key[T]): Option[T] = variables.get(key.identifier).map(serializer.deepCopy[T])

  def getOrElse[T: Manifest](key: Key[T], orElse: => T): T = get(key).getOrElse(orElse)

  def getOrError[T: Manifest](key: Key[T]): ErrorOr[T] = get(key).toRight(KeyNotFound(key.identifier))

  def contains(key: Key[_]): Boolean = variables.contains(key.identifier)

  def exists[T: Manifest](key: Key[T], f: T => Boolean): Boolean = get(key).exists(f)

  def forall[T: Manifest](key: Key[T], f: T => Boolean): Boolean = get(key).forall(f)

  def set[T: Manifest](key: Key[T], value: T): ExecutionContext = new ExecutionContext(variables.updated(key.identifier, serializer.deepCopy(value)), attempts, serializer)

  def unset(key: Key[_]): ExecutionContext = new ExecutionContext(variables - key.identifier, attempts, serializer)

}

class ExecutionContextFactory(serializer: Serializer) {
  def create(session: Session): ExecutionContext = new ExecutionContext(session.variables, session.attempts, serializer)
}