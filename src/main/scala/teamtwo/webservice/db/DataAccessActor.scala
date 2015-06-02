package teamtwo.webservice.db

import java.sql.Timestamp
import java.util.concurrent.Executors
import java.util.{Calendar, Date}

import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.pipe
import com.mchange.v2.c3p0.ComboPooledDataSource
import com.typesafe.config.ConfigFactory
import teamtwo.webservice.db.DataAccessActor._
import scala.concurrent.{ExecutionContext, Future}
import scala.slick.driver.MySQLDriver.simple._
import scala.slick.jdbc.StaticQuery
import util.control.Breaks._
import org.apache.commons.math3.stat.regression._

object DataAccessActor {
  sealed trait DataAccessActorMessage
  case class Put(quote: Quote) extends DataAccessActorMessage
  case class GetCurrent(symbol: String) extends DataAccessActorMessage
  case class Get(symbol: String, daysBack: Int) extends DataAccessActorMessage
  case object SymbolNotFound extends DataAccessActorMessage
  case class SymbolPrice(price: Int) extends DataAccessActorMessage
  // FIXME Statistics should not belong DataAccessActor
  case class GetMovingAverage(symbol: String, daysBack: Int, slidingWindow: Int) extends DataAccessActorMessage
  case class GetMidPoint(symbol: String) extends DataAccessActorMessage
  case class GetUpperBand(symbol: String) extends DataAccessActorMessage
  case class GetLowerBand(symbol: String) extends DataAccessActorMessage
  case class GetTrend (symbol: String, days: Integer) extends DataAccessActorMessage


  object DataConfig {
    private val config = ConfigFactory.load.getConfig("database")
    lazy val jdbc = config.getString("jdbc")
    lazy val driver = config.getString("driver")
    lazy val user = config.getString("user")
    lazy val password = config.getString("password")
  }

  def props = Props(classOf[DataAccessActor])
}

class DataAccessActor extends Actor {
  // Use a separate thread pool for the data accessors so they're not tying up the global thread pool
  implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(Runtime.getRuntime.availableProcessors() - 1))

  def receive = {
    case GetCurrent(symbol) => handleRequest(Future {StockTickerDao.getCurrentPrice(symbol)}, sender())
    case Get(symbol, days) => handleRequest(Future {StockTickerDao.getPriceNDaysBack(symbol, days)}, sender())
    case Put(quote) => StockTickerDao.insertQuote(quote)
    case GetMovingAverage(symbol, daysback, slidingWindow) => handleRequest[String](Future {StockTickerDao.getMovingAverageNDaysBack(symbol, daysback, slidingWindow)}, sender())
    case GetMidPoint(symbol) => handleRequest[Double](Future {StockTickerDao.getAverageNDaysBack(symbol, 20)}, sender())
    case GetUpperBand(symbol) => handleRequest[Double](Future {StockTickerDao.getUpperBand(symbol)}, sender())
    case GetLowerBand(symbol) => handleRequest[Double](Future {StockTickerDao.getLowerBand(symbol)}, sender())
    case GetTrend(symbol, days) => handleRequest[String](Future {StockTickerDao.getTrend(symbol, days)}, sender())
  }

  def handleRequest[T](responseData: Future[Option[T]], sender: ActorRef): Unit = responseData pipeTo sender

}

object StockTickerDao {
  // Set up a connection pool for our database connection
  val cpds = new ComboPooledDataSource()
  cpds.setJdbcUrl(DataConfig.jdbc)
  cpds.setUser(DataConfig.user)
  cpds.setPassword(DataConfig.password)
  cpds.setDriverClass(DataConfig.driver)
  cpds.setMaxPoolSize(40)

  val database = Database.forDataSource(cpds)

  println("StockTickerDao initialized")

  def getCurrentPrice(symbol: String): Option[Double] =
  {
    database withSession {implicit session => Quotes.quotes.filter(_.symbol === symbol).sortBy(_.datetime.desc).list.headOption} map {_.price}
  }

  def getPriceNDaysBack(symbol: String, days: Int): Option[Double] =
  {
    val datetime = new Timestamp(getPreviousDateString(days).getTime)
    database withSession {implicit session => Quotes.quotes.filter(_.symbol === symbol).filter(_.datetime <= datetime).sortBy(_.datetime.desc).list.headOption} map {_.price}
  }

