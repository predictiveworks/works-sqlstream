package de.kp.works.stream.sql.mqtt.ditto
/*
 * Copyright (c) 2019 - 2021 Dr. Krusche & Partner PartG. All rights reserved.
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
import de.kp.works.stream.sql.Logging
import org.eclipse.ditto.client.changes._
import org.eclipse.ditto.client.live.messages.RepliableMessage
import org.eclipse.ditto.client.messaging._
import org.eclipse.ditto.client.{DittoClient => EclipseClient, DittoClients => EclipseClients}
import org.eclipse.ditto.model.base.common.HttpStatusCode

object DittoClient {

  def build(options:DittoOptions):DittoClient =
    new DittoClient(options)

}

class DittoClient(options:DittoOptions) extends Logging {

  private var expose:DittoExpose = _

  private var client:EclipseClient = _

  def setExpose(dittoExpose:DittoExpose):Unit = {
    expose = dittoExpose
  }

  /**
   * This method suspends the consumption of Ditto's event
   * streams, and if registered, de-registers the respective
   * event handlers
   */
  def disconnect() {

    if (client != null) {
      
      /** CHANGE EVENTS **/
      
      client.twin().suspendConsumption()

      if (options.isThingChanges)
        client.twin().deregister(options.getThingsChangeHandler)
      
      if (options.isFeaturesChanges)
        client.twin().deregister(options.getFeaturesChangeHandler)
      
      if (options.isFeatureChanges)
        client.twin().deregister(options.getFeatureChangeHandler)

      /** LIVE MESSAGES **/
      
      client.live().suspendConsumption()
        
      if (options.isLiveMessages)
        client.live().deregister(options.getLiveMessagesHandler)

      client.destroy()
      
    }

  }

  def connect() {
    /*
     * Build Ditto web socket client
     */
    val messagingProvider:MessagingProvider = DittoHelper.getMessagingProvider(options)
    client = EclipseClients.newInstance(messagingProvider)
    
    /*
     * This Ditto web socket client subscribes to two protocol commands:
     * 
     * - PROTOCOL_CMD_START_SEND_EVENTS   :: "START-SEND-EVENTS"
     * - PROTOCOL_CMD_START_SEND_MESSAGES :: "START-SEND-MESSAGES"
     * 
     * Subscription to events is based on Ditto's twin implementation 
     * and refers to the TwinImpl.CONSUME_TWIN_EVENTS_HANDLER which
     * is initiated in the twin's doStartConsumption method
     * 
     * Subscription to events is based on Ditto's live implementation
     * and refers to the LiveImpl.CONSUME_LIVE_MESSAGES_HANDLER which
     * is initiated in the live's doStartConsumption method
     * 
     */
    client.twin().startConsumption().get() // EVENTS
    client.live().startConsumption().get() // MESSAGES
    
    registerForTwinEvents()
    registerForLiveMessages()
    
  }
  /**
   * This is the Ditto callback to listen to twin events
   */
  private def registerForTwinEvents() {
    
    val twin = client.twin()

    val thingId = options.getThingId
    /*
     * Check whether a certain feature identifier is provided to 
     * restrict events to a certain feature
     */
    val featureId = options.getFeatureId
      
    /***** THING CHANGES *****/

    if (options.isThingChanges) {

      val consumer = new java.util.function.Consumer[ThingChange] {
        override def accept(change:ThingChange):Unit = {

          val gson = DittoGson.thing2Gson(change)
          if (gson != null) expose.messageArrived(DittoMessage(`type` = "thing", payload = gson))

        }
      }

      val handler = options.getThingsChangeHandler
      if (thingId == null) {

        /* Register for changes of all things */
        twin.registerForThingChanges(handler, consumer)

      } else {

        /* Register for changes of thing with thingId */
        twin.forId(thingId).registerForThingChanges(handler, consumer)

      }

    }

    /***** FEATURES CHANGES *****/

    if (options.isFeaturesChanges){

      val consumer = new java.util.function.Consumer[FeaturesChange] {
        override def accept(change:FeaturesChange):Unit = {

          val gson = DittoGson.features2Gson(change)
          if (gson != null) expose.messageArrived(DittoMessage(`type` = "features", payload = gson))

        }
      }

      val handler = options.getFeaturesChangeHandler
      if (thingId == null) {
        /*
         * Register feature set changes of all things, as we currently
         * do not support the provisioning of a certain thing
         */
        twin.registerForFeaturesChanges(handler, consumer)

      } else {
        /*
         * Register feature set changes of all things, as we currently
         * do not support the provisioning of a certain thing
         */
        twin.forId(thingId).registerForFeaturesChanges(handler, consumer)

      }

    }

    /***** FEATURE CHANGES *****/

    if (options.isFeatureChanges) {

      val consumer = new java.util.function.Consumer[FeatureChange] {
        override def accept(change:FeatureChange):Unit = {

          val gson = DittoGson.feature2Gson(change)
          if (gson != null) expose.messageArrived(DittoMessage(`type` = "feature", payload = gson))

        }
      }

      val handler =options.getFeatureChangeHandler
      if (thingId != null) {

        if (featureId != null) {
          twin.registerForFeatureChanges(handler, featureId, consumer)

        } else
          twin.registerForFeatureChanges(handler, consumer)

      } else {

        if (featureId != null)
          twin.forId(thingId).registerForFeatureChanges(handler, featureId, consumer)

        else
          twin.forId(thingId).registerForFeatureChanges(handler, consumer)

      }

    }

  }

  /**
   * This is the Ditto callback to listen to live messages
   */
  private def registerForLiveMessages() {

    val live = client.live()
    /*
     * Check whether a certain thing identifier is provided to 
     * restrict events to a certain thing
     */
    val thingId = options.getThingId
    if (options.isLiveMessages) {

      val consumer = new java.util.function.Consumer[RepliableMessage[String, Any]] {
        override def accept(message:RepliableMessage[String, Any]) {

          val gson = DittoGson.message2Gson(message)
          if (gson != null) {
            expose.messageArrived(DittoMessage(`type` = "message", payload = gson))
            message.reply().statusCode(HttpStatusCode.OK).send()

          } else {
            message.reply().statusCode(HttpStatusCode.NO_CONTENT).send()

          }

        }
      }

      val handler = options.getLiveMessagesHandler

      if (thingId != null) {
        live.forId(thingId).registerForMessage(handler, "*", classOf[String], consumer)

      } else {
        live.registerForMessage(handler, "*", classOf[String], consumer)

      }

    }

  }
  
}