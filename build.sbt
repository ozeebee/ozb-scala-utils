organization := "org.ozb"

name := "ozb-scala-utils"

version := "0.2"

scalaVersion := "2.9.3"

libraryDependencies ++= Seq(
	"com.github.scopt" % "scopt_2.9.2" % "2.1.0" withSources()
)

// exclude old stuff from sources 
excludeFilter in unmanagedSources := new sbt.FileFilter {
	def accept(f: File): Boolean = ".*org/ozb/utils/jarfinder/.*".r.pattern.matcher(f.absolutePath).matches
}

scalacOptions ++= Seq("-deprecation", "-unchecked")
