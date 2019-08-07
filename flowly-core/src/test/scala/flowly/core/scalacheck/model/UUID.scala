package flowly.core.scalacheck.model

object UUID {

  def apply(): String = java.util.UUID.randomUUID.toString

}
