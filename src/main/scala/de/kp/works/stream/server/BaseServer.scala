package de.kp.works.stream.server
/*
 * Copyright (c) 2020 - 2021 Dr. Krusche & Partner PartG. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 * @author Stefan Krusche, Dr. Krusche & Partner PartG
 *
 */

import scopt.OptionParser

trait BaseServer {

  protected case class CliConfig(
    /*
     * The command line interface supports the provisioning
     * of a typesafe config compliant configuration file
     */
    conf:String = null
  )

  protected var programName:String
  protected var programDesc:String

  private val fileHelpText = "The path to the configuration file."

  def main(args:Array[String]):Unit = {

    try {
      launch(args)

    } catch {
      case t: Throwable =>

        println("[ERROR] ------------------------------------------------")
        println(s"[ERROR] $programName cannot be started: " + t.getMessage)
        println("[ERROR] ------------------------------------------------")
        /*
         * Sleep for 10 seconds so that one may see error messages
         * in Yarn clusters where logs are not stored.
         */
        Thread.sleep(10000)
        sys.exit(1)

    }

  }

  protected def launch(args:Array[String]):Unit

  protected def buildParser:OptionParser[CliConfig] = {

    val parser = new OptionParser[CliConfig](programName) {

      head(programDesc)

      opt[String]("c")
        .text(fileHelpText)
        .action((x, c) => c.copy(conf = x))

    }

    parser

  }

  protected def start(args:Array[String], service:BaseService):Unit = {

    /* Command line argument parser */
    val parser = buildParser

    /* Parse the argument and then run */
    parser.parse(args, CliConfig()).foreach{ c =>

      val cfg = if (c.conf == null) {

        println("[INFO] ------------------------------------------------")
        println(s"[INFO] Launch $programName with internal configuration.")
        println("[INFO] ------------------------------------------------")

        None

      } else {

        println("[INFO] ------------------------------------------------")
        println(s"[INFO] Launch $programName with external configuration.")
        println("[INFO] ------------------------------------------------")

        val source = scala.io.Source.fromFile(c.conf)
        val config = source.getLines.mkString("\n")

        source.close
        Some(config)
      }

      service.start(cfg)

      println("[INFO] ------------------------------------------------")
      println(s"[INFO] $programName service started.")
      println("[INFO] ------------------------------------------------")

    }

  }
}
