package zio.channel.benchmarks

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations.{Scope, *}
import zio.*
import zio.channel.*
import zio.profiling.jmh.BenchmarkUtils

@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Threads(1)
@Fork(1)
@Warmup(iterations = 10, timeUnit = TimeUnit.SECONDS, time = 1)
@Measurement(iterations = 10, timeUnit = TimeUnit.SECONDS, time = 1)
class Benchmarks:

  val numMessages = 1000

  private def executeZio[A](zio: Task[A]): A =
    BenchmarkUtils.unsafeRun(zio)

  // This benchmark sends and receives messages from a channel with a capacity of 1 which blocks each forked
  @Benchmark
  def sendMessagesChan: Unit = executeZio:
    for
      chan <- Channel.make[Int]
      _    <- chan.send(1).fork.repeatN(numMessages)
      _    <- chan.receive.repeatN(numMessages).ignore
    yield ()

  // This benchmark sends and receives messages from a queue with a capacity of 1 which blocks each forked
  @Benchmark
  def sendMessagesQueue: Unit = executeZio:
    for
      queue <- Queue.bounded[Int](1)
      _     <- queue.offer(1).fork.repeatN(numMessages)
      _     <- queue.take.repeatN(numMessages)
    yield ()

  // This benchmark sends and receives messages from a channel with a capacity of 1000 not blocking the sending fiber
  @Benchmark
  def sendMessagesChanBuffered: Unit = executeZio:
    for
      chan <- Channel.make[Int](numMessages + 1)
      _    <- chan.send(1).repeatN(numMessages).ignore
      _    <- chan.receive.repeatN(numMessages).ignore
    yield ()

  // This benchmark sends and receives messages from a queue with a capacity of 1000 not blocking the sending fiber
  @Benchmark
  def sendMessagesQueueBuffered: Unit = executeZio:
    for
      queue <- Queue.bounded[Int](numMessages + 1)
      _     <- queue.offer(1).repeatN(numMessages)
      _     <- queue.take.repeatN(numMessages)
    yield ()
