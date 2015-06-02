package teamtwo.webservice.endpoints

import java.sql.Timestamp

import akka.actor.Props
import spray.http.StatusCodes._
import spray.routing.RequestContext
import teamtwo.webservice.db.{DataAccessActor, Quote}
import teamtwo.webservice.endpoints.InsertPriceHandlerActor.InsertPrice
import java.text.SimpleDateFormat
import java.util.{Date, Calendar}
import teamtwo.webservice.server.StockTickerServer.actorSystem

object InsertPriceHandlerActor {
  sealed trait InsertPriceMessage
  case class InsertPrice(symbol: String, price: Double, date: Date) extends InsertPriceMessage

  def props(requestContext: RequestContext) = Props(classOf[InsertPriceHandlerActor], requestContext)
}

/**
 * Endpoint actor for handling the insertion of tickers. takes the symbol and requested price, will infer the date
 * based on the timestamp of the HttpRequest
 */
case class InsertPriceHandlerActor(requestContext: RequestContext) extends WebServiceActor with InsertPriceHandler {
  def receive = {
    case InsertPrice(symbol, price, date) => performInsertPrice(symbol, price, date)
  }

  def performInsertPrice(symbol: String, price: Double, date: Date): Unit = complete(OK, insertPrice(symbol, price, date))
}

trait InsertPriceHandler {
  // FIXME: Anything in here should be unit tested
  //taken from http://alvinalexander.com/scala/scala-get-current-date-time-hour-calendar-example
  def getCurrentHour: String = {
    val today = Calendar.getInstance().getTime
    val hourFormat = new SimpleDateFormat("hh")
    try {
      // returns something like "01" if i just return at this point, so cast it to
      // an int, then back to a string (or return the leading '0' if you prefer)
      val currentHour = Integer.parseInt(hourFormat.format(today))
      return "" + currentHour
    } catch {
      // TODO return Some/None/Whatever
      case e: Exception => return "0"
    }

    hourFormat.format(today)
  }

  //taken from http://alvinalexander.com/scala/scala-get-current-date-time-hour-calendar-example

  def getCurrentMinute: String = {
    val today = Calendar.getInstance().getTime
    val minuteFormat = new SimpleDateFormat("mm")
    try {
      // returns something like "01" if i just return at this point, so cast it to
      // an int, then back to a string (or return the leading '0' if you prefer)
      val currentMinute = Integer.parseInt(minuteFormat.format(today))
      return "" + currentMinute
    } catch {
      // TODO return Some/None/Whatever
      case e: Exception => return "0"
    }

    minuteFormat.format(today)
  }


//  def insertPrice(symbol: String, price: Double): Unit = {
//    val handler = actorSystem.actorOf(DataAccessActor.props)
//// FIXME: That string for date isn't right - should use the request time
//    handler ! DataAccessActor.Put(Quote(symbol, new Timestamp(new Date().getTime), price))
//  }

  def insertPrice(symbol: String, price: Double, date: Date): Unit = {
    val handler = actorSystem.actorOf(DataAccessActor.props)
    val timestamp = new Timestamp(date.getTime)
    handler ! DataAccessActor.Put(Quote(symbol, timestamp, price))
  }
}
