//> using scala "3.3.0-RC3"
//> using lib "dev.zio::zio:2.0.11"

//> using file "../../ziochannel/src/Ziochannel.scala"
//> using file "../../ziochannel/src/Helpers.scala"

import zio.*
import ziochannel.*

object ZioChan3 extends ZIOAppDefault:
  val run =
    for
      chan <- Channel.make[Int]
      _    <- Console.printLine("Sender 1 will send 1")
      f1   <- chan.receive.tap(i => Console.printLine(s"Receiver 1 received $i")).fork
      _    <- chan.send(1)
      _ <- Console.printLine(
             "Sender 1 will send 2",
           ) // <- Main fiber will block here if capacity is 1 or there is only one receiver
      _ <- chan.send(2)
      _ <- Console.printLine("Sender 1 will send 3")
      _ <- chan.send(3)
      _ <- Console.printLine("Sender 1 unblocked")
      _ <- f1.join
      _ <- Console.printLine("Done")
    yield ()
