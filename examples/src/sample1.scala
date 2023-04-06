//> using scala "3.3.0-RC3"
//> using lib "dev.zio::zio:2.0.10"

//> using file "../../ziochannel/src/Ziochannel.scala"
//> using file "../../ziochannel/src/Helpers.scala"

import zio.*
import ziochannel.*

object ZioChan3 extends ZIOAppDefault:
  val run =
    for
      chan <- Channel.make[Int]
      _    <- Console.printLine("Sender 1 will send")
      _    <- chan.send(1)
      _    <- Console.printLine("Sender 1 will send again")
      _    <- chan.send(2)
      _    <- Console.printLine("Sender 1 unblocked")
      _    <- Console.printLine("Done")
    yield ()
  // val run =
  //   for
  //     queue <- Queue.bounded[Int](1)
  //     _     <- Console.printLine("Sender 1 will send")
  //     _     <- queue.offer(1)
  //     _     <- Console.printLine("Sender 1 will send again")
  //     _     <- queue.offer(2)
  //     _     <- Console.printLine("Sender 1 unblocked")
  //     _     <- Console.printLine("Done")
  //   yield ()
