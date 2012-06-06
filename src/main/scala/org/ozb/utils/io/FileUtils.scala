package org.ozb.utils.io

import java.io.{File,FileInputStream,FileOutputStream}
import java.io.InputStream
import java.io.FileWriter
import java.io.FileFilter

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
}