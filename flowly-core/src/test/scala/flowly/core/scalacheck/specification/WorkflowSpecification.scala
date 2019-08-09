package flowly.core.scalacheck.specification

import flowly.core.repository.model.Status
import flowly.core.{Param, Workflow}
import flowly.core.scalacheck.generators.{ParamsGenerator, ParamsGroupsGenerator, WorkflowGenerator}
import org.scalacheck.Prop.forAll
import org.scalacheck._

object WorkflowSpecification extends Properties("Workflow"){

  private val MAX_EXECUTIONS: Int = 100

  private def maxExecutionAttemptsReach(executionN: Int): Boolean = executionN == MAX_EXECUTIONS

  private def isWorkflowFinished(workflow: Workflow, sessionId: String): Boolean = workflow.repository.getSession(sessionId).map(s => s.status).contains(Status.FINISHED)

  property("initStatus") = forAll(WorkflowGenerator.genWorkflow, ParamsGenerator.getParams) { (workflow: Workflow, params: List[Param]) =>
        val initResult = workflow.init(params:_*)
        val sessionId = initResult.getOrElse("")
        val status = workflow.repository.getSession(sessionId).map(s => s.status)
        initResult.isRight && status.contains(Status.CREATED)
  }

  property("FinishWorkflow") = forAll(WorkflowGenerator.genWorkflow, ParamsGroupsGenerator.getParamsGroups) { (workflow: Workflow, paramGroups: Iterator[List[Param]]) =>
    val initResult = workflow.init(paramGroups.next():_*)
    val sessionId = initResult.getOrElse("")
    var result = workflow.execute(sessionId)
    var executionN = 1
    while(!isWorkflowFinished(workflow, sessionId) && !maxExecutionAttemptsReach(executionN)){
      val params = paramGroups.next()
      result = workflow.execute(sessionId, params:_*)
      executionN = executionN + 1
    }
    if(!maxExecutionAttemptsReach(executionN))
      println(s"Workflow finish after $executionN executions with result: $result")
    else
      println(s"Workflow execution interrupted. Max execution attempts reach")
    val status = workflow.repository.getSession(sessionId).map(s => s.status)
    status.contains(Status.FINISHED)
  }


}
