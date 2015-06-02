package teamtwo.webservice.endpoints

import akka.actor.{ActorRef, Props}
import akka.pattern.ask
import spray.routing.RequestContext
import teamtwo.webservice.db.DataAccessActor
import DataAccessActor.{GetCurrent, Get}
import teamtwo.webservice.endpoints.PriceQueryHandlerActor.{PriceQuery, CurrentPriceQuery}
import spray.http.StatusCodes._
import teamtwo.webservice.server.StockTickerServer.{actorSystem, timeout}

import scala.concurrent.Future
import scala.util.Failure

object PriceQueryHandlerActor{
  sealed trait PriceQueryHandlerMessage
  case class CurrentPriceQuery(symbol: String) extends PriceQueryHandlerMessage
  case class PriceQuery(symbol: String, days: Int) extends PriceQueryHandlerMessage

  def props(requestContext: RequestContext) = Props(classOf[PriceQueryHandlerActor], requestContext)
}

/**
 * Endpoint actor which will handle any requests to return the price of a stock. It can return either the current
 * price or a price as of N-days back.
 *
 * By convention 0-days back is "current", 1-days back is "end of day yesterday", 2-days back is "end of day the day
 * before yesterday" and so on.
 */
case class PriceQueryHandlerActor(requestContext: RequestContext) extends WebServiceActor{
  val dataAccessActor = actorSystem.actorOf(DataAccessActor.props)

  def receive = {
    case CurrentPriceQuery(requestedSymbol) => handleResponse(dataAccessActor ? GetCurrent(requestedSymbol), requestedSymbol, sender())
    case PriceQuery(requestedSymbol, daysBack) => handleResponse(dataAccessActor ? Get(requestedSymbol, daysBack), requestedSymbol, sender())
  }


}
