scalaVersion := "3.3.1"

name := "funpar-project"
organization := "your.organization"
version := "1.0"

libraryDependencies ++= Seq(
  // Core Scala libraries
  "org.scala-lang.modules" %% "scala-parser-combinators" % "2.3.0",
  "org.scala-lang.modules" %% "scala-parallel-collections" % "1.0.4",
  
  // JavaCV/OpenCV - Core dependencies
  "org.bytedeco" % "javacv" % "1.5.8",
  "org.bytedeco" % "opencv" % "4.6.0-1.5.8",
  "org.bytedeco" % "openblas" % "0.3.21-1.5.8",
  
  // Windows-specific native libraries
  "org.bytedeco" % "opencv" % "4.6.0-1.5.8" classifier "windows-x86_64",
  "org.bytedeco" % "openblas" % "0.3.21-1.5.8" classifier "windows-x86_64",
  "org.bytedeco" % "javacpp" % "1.5.8" classifier "windows-x86_64"
)

// Essential performance settings for Windows
fork in run := true
javaOptions in run ++= Seq(
  "-Xmx4G",
  "-Djava.library.path=target/native-library"
)

// Better compiler warnings
scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Xlint"
)

// Create target/native-library directory
initialize := {
  val _ = initialize.value
  val nativeLib = file("target/native-library")
  IO.createDirectory(nativeLib)
}