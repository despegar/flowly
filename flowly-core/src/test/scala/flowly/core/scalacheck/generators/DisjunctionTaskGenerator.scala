package flowly.core.scalacheck.generators

import flowly.core.{BooleanKey, IntKey}
import flowly.core.context.ReadableExecutionContext
import flowly.core.scalacheck.generators.BlockingTaskGenerator.getTaskId

import scala.jdk.CollectionConverters._
import flowly.core.tasks.basic.{DisjunctionTask, Task}
import org.scalacheck.Gen

object DisjunctionTaskGenerator extends TaskGenerator {

  def genTask(depth: Int): Gen[Task] = {

    for {

      disjunctionBranches <- Gen.oneOf(genBooleanDisjunctionTaskBranches(depth),
                                       genIntDisjunctionTaskBranches(depth))

      taskId <- getTaskId

      disjunction = new DisjunctionTask {

        override val id: String = taskId

        override protected def branches: List[(ReadableExecutionContext => Boolean, Task)] = disjunctionBranches
      }

    } yield disjunction

  }


  private def genBooleanDisjunctionTaskBranches(depth: Int): Gen[List[(ReadableExecutionContext => Boolean, Task)]] = for {

    task1 <- genNextTask(depth)

    task2 <- genNextTask(depth)

  } yield List(((c: ReadableExecutionContext) => c.get(BooleanKey).exists(v => v), task1),
               ((c: ReadableExecutionContext) => !c.contains(BooleanKey) || c.get(BooleanKey).exists(v => !v), task2))

  private def genIntDisjunctionTaskBranches(depth: Int): Gen[List[(ReadableExecutionContext => Boolean, Task)]] = for {

    n <- Gen.choose(1, 5)

    branches <- Gen.sequence(for(i <- 0 to n) yield getIntBranch(i, n, depth))

    defaultNextTask <- genNextTask(depth)

    defaultBranch = ((c: ReadableExecutionContext) => !c.contains(IntKey), defaultNextTask)

  } yield branches.asScala.toList :+ defaultBranch

  private def getIntBranch(i: Int, n: Int, depth: Int): Gen[(ReadableExecutionContext => Boolean, Task)] = for {

    nextTask <- genNextTask(depth)

    intervalLength = (Int.MaxValue.toLong - Int.MinValue.toLong) / n

    condition = (c: ReadableExecutionContext) => c.get(IntKey).exists(v => ((Int.MinValue + i * intervalLength) <= v && v < (Int.MinValue + (i + 1) * intervalLength)) || (i == n && Int.MaxValue == v))

  } yield (condition, nextTask)

}
