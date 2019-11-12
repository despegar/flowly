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

package flowly.core.tasks.basic

import flowly.core.{DisjunctionTaskError, ErrorOr}
import flowly.core.tasks.model.{Block, Continue, OnError, TaskResult}
import flowly.core.context.{ExecutionContext, ReadableExecutionContext}

/**
  * An instance of this [[Task]] will choose a branch of execution between different paths based on given conditions.
  *
  * It will test each condition until find any that works. If no condition works, this [[Task]] will fail or block depending on blockOnNoCondition value.
  *
  */
trait DisjunctionTask extends Task {

  protected def branches: List[(ReadableExecutionContext => ErrorOr[Boolean], Task)]

  /**
    * This task is going to block instead of fail when there are no conditions that match
    */
  protected def blockOnNoCondition:Boolean

  private[flowly] def execute(sessionId: String, executionContext: ExecutionContext): TaskResult = try {

    val branch = branches.to(LazyList).map{case (condition, nextTask) => (condition(executionContext), nextTask)}.dropWhile({
      case (Right(false), _) => true
      case _ => false
    }).headOption

    branch match {
      case Some((Right(true), nextTask)) => Continue(nextTask, executionContext)
      case Some((Right(false), _)) | None if blockOnNoCondition => Block
      case Some((Right(false), _)) | None => OnError(DisjunctionTaskError(name))
      case Some((Left(throwable),_)) => OnError(throwable)
    }

  } catch {
    case throwable: Throwable => OnError(throwable)
  }

  private[flowly] def followedBy: List[Task] = branches.collect { case (_, task) => task }

}

