package teamtwo.webservice.db

import java.sql.Timestamp

import scala.slick.driver.MySQLDriver.simple._
import java.util.Date

// FIXME: datetime shouldn't be a String
case class Quote(symbol: String, datetime: Timestamp, price: Double)

object Quotes {
  lazy val quotes = TableQuery[Quotes]
}

class Quotes(tag: Tag) extends Table[Quote](tag, "quote") {
  def symbol = column[String]("symbol", O.NotNull)

  // O.PrimaryKey) //ask Alex
  def datetime = column[Timestamp]("datetime", O.NotNull)

  // ask Alex composite Primary Key
  def price = column[Double]("price", O.NotNull)

  def * = (symbol, datetime, price) <> (Quote.tupled, Quote.unapply)
}

