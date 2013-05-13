package org.ozb.utils.io

import java.io.{File,FileInputStream,FileOutputStream}
import java.io.InputStream
import java.io.FileWriter
import java.io.FileFilter
import java.util.zip.ZipFile
import java.util.zip.ZipException
import java.io.IOException
import java.util.zip.ZipOutputStream
import java.util.zip.ZipEntry

object FileUtils {
	
	/**
	 * Create a temporary file and fill it with String contents
	 */
	def createTempFile(contents: String, deleteOnExit: Boolean = true): File = {
		val file = File.createTempFile("ozb", null)
		if (deleteOnExit)
			file.deleteOnExit() // to be removed when finished
		// fill-in file
		val writer = new java.io.FileWriter(file)
		try {
			writer.write(contents)
			file
		}
		finally {
			writer.close()
		}
	}	

	/**
	 * Create a temporary directory
	 * based on http://stackoverflow.com/questions/617414/create-a-temporary-directory-in-java
	 */
	def createTempDir(prefix: String = "ozb", suffix: String = "tempDir"): File = {
		val file = File.createTempFile(prefix, suffix)
		assert(file.delete(), "could not delete temp file " + file)
		assert(file.mkdirs(), "could not create temp dir " + file)
		file
	}
	
	/**
	 * Delete a directory RECURSIVELY
	 * Watch out when using this
	 */
	def deleteDir(dir: File): Unit = {
		assert(dir.isDirectory(), "dir is not a directory " + dir)
		// first delete all directory contents
		dir.listFiles() foreach (file => if (file.isDirectory()) deleteDir(file) else file.delete())
		// then delete empty dir
		assert(dir.listFiles().isEmpty, "dir is not empty ! " + dir)
		dir.delete()
	}

	/**
	 * usage:
	 * 	val tempDir = new File(...)
	 * 	deletingDir(tempDir) {
	 * 		// actual code using tempdir
	 * 	}
	 * 
	 */
	def deletingDir[R](dir: File)(f: => R): R = {
		//log.debug("dir is %s", dir)
		// execute function with no arg and delete dir after
		try { f } finally { /*println("after invocation");*/ FileUtils.deleteDir(dir) }
	}

	/**
	 * Copy file
	 * adapted from http://stackoverflow.com/questions/2225214/scala-script-to-copy-files
	 */
	def copyFile(srcFile: File, destFile: File, overwrite: Boolean = false): Boolean = {
		assert(srcFile.canRead(), "cannot read source file " + srcFile)
		if (! overwrite && destFile.exists())
			false
		else {
			if (destFile.createNewFile()) {
				new FileOutputStream(destFile) getChannel() transferFrom(
						new FileInputStream(srcFile) getChannel, 0, Long.MaxValue)
				true
			}
			else
				false
		}
	}
	
	def copyFile(srcPath: String, destPath: String, overwrite: Boolean): Boolean = 
			copyFile(new File(srcPath), new File(destPath), overwrite)
			
	def copyFile(srcPath: String, destPath: String): Boolean = 
			copyFile(new File(srcPath), new File(destPath), false)

	/**
	 * Write text data to a given file
	 */
	def writeToFile(file: File, content: String, append: Boolean = false): Unit = {
		val w = new FileWriter(file, append)
		w.write(content)
		w.close()
	}
	
	/**
	 * Detect if the passed inputsteam contains binary content (as opposed to text content).
	 * Inspired from http://stackoverflow.com/questions/3093580/how-to-check-whether-the-file-is-binary
	 */
	def isBinaryContent(is: InputStream, resetInputStream: Boolean = true): Boolean = {
		val max = 500
		val buffer: Array[Byte] = new Array(max)
		if (resetInputStream)
			is.mark(max)
		val count = is.read(buffer, 0, max)
		if (resetInputStream)
			is.reset()
		//val bin = buffer.take(count).count(b => !Character.isWhitespace(b) && Character.isISOControl(b))
		//val bin = buffer.take(count).count(b => !Character.isWhitespace(b) && (b < 32 || b > 255))
		//val bin = buffer.take(count).count(b => !Character.isWhitespace(b) && (b < 32 || b > 127))
		var bin = 0
		var hasNullByte = false
		var i = 0
		while (! hasNullByte && i < count) {
			val byte = buffer(i)
			if (byte == 0X00)
				hasNullByte = true
			else {
				if (!Character.isWhitespace(byte) && (byte < 32 || byte > 127))
					bin += 1
				i += 1
			}
		}
		//println("hasNullByte = " + hasNullByte + " bin = " + bin + " count = " + count)
		// we consider the content to be binary if 20% of checked characters are binary or if it contains null bytes 0X00
		//hasNullByte || (bin >= count / 5)
		// we consider the content to be binary if 10% of checked characters are binary or if it contains null bytes 0X00
		hasNullByte || (bin >= count / 10)
	}
	
