// Run with `scala-cli sample2.scala`

//> using scala "3.3.0-RC3"
//> using lib "dev.zio::zio:2.0.10"

//> using file "../../ziochannel/src/Ziochannel.scala"
//> using file "../../ziochannel/src/Helpers.scala"

import zio.*
import ziochannel.*
import java.io.IOException

def messageSender(channel: Channel[String]): ZIO[Any, IOException, Unit] =
  for
    _ <- channel.send("Hello")
    _ <- Console.printLine("Sender 1 unblocked")
    _ <- channel.send("World")
    _ <- Console.printLine("Sender 1 unblocked again")
    _ <- channel.close
    _ <- Console.printLine("Sender 1 closed channel")
  yield ()

def messageReceiver(channel: Channel[String]): ZIO[Any, IOException, Unit] =
  foreverWhile:
    for
      // Receive a message from the channel
      res <- ZIO.whenCaseZIO(channel.receive):
               case Right(data) =>
                 Console.printLine(s"Received: $data") *> ZIO.succeed(true)
               case Left(Closed) =>
                 Console.printLine(s"Channel closed") *> ZIO.succeed(false)
    yield res.get

object ZioChan2 extends ZIOAppDefault:
  val run =
    for
      channel <- Channel.make[String]
      fiberA  <- messageSender(channel).fork
      _       <- messageReceiver(channel).fork // No need to join since the loop will be interrupted by channel closing
      _       <- fiberA.join
      _       <- Console.printLine("Done")
    yield ()
