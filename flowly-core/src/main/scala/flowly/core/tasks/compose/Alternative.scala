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

import flowly.core.context.ExecutionContext
import flowly.core.tasks.basic.Task
import flowly.core.tasks.model.{Continue, OnError, TaskResult}

trait Alternative extends Task with Dependencies {

  protected val nextOnError:Task

  abstract override private[flowly] def execute(sessionId: String, executionContext: ExecutionContext): TaskResult = {
    super.execute(sessionId, executionContext) match {
      case OnError(_) => Continue(nextOnError, executionContext)
      case otherwise => otherwise
    }
  }

  /**
    * A list of tasks that follows this task
    */
  abstract override private[flowly] def followedBy: List[Task] = nextOnError :: super.followedBy

  /**
    * This method give us a compile-time check about [[Alternative]] use
    */
  override private[flowly] final def alternativeAfterAll(): Unit = ()

}