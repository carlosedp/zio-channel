import mill._, scalalib._
import scalafmt._, publish._
import scalajslib._, scalanativelib._

import $ivy.`com.lihaoyi::mill-contrib-scoverage:$MILL_VERSION`
import mill.contrib.scoverage._
import $ivy.`io.chris-kipp::mill-ci-release::0.1.9`
import io.kipp.mill.ci.release.{CiReleaseModule, SonatypeHost}
import $ivy.`com.github.lolgab::mill-crossplatform::0.2.3`
import com.github.lolgab.mill.crossplatform._
import $ivy.`com.goyeau::mill-scalafix::0.3.1`
import com.goyeau.mill.scalafix.ScalafixModule
import $ivy.`com.carlosedp::mill-aliases::0.2.1`
import com.carlosedp.aliases._

object versions {
  val scala3      = "3.3.0"
  val scalajs     = "1.13.2"
  val scalanative = "0.4.14"
  val zio         = "2.0.15"
  val scoverage   = "2.0.8"
}

trait Publish extends CiReleaseModule {
  def pomSettings = PomSettings(
    description = "Zio-channel is a Go-like channel implementation for ZIO",
    organization = "com.carlosedp",
    url = "https://github.com/carlosedp/zio-channel",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github("carlosedp", "zio-channel"),
    developers = Seq(
      Developer("carlosedp", "Carlos Eduardo de Paula", "https://github.com/carlosedp")
    ),
  )
  override def sonatypeHost = Some(SonatypeHost.s01)
}

trait Common extends ScalaModule {
  def scalaVersion = versions.scala3
  override def scalacOptions = T {
    super.scalacOptions() ++ Seq("-Wunused:all", "-Wvalue-discard", "-source:future")
  }
  def ivyDeps = Agg(
    ivy"dev.zio::zio:${versions.zio}"
  )
}

trait CommonTests extends TestModule.ZioTest {
  def ivyDeps = Agg(
    ivy"dev.zio::zio-test::${versions.zio}",
    ivy"dev.zio::zio-test-sbt::${versions.zio}",
  )
}

object `zio-channel` extends CrossPlatform {
  trait Shared extends CrossPlatformScalaModule
    with Common
    with ScalafmtModule
    with ScalafixModule
    with Publish {
    // common settings here
  }
  object jvm extends Shared with ScoverageModule {
    // jvm specific settings here
    def scoverageVersion = versions.scoverage
    object test extends ScalaTests with CommonTests with ScoverageTests
  }
  object js extends Shared with ScalaJSModule {
    def scalaJSVersion = versions.scalajs
    // js specific settings here
    object test extends ScalaTests with CommonTests
  }
  object native extends Shared with ScalaNativeModule {
    // native specific settings here
    def scalaNativeVersion = versions.scalanative
    object test extends ScalaTests with CommonTests
  }
}

object examples extends Common {
  def moduleDeps = Seq(`zio-channel`.jvm)
}

object scoverage extends ScoverageReport {
  override def scalaVersion     = versions.scala3
  override def scoverageVersion = versions.scoverage
}

object MyAliases extends Aliases {
  def fmt      = alias("mill.scalalib.scalafmt.ScalafmtModule/reformatAll __.sources")
  def checkfmt = alias("mill.scalalib.scalafmt.ScalafmtModule/checkFormatAll __.sources")
  def lint     = alias("mill.scalalib.scalafmt.ScalafmtModule/reformatAll __.sources", "__.fix")
  def deps     = alias("mill.scalalib.Dependency/showUpdates")
  def pub      = alias("io.kipp.mill.ci.release.ReleaseModule/publishAll")
  def publocal = alias("zio-channel.__.publishLocal")
  def testall  = alias("__.test")
  def coverage = alias(s"__.test", "scoverage.htmlReportAll", "scoverage.xmlReportAll", "scoverage.consoleReportAll")
}
