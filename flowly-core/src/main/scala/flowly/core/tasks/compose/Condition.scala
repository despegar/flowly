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

package flowly.core.tasks.compose

import flowly.core.ErrorOr
import flowly.core.context.{ExecutionContext, ReadableExecutionContext}
import flowly.core.tasks.basic.{ExecutionTask, Task}
import flowly.core.tasks.model.{OnError, SkipAndContinue, TaskResult}

trait Condition extends Task with Dependencies {
  this: ExecutionTask =>

  protected def condition(executionContext: ReadableExecutionContext): ErrorOr[Boolean]

  abstract override private[flowly] def execute(sessionId: String, executionContext: ExecutionContext): TaskResult = {
    try {
      condition(executionContext) match {
        case Right(true) => super.execute(sessionId, executionContext)
        case Right(false) => SkipAndContinue(next, executionContext)
        case Left(throwable) => OnError(throwable)
      }
    } catch {
      case throwable: Throwable => OnError(throwable)
    }
  }

  /**
    * This method give us a compile-time check about [[Alternative]] use
    */
  override private[flowly] def alternativeAfterAll(): Unit = ()

}