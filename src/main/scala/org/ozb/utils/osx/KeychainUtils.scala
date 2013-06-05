package org.ozb.utils.osx

import scala.io.Source
import scala.util.matching.Regex
import scala.sys.process.Process
import scala.sys.process.ProcessLogger

/**
 * Functions to store/read passwords from Mac OS X's Keychain
 */
object KeychainUtils {

	def main(args: Array[String]) {
		println("[%s]" format KeychainUtils.getKeychainPassword("TOTO"))
	}
	
	/**
	 * This function will call MacOSX security process which should return (if successfull)
	 * the password in the standard output
	 */
	def getKeychainPassword(serviceName: String): Option[String] = {
		var output: String = null
		val retcode = Process(Seq("security", "find-generic-password", "-ws", serviceName)) ! ProcessLogger(output = _)
		if (retcode == 0)
			Some(output)
		else
			None
	}

	/**
	 * Create or update a password in the Mac OS X default keychain for the
	 * given service. By default the account of the current user will be used.
	 */
	def addKeychainPassword(serviceName: String, password: String, account: String = System.getProperty("user.name")) {
		var output: String = null
		val cmd = Seq(
				"security", "add-generic-password", 
				"-a", account, 
				"-s", serviceName, 
				"-w", password, 
				"-U", 
				"-T", "")
		//println("running command : [%s]" format cmd.mkString(" "))
		val retcode = Process(cmd) ! ProcessLogger(output = _)
		//println("retcode = " + retcode)
		if (retcode != 0)
			throw new java.io.IOException("keychain password creation failed : %s" format output)
	}
}