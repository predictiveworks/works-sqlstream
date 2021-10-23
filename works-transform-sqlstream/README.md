
# Transformation Support for Structured Streaming

This package transforms Works. Beats events into Apache Spark Sql schema compliant [Row]s. 
Event transformation is supported for

* FIWARE
* FLEET (OSQUERY)
* OPC-UA
* OPENCTI
* OSQUERY  
* THINGSBOARD
* ZEEK

Beats events can be sent via different output channels. The current implementation supports
MQTT and SSE.