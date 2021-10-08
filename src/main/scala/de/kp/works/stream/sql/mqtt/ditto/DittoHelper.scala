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
import org.eclipse.ditto.client.configuration._
import org.eclipse.ditto.client.messaging._

import org.eclipse.ditto.model.base.json.JsonSchemaVersion
import com.neovisionaries.ws.client.WebSocket

object DittoHelper {

  def getMessagingProvider(options:DittoOptions): MessagingProvider = {
    
    val builder = WebSocketMessagingConfiguration.newBuilder()

    /* See Bosch IoT examples */
    builder.jsonSchemaVersion(JsonSchemaVersion.V_2)
    
    val endpoint = options.getEndpoint
    builder.endpoint(endpoint)

    val proxyConfiguration: ProxyConfiguration = {

      val (proxyHost, proxyPort) = options.getProxy
      if (proxyHost.isDefined && proxyPort.isDefined) {

        val proxyConf = ProxyConfiguration.newBuilder()
          .proxyHost(proxyHost.get)
          .proxyPort(Integer.parseInt(proxyPort.get))
          .build

        proxyConf

      } else null
      
    }

    val (location, password) = options.getTrustStore
    if (location.isDefined && password.isDefined) {

      val trustStoreConf = TrustStoreConfiguration.newBuilder()
        .location(new java.net.URL(location.get))
        .password(password.get)
        .build

      builder.trustStoreConfiguration(trustStoreConf)
            
    }
    
    val authProvider = getAuthProvider(proxyConfiguration, options)
    MessagingProviders.webSocket(builder.build(), authProvider)
    
  }
  
  def getAuthProvider(proxyConf:ProxyConfiguration, options:DittoOptions):AuthenticationProvider[WebSocket] = {

    val (user, pass) = options.getUserAndPass
    if (user.isDefined && pass.isDefined) {
      
      /** BASIC AUTHENTICATION **/

      val basicAuthConf = BasicAuthenticationConfiguration.newBuilder()
        .username(user.get)
        .password(pass.get)
 
      if (proxyConf != null) basicAuthConf.proxyConfiguration(proxyConf)  

      AuthenticationProviders.basic(basicAuthConf.build)
      
    } else {
      
      /** AUTH2 AUTHENTICATION **/
      
      try {
        
        val scopes = {
          val tokens = options.getOAuthScopes.get.split(",")
          
          val s = new java.util.ArrayList[String]()
          tokens.foreach(t => s.add(t.trim))
          
          s
        }
        
        val oAuthConf = ClientCredentialsAuthenticationConfiguration.newBuilder()
          .clientId(options.getOAuthClientId.get)
          .clientSecret(options.getOAuthClientSecret.get)
          .scopes(scopes)
          .tokenEndpoint(options.getOAuthTokenEndpoint.get)
 
        if (proxyConf != null) oAuthConf.proxyConfiguration(proxyConf)  
        AuthenticationProviders.clientCredentials(oAuthConf.build)
        
      } catch {
        case _: Throwable =>
          throw new IllegalArgumentException("Missing parameters for OAuth authentication.")
      }

    }

  }
  
}