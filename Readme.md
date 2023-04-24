# Ziochannel, Go-like channels for your ZIO application

The concept behind this lib is to have a Golang / Erlang / Stackless Python like channels for your ZIO Scala application.

The development has been influenced in the implementations for previous languages and a [recent post](https://softwaremill.com/go-like-channels-using-project-loom-and-scala/) from Adam Warsky from SoftwareMill implementing this using Project Loom.

Channels can be unbuffered, with only one position or buffered with N positions. Fibers sending or receiving to this channel block if it's an unbuffered channel or once the buffered channel becomes full.

This is still a prototype with basic functionality to gather interest and ideas for improvements. Since this is a prototype, it's currently built only for Scala 3.3 on JVM. If there is interest, I can add other targets. I don't plan to support Scala 2.

[![zio-channel Scala version support](https://index.scala-lang.org/carlosedp/zio-channel/zio-channel/latest-by-scala-version.svg?platform=jvm)](https://index.scala-lang.org/carlosedp/zio-channel/zio-channel)
[![Scala CI](https://github.com/carlosedp/zio-channel/actions/workflows/scala.yml/badge.svg)](https://github.com/carlosedp/riscvassembler/actions/workflows/scala.yml)
[![codecov](https://codecov.io/gh/carlosedp/zio-channel/branch/main/graph/badge.svg?token=Wln0zziII9)](https://codecov.io/gh/carlosedp/zio-channel)[![Scaladoc](https://www.javadoc.io/badge/com.carlosedp/zio-channel_3.svg?color=blue&label=Scaladoc)](https://javadoc.io/doc/com.carlosedp/zio-channel_3/latest)



## Installation

Add to your `build.sbt` / `build.sc` / scala-cli:

```scala
// For Mill:
def ivyDeps = Agg(ivy"com.carlosedp::zio-channel:<version>")
// or for SBT:
libraryDependencies += "com.carlosedp" %% "zio-channel" % "<version>"
// or in scala-cli app:
//> using lib "com.carlosedp::zio-channel:<version>"
```

## Usage

Below is a simple example that creates a one-slot channel (unbuffered) where the forked receiver blocks waiting for a message in the channel and gets unblocked when the main fiber sends a message allowing it to use the value and continue. Save the file locally and run with [scala-cli](https://scala-cli.virtuslab.org/):

```scala
//> using scala "3.3.0-RC4"
//> using lib "dev.zio::zio:2.0.13"
//> using lib "com.carlosedp::zio-channel:0.1.0"
import zio.*
import zio.channel.*

object ZioChanel extends ZIOAppDefault:
  val run =
    for
      chan <- Channel.make[Int]
      // This receiver will block waiting for messages
      f1   <- {chan.receive.tap(i => Console.printLine(s"Receiver 1 received $i")) *> Console.printLine("Receiver resumed")}.fork
      _    <- Console.printLine("Sender 1 will send 1")
      _    <- chan.send(1) // This will send and unblock the receiver
      _ <- Console.printLine("Done")
    yield ()
```

Run it with `scala-cli ziochannel.scala`.

Channels also can be created with multiple slots (buffered) where the fibers doesn't block sending or receiving to it until full. To create a 5 slot buffered channel, use `chan <- Channel.make[Int](5)`.

It's also possible to get the channel `status` checking the amount of messages waiting, positive for senders and negative for receivers and `close` a channel to remove all messages and unblock the waiting fibers.

There are some additional examples at [./examples/src/](./examples/src/) which can be run with scala-cli.

## Missing / Future features

- [ ] Better error handling
- [ ] Timeouts sending/receiving
- [ ] Select from multiple channels
