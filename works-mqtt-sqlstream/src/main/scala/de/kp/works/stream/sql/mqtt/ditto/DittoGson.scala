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
import java.util.Optional
import com.google.gson._
import org.eclipse.ditto.client.changes._
import org.eclipse.ditto.client.live.messages.RepliableMessage
import org.eclipse.ditto.json
import org.eclipse.ditto.model.things._

object DittoGson {
  /**
   * This method transforms a [ThingChange] message
   * into a JSON object:
   *
   * {
   *    timestamp: ...
   *    name: ...
   *    namespace: ...
   *    features: [
   *      {
   *        id: ....
   *        properties: [
   *          {
   *            name: ...
   *            type: ...
   *            value: ...
   *          }
   *        ]
   *      }
   *    ]
   * }
   */
  def thing2Gson(change: ThingChange): String = {
    /*
     * The thing consumer listens to thing updates, 
     * i.e. create & delete actions are ignored
     */
    val action = change.getAction
    if (action.name != "UPDATED") return null
  
    val thing = change.getThing
    if (!thing.isPresent) return null
      
    val entity = thing.get
    /* 
     * The thing consumer listens to things with 
     * features assigned
     */
    if (!entity.getFeatures.isPresent) return null
    
    /* Name and namespace of thing */    
    val (name, namespace) = getNameNS(entity.getEntityId)  
   
    val gson = new JsonObject()
    
    /* Timestamp of change */
    val ts = getTime(change)
    gson.addProperty("timestamp", ts)
    
    gson.addProperty("name", name)
    gson.addProperty("namespace", namespace)
    
    /* Features */
    
    val features = features2Gson(entity.getFeatures.get)
    gson.add("features", features)
    
    gson.getAsString

  }
  /**
   * This method transforms a [FeaturesChange] message
   * into a JSON object:
   *
   * {
   *    timestamp: ...
   *    features: [
   *      {
   *        id: ....
   *        properties: [
   *          {
   *            name: ...
   *            type: ...
   *            value: ...
   *          }
   *        ]
   *      }
   *    ]
   * }
   */
  def features2Gson(change: FeaturesChange): String = {
    
    val gson = new JsonObject()
    
    /* Timestamp of change */
    val ts = getTime(change)
    gson.addProperty("timestamp", ts)
    
    val features = features2Gson(change.getFeatures)
    gson.add("features", features)
    
    gson.toString
    
  }
  /**
   * This method transforms a [FeatureChange] message
   * into a JSON object:
   *
   * {
   *    timestamp: ...
   *    id: ....
   *    properties: [
   *      {
   *        name: ...
   *        type: ...
   *        value: ...
   *      }
   *    ]
   * }
   */
  def feature2Gson(change: FeatureChange): String = {
    
    val gson = new JsonObject()
    
    /* Timestamp of change */
    val ts = getTime(change)
    gson.addProperty("timestamp", ts)

    val feature = change.getFeature

    /* Feature identifier */
    gson.addProperty("id", feature.getId)
     
    /* Properties */
    if (feature.getProperties.isPresent) {
      
      val properties = properties2Gson(feature.getProperties.get)
      gson.add("properties", properties)
      
    } else
      gson.add("properties", new JsonArray())
      
    gson.toString
    
  }
  /**
   * This method transforms a [LiveMessage] message
   * into a JSON object:
   *
   * {
   *    timestamp: ...
   *    name: ...
   *    namespace: ...
   *    subject: ....
   *    payload: ....
   * }
   */

  def message2Gson(message:RepliableMessage[String, Any]): String = {
    
    val gson = new JsonObject()
    
    val payload = message.getPayload
    if (!payload.isPresent) return null
    
    val content = payload.get

    /* Timestamp of the message */    
    val ts = {
       if (message.getTimestamp.isPresent) {
         message.getTimestamp.get.toInstant.toEpochMilli
         
       } else
         new java.util.Date().getTime
    }

    gson.addProperty("timestamp", ts)
    
    /* Name and namespace of thing */   
    gson.addProperty("name", message.getThingEntityId.getName)
    gson.addProperty("namespace", message.getThingEntityId.getNamespace)
    
    gson.addProperty("subject", message.getSubject)
    gson.addProperty("payload", content)
    
    gson.toString
    
  }
  /** HELPER METHODS **/

