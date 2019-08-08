package flowly.core.scalacheck.model

import flowly.core.tasks.basic.Task

trait TestingTask extends Task {

  override val id = UUID()

}