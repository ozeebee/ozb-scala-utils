package org.ozb.utils.ivy

import scopt.OptionParser
import scala.sys.process.Process
import scala.sys.process.ProcessLogger
import scala.xml.Elem
import org.ozb.utils.io.FileUtils
import scala.xml.PrettyPrinter

object IvyLocalRepositoryInstaller {

	case class Input(
		var ivyJar: String = null,
		var organisation: String = null,
		var module: String = null,
		var revision: String = null,
		var jar: String = null,
		var src: Option[String] = None
	)
	
	def main(args: Array[String]) {
		val parser = new OptionParser[Input]("IvyLocalRepositoryInstaller") {
			head("IvyLocalRepositoryInstaller", "v0.1")
			arg[String]("<ivyjar>") action { (x, c) => 
				c.copy(ivyJar = x) } text("path to the Ivy jar file")
			arg[String]("<organisation>") action { (x, c) => 
				c.copy(organisation = x) } text("organisation name")
			arg[String]("<module>") action { (x, c) => 
				c.copy(module = x) } text("module name")
			arg[String]("<revision>") action { (x, c) => 
				c.copy(revision = x) } text("revision name")
			arg[String]("<jar>") action { (x, c) => 
				c.copy(jar = x) } text("path to the artifact's jar file")
			arg[String]("<src>") optional() action { (x, c) => 
				c.copy(src = Some(x)) } text("(optional) path to the artifact's jar/zip source file")
		}
		
//		val theargs = Array(
//			System.getProperty("user.home") + "/.ivy2/cache/org.apache.ivy/ivy/jars/ivy-2.2.0.jar",
//			"org.eclipse.swt", "swt-cocoa-macosx", "3.7.2",
//			System.getProperty("user.home") + "/Dev/Eclipse/eclipse-jee-indigo-SR2-AJO/plugins/org.eclipse.swt.cocoa.macosx_3.7.2.v3740f.jar",
//			System.getProperty("user.home") + "/Dev/Eclipse/eclipse-jee-indigo-SR2-AJO/plugins/org.eclipse.swt.cocoa.macosx.source_3.7.2.v3740f.jar"
//		)
		val theargs = args
		
		parser.parse(theargs, Input()) map { input =>
			checkPath(input.ivyJar, "invalid path for Ivy jar file : %s" format input.ivyJar)
			checkPath(input.jar, "invalid path for artifact jar file : %s" format input.jar)
			input.src foreach (p => checkPath(p, "invalid path for artifact sources jar/zip file : %s" format p))
			println("ok")
			install(input)
		} getOrElse {
			parser.showUsage
		}
	}
	
	private def checkPath(path: String, errMsg: String) {
		if (! new java.io.File(path).canRead()) {
			System.err.println(errMsg)
			System.exit(1)
		}
	}
	
	def install(input: Input) {
		// NOTE: using module name as artifact name
		val ivyModule = IvyModule(input.organisation, input.module, input.revision, 
			Seq(Artifact(input.module, "jar", "jar", input.jar)) ++ 
			input.src.map { srcjar =>
				Seq(Artifact(input.module + "-sources", getExt(srcjar), "src", srcjar))
			}.getOrElse(Seq.empty)
		)
		publish(input.ivyJar, ivyModule)		
	}
	
	def test(args: Array[String]) {
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
		println("installing (%s %% %s %% %s) in ivy local repository" format (ivyModule.organisation, ivyModule.module, ivyModule.revision))
		// create ivyXml
		var ivyXml = new PrettyPrinter(360, 4).format(ivyModule.toIvyXml)
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
			// if only one artifact, the publish pattern is the file path
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
					"-deliverto", ivyXmlFile, // if omitted, ivy will deliver the ivy.xml to the current directory, so we force it to deliver to our temporary file
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
	
	def getJavaExecutablePath(): String = System.getProperty("java.home") + ( 
			if (System.getProperty("os.name").startsWith("Windows"))
				"/bin/java.exe"
			else
				"/bin/java"
	)
	
	def getExt(name: String) = {
		val pos = name.lastIndexOf(".")
		if (pos == -1 || name.length() == pos+1)
			fatal("cannot determine file extension for path %s" format name)
		name.substring(pos+1)
	}
	
	def fatal(msg: String, exitCode: Int = 1) {
		System.err.println(msg)
		System.exit(exitCode)
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
