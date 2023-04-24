// Run with `scala-cli sample3.scala`

//> using scala "3.3.0-RC4"
//> using lib "dev.zio::zio:2.0.1"

//> using file "../../zio-channel/src/Ziochannel.scala"
//> using file "../../zio-channel/src/Helpers.scala"

import zio.*
import zio.channel.*
import java.io.IOException

def messageSender(channel: Channel[String]): ZIO[Any, ChannelStatus, Unit] =
  for
    _ <- channel.send("Hello")
    _ <- Console.printLine("Sender 1 unblocked").ignore
    _ <- channel.send("World")
    _ <- Console.printLine("Sender 1 unblocked again").ignore
    _ <- channel.close
    _ <- Console.printLine("Sender 1 closed channel").ignore
  yield ()

def messageReceiver(channel: Channel[String]): ZIO[Any, Nothing, Unit] =
  foreverWhile:
    for
      // I'll keep receiving messages until the channel is closed
      res <- ZIO.whenCaseZIO(channel.receive):
               case Right(data) =>
                 Console.printLine(s"Received: $data").ignore *> ZIO.succeed(true)
               case Left(Closed) =>
                 Console.printLine(s"Received: Channel closed").ignore *> ZIO.succeed(false)
    yield res.get

object ZioChan3 extends ZIOAppDefault:
  val run =
    for
      channel <- Channel.make[String]
      fiberA  <- messageSender(channel).fork
      _       <- messageReceiver(channel).fork // No need to join since the loop will be interrupted by channel closing
      _       <- fiberA.join
      _       <- Console.printLine("Done")
    yield ()
