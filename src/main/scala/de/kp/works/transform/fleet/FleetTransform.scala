package de.kp.works.transform.fleet

import com.google.gson.JsonElement
import de.kp.works.transform.BaseTransform
import org.apache.spark.sql.Row

object FleetTransform extends BaseTransform {

  def fromValues(eventType: String, eventData: JsonElement): Option[Seq[Row]] = {

    val tokens = eventType.split("\\/")
    ???
  }

}
