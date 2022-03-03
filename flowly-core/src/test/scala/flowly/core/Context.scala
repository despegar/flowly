package flowly.core

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.ScalaObjectMapper
import flowly.core.serialization.Serializer
import org.specs2.specification.Scope

trait Context extends Scope {
  lazy val objectMapper: ObjectMapper with ScalaObjectMapper = {
    val om = new ObjectMapper() with ScalaObjectMapper
    om.registerModule(new DefaultScalaModule)
    om
  }
  lazy val serializer: Serializer = new JacksonSerializer(objectMapper)
}


class JacksonSerializer(objectMapper: ObjectMapper with ScalaObjectMapper) extends Serializer {

  override def write(obj: Any): String = {
    try {
      objectMapper.writeValueAsString(obj)
    } catch {
      case cause: Throwable => throw SerializationException(s"Error trying to serialize $obj", cause)
    }
  }

  override def read[T: Manifest](value: String): T = {
    try {
      objectMapper.readValue[T](value)
    } catch {
      case cause: Throwable => throw SerializationException(s"Error trying to deserialize $value", cause)
    }
  }

}