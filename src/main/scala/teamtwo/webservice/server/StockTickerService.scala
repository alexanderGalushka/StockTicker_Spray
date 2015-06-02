package teamtwo.webservice.server

import akka.actor.{Props, Actor}
import spray.routing.HttpService
import teamtwo.webservice.endpoints._
import java.text.{DateFormat, SimpleDateFormat}
import java.util.Locale

object StockTickerServiceActor {
  def props = Props(classOf[StockTickerServiceActor])
}

class StockTickerServiceActor extends Actor with StockTickerService {
  def actorRefFactory = context
  def receive = runRoute(possibleRoutes)
}

/**
 * Routing service for Spray. Takes an incoming HttpRequest, looks at the URL signature and HTTP request type and uses
 * that to determine how to handle things. This is implemented in a non-blocking manner by spinning up an Actor of the
 * appropriate type (each logical endpoint has its own Actor) and handing it both the necessary information *and* the RequestContext
 * which will allow that Actor instance to fully process the request while the server is free to do other things
 */
trait StockTickerService extends HttpService {
  /*
    For each route (e.g. /price/GOOG/current or /trend/FB/5 - spin up an appropriate actor w/ the RequestContext, pass
    it any necessary information and it will a) compute the answer and b) complete the response to the client
   */
  val possibleRoutes =
    pathPrefix("price") {
      path(Segment / "current") { // Get price of symbol (Segment) at current moment
        symbol =>
          get {
            requestContext =>
              val handler = actorRefFactory.actorOf(PriceQueryHandlerActor.props(requestContext))
              handler ! PriceQueryHandlerActor.CurrentPriceQuery(symbol)
          }
      } ~
      path(Segment / IntNumber) { // Get price of symbol (Segment) at N (IntNumber) days back
        (symbol, days) =>
          get {
            requestContext =>
              val handler = actorRefFactory.actorOf(PriceQueryHandlerActor.props(requestContext))
              handler ! PriceQueryHandlerActor.PriceQuery(symbol, days)
          }
      }
    } ~
    pathPrefix("trend") {
      path(Segment / IntNumber) { // Get trend of symbol (Segment) over N (IntNumber) days
        (symbol, days) =>
          get {
            requestContext =>
              val handler = actorRefFactory.actorOf(TrendQueryHandlerActor.props(requestContext))
              handler ! TrendQueryHandlerActor.TrendQuery(symbol, days)
          }
      }
    } ~
    pathPrefix("movingaverage") {
      path(Segment / IntNumber / IntNumber) { // Get movingaverage of symbol (Segment) over N (IntNumber) days with N-sliding window
        (symbol, days, slidingWindow) =>
          get {
            requestContext =>
              val handler = actorRefFactory.actorOf(MovingAverageQueryHandlerActor.props(requestContext))
              handler ! MovingAverageQueryHandlerActor.MovingAverageQuery(symbol, days, slidingWindow)
          }
      }
    } ~
    path("insert" / Segment / Segment / Segment) { // Insert a ticker for symbol (Segment) at price X (IntNumber)
      (symbol, price, date) =>
        get {
          requestContext =>
            val priceDouble = price.toDouble
            val df = new SimpleDateFormat("yyyyMMdd hh:mm:ss", Locale.ENGLISH)
            val datetime =  df.parse(date + " 16:00:00")
            val handler = actorRefFactory.actorOf(InsertPriceHandlerActor.props(requestContext))
            handler ! InsertPriceHandlerActor.InsertPrice(symbol, priceDouble, datetime)
        }
    } ~
      pathPrefix("bollinger") {
        path(Segment / "midpoint") {
          symbol =>
            get {
              requestContext =>
                val handler = actorRefFactory.actorOf(BollingerHanderActor.props(requestContext))
                handler ! BollingerHanderActor.MidPoint(symbol)
            }
        } ~
          path(Segment / "upperband") {
            symbol =>
              get {
                requestContext =>
                  val handler = actorRefFactory.actorOf(BollingerHanderActor.props(requestContext))
                  handler ! BollingerHanderActor.UpperBand(symbol)
              }
          } ~
          path(Segment / "lowerband") {
            symbol =>
              get {
                requestContext =>
                  val handler = actorRefFactory.actorOf(BollingerHanderActor.props(requestContext))
                  handler ! BollingerHanderActor.LowerBand(symbol)
              }
          }
      }
}
