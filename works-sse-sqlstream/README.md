
# Server-Sent Events Structured Streaming Source

This package defines the SSE endpoint for the WorksBeats. The following
beat publish their events via SSE:

## Fiware Beat
## Fleet Beat

This WorksBeat monitors the file system logging of a Fleet Sensor instance
and publishes detected file changes via multiple output channel. One of these
channels is SSE.

The Fleet events are formatted in an NGSI alike format and are transformed into
an Apache Spark compliant Row format, with a schema that respects one of the Osquery
schema definitions.

## OPC-UA Beat
## OpenCTI Beat
## Osquery Beat
## Things Beat
## Zeek Beat