  def insertQuote(quote: Quote): Unit =
  {
    database withSession { implicit session => Quotes.quotes.map(p => (p.symbol, p.datetime, p.price)).insert((quote.symbol, quote.datetime, quote.price))}
  }

  def getListOfPrices (symbol: String, days: Int): List[Double] =
  {
    val query: String = "SELECT price FROM (SELECT * FROM quote WHERE symbol =  ? AND DATETIME > ( DATE_SUB( NOW( ) , INTERVAL ? DAY ) ) ORDER BY DATETIME DESC)t1"
    val stocksByPrice = database withSession {implicit session => StaticQuery.query[(String,Int),Double](query)}
    database withSession {implicit session => stocksByPrice(symbol, days).list}
  }

  /********************************************************************************************************************/

  def getTrend (symbol: String, days: Int): Option[String] =
  {
    val pricesList = getListOfPrices(symbol, days)

    if (!pricesList.isEmpty)
    {
      // have to reverse the array cause prices are arranged by dates in the desc. order
      // mock up the dates as array indices
      val regressionData = pricesList.reverse.toArray.zipWithIndex.map{ case (price,i) => Array(i.toDouble,price) }
      var regression = new SimpleRegression()
      regression.addData(regressionData)

      if (regression.getSlope() < 0)
      {
        Option("-")
      }
      else
      {
        Option("+")
      }
    }
    else
    {
      Option("List is empty")
    }
  }

  def getAverageNDaysBack(symbol: String, days: Int): Option[Double] =
  {
    val pricesList = getListOfPrices (symbol, days)
    if (!pricesList.isEmpty)
    {
      Option(pricesList.sum / pricesList.size.toDouble)
    }
    else
    {
      Option(0d)
    }
  }

  def getMovingAverageNDaysBack(symbol: String, days: Int, slidingWindow: Int): Option[String] =
  {
    // FIXME Rewrite the code in a functional way!!!!

    var resultString = ""

    if (days >slidingWindow )
    {
      val pricesList = getListOfPrices(symbol, days)
      if (!pricesList.isEmpty)
      {
        val resultList = scala.collection.mutable.ListBuffer.empty[Double]
        breakable
        {
          for (i <- 0 until pricesList.size)
          {
            if (i + slidingWindow <= pricesList.size)
            {
              val tempMovingList = scala.collection.mutable.ListBuffer.empty[Double]
              for (j <- i until i + slidingWindow)
              {
                tempMovingList.+=(pricesList(j))
              }
              resultList.+=(tempMovingList.sum / slidingWindow.toDouble)
            }
            else
            {
              break
            }
          }
        }
        resultString = resultList.toArray.mkString("\n")
      }
      else
      {
        resultString = "Query has returned empty list of prices from the DB"
      }
    }
    else
    {
      resultString = "Sliding widow should be less than requested days back"
    }
    Option(resultString)
  }

  def getUpperBand(symbol: String): Option[Double] =
  {
    val pricesList = getListOfPrices (symbol, 20)
    if (!pricesList.isEmpty)
    {
      val mean = pricesList.sum / pricesList.size.toDouble
      val stdDev = calcStdDev(pricesList)
      Option(mean + 2 * stdDev)
    }
    else
    {
      Option(0d)
    }
  }

  def getLowerBand(symbol: String): Option[Double] =
  {
    val pricesList = getListOfPrices (symbol, 20)
    if (!pricesList.isEmpty)
    {
      val mean = pricesList.sum / pricesList.size.toDouble
      val stdDev = calcStdDev(pricesList)
      Option(mean - 2 * stdDev)
    }
    else
    {
      Option(0d)
    }
  }

  def calcStdDev (pricesList: List[Double]): Double =
  {
    val mean = pricesList.sum / pricesList.size.toDouble
    val devs = pricesList.map(price => (price - mean) * (price - mean))
    Math.sqrt(devs.sum / pricesList.size)
  }

  def getPreviousDateString(daysback: Int): Date =
  {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DATE, -1 * daysback)
    cal.set(Calendar.HOUR_OF_DAY, 23)
    cal.set(Calendar.MINUTE, 59)
    cal.set(Calendar.SECOND, 59)
    cal.set(Calendar.MILLISECOND, 999)
    cal.getTime
  }
}



