
# MQTT Streaming Sources

## Multiple MQTT protocols

The current implementation of the MQTT streaming source leverages *Eclipse Paho* and *HiveMQ* as clients
to connect to a certain MQTT Broker. This approach offers support for protocol versions MQTT v3.1, v3.1.1 and
MQTT v5 (HiveMQ).

## Schema support

The current implementation supports plain MQTT events with multiple events formats per selected topics. The MQTT
streaming source also serves as an endpoint for Works. **Beats**.

These (data source) beats are implemented as standalone Akka-based HTTP(s) servers and connect to

* FIWARE Context Brokers,
* OPC-UA Servers,
* OpenCTI Platform,
* Osquery based Fleet Management Platform,
* ThingsBoard Gateway Service, and
* Zeek Sensor.

In addition, schema support is provided for other sources like *The Things Network* and its uplink MQTT formats.

