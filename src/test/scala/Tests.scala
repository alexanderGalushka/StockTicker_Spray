/**
 * Created by oinbar on 5/10/15.
 */
import scalaj.http.HttpResponse
import scalaj.http.Http

object Tests extends App{



  def testCurrentPriceQuery() = {
    // fixme

    val symbol = ???

    val response: HttpResponse[String] = Http("http://localhost:8000/price/" + symbol + "/current" )
      .timeout(connTimeoutMs = 10000, readTimeoutMs = 50000)
      .asString

    assert(response == "price at latest date for symbol")
  }


  def testPriceQuery() = {
    // fixme

    val symbol = ???
    val days = ???

    val response: HttpResponse[String] = Http("http://localhost:8000/price/" + symbol + "/" + days + "/current" )
      .timeout(connTimeoutMs = 10000, readTimeoutMs = 50000)
      .asString

    assert(response == "price at n days back for symbol")

  }

  def testTrendQuery() = {
    // fixme

    val symbol = ???
    val days = ???

    val response: HttpResponse[String] = Http("http://localhost:8000/price/" + symbol + "/" + days + "/current" )
      .timeout(connTimeoutMs = 10000, readTimeoutMs = 50000)
      .asString

    assert(response == "+/- for n symbol trend days back")

  }

  testCurrentPriceQuery()
  testPriceQuery()
  testTrendQuery()

}
