package de.kp.works.stream.sql.transform.fleet

import com.google.gson.JsonElement
import de.kp.works.stream.sql.transform.BaseTransform
import org.apache.spark.sql.Row

object FleetTransform extends BaseTransform {

  def fromValues(eventType: String, eventData: JsonElement): Option[Seq[Row]] = {

    val tokens = eventType.split("\\/")
    ???
  }

}
