package teamtwo.webservice.endpoints

import akka.actor.{ActorRef, Actor}
import spray.http.StatusCode
import spray.http.StatusCodes._
import spray.routing.RequestContext
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future
import scala.util.{Success, Failure}

/**
 * Defines an actor for use in a Spray web service situation. Provides a complete()
 * method which will complete a request w/ some serialized object with a status code
 * and then killing the actor
 */
trait WebServiceActor extends Actor {
  implicit val system = context.system
  val requestContext: RequestContext

  /**
   * Returns a status code and object payload to the request context and then shuts down the Actor
   */
  def complete[T](status: StatusCode, responseObj: T) = {
    requestContext.complete(status, responseObj.toString)
    context.stop(self)
  }

  def handleResponse[T](priceResponse: Future[Any], symbol: String, sender: ActorRef): Unit = {
    val optionIntPrice = priceResponse.mapTo[Option[T]]
    optionIntPrice onComplete {
      case Success(v) if v.isDefined => v foreach {x => complete(OK, x)}
      case Success(v) => complete(ServiceUnavailable, s"No data available for symbol $symbol")
      case Failure(e) => complete(InternalServerError, s"Error retrieving price data for $symbol")
    }
  }


}
