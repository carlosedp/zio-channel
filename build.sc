import mill._
import mill.scalalib._

trait Base extends ScalaModule {
  def scalaVersion  = "3.3.0-RC3"
  def scalacOptions = super.scalacOptions() ++ Seq("-source:future")
  def ivyDeps = super.ivyDeps() ++ Seq(
    ivy"dev.zio::zio:2.0.10",
  )

  object test extends Tests {
    def ivyDeps = super.ivyDeps() ++ Seq(
      ivy"dev.zio::zio:2.0.10",
    )
  }
}

object ziochannel extends Base
object examples extends Base {
  def moduleDeps = Seq(ziochannel)
}
