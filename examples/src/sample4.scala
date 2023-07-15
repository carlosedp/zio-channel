//> using scala "3.3.0"
//> using lib "dev.zio::zio:2.0.15"

// //> using lib "com.carlosedp::zio-channel:0.5.0"
// Uncomment line above and remove lines below to use the published zio-channel lib
//> using file "../../zio-channel/src/Ziochannel.scala"
//> using file "../../zio-channel/src/Helpers.scala"

import zio.*
import zio.channel.*

object ZioChanSelect extends ZIOAppDefault:

  val program =
    for
      chan1 <- Channel.make[Int]
      chan2 <- Channel.make[Int]
      _     <- chan1.send(1).fork
      _     <- chan2.send(2).fork
      s     <- Channel.select(chan1, chan2)
      _     <- Console.printLine(s"Received $s")
    yield ()

  val run = program
