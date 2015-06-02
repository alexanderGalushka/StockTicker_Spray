package teamtwo.webservice.endpoints

import akka.actor.Props
import akka.pattern.ask
import spray.routing.RequestContext
import teamtwo.webservice.db.DataAccessActor
import teamtwo.webservice.db.DataAccessActor.GetTrend
import teamtwo.webservice.endpoints.TrendQueryHandlerActor.TrendQuery

import teamtwo.webservice.server.StockTickerServer.{actorSystem, timeout}


object TrendQueryHandlerActor {
  sealed trait TrendQueryHandlerMessage
  case class TrendQuery(symbol: String, daysBack: Int) extends TrendQueryHandlerMessage

  def props(requestContext: RequestContext) = Props(classOf[TrendQueryHandlerActor], requestContext)
}

/**
 *  Endpoint actor for handling trend based requests over N-days.
 *
 *  By convention 0-days back is "current", 1-days back is "end of day yesterday", 2-days back is "end of day the day
 *  before yesterday" and so on.
 */
case class TrendQueryHandlerActor(requestContext: RequestContext) extends WebServiceActor
{
  val dataAccessActor = actorSystem.actorOf(DataAccessActor.props)

  def receive =
  {
    case TrendQuery(requestedSymbol, daysBack) => handleResponse(dataAccessActor ? GetTrend(requestedSymbol, daysBack), requestedSymbol, sender())
  }

}

