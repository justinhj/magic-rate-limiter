import sbt._

lazy val fp1 = (project in file(".")).
  settings (
    name := "magicratelimiter",
    organization := "org.example",
    version := "0.1-SNAPSHOT",
    scalaVersion := "2.13.3"
  )

addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full)

// Source https://tpolecat.github.io/2017/04/25/scalac-flags.html
// Have commented out some options that no longer work with Scala 2.13.3 and later
scalacOptions ++= Seq(
  "-deprecation",                      // Emit warning and location for usages of deprecated APIs.
  "-encoding", "utf-8",                // Specify character encoding used by source files.
  "-explaintypes",                     // Explain type errors in more detail.
  "-feature",                          // Emit warning and location for usages of features that should be imported explicitly.
  "-language:existentials",            // Existential types (besides wildcard types) can be written and inferred
  "-language:experimental.macros",     // Allow macro definition (besides implementation and application)
  "-language:higherKinds",             // Allow higher-kinded types
  "-language:postfixOps",              // Allow post fix operations
  "-language:implicitConversions",     // Allow definition of implicit functions called views
  "-unchecked",                        // Enable additional warnings where generated code depends on assumptions.
  "-Xcheckinit",                       // Wrap field accessors to throw an exception on uninitialized access.
  //"-Xfatal-warnings",                  // Fail the compilation if there are any warnings.
  //"-Xfuture",                          // Turn on future language features.
  "-Xlint:adapted-args",               // Warn if an argument list is modified to match the receiver.
  //"-Xlint:by-name-right-associative",  // By-name parameter of right associative operator.
  "-Xlint:constant",                   // Evaluation of a constant arithmetic expression results in an error.
  "-Xlint:delayedinit-select",         // Selecting member of DelayedInit.
  "-Xlint:doc-detached",               // A Scaladoc comment appears to be detached from its element.
  "-Xlint:inaccessible",               // Warn about inaccessible types in method signatures.
  "-Xlint:infer-any",                  // Warn when a type argument is inferred to be `Any`.
  "-Xlint:missing-interpolator",       // A string literal appears to be missing an interpolator id.
  //"-Xlint:nullary-override",           // Warn when non-nullary `def f()' overrides nullary `def f'.
  //"-Xlint:nullary-unit",               // Warn when nullary methods return Unit.
  "-Xlint:option-implicit",            // Option.apply used implicit view.
  "-Xlint:package-object-classes",     // Class or object defined in package object.
  "-Xlint:poly-implicit-overload",     // Parameterized overloaded implicit methods are not visible as view bounds.
  "-Xlint:private-shadow",             // A private field (or class parameter) shadows a superclass field.
  "-Xlint:stars-align",                // Pattern sequence wildcard must align with sequence component.
  "-Xlint:type-parameter-shadow",      // A local type parameter shadows a type already in scope.
  //"-Xlint:unsound-match",              // Pattern match may not be typesafe.
  //"-Yno-adapted-args",                 // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver.
  //"-Ypartial-unification",             // Enable partial unification in type constructor inference
  "-Ywarn-dead-code",                  // Warn when dead code is identified.
  "-Ywarn-extra-implicit",             // Warn when more than one implicit parameter section is defined.
  //"-Ywarn-inaccessible",               // Warn about inaccessible types in method signatures.
  //"-Ywarn-infer-any",                  // Warn when a type argument is inferred to be `Any`.
  //"-Ywarn-nullary-override",           // Warn when non-nullary `def f()' overrides nullary `def f'.
  //"-Ywarn-nullary-unit",               // Warn when nullary methods return Unit.
  "-Ywarn-numeric-widen",              // Warn when numerics are widened.
  "-Ywarn-unused:implicits",           // Warn if an implicit parameter is unused.
  "-Ywarn-unused:imports",             // Warn if an import selector is not referenced.
  "-Ywarn-unused:locals",              // Warn if a local definition is unused.
  "-Ywarn-unused:params",              // Warn if a value parameter is unused.
  "-Ywarn-unused:patvars",             // Warn if a variable bound in a pattern is unused.
  "-Ywarn-unused:privates",            // Warn if a private member is unused.
  "-Ywarn-value-discard"               // Warn when non-Unit expression results are unused.
)

javacOptions ++= Seq("-Xlint:unchecked", "-Xlint:deprecation")

val ZIOVersion = "1.0.4-2"
val AmmoniteVersion = "2.3.8"
val SttpVersion = "3.1.7"

libraryDependencies ++= Seq(
  // Zio
  "dev.zio" %% "zio" % ZIOVersion,
  "dev.zio" %% "zio-test" % ZIOVersion % "test",
  "dev.zio" %% "zio-test-sbt" % ZIOVersion % "test",
  "dev.zio" %% "zio-streams" % ZIOVersion,
  // Zio Magic
  "io.github.kitlangton" %% "zio-magic" % "0.1.12",
  // sttp and json
  "com.softwaremill.sttp.client3" %% "core" % SttpVersion,
  "com.softwaremill.sttp.client3" %% "httpclient-backend-zio" % SttpVersion,
  "dev.zio" %% "zio-json" % "0.1.2",
  "io.github.kitlangton" %% "zio-magic" % "0.1.12",
  // For nice dates
  "org.ocpsoft.prettytime" % "prettytime" % "3.2.7.Final",
  // Embed Li Haoyi's Ammonite repl in your test project because it's cool
  "com.lihaoyi" %% "ammonite" % AmmoniteVersion % "test" cross CrossVersion.full
)

testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")

resolvers ++= Seq(
  "Secured Central Repository" at "https://repo1.maven.org/maven2",
  Resolver.sonatypeRepo("snapshots")
)

// Ammonite repl launch command (Run the test:run task in sbt)
sourceGenerators in Test += Def.task {
  val file = (sourceManaged in Test).value / "amm.scala"
  IO.write(file, """object amm extends App { ammonite.Main().run() }""")
  Seq(file)
}.taskValue
