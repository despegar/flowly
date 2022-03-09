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

package flowly.core.events

import flowly.core.Variables
import flowly.core.repository.model.Attempts
import flowly.core.repository.model.Session.SessionId
import flowly.core.context.ReadableExecutionContext

class BaseEventListener extends EventListener {

  def onInitialization(sessionId: SessionId, vars: Variables): Unit = ()

  def onStart(sessionId: SessionId, executionContext: ReadableExecutionContext, taskName: String): Unit = ()

  def onResume(sessionId: SessionId, executionContext: ReadableExecutionContext, taskName: String): Unit = ()

  def onContinue(sessionId: SessionId, executionContext: ReadableExecutionContext, currentTask: String, nextTask:String): Unit = ()

  def onSkipAndContinue(sessionId: SessionId, executionContext: ReadableExecutionContext, currentTask: String, nextTask: String): Unit = ()

  def onBlock(sessionId: SessionId, executionContext: ReadableExecutionContext, currentTask: String): Unit = ()

  def onFinish(sessionId: SessionId, executionContext: ReadableExecutionContext, currentTask: String): Unit = ()

  def onError(sessionId: SessionId, executionContext: ReadableExecutionContext, currentTask: String, cause:Throwable): Unit = ()

  def onToRetry(sessionId: SessionId, executionContext: ReadableExecutionContext, currentTask: String, cause: Throwable, attempts: Attempts): Unit = ()

  def onCancel(sessionId: SessionId, executionContext: ReadableExecutionContext, currentTask: String): Unit = ()
}
