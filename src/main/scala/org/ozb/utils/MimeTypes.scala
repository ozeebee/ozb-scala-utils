package org.ozb.utils

import scala.util.parsing.combinator._
import scala.io.Source
import org.ozb.utils.io.FileUtils

/**
 * This object provides file extension to mime type mapping.
 * Based on mime types provided by apache httpd server : http://svn.apache.org/repos/asf/httpd/httpd/trunk/docs/conf/mime.types
 * See also http://stackoverflow.com/questions/7609208/complete-list-of-mime-type-file-extension-mapping
 */
object MimeTypes {
	private lazy val mimeTypesByExt: Map[String, String] = parseMimeTypes()
	
	def main(args: Array[String]): Unit = {
		println("htm  => " + fromExtension("htm"))
		println("html => " + fromExtension("html"))
		println("toto => " + fromExtension("toto"))
		println("xyz.html => " + fromFilename("xyz.html"))
		println("abc      => " + fromFilename("abc"))
	}
	
	def fromExtension(ext: String): Option[String] = mimeTypesByExt.get(ext)
	
	def fromFilename(filename: String): Option[String] = FileUtils.getExtension(filename).flatMap(fromExtension(_))
	
	private def parseMimeTypes(): Map[String, String] = {
		val lines = Source.fromInputStream(this.getClass().getResourceAsStream("mime.types")).getLines
		val parser = new MTParser()
		
		lines.foldLeft(Map.empty[String, String]) { (rslt, line) =>
			parser.parse(line) match {
				case comment: String => rslt // skip comments
				case (mimeType: String, extensions: List[_]) => // cannot use List[String] due to type erasure ...
					rslt ++ extensions.map((_.toString -> mimeType)).toMap
			}
		}
	}
	
	/** parser capable of parsing lines from mime.types file */
	private class MTParser extends JavaTokenParsers {
		// parse mime type such as "application/atom+xml"
		private val mimeType: Parser[String] = "[^\\s]+".r
		// parse a file extension
		private val ext: Parser[String] = "[a-z0-9-]\\w*".r
		// parse a mime type to extension list def (see file mime.types) ex: "application/msword				doc dot"
		private val mimeTypeDef: Parser[(String, List[String])] = mimeType~rep1(ext) ^^ {
			case mt~list => (mt, list)
		}
		// parse a comment line
		private val comment: Parser[String] = "^#.*".r
		// parse a line form mime.types file (either a comment or a mime type definition line)
		private val lineMatcher: Parser[Any] = comment | mimeTypeDef
		
		def parse(line: String): Any = parseAll(lineMatcher, line) match {
			case Success(result, _) => result
			case failure: NoSuccess => sys.error("parse error " + failure.msg)
		}
	}
}