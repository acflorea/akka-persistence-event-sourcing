package akka.persistence.titan

import com.thinkaurelius.titan.core.{TitanFactory, TitanGraph}
import com.thinkaurelius.titan.graphdb.database.StandardTitanGraph
import com.typesafe.config.Config
import org.apache.commons.configuration.BaseConfiguration
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._

/**
 * Created by aflorea on 18.07.2016.
 */
class TitanPluginConfig(conf: Config) {

  final val logger = LoggerFactory.getLogger(getClass.getName)

  lazy val graph = initGraph

  private def initGraph: TitanGraph = {

    val graphConfiguration = new BaseConfiguration

    val map: Map[String, Object] = conf.getConfig("graph").entrySet().asScala.map({ entry =>
      entry.getKey -> entry.getValue.unwrapped()
    })(collection.breakOut)

    map.foreach { entry =>
      graphConfiguration.addProperty(entry._1, entry._2)
    }

    TitanFactory.open(graphConfiguration)
  }


}
