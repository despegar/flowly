package flowly.core.compat

import java.util
import scala.collection.JavaConverters.mapAsJavaMapConverter

object CompatUtils {

  def toLazyColl[T](l: List[T]): Stream[T] = {
    l.to(Stream.canBuildFrom)
  }

  def asJavaMap(values:(String, AnyRef)*): util.Map[String, AnyRef] = {
    Map(values:_*).asJava
  }

}
