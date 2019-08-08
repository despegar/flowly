package flowly.core.scalacheck.generators

import flowly.core.MainTest.Components
import flowly.core.{JacksonSerializer, Workflow}
import flowly.core.repository.InMemoryRepository
import flowly.core.context.ExecutionContextFactory
import flowly.core.repository.Repository
import flowly.core.tasks.basic._
import org.scalacheck.{Arbitrary, Gen}

object WorkflowGenerator extends TaskGenerator {

  implicit def arbitraryWorkflow: Arbitrary[Workflow] = Arbitrary{ genWorkflow }

  def genWorkflow: Gen[Workflow] = for {

    firstTask <- genNextTask(0)

    workflow <- Gen.const(new Workflow {

      override def initialTask: Task = firstTask

      override val repository: Repository = new InMemoryRepository()

      override val executionContextFactory: ExecutionContextFactory = new ExecutionContextFactory(new JacksonSerializer(Components.objectMapper))
    })

  } yield workflow

}

