package flowly.demo

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

import java.io.IOError
import java.time.Instant
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.mongodb.client.MongoClients
import flowly.core.context.{ExecutionContextFactory, Key, ReadableExecutionContext, WritableExecutionContext}
import flowly.core.events.EventListener
import flowly.core.repository.model.Attempts
import flowly.core.repository.{InMemoryRepository, Repository}
import flowly.core.tasks.basic._
import flowly.core.tasks.compose.{Alternative, Retry, Retryable}
import flowly.core.tasks.strategies.scheduling.SchedulingStrategy
import flowly.core.tasks.strategies.stopping.StoppingStrategy
import flowly.core.Workflow
import flowly.mongodb.{CustomDateModule, MongoDBRepository}

import scala.util.Try


object MainTest extends App {

  trait RepositoryComponent {
    this: ObjectMapperRepositoryComponent =>
    val client = MongoClients.create("localhost:s")
    lazy val repository = new MongoDBRepository(client, "flowly", "demo", objectMapperRepository)
    //    lazy val repository = new InMemoryRepository
  }

  trait ObjectMapperRepositoryComponent {
    lazy val objectMapperRepository = new ObjectMapper with ScalaObjectMapper
    objectMapperRepository.registerModule(new DefaultScalaModule)
    objectMapperRepository.registerModule(CustomDateModule)
    objectMapperRepository.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    objectMapperRepository.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
  }

  trait ObjectMapperContextComponent {
    lazy val objectMapperContext = new ObjectMapper with ScalaObjectMapper
    objectMapperContext.registerModule(new DefaultScalaModule)
    objectMapperContext.registerModule(new JavaTimeModule)
    objectMapperContext.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    objectMapperContext.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
  }

  trait Finish1Component {
    lazy val finish = FinishTask("FINISH 1")
  }

  trait Finish2Component {
    lazy val finish2 = FinishTask("FINISH 2")
  }

  trait BlockingComponent {
    this: Finish1Component =>
    lazy val blocking: Task = new BlockingTask {
      override val next: Task = finish

      override protected def condition(executionContext: ReadableExecutionContext) = Right(executionContext.contains(Key3))

      override protected def customAllowedKeys = List(Key3)
    }
  }

  trait SecondComponent {
    this: ThirdComponent =>
    lazy val second: Task = new ExecutionTask with Retry {

      val next: Task = third

      protected def perform(sessionId: String, executionContext: WritableExecutionContext) = {
        Left(new RuntimeException("todo mal"))
      }

      protected def schedulingStrategy = Now

      protected def stoppingStrategy = Always
    }
  }

  trait ThirdComponent {
    this: BlockingComponent =>
    lazy val third: Task = new ExecutionTask {

      val next: Task = blocking

      override protected def perform(sessionId: String, executionContext: WritableExecutionContext) = {
        val maybeInstant = executionContext.get(Key7)
        println(s" la fecha es $maybeInstant")
        Right(executionContext)
      }
    }
  }

  trait BlockingDisjunctionComponent {
    this: Finish2Component with DisjunctionComponent =>
    lazy val blockingDisjunction: Task = new DisjunctionTask {
      protected def branches = List((e => Right(e.contains(Key5)), disjunction), (e => Right(e.contains(Key6)), finish2))

      protected def customAllowedKeys = List(Key5, Key6)

      protected def blockOnNoCondition = true
    }
  }

  trait DisjunctionComponent {
    this: Finish2Component with SecondComponent =>
    lazy val disjunction: Task = new DisjunctionTask {
      protected def branches = List((e => Right(e.contains(Key4)), finish2), (_ => Right(true), second))

      protected def customAllowedKeys = Nil

      protected def blockOnNoCondition = true
    }
  }

  trait FirstComponent {
    this: BlockingDisjunctionComponent =>
    lazy val first: Task = new ExecutionTask with Retry {

      val next: Task = blockingDisjunction

      protected def perform(sessionId: String, executionContext: WritableExecutionContext) = {
        //        Right(executionContext.set(Key1, "foo bar baz"))
        Left(CustomError("todo mal"))
      }

      override protected def schedulingStrategy = Now

      override protected def stoppingStrategy = Always
    }
  }

  trait WorkflowComponent {
    self: ObjectMapperContextComponent with RepositoryComponent =>
    lazy val workflow: Workflow = new Workflow {
      def initialTask: Task = Components.first

      override def eventListeners: List[EventListener] = List(new DummyEventListener)

      override val executionContextFactory = new ExecutionContextFactory(new JacksonSerializer(self.objectMapperContext))
      override val repository: Repository = self.repository
    }
  }

  object Components extends WorkflowComponent with FirstComponent with SecondComponent with ThirdComponent with DisjunctionComponent with BlockingDisjunctionComponent with BlockingComponent with Finish1Component with Finish2Component with ObjectMapperRepositoryComponent with ObjectMapperContextComponent with RepositoryComponent


  val r = for {

    id <- Components.workflow.init()

    result <- Components.workflow.execute(id)

  } yield result


  println(r)

}

case object Key1 extends Key[String]
case object Key2 extends Key[Int]
case object Key3 extends Key[Boolean]
case object Key4 extends Key[Boolean]
case object Key5 extends Key[Int]
case object Key6 extends Key[Int]
case object Key7 extends Key[Instant]

object Always extends StoppingStrategy {
  override def shouldRetry(executionContext: ReadableExecutionContext, attempts: Attempts): Boolean = true
}

object Now extends SchedulingStrategy {
  override def nextRetry(executionContext: ReadableExecutionContext, attempts: Attempts): Instant = Instant.now.plusSeconds(60)
}

case class CustomError(msg:String) extends RuntimeException(msg) with Retryable {
  override def canBeRetried: Boolean = false
}