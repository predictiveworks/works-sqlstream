package de.kp.works.stream.sql.mqtt.ditto

object DITTO_STREAM_SETTINGS {

  val FORMAT = "de.kp.works.stream.sql.mqtt.ditto.DittoSourceProvider"

  val DITTO_ENDPOINT = "ditto.endpoint"

  /** THING **/

  /* namespace:name */
  val DITTO_THING_ID = "ditto.thing.id"

  val DITTO_THING_NAME = "ditto.thing.name"
  val DITTO_THING_NAMESPACE = "ditto.thing.namespace"

  val DITTO_FEATURE_ID = "ditto.feature.id"

  /** BASIC AUTHENTICATION **/

  val DITTO_USER = "ditto.user"
  val DITTO_PASS = "ditto.pass"

  /** OAUTH2 **/

  val DITTO_OAUTH_CLIENT_ID     = "ditto.oauth.client.id"
  val DITTO_OAUTH_CLIENT_SECRET = "ditto.oauth.client.secret"

  val DITTO_OAUTH_SCOPES = "ditto.oauth.scopes"
  val DITTO_OAUTH_TOKEN_ENDPOINT = "ditto.oauth.token.endpoint"

  /** TRUST STORE **/

  val DITTO_TRUSTSTORE_LOCATION = "ditto.truststore.location"
  val DITTO_TRUSTSTORE_PASSWORD = "ditto.truststore.password"

  /** PROXY **/

  val DITTO_PROXY_HOST = "ditto.proxy.host"
  val DITTO_PROXY_PORT = "ditto.proxy.port"

  /** HANDLER **/

  val DITTO_THING_CHANGES = "ditto.thing.changes"
  val DITTO_THING_CHANGES_HANDLER = "DITTO_THING_CHANGES"

  val DITTO_FEATURES_CHANGES = "ditto.features.changes"
  val DITTO_FEATURES_CHANGES_HANDLER = "DITTO_FEATURES_CHANGES"

  val DITTO_FEATURE_CHANGES = "ditto.feature.changes"
  val DITTO_FEATURE_CHANGES_HANDLER = "DITTO_FEATURE_CHANGES"

  val DITTO_LIVE_MESSAGES = "ditto.live.messages"
  val DITTO_LIVE_MESSAGES_HANDLER = "DITTO_LIVE_MESSAGES"

  val PERSISTENCE = "persistence"
  /*
   * The schema type controls the output schema
   * assigned to the incoming MQTT stream
   */
  val SCHEMA_TYPE = "schema.type"

}
