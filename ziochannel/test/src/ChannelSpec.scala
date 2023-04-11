import zio.*
import zio.test.*
import zio.test.Assertion.*

import ziochannel.*

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
            _          <- Live.live(ZIO.sleep(100.millis))
            chanStatus <- chan.status
            result1    <- chan.receive
            result2    <- chan.receive
            _          <- f1.join
            _          <- f2.join
          yield assertTrue(result1 == Right(1), result2 == Right(1), chanStatus == Right(2))
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
          yield assertTrue(result1 == Right(1), result2 == Right(1))
        ,
        test("sender fiber gets blocked by sending to a channel without receivers"):
          for
            chan        <- Channel.make[Int]
            f1          <- chan.send(1).fork
            _           <- Live.live(ZIO.sleep(100.millis))
            fiberStatus <- f1.status
            chanStatus  <- chan.status
          yield assertTrue(chanStatus == Right(1), fiberStatus.isSuspended == true)
        ,
        test("receiver fiber gets blocked by receiving to a channel without senders"):
          for
            chan        <- Channel.make[Int]
            f1          <- chan.receive.fork
            _           <- Live.live(ZIO.sleep(100.millis))
            fiberStatus <- f1.status
            chanStatus  <- chan.status
            _           <- f1.interruptFork
          yield assertTrue(chanStatus == Right(-1), fiberStatus.isSuspended == true)
        ,
        test("one sender gets blocked and another unblocks after receive"):
          for
            chan         <- Channel.make[Int]
            f1           <- chan.send(1).fork
            f2           <- chan.send(1).fork
            _            <- chan.receive
            _            <- Live.live(ZIO.sleep(100.millis))
            fiber1Status <- f1.status
            fiber2Status <- f2.status
            _            <- f1.join
            f1Result      = fiber1Status.isSuspended
          yield assertTrue(
            fiber1Status.isSuspended == f1Result,
            fiber2Status.isSuspended == !f1Result,
          )
        ,
        test("one receiver gets blocked and another unblocks after send"):
          for
            chan         <- Channel.make[Int]
            f1           <- chan.receive.fork
            f2           <- chan.receive.fork
            _            <- chan.send(1)
            _            <- Live.live(ZIO.sleep(100.millis))
            fiber1Status <- f1.status
            fiber2Status <- f2.status
            _            <- f1.join
            f1Result      = fiber1Status.isSuspended
          yield assertTrue(
            fiber1Status.isSuspended == f1Result,
            fiber2Status.isSuspended == !f1Result,
          )
        ,
        test("receiving fibers are unblocked when channel is closed"):
          for
            chan            <- Channel.make[Int]
            f1              <- chan.receive.fork
            f2              <- chan.receive.fork
            _               <- Live.live(ZIO.sleep(100.millis))
            chanStatus      <- chan.status
            fiber1StatusBef <- f1.status
            fiber2StatusBef <- f2.status
            _               <- chan.close
            fiber1Status    <- f1.status
            fiber2Status    <- f2.status
          yield assertTrue(
            chanStatus == Right(-2),
            fiber1StatusBef.isSuspended == true,
            fiber2StatusBef.isSuspended == true,
            fiber1Status.isSuspended == false,
            fiber2Status.isSuspended == false,
          )
        ,
        test("sending fibers are unblocked when channel is closed"):
          for
            chan <- Channel.make[Int]
            f1   <- chan.send(1).fork
            f2   <- chan.send(1).fork
            _    <- Live.live(ZIO.sleep(100.millis))
            // _               <- ZIO.debug(s"Before close")
            chanStatus      <- chan.status
            fiber1StatusBef <- f1.status
            fiber2StatusBef <- f2.status
            _               <- chan.close
            _               <- Live.live(ZIO.sleep(100.millis))
            // _               <- ZIO.debug(s"After close")
            fiber1Status <- f1.status
            fiber2Status <- f2.status
          // _               <- ZIO.debug("Fiber 1", fiber1StatusBef)
          // _               <- ZIO.debug("Fiber 2", fiber2StatusBef)
          // _               <- ZIO.debug("Fiber 1", fiber1Status)
          // _               <- ZIO.debug("Fiber 2", fiber2Status)
          yield assertTrue(
            chanStatus == Right(2),
            fiber1StatusBef.isSuspended == true,
            fiber2StatusBef.isSuspended == true,
            // fiber1Status.isDone == true,
            fiber2Status.isSuspended == false,
          ),
      ),
      // Buffered channel tests
      suite("Buffered Channel")(
        test("two senders don't get blocked on a channel with capacity 2"):
          for
            chan         <- Channel.make[Int](2)
            f1           <- chan.send(1).fork
            f2           <- chan.send(1).fork
            _            <- Live.live(ZIO.sleep(100.millis))
            chanStatus1  <- chan.status
            fiber1Status <- f1.status
            fiber2Status <- f2.status
            _            <- f1.join
            _            <- f2.join
          yield assertTrue(
            fiber1Status.isSuspended == false,
            fiber2Status.isSuspended == false,
            chanStatus1 == Right(2),
          )
        ,
        test("third sender get blocked on a channel with capacity 2"):
          for
            chan        <- Channel.make[Int](2)
            _           <- chan.send(1)
            _           <- chan.send(1)
            f1          <- chan.send(1).fork
            _           <- Live.live(ZIO.sleep(100.millis))
            chanStatus1 <- chan.status
            fiberStatus <- f1.status
          yield assertTrue(
            fiberStatus.isSuspended == true,
            chanStatus1 == Right(3),
          ),
      ),
    ) @@ TestAspect.flaky
