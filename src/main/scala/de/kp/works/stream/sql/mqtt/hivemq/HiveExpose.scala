package de.kp.works.stream.sql.mqtt.hivemq

trait HiveExpose {

  def messageArrived(topic:String, payload: Array[Byte], qos: Int, duplicate: Boolean, retained: Boolean): Unit

}
