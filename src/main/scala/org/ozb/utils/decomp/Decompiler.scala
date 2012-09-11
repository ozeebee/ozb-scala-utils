package org.ozb.utils.decomp

import scala.sys.process.ProcessBuilder
import scala.sys.process.Process
import scala.sys.process.ProcessLogger
import java.io.File
import org.ozb.utils.io.FileUtils

class Decompiler(jadHome: String) {
	/**
	 * Decompile the given jar file
	 */
	def decompileJar(jar: File, zipOutputDir: File, decompOutputDir: File): Unit = {
		// first unzip jar contents
		FileUtils.unzip(jar, zipOutputDir)
		
		// then decompile all classes
		decompileDir(zipOutputDir, true, decompOutputDir)
	}
	
	/**
	 * Decompile the given directory
	 */
	def decompileDir(basedir: File, recursive: Boolean, outputDir: File): Unit = {
		val filter = new java.io.FileFilter {
			def accept(file: File): Boolean = {
				file.isDirectory()
			}
		}
		basedir.listFiles(filter) foreach { dir =>
			decompileClassDir(dir, outputDir)
			if (recursive)
				decompileDir(dir, recursive, outputDir)
		}
	}
	
	def decompileClassDir(dir: File, outputDir: File): Boolean = decompile(dir.getAbsolutePath() + "/*.class", outputDir)
	
	def decompileClass(classPath: String, outputDir: File): Boolean = decompile(classPath, outputDir)
	
	protected def decompile(arg: String, outputDir: File): Boolean = {
		println("decompiling...")
		println("  arg = [%s]" format arg)
		
		val processBuilder = Process(
				jadHome + "/jad", Seq(
					"-o", // overwrite without confirmation
					"-r", // restore package directory structure
					"-sjava", // set file name suffix
					"-ff", // output class fields before methods
					"-lnc", //annotate the output with line numbers
					"-noctor", // suppress empty constructors
					"-nonlb", // don't output a newline before opening brace
					"-space", // output space between keyword (if/for/while/etc) and expression
					"-stat", // display the total number of processed classes/methods/fields
					"-d" + outputDir.getAbsolutePath(), // output directory
					arg // path to class or pattern to decompile (ex: toto.class, *.class, 'tree/**/*.class')
					)
		)
		val plogger = ProcessLogger(line => {
			println(line)
		})
		val exitCode = processBuilder ! plogger
		
		println("  exitCode = " + exitCode)
		exitCode == 0
	}
}
