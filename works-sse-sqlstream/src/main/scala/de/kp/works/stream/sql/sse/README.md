
# Server-Sent Events Structured Streaming Source

This package defines the endpoint for all Works. Beats. The current implementation
distinguishes SSEs from 

* Fiware Beat
* Fleet Beat
* OPC-UA Beat
* OpenCTI Beat
* Osquery Beat
* Things Beat
* Zeek Beat

The SSE event type is used to distinguish between these different beats:

| Beat | Event type | Example |
| --- | --- | --- |
| Fiware | beat/fiware/notification | |
| Fleet | beat/fleet/[query name] | beat/fleet/processes |
| OPC-UA | beat/opcua | |
| OpenCTI | beat/opencti/[action] | beat/opencti/create |
| Osquery | beat/osquery/[format] | beat/osquery/result |
| Things | beat/things/device | |
| Zeek | beat/zeek/[file name] | beat/zeek/conn.log |

