//> using scala 3
//> using dep dev.zio::zio:2.0.21
//> using dep com.carlosedp::zio-channel:0.5.5

import zio.*
import zio.channel.*

object ZioChannel extends ZIOAppDefault:
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
