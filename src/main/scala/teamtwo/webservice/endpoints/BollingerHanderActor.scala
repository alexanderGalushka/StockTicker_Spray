package teamtwo.webservice.endpoints

import akka.actor.{Props}
import akka.pattern.ask
import spray.routing.RequestContext
import teamtwo.webservice.db.DataAccessActor
import teamtwo.webservice.db.DataAccessActor._
import teamtwo.webservice.endpoints.BollingerHanderActor.{LowerBand, UpperBand, MidPoint}

import teamtwo.webservice.server.StockTickerServer.{actorSystem, timeout}

/**
 * Bollinger Bands are adoptive trading bands that answer the question "Are prices high or low?" on a relative basis
 * The adoptive mechanism is volatility
 *
 */
object BollingerHanderActor
{
  sealed trait BollingerHandlerMessage

  case class MidPoint(symbol: String) extends BollingerHandlerMessage
  case class UpperBand(symbol: String) extends BollingerHandlerMessage
  case class LowerBand(symbol: String) extends BollingerHandlerMessage

  def props(requestContext: RequestContext) = Props(classOf[BollingerHanderActor], requestContext)
}


case class BollingerHanderActor(requestContext: RequestContext) extends WebServiceActor
{
  val dataAccessActor = actorSystem.actorOf(DataAccessActor.props)

  def receive =
  {
    case MidPoint(requestedSymbol) => handleResponse(dataAccessActor ? GetMidPoint(requestedSymbol), requestedSymbol, sender())
    case UpperBand(requestedSymbol) => handleResponse(dataAccessActor ? GetUpperBand(requestedSymbol), requestedSymbol, sender())
    case LowerBand(requestedSymbol) => handleResponse(dataAccessActor ? GetLowerBand(requestedSymbol), requestedSymbol, sender())
  }


}
