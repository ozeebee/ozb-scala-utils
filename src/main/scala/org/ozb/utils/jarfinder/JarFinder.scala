package org.ozb.utils.jarfinder

import scopt.OptionParser
import java.io.File
import java.io.FilenameFilter
import java.io.FileFilter
import java.util.zip.ZipFile
import scala.collection.JavaConversions._
import scala.util.matching.Regex
import java.util.zip.ZipException

object JarFinder {
	val allJavaArchives = List("jar", "war", "ear") 
	val allArchives = allJavaArchives :+ "zip"
	
	val config = new Config()
	var fileFilter: FileFilter = _
	
	def main(args: Array[String]) {
		//args.foreach(println) // dump args
		val parser = new OptionParser("JarFinder") {
			//opt("d", "dir", "<dir>", "the directory to search in", {v: String => config.basedir = v; config.dir = new java.io.File(v)})
			arg("<dir>", "<dir> : directory to search in", {v: String => config.basedir = v; config.dir = new java.io.File(v)})
			arg("<pattern>", "<pattern> : pattern to look for", {v: String => config.pattern = v})
			opt("i", "ignorecase", "ignore case", {config.ignoreCase = true})
			opt("a", "allarchives", "all archives (include zip files)", {config.allArchives = true})
		}
		
		//val testArgs = "/Users/ajo/Dev/Oracle/Middleware_10.3.5 EJBException".split(" ")
		//val testArgs = "/Users/ajo/Dev/Oracle/Domains/tests_10.3.5 EJBException".split(" ")
		//val testArgs = "/Users/ajo/Dev/Oracle/Middleware_10.3.5/coherence_3.6/lib/security Keystore".split(" ")
		//val testArgs = "/Users/ajo/Dev/Oracle/Middleware_10.3.5/coherence_3.6 Util".split(" ")
		//val testArgs = "/Users/ajo/Dev/Oracle/Middleware_10.3.5/coherence_3.6 cleaner".split(" ")
		//val theargs = if (args.isEmpty) testArgs else args
		val theargs = args
		
		if (parser.parse(theargs)) {
			val stats = Stats()
			println("looking for [" + config.pattern + "] in dir [" + config.basedir + "]")
			proceed(stats)
			println("found " + stats.matchCount + " entries in " + stats.archCount + " processed archives, scanned " + stats.dirCount + " directories")
			println("in " + stats.elapsedTime() + " millis")
		}
	}
	
	def proceed(stats: Stats) {
		// check directory exists and is a dir
		if (! config.dir.isDirectory())
			throw new IllegalArgumentException("dir [" + config.dir + "] is not a valid directory")
		
		fileFilter = new FileFilter {
			def accept(file: File): Boolean = {
				if (file.isDirectory())
					true
				else {
					val ext = getExtension(file.getName())
					val archiveList = if (config.allArchives) allArchives else allJavaArchives
					archiveList.contains(ext)
				}
			}
		}
		
		// the regex to be matched against archive entries
		val regex = if (config.ignoreCase) new Regex("(?i)" + config.pattern) else new Regex(config.pattern)
		scanDir(config.dir, regex, stats)
	}
	
	def scanDir(dir: File, regex: Regex, stats: Stats) {
		//println("scanning dir [" + dir + "]");
		stats.dirCount += 1
		val files = dir.listFiles(fileFilter)
		files foreach { file =>
			if (! file.isDirectory())
				scanArchive(file, regex, stats)
			else
				scanDir(file, regex, stats)	
		}
	}
	
	def scanArchive(file: File, regex: Regex, stats: Stats) {
		stats.archCount += 1
		//println("scanning archive : " + file)
		
		var zfile: ZipFile = null
		try {
			zfile = new ZipFile(file, ZipFile.OPEN_READ)
			val entries = zfile.entries() // converted to scala Iterator thanks to implicit definitions in JavaConversions
			// find entries matching the pattern and that are not paths (directories)
			val matches = entries filter (entry =>
				! (regex findFirstIn entry.getName).isEmpty && ! entry.getName().endsWith("/") 
			)
			if (! matches.isEmpty) {
				println("." + file.getPath diff config.basedir)
				matches foreach {entry => 
					println(" ==> " + entry.getName())
					stats.matchCount += 1
				}
			}
		} catch {
			case ex: ZipException => err("Could not open zip file %s, exception : %s" format (file, ex))
		} finally {
			if (zfile != null)
				zfile.close()
		}
	}
	
	/** 
	 * @return the extension of the file or null if none
	 */
	def getExtension(file: File): String = {
		val arr = file.getName().split("\\.")
		if (arr.length > 1) arr.last else null
	}
	
	def getExtension(filename: String): String = {
		filename.lastIndexOf('.') match {
			case -1 => null
			case idx => filename.substring(idx + 1)
		}
	}
	
	def err(msg: String) = {
		println("[ERR] " + msg)
	}
}

case class Stats (
	var archCount: Int = 0, // number of processed archives
	var matchCount: Int = 0, // number of matching entries
	var dirCount: Int = 0, // number of scanned directories
	private var time0: Long = System.currentTimeMillis // elapsed time
) {
	def elapsedTime(): Long = System.currentTimeMillis - time0
}

case class Config (
	var basedir: String = null,
	var dir: java.io.File = null,
	var pattern: String = null,
	var ignoreCase: Boolean = false,
	var allArchives: Boolean = false
)
