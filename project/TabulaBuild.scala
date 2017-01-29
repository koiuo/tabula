import sbt._
import Keys._
import com.typesafe.sbt.SbtScalariform._
import scalariform.formatter.preferences._

object Versions {
  val ScalaVersion211 = "2.11.8"
  val JodaTimeVersion = "2.9.7"
  val JodaConvertVersion = "1.8.1"
  val ShapelessVersion = "2.3.2"
  val PoiVersion = "3.14"
  val Json4sVersion = "3.5.0"
  val CommonsLangVersion = "3.5"
  val SpecsVersion = "3.8.7"
  val CatsVersion = "0.8.1"
}

object BuildSettings {
  import Versions._

  def prompt(state: State) =
    "[%s]> ".format(Project.extract(state).currentProject.id)

  lazy val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := "com.bumnetworks",
    version := "0.1.3",
    scalaVersion := ScalaVersion211,
    scalacOptions ++= Seq("-deprecation",  "-unchecked", "-feature", "-language:implicitConversions", "-language:reflectiveCalls"),
    shellPrompt := prompt,
    showTiming := true,
    parallelExecution := true,
    parallelExecution in Test := false,
    testFrameworks += TestFrameworks.Specs,
    libraryDependencies += "org.specs2" %% "specs2-core" % SpecsVersion % "test",
    offline := false,
    resolvers += Resolver.sonatypeRepo("snapshots"),
    initialCommands in console in Test := """
    import tabula._, Tabula._, Column._
    import shapeless._
    """) ++ scalariformSettings ++ formatSettings

  lazy val publishSettings = Seq(
    publishTo <<= (version) {
      v =>
      val repo = file(".") / ".." / "repo"
      Some(Resolver.file("repo",
        if (v.trim.endsWith("SNAPSHOT")) repo / "snapshots"
        else repo / "releases"))
    }
  )

  lazy val formatSettings = Seq(
    ScalariformKeys.preferences in Compile := formattingPreferences,
    ScalariformKeys.preferences in Test    := formattingPreferences
  )

  lazy val formattingPreferences = {
    FormattingPreferences().
    setPreference(AlignParameters, true).
    setPreference(AlignSingleLineCaseStatements, true).
    setPreference(CompactControlReadability, true).
    setPreference(CompactStringConcatenation, true).
    setPreference(DoubleIndentClassDeclaration, true).
    setPreference(FormatXml, true).
    setPreference(IndentLocalDefs, true).
    setPreference(IndentPackageBlocks, true).
    setPreference(IndentSpaces, 2).
    setPreference(MultilineScaladocCommentsStartOnFirstLine, true).
    setPreference(PlaceScaladocAsterisksBeneathSecondAsterisk, true).
    setPreference(PreserveSpaceBeforeArguments, false).
    setPreference(PreserveDanglingCloseParenthesis, false).
    setPreference(RewriteArrowSymbols, false).
    setPreference(SpaceBeforeColon, false).
    setPreference(SpaceInsideBrackets, false).
    setPreference(SpacesWithinPatternBinders, true)
  }
}

object Deps {
  import Versions._

  val cats = "org.typelevel" %% "cats" % CatsVersion % "provided"
  val joda_time = "joda-time" % "joda-time" % JodaTimeVersion % "provided"
  val joda_convert = "org.joda" % "joda-convert" % JodaConvertVersion % "provided"
  val commons_lang = "org.apache.commons" % "commons-lang3" % CommonsLangVersion % "test"
  val poi = "org.apache.poi" % "poi" % PoiVersion % "provided"
  val json4s = "org.json4s" %% "json4s-native" % Json4sVersion % "provided"
  val shapeless = "com.chuusai" %% "shapeless" % ShapelessVersion % "provided"
  val Reflection = Seq(libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value)

  val CoreDeps = Seq(cats, joda_time, joda_convert, commons_lang, shapeless)
  val JsonDeps = Seq(json4s)
  val ExcelDeps = Seq(poi)
}

object TabulaBuild extends Build {
  import BuildSettings._
  import Deps._

  lazy val root = Project(
    id = "tabula", base = file("."),
    settings = buildSettings ++ Seq(publish := {})
  ) aggregate(core, json, excel)

  lazy val core = Project(
    id = "tabula-core", base = file("core"),
    settings = buildSettings ++ publishSettings ++ Seq(libraryDependencies ++= CoreDeps) ++ Reflection
  )

  lazy val json = Project(
    id = "tabula-json", base = file("json"),
    settings = buildSettings ++ publishSettings ++ Seq(libraryDependencies ++= CoreDeps ++ JsonDeps)
  ) dependsOn(core % "compile->test")

  lazy val excel = Project(
    id = "tabula-excel", base = file("excel"),
    settings = buildSettings ++ publishSettings ++ Seq(libraryDependencies ++= CoreDeps ++ ExcelDeps)
  ) dependsOn(core % "compile->test")
}
