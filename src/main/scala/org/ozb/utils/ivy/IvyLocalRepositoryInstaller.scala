package org.ozb.utils.ivy

import scala.sys.process.Process
import scala.sys.process.ProcessLogger
import scala.xml.Elem
import org.ozb.utils.io.FileUtils

object IvyLocalRepositoryInstaller {
	
	def main(args: Array[String]) {
		println("**** starting ****")
	
		val ivyJar = System.getProperty("user.home") + "/.ivy2/cache/org.apache.ivy/ivy/jars/ivy-2.2.0.jar"

		val ivyModule = IvyModule(
				"com.google.api.client", 
				"google-api-client", 
				"1.6.0-beta",
				Seq(
					Artifact("google-api-client", "jar", "jar", "/Users/ajo/Dev/Java/google-api-java-client-1.6.0-beta/google-api-client-1.6.0-beta.jar"),
					Artifact("google-api-client-src", "jar", "jar", "/Users/ajo/Dev/Java/google-api-java-client-1.6.0-beta/google-api-client-1.6.0-beta-sources.jar")
				)
		)
		publish(ivyJar, ivyModule)		
	}
	
	def publish(ivyJar: String, ivyModule: IvyModule): Boolean = {
		println("installing " + ivyModule.organisation + " % " + ivyModule.module + " % " + ivyModule.revision + " in ivy local repository")
		// create ivyXml
		var ivyXml = ivyModule.toIvyXml
		// create temporary ivy xml file
		val file = FileUtils.createTempFile(ivyXml.toString())
		// publish
		if (ivyModule.artifacts.size > 1) {
			// copy artifacts file to temp dir and rename them to create a working publish pattern
			val tempDir = FileUtils.createTempDir()
			try {
				println("created temp dir " + tempDir)
				ivyModule.artifacts foreach { artifact =>
					FileUtils.copyFile(artifact.filePath, tempDir.getPath() + "/" + artifact.name + "." + artifact.artExt)
				}
				val publishPattern = tempDir.getPath() + "/[artifact].[ext]"
				publish(ivyJar, file.getPath(), ivyModule.revision, publishPattern)
			}
			finally {
				FileUtils.deleteDir(tempDir)
			}
		}
		else {
			// if only one artifact, the publish pattern if the file path
			val publishPattern = ivyModule.artifacts(0).filePath
			publish(ivyJar, file.getPath(), ivyModule.revision, publishPattern)
		}
	}

	def publish(ivyJar: String, ivyXmlFile: String, revision: String, pubPattern: String): Boolean = {
		val resolver = "local" // use local resolver
		//var pattern = "[organisation]/[module]/[revision]/[type]s/[artifact].[ext]" // use default pattern
		
		println("ivyXmlFile = " + ivyXmlFile)
		val processBuilder = Process(
				getJavaExecutablePath(), Seq(
					"-jar", ivyJar,
					"-publish", resolver,
					"-publishpattern", pubPattern,
					"-revision", revision,
					// "-status", "integration",
					"-overwrite",
					"-ivy", ivyXmlFile
					)
		)
		println("processBuilder = " + processBuilder)
		val plogger = ProcessLogger(line => {
			println(line)
		})
		val exitCode = processBuilder ! plogger
		
		println("  exitCode = " + exitCode)
		exitCode == 0
	}
	
	def getJavaExecutablePath(): String = {
		System.getProperty("java.home") + "/bin/java"
	}
	
	case class IvyModule(
			organisation: String, 
			module: String, 
			revision: String,
			artifacts: Seq[Artifact]) {
		
		def toIvyXml: xml.Elem = {
			var ivyXml =
				<ivy-module version="2.0">
				<info organisation={organisation} module={module}/>
					<publications> { artifacts.map { artifact =>
							<artifact name={artifact.name} ext={artifact.artExt} type={artifact.artType}/>
					} }	</publications>
				</ivy-module>
			ivyXml
		}
	}
	
	case class Artifact(
			name: String, 
			artExt:String, 
			artType: String,
			filePath: String
	)
}