package org.ozb.utils.jarfinder

import scopt.OptionParser
import scala.collection.JavaConversions._
import scala.util.matching.Regex
import java.io.File
import java.io.FilenameFilter
import java.io.FileFilter
import java.util.zip.ZipFile
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.io.InputStream
import scala.io.Source
import org.ozb.utils.io.FileUtils

/**
 * [ABANDONNED - development has moved to project ozb-jartools]
 * 
 * Example usages:
 * 
 * 1. Search for all occurances of 'setDomainEnv' in jar file toto.jar:
 * 		$ jargrep setDomainEnv toto.jar
 * 2. Search for all occurances of 'setDomainEnv', recursively in all archives under the mydir/ directory
 * 		$ jargrep -r setDomainEnv mydir/
 */
object JarGrep {	
	val allJavaArchives = List("jar", "war", "ear") 
	val allArchives = allJavaArchives :+ "zip"

	val config = new Config()
	var fileFilter: FileFilter = _

	def main(args: Array[String]) {
		//args.foreach(println) // dump args
		val parser = new OptionParser("JarGrep") {
			//opt("d", "dir", "<dir>", "the directory to search in", {v: String => config.basedir = v; config.dir = new java.io.File(v)})
			arg("<pattern>", "<pattern> : pattern to look for", {v: String => config.pattern = v})
			arg("<file>", "<file> : archive file to grep or directory to recurse into", {v: String => config.file = new java.io.File(v)})
			opt("i", "ignorecase", "ignore case", {config.ignoreCase = true})
			opt("v", "verbose", "show informartion on each file/entry processed", {config.verbose = true})
			opt("r", "recurse", "recursively process the given directory", {config.recurse = true})
			opt(None, "enc", "<encoding>", "use given encoding instead of platform's default. Ex. 'UTF-8' or 'ISO-8859-1'.", {v: String =>	config.encoding = Some(v) })
			opt(None, "include", "<name pattern>", "include only archives whose name match the pattern", {v: String =>
				// un-quote pattern if it's quoted
				val QuotedPat = """"(.*)"""".r
				val pattern = v match { // unquote pattern
					case QuotedPat(p) =>p
					case _ => v
				}
				config.includePattern = Some(pattern.replace(".", "\\.").replace("*", ".*"))
			})
			opt(None, "includeEntry", "<name pattern>", "process archive entries whose name match the pattern", {v: String =>
				// un-quote pattern if it's quoted
				val QuotedPat = """"(.*)"""".r
				val pattern = v match { // unquote pattern
					case QuotedPat(p) =>p
					case _ => v
				}
				config.includeEntryPattern = Some(pattern.replace(".", "\\.").replace("*", ".*"))
			})
			opt("a", "allarchives", "all archives (include zip files)", {config.allArchives = true})		
		}
		
		//val testArgs = "setDomainEnv /Users/ajo/Dev/Oracle/middlewarePS5/user_templates/soaclusterdomain_10.3.6.0_oel6.jar".split(" ")
		//val testArgs = "-v -r setDomainEnv /Users/ajo/Dev/Oracle/middlewarePS5/user_templates/soaclusterdomain_10.3.6.0_oel6/".split(" ")
		//val testArgs = """-r COMMON_COMPONENTS_HOME /Users/ajo/Dev/Oracle/middlewarePS5 --include "*template*.jar"""".split(" ")
		//val testArgs = """-r UIBrokerTopic /Users/ajo/Dev/Oracle/middlewarePS5 --include "*template*.jar"""".split(" ")
		//val testArgs = """-r SOA-MGD-SVRS /Users/ajo/Dev/Oracle/middlewarePS5 """.split(" ")
		//val theargs = if (args.isEmpty) testArgs else args
		val theargs = args
		
		if (parser.parse(theargs)) {
			val stats = Stats()
			if (config.recurse)
				println("looking for [" + config.pattern + "] in dir [" + config.file + "]" +
							(config.includePattern.map(" including archives matching " + _).getOrElse("")) +
							(config.includeEntryPattern.map(" including entries matching " + _).getOrElse(""))
						)
			else
				println("looking for [" + config.pattern + "] in file [" + config.file + "]")
			proceed(stats)
			if (config.recurse)
				println("found %d matches in %d entries and %d archives, processed %d entries and %d archives" format (stats.lineMatchCount, stats.entryMatchCount, stats.archMatchCount, stats.entryCount, stats.archCount))
			else
				println("found %d matches in %d entries, processed %d entries" format (stats.lineMatchCount, stats.entryMatchCount, stats.entryCount))
			println("in " + stats.elapsedTime() + " millis")
		}
	}
	
	def proceed(stats: Stats) {
		// the regex to be matched
		val regex = if (config.ignoreCase) new Regex("(?i)" + config.pattern) else new Regex(config.pattern)
		
		if (config.recurse) {
			// check directory exists and is a dir
			if (! config.file.isDirectory())
				throw new IllegalArgumentException("dir [" + config.file + "] is not a valid directory")
			
			fileFilter = new FileFilter {
				def accept(file: File): Boolean = {
					if (file.isDirectory())
						true
					else {
						FileUtils.getExtension(file.getName()) match {
							case None => false
							case Some(ext) =>
								val archiveList = if (config.allArchives) allArchives else allJavaArchives
								// the following will return false ONLY if the include pattern option is defined and if it DOES NOT match filename, otherwise true will be returned
								val incrslt = config.includePattern.map(p => new Regex(p).pattern.matcher(file.getName()).matches()).getOrElse(true)
								archiveList.contains(ext) && incrslt
						}
					}
				}
			}
			
			// the regex to be matched against archive entries
			scanDir(config.file, regex, stats)
		}
		else {
			// check file exists and is a readable
			if (! config.file.canRead())
				throw new IllegalArgumentException("file [" + config.file + "] cannot be read")
					
			// the regex to be matched
			processArchive(config.file, regex, stats)
		}
	}

