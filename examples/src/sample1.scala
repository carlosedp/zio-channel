//> using scala "3.3.0"
//> using lib "dev.zio::zio:2.0.15"

// //> using lib "com.carlosedp::zio-channel:0.5.4"
// Uncomment line above and remove lines below to use the published zio-channel lib
//> using file "../../zio-channel/src/Ziochannel.scala"
//> using file "../../zio-channel/src/Helpers.scala"

import zio.*
import zio.channel.*

object ZioChan1 extends ZIOAppDefault:
  val run =
    for
      chan <- Channel.make[Int]
      f1 <-
        (chan.receive.tap(i =>
          Console.printLine(s"Receiver 1 received $i and app will block now requiring Ctrl-C")
        ) *> Console.printLine("Receiver resumed")).fork
      _ <- Console.printLine("Sender 1 will send 1")
      _ <- chan.send(1)
      _ <- Console.printLine("Sender 1 will send 2")
      // <- Main fiber will block here on purpose requiring a Ctrl-C to stop the program
      _ <- chan.send(2)
      _ <- Console.printLine("Sender 1 will send 3")
      _ <- chan.send(3)
      _ <- Console.printLine("Sender 1 unblocked")
      _ <- f1.join
      _ <- Console.printLine("Done")
    yield ()
