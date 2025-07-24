# Zio-channel, Go-like channels for your ZIO application

Channel is a construct that provides a meeting point for two fibers to communicate. It requires both the sender and receiver to "meet" to exchange messages, blocking one until the other is available. Channels also can be constructed with a buffer to provide some capacity slots allowing the fibers to continue running until the channel gets full.

The concept comes from [Golang](https://go.dev/tour/concurrency/2) Channels, Erlang [message passing](https://www.erlang.org/blog/message-passing/) and also [Stackless Python](https://stackless.readthedocs.io/en/2.7-slp/library/stackless/channels.html) but now in a functional paradigm as a ZIO library for Scala applications.

The idea to create this came when I read Adam Warsky's [post](https://softwaremill.com/go-like-channels-using-project-loom-and-scala/) implementing this using Project Loom on Scala Ox lib.

Channels can be unbuffered, with only one slot or buffered with N slots. Fibers sending or receiving to this channel block if it's an unbuffered channel or once the buffered channel becomes full.

This is still a prototype with basic functionality to gather interest and ideas for improvements. The lib is currently built for Scala 3.3 on JVM, Scala Native and ScalaJs. I don't plan to support Scala 2.

[![zio-channel Scala version support](https://index.scala-lang.org/carlosedp/zio-channel/zio-channel/latest-by-scala-version.svg?platform=jvm)](https://index.scala-lang.org/carlosedp/zio-channel/zio-channel)
[![zio-channel Scala version support](https://index.scala-lang.org/carlosedp/zio-channel/zio-channel/latest-by-scala-version.svg?platform=native0.4)](https://index.scala-lang.org/carlosedp/zio-channel/zio-channel)
[![zio-channel Scala version support](https://index.scala-lang.org/carlosedp/zio-channel/zio-channel/latest-by-scala-version.svg?platform=sjs1)](https://index.scala-lang.org/carlosedp/zio-channel/zio-channel)
[![Sonatype Snapshots](https://img.shields.io/nexus/s/com.carlosedp/zio-channel_3?server=https%3A%2F%2Fs01.oss.sonatype.org)](https://s01.oss.sonatype.org/content/repositories/snapshots/com/carlosedp//zio-channel_3/)

[![Scala CI](https://github.com/carlosedp/zio-channel/actions/workflows/scala.yml/badge.svg)](https://github.com/carlosedp/zio-channel/actions/workflows/scala.yml)
[![codecov](https://codecov.io/gh/carlosedp/zio-channel/branch/main/graph/badge.svg?token=Wln0zziII9)](https://codecov.io/gh/carlosedp/zio-channel)[![Scaladoc](https://www.javadoc.io/badge/com.carlosedp/zio-channel_3.svg?color=blue&label=Scaladoc)](https://javadoc.io/doc/com.carlosedp/zio-channel_3/latest)

## Installation

Add to your `build.sbt` / `build.sc` / scala-cli:

```scala
// For Mill:
def ivyDeps = Agg(ivy"com.carlosedp::zio-channel:0.7.0")
// or for SBT:
libraryDependencies += "com.carlosedp" %% "zio-channel" % "0.7.0"
// or in scala-cli app:
//> using dep "com.carlosedp::zio-channel:0.7.0
```

## Usage

Below is a simple example that creates a one-slot channel (unbuffered) where the forked receiver blocks waiting for a message in the channel and gets unblocked when the main fiber sends a message allowing it to use the value and continue. Save the file locally and run with [scala-cli](https://scala-cli.virtuslab.org/):

```scala
//> using scala 3
//> using dep dev.zio::zio:2.1.20
//> using dep com.carlosedp::zio-channel:0.7.0

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
```

Run it with `scala-cli examples/src/sample-readme.scala`.

To create a multiple slot(buffered) channel where the fibers doesn't block sending or receiving to it until full, use `chan <- Channel.make[Int](5)`.

It's also possible to get the channel `status` to check the amount of messages waiting, positive for senders and negative for receivers and `close` a channel to remove all messages and unblock the waiting fibers.

There are some additional examples at [./examples/src/](./examples/src/) including `channel.select` to receive from a collection of channels. all of them can be run with scala-cli.

Check benchmarks comparing zio-channel with native ZIO queues for latest commit to main [here](https://jmh.morethan.io/?source=https://raw.githubusercontent.com/carlosedp/zio-channel/main/benchmark-files/jmh-result-latest.json). The benchmark shows the time taken to send and receive 1000 messages (smaller is better).
