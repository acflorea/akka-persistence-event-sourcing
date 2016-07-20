package akka.persistence.titan


import com.thinkaurelius.titan.core.{Cardinality, TitanFactory, TitanGraph}
import com.typesafe.config.Config
import org.apache.commons.configuration.BaseConfiguration
import org.slf4j.LoggerFactory
import akka.persistence.titan.TitanCommons._

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

    val _graph = TitanFactory.open(graphConfiguration)

    // Keys
    val mngmt = _graph.openManagement()

    if (!mngmt.containsPropertyKey(TIMESTAMP_KEY)) {
      mngmt.makePropertyKey(TIMESTAMP_KEY).dataType(classOf[java.lang.Long]).cardinality(Cardinality.SINGLE).make()
    }
    if (!mngmt.containsPropertyKey(SEQUENCE_NR_KEY)) {
      mngmt.makePropertyKey(SEQUENCE_NR_KEY).dataType(classOf[java.lang.Long]).cardinality(Cardinality.SINGLE).make()
    }
    mngmt.commit()

    _graph
  }


}
