package zim

import db.DatabaseExecutionContext
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.db.Database

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future

@Singleton
class ZimRepository @Inject()(db: Database)(implicit ec: DatabaseExecutionContext) {

  def fetchSayings(): Future[List[ZimQuote]] = Future {
    db.withConnection { conn =>
      val stmt = conn.createStatement
      val rs = stmt.executeQuery("SELECT * FROM ZimQuotes")

      val buf = new ListBuffer[ZimQuote]
      while (rs.next()) {
        buf.append(ZimQuote(rs.getString("quote"), rs.getString("character")))
      }

      Logger.info(s"Returning ${buf.size} results")
      buf.toList
    }
  }

}
