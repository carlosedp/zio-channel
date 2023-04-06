package ziochannel

import zio.*

/**
 * Runs the effect while the condition is true
 *
 * @param effect
 *   the effect to run that returns a boolean
 * @return
 *   a ZIO that runs the effect forever while the condition is true
 */
def foreverWhile[R, E](effect: ZIO[R, E, Boolean]): ZIO[R, E, Unit] =
  effect.flatMap: bool =>
    if bool then foreverWhile(effect)
    else ZIO.unit

// Use as follows:
// foreverWhile:
//   for
//     // Receive a message from the channel
//     res <- ZIO.whenCaseZIO(channel.receive):
//              case Right(data) =>
//                Console.printLine(s"Received: $data") *>
//                  ZIO.succeed(true)
//              case Left(Closed) =>
//                Console.printLine(s"Channel closed") *>
//                  ZIO.succeed(false)
//   yield res.get
