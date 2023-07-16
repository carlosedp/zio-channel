package zio.channel.benchmarks

import org.openjdk.jmh.annotations.{Scope, *}
import java.util.concurrent.TimeUnit
import zio.profiling.jmh.BenchmarkUtils

import zio.*
import zio.channel.*

@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Threads(1)
@Fork(1)
@Warmup(iterations = 10, timeUnit = TimeUnit.SECONDS, time = 1)
@Measurement(iterations = 10, timeUnit = TimeUnit.SECONDS, time = 1)
class Benchmarks:

  val numMessages = 10000

  private def executeZio[A](zio: Task[A]): A =
    BenchmarkUtils.unsafeRun(zio)

  @Benchmark
  def sendMessagesChan: Unit = executeZio:
    for
      chan <- Channel.make[Int]
      _    <- chan.send(1).fork.repeatN(numMessages)
      _    <- chan.receive.repeatN(numMessages).ignore
    yield ()

  @Benchmark
  def sendMessagesQueue: Unit = executeZio:
    for
      queue <- Queue.bounded[Int](1)
      _     <- queue.offer(1).fork.repeatN(numMessages)
      _     <- queue.take.repeatN(numMessages)
    yield ()

  @Benchmark
  def sendMessagesChanBuffered: Unit = executeZio:
    for
      chan <- Channel.make[Int](numMessages)
      _    <- chan.send(1).fork.repeatN(numMessages)
      _    <- chan.receive.repeatN(numMessages).ignore
    yield ()

  @Benchmark
  def sendMessagesQueueBuffered: Unit = executeZio:
    for
      queue <- Queue.bounded[Int](numMessages)
      _     <- queue.offer(1).fork.repeatN(numMessages)
      _     <- queue.take.repeatN(numMessages)
    yield ()
