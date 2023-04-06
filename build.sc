import mill._
import mill.scalalib._

object versions {
  val scala = "3.3.0-RC3"
  val zio   = "2.0.11"
}
trait Base extends ScalaModule {
  def scalaVersion  = versions.scala
  def scalacOptions = super.scalacOptions() ++ Seq("-source:future")
  def ivyDeps = super.ivyDeps() ++ Seq(
    ivy"dev.zio::zio:${versions.zio}",
  )

  object test extends Tests {
    def ivyDeps = super.ivyDeps() ++ Seq(
      ivy"dev.zio::zio:${versions.zio}",
    )
  }
}

object ziochannel extends Base
object examples extends Base {
  def moduleDeps = Seq(ziochannel)
}
