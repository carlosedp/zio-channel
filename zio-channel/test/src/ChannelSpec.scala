import zio.*
import zio.test.*
import zio.test.Assertion.*

import zio.channel.*
import TestUtils.*

object ChannelSpec extends ZIOSpecDefault:

  def spec =
    suite("Channel")(
      // Direct channel (unbuffered) tests
      suite("Direct channel")(
        test("send forked / receive by main"):
          for
            chan   <- Channel.make[Int]
            f1     <- chan.send(1).fork
            result <- chan.receive
            _      <- f1.join
          yield assertTrue(result == Right(1))
        ,
        test("receive forked / send by main"):
          for
            chan   <- Channel.make[Int]
            f1     <- chan.receive.fork
            _      <- chan.send(1)
            result <- f1.join
          yield assertTrue(result == Right(1))
        ,
        test("multiple sends forked / receive by main"):
          for
            chan       <- Channel.make[Int]
            f1         <- chan.send(1).fork
            f2         <- chan.send(1).fork
            chanStatus <- waitForSize(chan, 2)
            result1    <- chan.receive
            result2    <- chan.receive
          yield assertTrue(result1 == Right(1), result2 == Right(1), chanStatus == 2)
        ,
        test("multiple receive forked / send by main"):
          for
            chan    <- Channel.make[Int]
            f1      <- chan.receive.fork
            f2      <- chan.receive.fork
            _       <- chan.send(1)
            _       <- chan.send(1)
            result1 <- f1.join
            result2 <- f2.join
          yield assertTrue(
            result1 == Right(1),
            result2 == Right(1),
          )
        ,
        test("sender fiber gets blocked by sending to a channel without receivers"):
          for
            chan        <- Channel.make[Int]
            f1          <- chan.send(1).fork
            chanStatus  <- waitForSize(chan, 1)
            fiberStatus <- waitUntilSuspended(f1)
          yield assertTrue(chanStatus == 1, fiberStatus == true)
        ,
        test("receiver fiber gets blocked by receiving to a channel without senders"):
          for
            chan        <- Channel.make[Int]
            f1          <- chan.receive.fork
            _           <- waitForSize(chan, 1)
            fiberStatus <- waitUntilSuspended(f1)
            chanStatus  <- chan.status
            _           <- f1.interruptFork
          yield assertTrue(chanStatus == Right(-1), fiberStatus == true)
        ,
        test("one sender gets blocked and another unblocks after receive"):
          for
            chan       <- Channel.make[Int]
            f1         <- chan.send(1).fork
            f2         <- chan.send(1).fork
            _          <- chan.receive
            chanStatus <- waitForSize(chan, 2)
            oneSusp    <- waitUntilEitherFiberIsSuspended(f1, f2)
          yield assertTrue(
            chanStatus == 2,
            oneSusp == true,
          )
        ,
        test("one receiver gets blocked and another unblocks after send"):
          for
            chan       <- Channel.make[Int]
            f1         <- chan.receive.fork
            f2         <- chan.receive.fork
            _          <- chan.send(1)
            chanStatus <- waitForSize(chan, -1)
            oneSusp    <- waitUntilEitherFiberIsSuspended(f1, f2)
          yield assertTrue(
            oneSusp == true,
            chanStatus == -1,
          )
        ,
        test("receiving fibers are unblocked when channel is closed"):
          for
            chan            <- Channel.make[Int]
            f1              <- chan.receive.fork
            f2              <- chan.receive.fork
            chanStatus      <- waitForSize(chan, -2)
            fiber1StatusBef <- waitUntilSuspended(f1)
            fiber2StatusBef <- waitUntilSuspended(f2)
            _               <- chan.close
            chanStatusAft   <- waitForSize(chan, 0)
            fiber1StatusAft <- waitUntilNotSuspended(f1)
            fiber2StatusAft <- waitUntilNotSuspended(f2)
          yield assertTrue(
            chanStatus == -2,
            chanStatusAft == 0,
            fiber1StatusBef == true,
            fiber2StatusBef == true,
            fiber1StatusAft == true,
            fiber2StatusAft == true,
          )
        ,
        test("sending fibers are unblocked when channel is closed")(
          for
            chan            <- Channel.make[Int]
            f1              <- chan.send(1).fork
            f2              <- chan.send(1).fork
            chanStatusBef   <- waitForSize(chan, 2)
            fiber1SuspBef   <- waitUntilSuspended(f1)
            fiber2SuspBef   <- waitUntilSuspended(f2)
            _               <- chan.close
            chanStatusAft   <- waitForSize(chan, 0)
            fiber1StatusAft <- f1.status
            fiber2StatusAft <- f2.status
          yield assertTrue(
            chanStatusBef == 2,
            fiber1SuspBef == true,
            fiber2SuspBef == true,
            chanStatusAft == 0,
            fiber1StatusAft.isSuspended == false,
            fiber1StatusAft.isSuspended == false,
          )
        ) @@ TestAspect.flaky, // TODO: Make this test more reliable using nonFlaky
      ),
      // Buffered channel tests
      suite("Buffered Channel")(
        test("two senders don't get blocked on a channel with capacity 2"):
          for
            chan         <- Channel.make[Int](2)
            f1           <- chan.send(1).fork
            f2           <- chan.send(1).fork
            chanStatus1  <- waitForSize(chan, 2)
            fiber1Status <- f1.status
            fiber2Status <- f2.status
            _            <- f1.join
            _            <- f2.join
          yield assertTrue(
            fiber1Status.isSuspended == false,
            fiber2Status.isSuspended == false,
            chanStatus1 == 2,
          )
        ,
        test("third sender get blocked on a channel with capacity 2"):
          for
            chan        <- Channel.make[Int](2)
            _           <- chan.send(1)
            _           <- chan.send(1)
            f1          <- chan.send(1).fork
            chanStatus1 <- waitForSize(chan, 3)
            fiberStatus <- waitUntilSuspended(f1)
          yield assertTrue(
            fiberStatus == true,
            chanStatus1 == 3,
          ),
      ),
    ) @@ TestAspect.nonFlaky

object TestUtils:
  def waitForValue[T](ref: UIO[Either[ChannelStatus, T]], value: T): UIO[Either[ChannelStatus, T]] =
    Live.live((ref <* Clock.sleep(2.millis)).repeatUntil(_ == value))

  def waitForSize[A](chan: Channel[A], size: Int): UIO[Int] =
    waitForValue(chan.status, size)
    ZIO.succeed(size)

  def waitUntilSuspended[A](fiber: Fiber.Runtime[ChannelStatus, A]): ZIO[Any, Nothing, Boolean] =
    Live.live((fiber.status <* Clock.sleep(2.millis)).repeatUntil(_.isSuspended).map(_.isSuspended))

  def waitUntilNotSuspended[A](fiber: Fiber.Runtime[ChannelStatus, A]) =
    Live.live(
      (fiber.status <* Clock.sleep(2.millis)).repeatUntil(!_.isSuspended).map(!_.isSuspended)
    )

  def waitUntilEitherFiberIsSuspended[A, B](
    fiber1: Fiber.Runtime[ChannelStatus, A],
    fiber2: Fiber.Runtime[ChannelStatus, B],
  ): ZIO[Any, Nothing, Boolean] =
    Live.live(
      (fiber1.status.zipWithPar(fiber2.status)(_.isSuspended || _.isSuspended) <* Clock.sleep(2.millis)).repeatUntil(
        _ == true
      )
    )
