// Run with `scala-cli sample3.scala`

//> using scala 3
//> using dep dev.zio::zio:2.1.9

// //> using dep com.carlosedp::zio-channel:0.7.0
// Uncomment line above and remove lines below to use the published zio-channel lib
//> using file "../../zio-channel/src/Ziochannel.scala"
//> using file "../../zio-channel/src/Helpers.scala"

import zio.*
import zio.channel.*

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
      data <- channel.receive.catchAll(_ => ZIO.succeed(false))
      res  <- Console.printLine(s"Received: $data").ignore *> ZIO.succeed(true)
    yield res

object ZioChan3 extends ZIOAppDefault:
  val run =
    for
      channel <- Channel.make[String]
      fiberA  <- messageSender(channel).fork
      _       <- messageReceiver(channel).fork // No need to join since the loop will be interrupted by channel closing
      _       <- fiberA.join
      _       <- Console.printLine("Done")
    yield ()
