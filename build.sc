import mill._, mill.scalalib._, mill.scalalib.publish._
import mill.scalajslib._, mill.scalajslib.api._
import mill.scalanativelib._, mill.scalanativelib.api._
import scalafmt._

import $ivy.`com.lihaoyi::mill-contrib-scoverage:`
import mill.contrib.scoverage.{ScoverageModule, ScoverageReport}
import $ivy.`io.chris-kipp::mill-ci-release::0.1.5`
import io.kipp.mill.ci.release.{CiReleaseModule, SonatypeHost}
import $ivy.`com.github.lolgab::mill-crossplatform::0.1.5`
import com.github.lolgab.mill.crossplatform._

object versions {
  val scala3      = "3.3.0"
  val scalajs     = "1.13.1"
  val scalanative = "0.4.12"
  val zio         = "2.0.14"
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

trait CommonTests extends TestModule {
  def ivyDeps = Agg(
    ivy"dev.zio::zio-test::${versions.zio}",
    ivy"dev.zio::zio-test-sbt::${versions.zio}",
  )
  override def testFramework = T("zio.test.sbt.ZTestFramework")
}

object `zio-channel` extends CrossPlatform {
  trait Shared extends CrossPlatformScalaModule
    with Common
    with ScalafmtModule
    with Publish {
    // common settings here
  }
  object jvm extends Shared with ScoverageModule {
    // jvm specific settings here
    def scoverageVersion = versions.scoverage
    object test extends Tests with CommonTests with ScoverageTests
  }
  object js extends Shared with ScalaJSModule {
    def scalaJSVersion = versions.scalajs
    // js specific settings here
    object test extends Tests with CommonTests
  }
  object native extends Shared with ScalaNativeModule {
    // native specific settings here
    def scalaNativeVersion = versions.scalanative
    object test extends Tests with CommonTests
  }
}

object examples extends Common {
  def moduleDeps = Seq(`zio-channel`.jvm)
}

object scoverage extends ScoverageReport {
  override def scalaVersion     = versions.scala3
  override def scoverageVersion = versions.scoverage
}

// -----------------------------------------------------------------------------
// Command Aliases
// -----------------------------------------------------------------------------
// Alias commands are run like `./mill run [alias]`
// Define the alias as a map element containing the alias name and a Seq with the tasks to be executed
val aliases: Map[String, Seq[String]] = Map(
  "fmt"      -> Seq("mill.scalalib.scalafmt.ScalafmtModule/reformatAll __.sources"),
  "checkfmt" -> Seq("mill.scalalib.scalafmt.ScalafmtModule/checkFormatAll __.sources"),
  "deps"     -> Seq("mill.scalalib.Dependency/showUpdates"),
  "pub"      -> Seq("io.kipp.mill.ci.release.ReleaseModule/publishAll"),
  "publocal" -> Seq("ziochannel.publishLocal"),
  "testall"  -> Seq("__.test"),
  "coverage" -> Seq(s"__.test", "scoverage.htmlReportAll", "scoverage.xmlReportAll", "scoverage.consoleReportAll"),
)

def run(ev: eval.Evaluator, alias: String = "") = T.command {
  aliases.get(alias) match {
    case Some(t) =>
      mill.main.MainModule.evaluateTasks(ev, t.flatMap(x => Seq(x, "+")).flatMap(_.split("\\s+")).init, false)(identity)
    case None =>
      Console.err.println("Use './mill run [alias]'."); Console.out.println("Available aliases:")
      aliases.foreach(x => Console.out.println(s"${x._1.padTo(15, ' ')} - Commands: (${x._2.mkString(", ")})"));
      sys.exit(1)
  }
}
