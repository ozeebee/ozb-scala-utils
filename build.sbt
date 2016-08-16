organization := "org.ozb"

name := "ozb-scala-utils"

version := "0.2"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
	"com.github.scopt" %% "scopt" % "3.5.0" withSources(),
	// scala separate libraries starting from scala 2.11)
	"org.scala-lang.modules" %% "scala-xml" % "1.0.5",
	"org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4"
)

// exclude old stuff from sources 
excludeFilter in unmanagedSources := new sbt.FileFilter {
	def accept(f: File): Boolean = ".*org/ozb/utils/jarfinder/.*".r.pattern.matcher(f.absolutePath).matches
}

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature")
