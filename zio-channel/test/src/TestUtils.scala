import zio.*
import zio.test.*
import zio.channel.*

object TestUtils:
  val sleepTime = 2.millis

  // Wait for a channel to have a certain value
  def waitForValue[T](ref: IO[ChannelStatus, T], value: T): IO[ChannelStatus, T] =
    Live.live((ref <* Clock.sleep(sleepTime)).repeatUntil(_ == value))

  // Wait for a channel to have a certain size
  def waitForSize[A](chan: Channel[A], size: Int): UIO[Int] =
    waitForValue(chan.status, size)
    ZIO.succeed(size)

  // Wait until a fiber to be suspended
  def waitUntilSuspended[A](fiber: Fiber.Runtime[ChannelStatus, A]): ZIO[Any, Nothing, Boolean] =
    Live.live((fiber.status <* Clock.sleep(sleepTime)).repeatUntil(_.isSuspended).map(_.isSuspended))

  // Wait until a fiber to not be suspended
  def waitUntilNotSuspended[A](fiber: Fiber.Runtime[ChannelStatus, A]) =
    Live.live(
      (fiber.status <* Clock.sleep(sleepTime)).repeatUntil(!_.isSuspended).map(!_.isSuspended)
    )

  // Wait until either fiber is suspended
  def waitUntilEitherFiberIsSuspended[A, B](
      fiber1: Fiber.Runtime[ChannelStatus, A],
      fiber2: Fiber.Runtime[ChannelStatus, B],
    ): ZIO[Any, Nothing, Boolean] =
    Live.live(
      (fiber1.status.zipWithPar(fiber2.status)(_.isSuspended || _.isSuspended) <* Clock.sleep(sleepTime)).repeatUntil(
        _ == true
      )
    )
