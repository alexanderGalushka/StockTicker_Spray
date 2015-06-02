/**
 * Created by oinbar on 5/6/15.
 * Modified by fchang on 5/13/15.
 * 
 */

import scala.io.Source
import scalaj.http._
import java.io.File

object DataInserter extends App
{

  val urls = List(
    "https://www.dropbox.com/s/lud9yacy6ax1bj0/EBAY.TXT?dl=1",
    "https://www.dropbox.com/s/o6hfth0foqeupox/EMIF.TXT?dl=1",
    "https://www.dropbox.com/s/onxb29ofx0jrsxj/INTC.TXT?dl=1",
    "https://www.dropbox.com/s/w9ahvl1s6zkpeyr/MSFT.TXT?dl=1",
    "https://www.dropbox.com/s/6a9q0mna9u8afg8/NVDA.TXT?dl=1",
    "https://www.dropbox.com/s/v6k7wusqjsk53z2/YHOO.TXT?dl=1"
  )
  def castfunction(msg: Any):Tuple2[Char,Int] = {
    msg match { case (x: Char,y:Int) => Tuple2(x,y) }
   }


  def downloadFile(url: String, destination: String)
  {
    try
    {
      val src = scala.io.Source.fromURL(url)
      val out = new java.io.FileWriter(destination)
      out.write(src.mkString)
      out.close
    }
    catch
    {
      case e: java.io.IOException => throw e
    }
  }


  val homedirectory = new File(".").getAbsolutePath()
  urls.toParArray.foreach{ url=>
    val file = url.split('/')(5).split('?')(0)
    val dirPath = new File("src/main/resources/stockdata/").getAbsolutePath

// based on Jeff Gentry's suggestion 
  val shortpath = homedirectory.substring(0,castfunction(homedirectory.toVector.zipWithIndex.filter(xs =>( xs._1  == File.separator(0))).drop(2).find(_._1 > -1).getOrElse(Tuple2(File.separator(0),-1)))._2)
val  dest = "/" + shortpath + "/team-two/src/main/resources/stockdata" + "/" + file
println(dest)

    downloadFile(url,dest)
    val header :: lines = Source.fromFile(dest).getLines().toList
    lines.foreach
    { line =>
      val Array(date, numeric_delivery_month, open, high, low, close, volume) = line.split(",")
      val endpoint = "http://localhost:8000/insert/" + file.split('.')(0) + "/" + close + "/" +  date
      val response: HttpResponse[String] = Http(endpoint)
        .timeout(connTimeoutMs = 10000, readTimeoutMs = 50000)
        .asString
    }
  }
}