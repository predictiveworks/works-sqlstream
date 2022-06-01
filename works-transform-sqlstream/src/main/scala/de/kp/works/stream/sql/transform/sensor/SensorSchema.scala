package de.kp.works.stream.sql.transform.sensor

import org.apache.spark.sql.types.{ArrayType, StringType, StructField, StructType}

object SensorSchema {

  def schema():StructType = {

    val fields = Array(
      /*
       * Each sensor event refers to a certain sensor, uniquely
       * identified by its `id` and `type`
       */
      StructField("sensor_id",   StringType, nullable = false),
      StructField("sensor_type", StringType, nullable = false),
      /*
       * Each sensor is specified with a list of attributes,
       * and each attribute is described by name, type, value
       * and metadata.
       *
       * For a common representation, all attribute values are
       * represented by its STRING values
       */
      StructField("attr_name",  StringType, nullable = false),
      StructField("attr_type",  StringType, nullable = false),
      StructField("attr_value", StringType, nullable = false)
    )

    StructType(fields)

  }

}
