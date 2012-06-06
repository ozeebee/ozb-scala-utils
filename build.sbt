organization := "org.ozb"

name := "ozb-scala-utils"

version := "0.1"

scalaVersion := "2.9.1"

libraryDependencies ++= Seq(
	// required for org.ozb.utils.google.gata
	"com.google.api.client" % "google-api-client" % "1.6.0-beta",
	"com.google.gdata" % "gdata-core-1.0" % "1.45.0",
	"com.google.gdata" % "gdata-docs-3.0" % "1.45.0",
	"com.google.gdata" % "gdata-spreadsheet-3.0" % "1.45.0",
	"commons-lang" % "commons-lang" % "2.6",
	// required for org.ozb.utils.jarfinder
	"com.github.scopt" %% "scopt" % "1.1.2" withSources()
)

scalacOptions ++= Seq("-deprecation")