	def scanDir(dir: File, regex: Regex, stats: Stats) {
		//println("scanning dir [" + dir + "]");
		stats.dirCount += 1
		val files = dir.listFiles(fileFilter)
		files foreach { file =>
			if (! file.isDirectory())
				processArchive(file, regex, stats)
			else
				scanDir(file, regex, stats)	
		}
	}

	def processArchive(file: File, regex: Regex, stats: Stats, indent: String = "") {
		stats.archCount += 1
		if (config.verbose)	println("%sprocessing archive [%s]" format (indent, file))
		
		var zfile: ZipFile = null
		try {
			zfile = new ZipFile(file, ZipFile.OPEN_READ)
			val entries: Iterator[ZipEntry] = config.includeEntryPattern match { // converted to scala Iterator thanks to implicit definitions in JavaConversions
				case Some(p) => zfile.entries() filter (e => new Regex(p).pattern.matcher(e.getName()).matches)
				case None => zfile.entries()
			}
			var archiveNamePrinted = false
			val matchFound: Boolean = entries.foldLeft(false) { (result: Boolean, entry: ZipEntry) =>
				val entryMatches = processEntry(zfile, entry, regex, stats, indent + "  ")
				if (entryMatches.size > 0) {
					// print archive name (once) if an entry matches
					if (! archiveNamePrinted) {
						println("%s%s" format (indent, file))
						archiveNamePrinted = true
					}
					// print matching entry name
					stats.entryMatchCount += 1
					println("  %s%s" format (indent, entry.getName()))
					// print each matching line
					entryMatches.foreach(m => println("    %s  line %d : %s" format (indent, m._1, m._2)))
				}
				
				result || entryMatches.size>0
			}
			if (matchFound)
				stats.archMatchCount += 1
		} catch {
			case ex: ZipException => err("Could not open zip file %s, exception : %s" format (file, ex))
		} finally {
			if (zfile != null)
				zfile.close()
		}
	}
	
	/**
	 * return a sequence of matches : a Tuple2 containing the line number and the matching line
	 */
	def processEntry(zipfile: ZipFile, entry: ZipEntry, regex: Regex, stats: Stats, indent: String): Seq[Tuple2[Int, String]] = {
		if (entry.getName().endsWith("/"))
			return Seq.empty;	// skip directory entries
		
		stats.entryCount += 1
		
		if (entry.getSize() == 0) { // skip 0 length entries
			if (config.verbose)	println("%s  skipping entry [%s] : empty file" format (indent, entry.getName()))
			return Seq.empty;
		}

		if (isBinaryEntry(zipfile, entry)) { // skip binary entries
			if (config.verbose)	println("%s  skipping entry [%s] : binary file" format (indent, entry.getName()))
			return Seq.empty;
		}

		val is = zipfile.getInputStream(entry)
		try {
			if (config.verbose)	println("%sprocessing entry [%s]" format (indent, entry.getName()))
			var linenum = 0
			var filenamePrinted = false
			var matchFoundInArchive = false
			val source = config.encoding.map(Source.fromInputStream(is, _)).getOrElse(Source.fromInputStream(is))
			source.getLines().foldLeft(Seq.empty[Tuple2[Int, String]]) { (result: Seq[Tuple2[Int, String]], line: String) =>
				//println("%s    line : %s" format (indent, line))
				linenum += 1
				if (regex.findFirstIn(line).isDefined) { // match found !
					stats.lineMatchCount += 1
					result :+ (linenum, line) // add match : a Tuple2 containing the line number and the matching line
				}
				else
					result
			}
		}
		finally {
			is.close()
		}
	}

	def isBinaryEntry(zipfile: ZipFile, entry: ZipEntry): Boolean = {
		val is = zipfile.getInputStream(entry)
		try {
			return FileUtils.isBinaryContent(zipfile.getInputStream(entry), false)
		}
		finally {
			is.close()
		}
	}
	
	def err(msg: String) = {
		println("[ERR] " + msg)
	}
	
	case class Stats (
		var lineMatchCount: Int = 0, // number of matching lines
		var entryMatchCount: Int = 0, // number of matching entries
		var archMatchCount: Int = 0, // number of matching archives
		var entryCount: Int = 0, // number of processed entries
		var dirCount: Int = 0, // number of scanned directories
		var archCount: Int = 0, // number of processed archives
		private var time0: Long = System.currentTimeMillis // elapsed time
	) {
		def elapsedTime(): Long = System.currentTimeMillis - time0
	}
	
	case class Config (
		var file: java.io.File = null,
		var pattern: String = null,
		var ignoreCase: Boolean = false,
		var verbose: Boolean = false,
		var recurse: Boolean = false,
		var includePattern: Option[String] = None,
		var includeEntryPattern: Option[String] = None,
		var encoding: Option[String] = None,
		var allArchives: Boolean = false
	)
}
