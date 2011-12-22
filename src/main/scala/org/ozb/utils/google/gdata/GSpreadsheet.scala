package org.ozb.utils.google.gdata

import scala.collection.JavaConversions._
import org.ozb.utils.osx.KeychainUtils
import com.google.gdata.data.spreadsheet.SpreadsheetEntry
import com.google.gdata.client.spreadsheet.SpreadsheetService
import com.google.gdata.client.spreadsheet.SpreadsheetQuery
import com.google.gdata.client.spreadsheet.FeedURLFactory
import com.google.gdata.client.spreadsheet.ListQuery
import com.google.gdata.client.spreadsheet.WorksheetQuery
import com.google.gdata.data.spreadsheet.SpreadsheetFeed
import com.google.gdata.client.GoogleAuthTokenFactory
import com.google.gdata.client.GoogleAuthTokenFactory.UserToken
import com.google.gdata.data.spreadsheet.ListEntry
import com.google.gdata.data.spreadsheet.ListFeed
import com.google.gdata.data.spreadsheet.WorksheetEntry
import com.google.gdata.data.spreadsheet.WorksheetFeed

object GSpreadsheet {
	
	def main(args: Array[String]) {
		println("test")
		
		val service = new SpreadsheetService("GDocsTest")
		service.setUserToken("???")
		
		val sheetTitle = "music"
		println("looking for spreadsheet entry...")
		val entry = getSpreadsheetEntry(service, sheetTitle)
		println("entry is : " +  entry)

//		// branch type 1
//		entry match {
//			case Some(v) => println("entry title : " + v.getTitle().getPlainText()) 
//			case None => println("entry with name ["+sheetTitle+"] not found !"); exit(1)
//		}
		
		// branch type 2
		entry map { v =>
			println("entry title : " + v.getTitle().getPlainText())
			v.getWorksheets().foreach(ws => println("  worksheet = [" + ws.getTitle().getPlainText() + "]"))
		} getOrElse {
			println("entry with name ["+sheetTitle+"] not found !"); sys.exit(1)
		}
		
		
		println("worksheet = " + getWorksheetEntry(service, sheetTitle, "APP_MusicList").get.getTitle().getPlainText())
	}

	/**
	 * authenticate google service and return the auth token for subsequent requests
	 */
	def authenticate(service: SpreadsheetService, username: String, password: String): String = {
		service.setUserCredentials(username, password)
		val tokenFactory: GoogleAuthTokenFactory = service.getAuthTokenFactory().asInstanceOf[GoogleAuthTokenFactory]
		val userToken: UserToken = tokenFactory.getAuthToken().asInstanceOf[UserToken]
		userToken.getValue()
	}
	
	/**
	 * return the spreadsheet with the given title
	 */
	def getSpreadsheetEntry(service: SpreadsheetService, spreadsheetTitle: String): Option[SpreadsheetEntry] = {
		val query = new SpreadsheetQuery(FeedURLFactory.getDefault().getSpreadsheetsFeedUrl())
		query.setTitleQuery(spreadsheetTitle)
		val feed = service.query(query, classOf[SpreadsheetFeed])
		val entries = feed.getEntries()
		if (entries.isEmpty()) Option(null) else Some(entries.get(0))
	}
	
	def getWorksheetEntry(service: SpreadsheetService, spreadsheetTitle: String, worksheetTitle: String): Option[WorksheetEntry] = {
		getSpreadsheetEntry(service, spreadsheetTitle) map { spreadsheetEntry =>
			val worksheetQuery = new WorksheetQuery(spreadsheetEntry.getWorksheetFeedUrl())
			worksheetQuery.setTitleQuery(worksheetTitle)
			worksheetQuery.setTitleExact(true)
			val worksheets = service.query(worksheetQuery, classOf[WorksheetFeed]).getEntries()
			if (worksheets.isEmpty())
				null
			else
				worksheets.get(0)
		}
	}
	
	def searchWorksheet(worksheetEntry: WorksheetEntry, queryStr: String, isFullText: Boolean = false): Seq[ListEntry] = {
		val query = new ListQuery(worksheetEntry.getListFeedUrl())
		println("searching for [" + queryStr + "]")
		if (isFullText)
			query.setFullTextQuery(queryStr) // perform a fulltext search
		else
			query.setSpreadsheetQuery(queryStr) // perform a structured query (ex: index = "10", artist = "toto")
		val feed = worksheetEntry.getService().query(query, classOf[ListFeed])
		val entries = feed.getEntries()
		entries		
	}
	
	def searchSpreadsheet(spreadsheetEntry: SpreadsheetEntry, queryStr: String, isFullText: Boolean = false): Seq[ListEntry] = {
		searchWorksheet(spreadsheetEntry.getDefaultWorksheet(), queryStr, isFullText)
	}
}