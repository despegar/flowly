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

package flowly.core.tasks.model

import flowly.core.repository.model.Attempts
import flowly.core.tasks.basic.Task
import flowly.core.context.WritableExecutionContext

/**
  * Interface of a [[Task]] execution result
  */
trait TaskResult

/**
  * Current workflow execution must be continued
  *
  * @param nextTask  next task to be executed
  * @param executionContext current execution context
  */
case class Continue(nextTask: Task, executionContext: WritableExecutionContext) extends TaskResult

/**
  * Current workflow execution must be continued but current task was skipped
  *
  * @param nextTask  next task to be executed
  */
case class SkipAndContinue(nextTask: Task, executionContext: WritableExecutionContext) extends TaskResult

/**
  * Current workflow execution has finished
  *
  */
case object Finish extends TaskResult

/**
  * Current workflow execution has cancelled
  *
  */
case object Cancel extends TaskResult

/**
  * Current workflow execution cannot continue because a condition is not met
  *
  */
case object Block extends TaskResult

/**
  * There was an unexpected error during current workflow execution
  *
  * @param cause  error
  */
case class OnError(cause: Throwable) extends TaskResult

/**
  * There was an unexpected error during current workflow execution but the task can be retried
  *
  * @param cause  error
  * @param attempts information about attempts
  */
case class ToRetry(cause: Throwable, attempts:Attempts) extends TaskResult