package de.kp.works.stream.sql.server

object StreamServer extends BaseServer {

  override var programName: String = "StreamServer"
  override var programDesc: String = "Manage & monitor SQL streams."

  override def launch(args: Array[String]): Unit = {

    val service = new StreamService()
    start(args, service)

  }

}
