package org.ozb.utils.io

import java.io.{File,FileInputStream,FileOutputStream}

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
	def createTempDir(): File = {
		val file = File.createTempFile("ozb", "tempDir")
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
	 * Copy file
	 * adapted from http://stackoverflow.com/questions/2225214/scala-script-to-copy-files
	 */
	def copyFile(srcPath: String, destPath: String, overwrite: Boolean = false): Boolean = {
		val src = new File(srcPath)
		assert(src.canRead(), "cannot read source file " + src)
		val dest = new File(destPath)
		if (! overwrite && dest.exists())
			false
		else {
			if (dest.createNewFile()) {
				new FileOutputStream(dest) getChannel() transferFrom(
						new FileInputStream(src) getChannel, 0, Long.MaxValue)
				true
			}
			else
				false
		}
	}
}