package flowly.core.scalacheck.generators

import flowly.core.IntKey
import flowly.core.context.ReadableExecutionContext
import scala.jdk.CollectionConverters._
import flowly.core.scalacheck.model.{BooleanKey, TestingTask}
import flowly.core.tasks.basic.{DisjunctionTask, Task}
import org.scalacheck.Gen

object DisjunctionTaskGenerator extends TaskGenerator {

  def genTask(depth: Int): Gen[Task] = for {

    disjunctionBranches <- Gen.oneOf(genBooleanDisjunctionTaskBranches(depth),
                                     genIntDisjunctionTaskBranches(depth))

    disjunction <- new DisjunctionTask with TestingTask {
      override protected def branches: List[(ReadableExecutionContext => Boolean, Task)] = disjunctionBranches
    }

  } yield disjunction


  private def genBooleanDisjunctionTaskBranches(depth: Int): Gen[List[(ReadableExecutionContext => Boolean, Task)]] = for {

    task1 <- genNextTask(depth)

    task2 <- genNextTask(depth)

    task3 <- genNextTask(depth)

  } yield List(((c: ReadableExecutionContext) => c.get(BooleanKey).exists(v => v),task1),
               ((c: ReadableExecutionContext) => c.get(BooleanKey).exists(v => !v),task2),
               ((c: ReadableExecutionContext) => !c.contains(BooleanKey), task3))

  private def genIntDisjunctionTaskBranches(depth: Int): Gen[List[(ReadableExecutionContext => Boolean, Task)]] = for {

    n <- Gen.choose(1, 5)

    branches <- Gen.sequence(for(i <- 0 to n) yield getIntBranch(i, n, depth))

  } yield branches.asScala

  private def getIntBranch(i: Int, n: Int, depth: Int): Gen[(ReadableExecutionContext => Boolean, Task)] = for {

    nextTask <- genNextTask(depth)

    intervalLength = (Int.MaxValue.toLong - Int.MinValue.toLong) / n

    condition = (c: ReadableExecutionContext) => c.get(IntKey).exists(v => (Int.MinValue + i * intervalLength) <= v && v < (Int.MinValue + (i + 1) * intervalLength))

  } yield (condition, nextTask)

}