	/**
	 * recursively search for a file based on a pattern in the given directory
	 */
	def find(dir: File, search: String): Option[File] = {
		findRegex(dir, search.replace(".", "\\.").replace("*", ".*"))
	}
	
	def findRegex(dir: File, regex: String): Option[File] = {
		dir.listFiles().foreach { file =>
			if (file.getName().matches(regex))
				return Some(file)
			else if (file.isDirectory()) {
				val subrslt = findRegex(file, regex)
				if (subrslt.isDefined)
					return subrslt
			}
		}
		None
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
	
	def withZipFile[R](file: File, mode: Int = ZipFile.OPEN_READ)(f: ZipFile => R):R = {
		var zfile: ZipFile = null
		try {
			zfile = new ZipFile(file, mode)
			f(zfile)
		} finally {
			if (zfile != null)
				zfile.close()
		}
	}
	
	/**
	 * Unzip the given zip file in the given directory
	 */
	def unzip(file: File, outputDir: File) = {
		import scala.collection.JavaConversions._
		
		withZipFile(file) { zfile =>
			// check output dir, create it if necessary
			if (outputDir.exists() && !outputDir.isDirectory())
				throw new IOException("outputDir must be a directory")
			if (! outputDir.exists())
				outputDir.mkdirs()
			
			val entries = zfile.entries() // converted to scala Iterator thanks to implicit definitions in JavaConversions
			entries.foreach { entry =>
				println("  entry [%s] isDir ? %s" format (entry.getName(), entry.isDirectory()))
				
				val name = entry.getName()
				if (entry.isDirectory()) {
					println("    creating dir [%s]" format new File(outputDir, name))
					new File(outputDir, name).mkdirs()
				} else {
					val file = new File(outputDir, name)
					println("    creating file [%s]" format file)
					if (! file.getParentFile().isDirectory())
						file.getParentFile().mkdirs()
					
					file.createNewFile()
					val out = new FileOutputStream(file)
					val in = zfile.getInputStream(entry)
					val buffer = new Array[Byte](4096)
					var len: Int = in.read(buffer)
					while (len != -1) {
						out.write(buffer, 0, len)
						len = in.read(buffer)
					}
					out.close()
					in.close()
				}
			}
		}
	}

	/**
	 * Zip the given directory contents (recursively) in the given output zip file
	 */
	def zip(topdir: File, outputFile: File, includeEmptyDirs: Boolean = true) = {
		if (! topdir.isDirectory())
			throw new IllegalArgumentException("dir [%s] is not a directory" format topdir)
		val outStream = new FileOutputStream(outputFile)
		val zipos = new ZipOutputStream(outStream)
		val buffer = new Array[Byte](4096)
		val topdirPath = topdir.getPath() + "/"

		def addDirToZip(dir: File, zipos: ZipOutputStream): Unit = {
			val files = dir.listFiles()
			if (includeEmptyDirs && files.isEmpty) { // add emtpy dir to archive
				//println("  adding empty dir")
				zipos.putNextEntry(new ZipEntry((dir.getPath() diff topdirPath) + "/"))
				zipos.closeEntry()
			}
			files foreach { file =>
				if (file.isDirectory())
					addDirToZip(file, zipos)
				else {
					val relativePath = file.getPath() diff topdirPath
					//println("  adding entry [%s]" format relativePath)
					val entry = new ZipEntry(relativePath)
					zipos.putNextEntry(entry)
		
					val in = new FileInputStream(file)
					var len: Int = in.read(buffer)
					while (len != -1) {
						zipos.write(buffer, 0, len)
						len = in.read(buffer)
					}
					in.close()
					
					zipos.closeEntry()
				}
			}
		}
		addDirToZip(topdir, zipos)
		
		zipos.close()
		outStream.close()
	}
	
}