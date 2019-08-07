package flowly.core.scalacheck.model

import UUID
import flowly.core.tasks.basic.Task


trait TestingTask extends Task {

  override val id = UUID()

}