//> using scala "3.3.0-RC3"
//> using lib "dev.zio::zio:2.0.11"

//> using file "../../ziochannel/src/Ziochannel.scala"
//> using file "../../ziochannel/src/Helpers.scala"

import zio.*
import ziochannel.*

object ZioChan1 extends ZIOAppDefault:
  val run =
    for
      channel <- Channel.make[String](2)
      _       <- Clock.sleep(200.millis) *> channel.status.debug("Status 0") // Sleep a bit before checking status
      _       <- Console.printLine("Sender 1 will send")
      s1      <- (channel.send("Hello") *> Console.printLine("Sender 1 unblocked")).fork
      _       <- Clock.sleep(200.millis) *> channel.status.debug("Status 1")
      _       <- Console.printLine("Sender 2 will send")
      s2      <- (channel.send("World") *> Console.printLine("Sender 2 unblocked")).fork
      _       <- Clock.sleep(200.millis) *> channel.status.debug("Status 2")
      r1      <- (channel.receive.debug("Receive 1") *> Console.printLine("Receiver 1 unblocked")).fork
      _       <- Clock.sleep(200.millis) *> channel.status.debug("Status 3")
      r2      <- (channel.receive.debug("Receive 2") *> Console.printLine("Receiver 2 unblocked")).fork
      _       <- Clock.sleep(200.millis) *> channel.status.debug("Status 4")
      _       <- s1.join
      _       <- s2.join
      _       <- r1.join
      _       <- r2.join
      _       <- Console.printLine("Done")
    yield ()
