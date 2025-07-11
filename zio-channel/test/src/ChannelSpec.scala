import zio.*
import zio.test.*

import zio.channel.*
import TestUtils.*

object ChannelSpec extends ZIOSpecDefault:

  def spec =
    suiteAll("Channel"):
      // Direct channel (unbuffered) tests
      suiteAll("Direct channel"):
        test("send forked / receive by main"):
          for
            chan   <- Channel.make[Int]
            f1     <- chan.send(1).fork
            result <- chan.receive
            _      <- f1.join
          yield assertTrue(result == 1)

        test("receive forked / send by main"):
          for
            chan   <- Channel.make[Int]
            f1     <- chan.receive.fork
            _      <- chan.send(1)
            result <- f1.join
          yield assertTrue(result == 1)

        test("multiple sends forked / receive by main"):
          for
            chan       <- Channel.make[Int]
            f1         <- chan.send(1).fork
            f2         <- chan.send(1).fork
            chanStatus <- waitForSize(chan, 2)
            result1    <- chan.receive
            result2    <- chan.receive
          yield assertTrue(result1 == 1, result2 == 1, chanStatus == 2)

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
            result1 == 1,
            result2 == 1,
          )

        test("sender fiber gets blocked by sending to a channel without receivers"):
          for
            chan        <- Channel.make[Int]
            f1          <- chan.send(1).fork
            chanStatus  <- waitForSize(chan, 1)
            fiberStatus <- waitUntilSuspended(f1)
          yield assertTrue(chanStatus == 1, fiberStatus == true)

        test("receiver fiber gets blocked by receiving to a channel without senders"):
          for
            chan        <- Channel.make[Int]
            f1          <- chan.receive.fork
            _           <- waitForSize(chan, 1)
            fiberStatus <- waitUntilSuspended(f1)
            chanStatus  <- chan.status
            _           <- f1.interruptFork
          yield assertTrue(chanStatus == -1, fiberStatus == true)

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

        test("sending fiber is unblocked when channel is closed"):
          for
            chan             <- Channel.make[Int]
            f1               <- (chan.send(1) *> ZIO.succeed("done")).fork
            chanStatusBef    <- waitForSize(chan, 1)
            fiber1SuspBef    <- waitUntilSuspended(f1)
            _                <- chan.close
            chanStatusAft    <- waitForSize(chan, 0)
            fiber1NotSuspAft <- waitUntilNotSuspended(f1)
            f1Data           <- f1.join
          yield assertTrue(
            chanStatusBef == 1,
            fiber1SuspBef == true,
            chanStatusAft == 0,
            fiber1NotSuspAft == true,
            f1Data == "done",
          )

      // Buffered channel tests
      suiteAll("Buffered Channel"):
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
          )
      // Select tests
      suiteAll("Select"):
        test("select message from a channel"):
          for
            chan <- Channel.make[Int]
            _    <- chan.send(1).fork
            f1   <- Channel.select(chan).fork
            s1   <- f1.join
          yield assertTrue(
            s1 == 1
          )

        test("select message from a channel with two messages"):
          for
            chan <- Channel.make[Int]
            _    <- chan.send(1).repeatN(2).fork
            f1   <- Channel.select(chan).fork
            s1   <- f1.join
            f2   <- Channel.select(chan).fork
            s2   <- f2.join
          yield assertTrue(
            s1 == 1,
            s2 == 1,
          )

        test("select message from two channels where message comes from first channel"):
          for
            chan1 <- Channel.make[Int]
            chan2 <- Channel.make[Int]
            _     <- chan1.send(1).fork
            s1    <- Channel.select(chan1, chan2)
          yield assertTrue(
            s1 == 1
          )

        test("select message from a Seq containing two channels"):
          for
            chan1 <- Channel.make[Int]
            chan2 <- Channel.make[Int]
            _     <- chan1.send(1).fork
            s1    <- Channel.select(Seq(chan1, chan2))
          yield assertTrue(
            s1 == 1
          )

        test("select message from two channels where message comes from second channel"):
          for
            chan1 <- Channel.make[Int]
            chan2 <- Channel.make[Int]
            _     <- chan2.send(2).fork
            f1    <- Channel.select(chan1, chan2).fork
            s1    <- f1.join
          yield assertTrue(
            s1 == 2
          )

        test("select first message between two channels"):
          for
            chan1 <- Channel.make[Int]
            chan2 <- Channel.make[Int]
            _     <- chan1.send(1).fork
            _     <- chan2.send(2).fork
            f1    <- Channel.select(chan1, chan2).fork
            s1    <- f1.join
          yield assertTrue(
            s1 == 1 || s1 == 2 // order is not guaranteed
          )

        test("select multiple messages between two channels"):
          for
            chan1 <- Channel.make[Int]
            chan2 <- Channel.make[Int]
            _     <- chan1.send(1).fork
            _     <- chan2.send(2).fork
            s1    <- Channel.select(chan1, chan2)
            s2    <- Channel.select(chan1, chan2)
          yield assertTrue(
            // order is not guaranteed so if s1 == 1, s2 == 2
            s1 == 1 && s2 == 2 || s1 == 2 && s2 == 1
          )

        test("select multiple messages between two channels in different order"):
          for
            chan1 <- Channel.make[Int]
            chan2 <- Channel.make[Int]
            _     <- chan1.send(1).fork
            _     <- chan1.send(1).fork
            _     <- chan2.send(2).fork
            s1    <- Channel.select(chan1, chan2)
            s2    <- Channel.select(chan1, chan2)
            s3    <- Channel.select(chan1, chan2)
          yield assertTrue(
            // order is not guaranteed
            s1 == 1 && s2 == 2 && s3 == 1
              || s1 == 1 && s2 == 1 && s3 == 2
              || s1 == 2 && s2 == 1 && s3 == 1
          )

        test("make sure select and receive are not blocking each other"):
          for
            chan1  <- Channel.make[Int]
            chan2  <- Channel.make[Int]
            _      <- chan1.send(1).fork
            _      <- chan2.send(2).fork
            s1     <- chan1.receive
            s2     <- Channel.select(chan1, chan2)
            status <- chan1.status
          yield assertTrue(
            s1 == 1,
            s2 == 2,
            status == 0,
          )

        test("select on channels with no messages should block"):
          for
            chan1 <- Channel.make[Int]
            chan2 <- Channel.make[Int]
            f1    <- Channel.select(chan1, chan2).fork
            s1    <- waitUntilSuspended(f1)
          yield assertTrue(
            s1 == true
          )

        test("select on channels with no messages should block and then unblock when message is sent"):
          for
            chan1 <- Channel.make[Int]
            chan2 <- Channel.make[Int]
            f1    <- Channel.select(chan1, chan2).fork
            s1    <- waitUntilSuspended(f1)
            _     <- chan1.send(1)
            s2    <- f1.join
          yield assertTrue(
            s1 == true,
            s2 == 1,
          )

        test("trySend on buffered channel should succeed without blocking"):
          for
            chan      <- Channel.make[Int](2)
            result1   <- chan.trySend(1)
            result2   <- chan.trySend(2)
            result3   <- chan.trySend(3) // Should fail as capacity is 2
            received1 <- chan.tryReceive
            received2 <- chan.tryReceive
            received3 <- chan.tryReceive
          yield assertTrue(
            result1 == true,
            result2 == true,
            result3 == false,
            received1 == Some(1),
            received2 == Some(2),
            received3 == None,
          )

        test("trySend on unbuffered channel should fail"):
          for
            chan   <- Channel.make[Int](0)
            result <- chan.trySend(1)
          yield assertTrue(result == false)

        test("tryReceive on empty channel should return None"):
          for
            chan   <- Channel.make[Int](1)
            result <- chan.tryReceive
          yield assertTrue(result == None)
    @@ TestAspect.timeout(10.seconds)
end ChannelSpec
