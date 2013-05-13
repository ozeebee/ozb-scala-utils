package org.ozb.utils.osx
import java.io.File

/**
 * Various Mac OS X related utility functions
 */
object OSXUtils {
	def isMacOSX = System.getProperty("os.name") == "Mac OS X"

	/**
	 * Open a quick look window for the given file
	 */
	def quickLook(file: File, viewAsText: Boolean = false) {
		val seq = "qlmanage" +: (
			(if (viewAsText) Seq("-c", "public.plain-text") else Seq.empty) ++
			Seq("-p", file.getPath())
		)
		sys.process.Process(seq).!
		// preview as plain-text: add -c public.plain-text (see https://developer.apple.com/library/mac/#documentation/Miscellaneous/Reference/UTIRef/Articles/System-DeclaredUniformTypeIdentifiers.html)
	}
}