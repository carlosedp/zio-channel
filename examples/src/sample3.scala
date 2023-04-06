//> using scala "3.3.0-RC3"
//> using lib "dev.zio::zio:2.0.10"

import zio.*

object ZioChan3 extends ZIOAppDefault:
  val run =
    for
      queue <- Queue.bounded[Int](1)
      _     <- Console.printLine("Sender 1 will send")
      _     <- queue.offer(1)
      _     <- Console.printLine("Sender 1 will send again")
      _     <- queue.offer(2)
      _     <- Console.printLine("Sender 1 unblocked")
      _     <- Console.printLine("Done")
    yield ()
