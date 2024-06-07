import zio.*
import zio.test.*
import zio.test.Assertion.*

import zio.channel.*

object HelpersSpec extends ZIOSpecDefault:
    def spec =
        suiteAll("Helpers"):
            suiteAll("foreverWhile"):
                test("foreverWhile loop should finish"):
                    for
                        res <- foreverWhile:
                            for
                                r <- ZIO.succeed(false)
                            yield r
                    yield assertCompletes

                test("foreverWhile loop should finish after 10 loops"):
                    var counter: Int = 0
                    for
                        res <- foreverWhile:
                            for
                                r <- ZIO.succeed {
                                    counter += 1
                                    counter < 10
                                }
                            yield r
                    yield assert(counter)(equalTo(10))
end HelpersSpec