  private def features2Gson(features: Features): JsonArray = {
    
    val gFeatures = new JsonArray()

    /* Extract features */
    val iter = features.stream.iterator
    while (iter.hasNext) {

      val feature = iter.next
      
      /* Feature identifier */
      val gFeature = new JsonObject()      
      gFeature.addProperty("id", feature.getId)
     
      /* Properties */
      if (feature.getProperties.isPresent) {
        
        val properties = properties2Gson(feature.getProperties.get)
        gFeature.add("properties", properties)
        
      } else
        gFeature.add("properties", new JsonArray())
        
      gFeatures.add(gFeature)
      
    }
    
    gFeatures
    
  }

  private def properties2Gson(properties:FeatureProperties):JsonArray = {
    
    val gProperties = new JsonArray()
    
    /* Extract properties */
    val iter = properties.stream.iterator
    while (iter.hasNext) {
      
      val property = iter.next
      gProperties.add(property2Gson(property))
      
    }
    gProperties

  }

  private def property2Gson(property: json.JsonField): JsonObject = {

    val gson = new JsonObject()
    
    val name = property.getKeyName
    gson.addProperty("name", name)

    val value = property.getValue  
    value2Gson(gson, value)
    
    gson
    
  }
  
  private def value2Gson(gson: JsonObject, value: json.JsonValue):Unit = {

    if (value.isNull) {
      
      gson.addProperty("type", "NULL")
      gson.add("value", JsonNull.INSTANCE)
      
    }  
    else if (value.isBoolean) {
      
      gson.addProperty("type", "BOOLEAN")
      gson.addProperty("value", value.asBoolean)
      
    }
    else if (value.isDouble) {
      
      gson.addProperty("type", "DOUBLE")
      gson.addProperty("value", value.asDouble)
      
    }
    else if (value.isInt) {
      
      gson.addProperty("type", "INTEGER")
      gson.addProperty("value", value.asInt)
      
    }
    else if (value.isLong) {
      
      gson.addProperty("type", "LONG")
      gson.addProperty("value", value.asLong)
      
    }
    else if (value.isString) {
      
      gson.addProperty("type", "STRING")
      gson.addProperty("value", value.asString)
      
    }
    else if (value.isArray) {

      gson.addProperty("type", "ARRAY")
      gson.add("value", array2Gson(value.asArray))
      
    }
    else if (value.isObject) {

      gson.addProperty("type", "OBJECT")
      gson.add("value", object2Gson(value.asObject))
      
    }
    
  }
  
  private def array2Gson(value: json.JsonArray): JsonArray = {
    
    val gson = new JsonArray()
    
    val iter = value.iterator
    while (iter.hasNext) {
    
      val item = new JsonObject()
      value2Gson(item, iter.next)

      gson.add(item)
    
    }

    gson
    
  }
  
  private def object2Gson(obj: json.JsonObject): JsonObject = {
    
    val gson = new JsonObject()
    
    val names = obj.getKeys.iterator
    while (names.hasNext) {
      
      val name = names.next
      val value = obj.getValue(name)
      
      val inner = new JsonObject()
      if (value.isPresent) {
        
        value2Gson(inner, value.get)
        
      } else {
        
        inner.addProperty("type", "NULL")
        inner.add("value", JsonNull.INSTANCE)
        
      }
      
      gson.add(name.toString, inner)
      
    }
    
    gson

  }

  private def getNameNS(thingId: Optional[ThingId]): (String, String) = {

    /* Name and namespace of thing */
    val (name, namespace) = {
      
      if (!thingId.isPresent)
        ("*", "*")
      
      else {
        (thingId.get.getName, thingId.get.getNamespace)
      }
    
    }

    (name, namespace)
        
  }
  
  /*
   * A helper method to extract the timestamp 
   * of a certain change
   */
  private def getTime(change:Change): Long = {
    
    val instant = change.getTimestamp

    if (instant.isPresent) {
      instant.get.toEpochMilli
    
    } else new java.util.Date().getTime
    
  }
  
}