package org.ozb.utils.io
import java.io.InputStream
import java.io.OutputStream

object IOUtils {

	def withInputStream[R, I <: InputStream](is: I)(f: I => R): R = {
		try {
			f(is)
		} finally {
			is.close()
		}
	}
	
	def withOutputStream[R, I <: OutputStream](os: I)(f: I => R): R = {
		try {
			f(os)
		} finally {
			os.close()
		}
	}
	
	def withInputAndOutputStream[R, I <: InputStream, O <: OutputStream](is: I, os: O)(f: (I, O) => R): R = {
		try {
			f(is, os)
		} finally {
			is.close()
			os.close()
		}
	}
}