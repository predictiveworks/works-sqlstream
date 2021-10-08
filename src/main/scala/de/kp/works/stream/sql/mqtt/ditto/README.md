
# Eclipse Ditto Structured Streaming Source

**Eclipse Ditto™** is an IoT technology that implements a software pattern called "digital twin". 
This pattern specifies a virtual, cloud based, representation of real world assets (real world “Things”, 
e.g. devices like sensors, smart heating, connected cars, smart grids, EV charging stations, and more).

**Eclipse Ditto™** abstracts from the IoT protocol layers and introduces a common semantic model to
describe MQTT and other protocol payloads. From this perspective, Ditto is on a similar level as NGSI v2
or NGSI LD is. It is an initiative to ease and accelerate the implementation of IoT solutions.

For IoT analytics, common semantic models always a better starting point than plain protocol payload.

The Ditto source for Apache Spark structured streaming listens to both Ditto communication channels, *twin* and
*live*. The **twin** channel connects to the digital representation of things and reports changes on the levels
*thing*, *features* and *feature*. The *live* channel routes commands/messages towards devices and can be used
e.g. for monitoring.

Messages of both communication channels are transformed into Apache Spark Row representations, accompanied by
predefined semantic schema representations (Thing, Features Schema and more.)

This approach transforms Eclipse Ditto™ messages into feature-rich Apache Spark dataframes that can directly be
used to apply transformations or more advanced machine intelligence.

