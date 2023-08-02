//> using scala "3.3.0"
//> using lib "dev.zio::zio:2.0.15"
//> using lib "com.carlosedp::zio-channel:0.5.4"

import zio.*
import zio.channel.*

object ZioChanel extends ZIOAppDefault:
  val run =
    for
      chan <- Channel.make[String]
      // This receiver will block waiting for messages
      f1 <- (chan.receive.tap(i => Console.printLine(s"Receiver received $i")) *>
        Console.printLine("Receiver resumed")).fork
      _ <- Console.printLine("Sender in main fiber will send message...")
      _ <- chan.send("Hi receiver 1!") // This will send and unblock the receiver
      _ <- f1.join
      _ <- Console.printLine("Done")
    yield ()
