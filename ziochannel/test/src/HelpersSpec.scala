import zio.*
import zio.test.*
import zio.test.Assertion.*

import ziochannel.*

object HelpersSpec extends ZIOSpecDefault:

  def spec =
    suite("Helpers")(
      suite("foreverWhile")(
        test("foreverWhile loop should finish"):
          for
            res <- foreverWhile:
                     for
                       r <- ZIO.succeed(false)
                     yield r
          yield assertCompletes,
      ),
    )
