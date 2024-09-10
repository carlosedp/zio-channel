//> using scala 3
//> using dep dev.zio::zio:2.1.9

// //> using dep com.carlosedp::zio-channel:0.7.0
// Uncomment line above and remove lines below to use the published zio-channel lib
//> using file "../../zio-channel/src/Ziochannel.scala"
//> using file "../../zio-channel/src/Helpers.scala"

import zio.*
import zio.channel.*

object ZioChan0 extends ZIOAppDefault:
    val run =
        for
            chan <- Channel.make[Int]
            f1 <- (Console.printLine("Receiver 1 will block until it gets a message")
                *> chan.receive.tap(i => Console.printLine(s"Receiver 1 received $i"))
                *> Console.printLine("Receiver resumed")).fork
            _ <- Console.printLine("Sender 1 will send 1")
            _ <- chan.send(1)
            _ <- f1.join
            _ <- Console.printLine("Done")
        yield ()
