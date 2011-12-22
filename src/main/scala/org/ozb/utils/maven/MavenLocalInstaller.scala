package org.ozb.utils.maven

import scala.io.Source
import scala.sys.process.ProcessBuilder
import scala.sys.process.Process
import scala.sys.process.ProcessLogger

object MavenLocalInstaller {

	def main(args: Array[String]) {
		println("testing")
		val rslt = install("/Users/ajo/Dev/Java/gdata-1.45.0/java/lib/gdata-core-1.0.jar", "com.google.gdata", "core", "1.0")
		if (rslt)
			println("installation succesful")
		else
			println("installation failed")
	}

	def install(file: String, groupId: String, artifactId: String, version: String, packaging: String = "jar", generatePom: Boolean = true): Boolean = {
		val mvnHome: String = System.getenv("MVN_HOME")
		assert(mvnHome != null, "MVN_HOME environment variable is not defined. please define it.")

		println("installing [" + file + "] in maven local repository...")

		val processBuilder = Process(
				mvnHome + "/bin/mvn", Seq(
					"install:install-file", 
					"-DgroupId=" + groupId,
					"-DartifactId=" + artifactId,
					"-Dversion=" + version,
					"-Dfile=" + file,
					"-Dpackaging=" + packaging,
					"-DgeneratePom=" + generatePom
					)
		)
		val plogger = ProcessLogger(line => {
			println(line)
		})
		val exitCode = processBuilder ! plogger
		
		println("  exitCode = " + exitCode)
		exitCode == 0
	}	

}