package de.kp.works.stream.sql.pubsub

/**
 * Copyright (c) 2019 - 2022 Dr. Krusche & Partner PartG. All rights reserved.
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

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.services.pubsub.PubsubScopes

import java.io.{File, FileInputStream, Serializable}

class PubSubAuth(val serviceAccountFilePath: String) extends Serializable {

  @transient var credential: Credential = _

  def getCredential: Credential = {

    if (credential == null) {
      loadFromFile()

    } else
      try {
        credential = GoogleCredential.getApplicationDefault()

      } catch {
        case t: Throwable =>
          throw new IllegalArgumentException("Unable to load credentials from environment: " + t.getLocalizedMessage)
      }

    credential

  }

  private def loadFromFile():Unit =  {

    try {

      val fis = new FileInputStream(new File(serviceAccountFilePath))
      credential = GoogleCredential
        .fromStream(fis)
        .createScoped(PubsubScopes.all())

    } catch {
      case t: Throwable =>
        throw new Exception(s"Unable to load credentials from '$serviceAccountFilePath': " + t.getLocalizedMessage)
    }

  }

}