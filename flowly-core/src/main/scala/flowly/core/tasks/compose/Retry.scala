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

import java.time.Instant

import flowly.core.context.ExecutionContext
import flowly.core.repository.model.Attempts
import flowly.core.tasks.basic.Task
import flowly.core.tasks.model.{OnError, TaskResult, ToRetry}
import flowly.core.tasks.strategies.scheduling.SchedulingStrategy
import flowly.core.tasks.strategies.stopping.StoppingStrategy

trait Retry extends Task with Dependencies {

  protected def schedulingStrategy: SchedulingStrategy

  protected def stoppingStrategy: StoppingStrategy

  abstract override private[flowly] def execute(sessionId: String, executionContext: ExecutionContext): TaskResult = {
    try {
      val attempts = executionContext.attempts.getOrElse(Attempts(1, Instant.now, None))
      super.execute(sessionId, executionContext) match {
        case OnError(cause: Retryable) if cause.canBeRetried && stoppingStrategy.shouldRetry(executionContext, attempts) =>
          ToRetry(cause, attempts.withNextRetry(schedulingStrategy.nextRetry(executionContext, attempts)))
        case
          otherwise => otherwise
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


trait Retryable {
  this: Throwable =>
  def canBeRetried: Boolean
}
