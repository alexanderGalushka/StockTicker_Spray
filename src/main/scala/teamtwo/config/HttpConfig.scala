package teamtwo.config

import com.typesafe.config.ConfigFactory

object HttpConfig {
  private val httpConfig = ConfigFactory.load.getConfig("http")

  lazy val interface = httpConfig.getString("interface")
  lazy val port = httpConfig.getInt("port")
}

