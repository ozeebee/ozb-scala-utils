package org.ozb.utils.osx

import scala.io.Source
import scala.util.matching.Regex

/**
 * TODO : replace java proces builder with scala.sys.process objects
 */
object KeychainUtils {

	def main(args: Array[String]) {
		println("pwd = " + KeychainUtils.getKeychainPassword("AJO_GDocs_test").get)
	}
	
	/**
	 * This function will call MacOSX security process which should return (if successfull)
	 * the password in the error stream which is then parsed to be returned
	 */
	def getKeychainPassword(serviceName: String): Option[String] = {
		val processBuilder = new ProcessBuilder("security", "find-generic-password", "-gs", serviceName)
		val process = processBuilder.start()
		val errorStream = process.getErrorStream()
		val str = Source.fromInputStream(errorStream).mkString
		//println("str = " + str)
		
		/*
		// 1st way : use regexp match (which will do a full match hence the complete regexp pattern as opposed to the 2nd way)
		val regex = new Regex("""(?s)password: "([^"]*)".*""") // AJO: the (?s) is essential to denote a multi-line pattern ... without that the (full) match will always fail
				str match {
			case regex(password) => Option(password) // AJO: this will call regex.unapply method
			case _ => Option(null)
		}
		*/
		
		// 2nd way : use findFirstMatchIn method
		val regex = new Regex("""(?s)password: "([^"]*)".*""")
		regex.findFirstMatchIn(str) match {
			case Some(m) => Some(m.group(1))
			case None => Option(null)	// AJO : Option(null) evaluates to None
		}
	}

}