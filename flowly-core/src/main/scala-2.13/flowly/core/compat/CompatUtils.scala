package flowly.core.compat

import scala.jdk.CollectionConverters._

object CompatUtils {

  def toLazyColl[T](l: List[T]): LazyList[T] = {
    l.to(LazyList)
  }

  def asJavaMap(values:(String, AnyRef)*): java.util.Map[String, AnyRef] = {
    Map(values:_*).asJava
  }

}
