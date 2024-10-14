package zio.channel.benchmarks

import java.util.concurrent.TimeUnit

import zio.*
import zio.channel.*
import zio.profiling.jmh.BenchmarkUtils

import org.openjdk.jmh.annotations.{Scope, *}

@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Threads(1)
@Fork(1)
@Warmup(iterations = 5, timeUnit = TimeUnit.SECONDS, time = 1)
@Measurement(iterations = 5, timeUnit = TimeUnit.SECONDS, time = 1)
class Benchmarks:
  val numMessages = 10000

  private def executeZio[A](zio: Task[A]): A =
    BenchmarkUtils.unsafeRun(zio)

  // This benchmark sends and receives messages from a zio-channel with a capacity of 1 which blocks each forked
  @Benchmark
  def sendMessagesZioChannel: Unit = executeZio:
    for
      chan <- Channel.make[Int]
      _    <- chan.send(1).fork.repeatN(numMessages)
      _    <- chan.receive.repeatN(numMessages).ignore
    yield ()

  // This benchmark sends and receives messages from a native ZIO queue with a capacity of 1 which blocks each forked
  @Benchmark
  def sendMessagesZioQueue: Unit = executeZio:
    for
      queue <- Queue.bounded[Int](1)
      _     <- queue.offer(1).fork.repeatN(numMessages)
      _     <- queue.take.repeatN(numMessages)
    yield ()

  // This benchmark sends and receives messages from a zio-channel with a capacity of 1000 not blocking the sending fiber
  @Benchmark
  def sendMessagesZioChannelBuffered: Unit = executeZio:
    for
      chan <- Channel.make[Int](numMessages + 1)
      _    <- chan.send(1).repeatN(numMessages).ignore
      _    <- chan.receive.repeatN(numMessages).ignore
    yield ()

  // This benchmark sends and receives messages from a native ZIO queue with a capacity of 1000 not blocking the sending fiber
  @Benchmark
  def sendMessagesZioQueueBuffered: Unit = executeZio:
    for
      queue <- Queue.bounded[Int](numMessages + 1)
      _     <- queue.offer(1).repeatN(numMessages)
      _     <- queue.take.repeatN(numMessages)
    yield ()
end Benchmarks
