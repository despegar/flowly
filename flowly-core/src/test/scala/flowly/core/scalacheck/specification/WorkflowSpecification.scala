package flowly.core.scalacheck.specification

import flowly.core.{Param, Workflow}
import flowly.core.scalacheck.generators.{ParamsGenerator, WorkflowGenerator}
import org.scalacheck.Prop.forAll
import org.scalacheck._

object WorkflowSpecification extends Properties("Workflow"){

  property("initStatus") = forAll(WorkflowGenerator.genWorkflow,ParamsGenerator.getParams) { (workflow: Workflow, params: Seq[Param]) =>
        val initResult = workflow.init(params:_*)
        val sessionId = initResult.getOrElse("")
        initResult.isRight && sessionId != ""
  }

}
