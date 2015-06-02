package teamtwo.webservice.endpoints

import akka.actor.{Props}
import akka.pattern.ask
import spray.routing.RequestContext
import teamtwo.webservice.db.DataAccessActor
import teamtwo.webservice.db.DataAccessActor.GetMovingAverage

import teamtwo.webservice.endpoints.MovingAverageQueryHandlerActor.MovingAverageQuery
import teamtwo.webservice.server.StockTickerServer.{actorSystem, timeout}

object MovingAverageQueryHandlerActor
{
  sealed trait MovingAverageQueryHandlerMessage
  case class MovingAverageQuery(symbol: String, days: Int, slidingWindow: Int) extends MovingAverageQueryHandlerMessage
  def props(requestContext: RequestContext) = Props(classOf[MovingAverageQueryHandlerActor], requestContext)
}

/**
 *  Endpoint actor for handling moving average based requests over N-days.
 *
 *  By convention 0-days back is "current", 1-days back is "end of day yesterday", 2-days back is "end of day the day
 *  before yesterday" and so on.
 */
case class MovingAverageQueryHandlerActor(requestContext: RequestContext) extends WebServiceActor
{
  val dataAccessActor = actorSystem.actorOf(DataAccessActor.props)
  def receive =
  {
    case MovingAverageQuery(requestedSymbol, daysBack, slidingWindow) => handleResponse(dataAccessActor ? GetMovingAverage(requestedSymbol, daysBack, slidingWindow), requestedSymbol, sender())
  }
}
