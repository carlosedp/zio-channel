import mill._, scalalib._
import scalafmt._, publish._
import scalajslib._, scalanativelib._

import $ivy.`com.lihaoyi::mill-contrib-scoverage:$MILL_VERSION`
import mill.contrib.scoverage._
import $ivy.`com.lihaoyi::mill-contrib-jmh:$MILL_VERSION`
import contrib.jmh.JmhModule
import $ivy.`io.chris-kipp::mill-ci-release::0.1.9`
import de.tobiasroeser.mill.vcs.version.VcsVersion
import io.kipp.mill.ci.release.{CiReleaseModule, SonatypeHost}
import $ivy.`com.github.lolgab::mill-crossplatform::0.2.4`
import com.github.lolgab.mill.crossplatform._
import $ivy.`com.goyeau::mill-scalafix::0.3.1`
import com.goyeau.mill.scalafix.ScalafixModule
import $ivy.`com.carlosedp::mill-aliases::0.4.1`
import com.carlosedp.aliases._
import $ivy.`io.github.davidgregory084::mill-tpolecat::0.3.5`
import io.github.davidgregory084.TpolecatModule

object versions {
  val scala3      = "3.3.1"
  val scalajs     = "1.13.2"
  val scalanative = "0.4.15"
  val zio         = "2.0.16"
  val scoverage   = "2.0.8"
  val jmh         = "1.37"
  val ziojmh      = "0.2.1"
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
  def publishVersion = T {
    val isTag = T.ctx().env.get("GITHUB_REF").exists(_.startsWith("refs/tags"))
    val state = VcsVersion.vcsState()
    if (state.commitsSinceLastTag == 0 && isTag) {
      state.stripV(state.lastTag.get)
    } else {
      val v = state.stripV(state.lastTag.get).split('.')
      s"${v(0)}.${(v(1).toInt) + 1}-SNAPSHOT"
    }
  }
  override def sonatypeHost = Some(SonatypeHost.s01)
}

trait Common extends ScalaModule
    with ScalafmtModule
    with ScalafixModule
    with TpolecatModule {
  def scalaVersion = versions.scala3
  def scalacOptions = T {
    super.scalacOptions() ++ Seq("-Wunused:all", "-Wvalue-discard")
  }
  def ivyDeps = Agg(
    ivy"dev.zio::zio:${versions.zio}"
  )
  def scalafixIvyDeps = super.scalacPluginIvyDeps() ++ Agg(ivy"com.github.xuwei-k::scalafix-rules:0.3.0")
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
      with Publish {
    def artifactName = "zio-channel"
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

object benchmarks extends Common with JmhModule {
  def jmhCoreVersion = versions.jmh
  def moduleDeps     = Seq(`zio-channel`.jvm)
  def ivyDeps        = super.ivyDeps() ++ Agg(ivy"dev.zio::zio-profiling-jmh:${versions.ziojmh}")
  def copyResultJson = T {
    val id =
      os.proc("git", "log", "-1", "--format=%h-%cd", "--date=format:%Y-%m-%dT%H:%M:%S").call().out.text().trim()
    val benchfile = s"jmh-result-${id}.json"
    os.copy.over(
      T.dest / os.up / "runJmh.dest" / "jmh-result.json",
      os.pwd / "benchmark-files" / benchfile,
    )
    os.copy.over(
      T.dest / os.up / "runJmh.dest" / "jmh-result.json",
      os.pwd / "benchmark-files" / "jmh-result-latest.json",
    )

  }
}

object scoverage extends ScoverageReport {
  def scalaVersion     = versions.scala3
  def scoverageVersion = versions.scoverage
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
  def bench    = alias(s"benchmarks.runJmh -rf json", "benchmarks.copyResultJson")
}
