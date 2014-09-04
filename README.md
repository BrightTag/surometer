# surometer - Suro JMeter Extension

## Install

Build the extension:

    mvn package

Install the extension into `$JMETER_HOME/lib/ext`:

    cp target/surometer-*-jar-with-dependencies.jar $JMETER_HOME/lib/ext

## Usage

### Suro Sampler

After installing `surometer`, add a Java Request Sampler and select the `SuroSampler`
class name. The following properties are required, but they have sensible defaults.

* **SuroSampler.MsgRoutingKey** Suro routing key for this particular message.  This determines to what sink the Suro server sends the message.
* **SuroSampler.MsgPayload** Message payload.

* **SuroClient.loadBalancerServer** Parameter for setting the Suro servers; it should be comma separated list of $hostname:$port".
* **SuroClient.clientType** Parameter for making requests either synchronous or asynchronous; should be "sync" or "async".  Note that "async" mode implies that the sampler will queue the request in the sampler thread and return immediately.  For Signal's use cases, "sync" is the best choice because our deployments host a Suro Server on each VM, and we expect that service to always be available.  We expect any queueing to happen in the Suro Server, outside of the JMeter load generator.

The following only apply if clientType = "async".

* **SuroClient.asyncBatchSize** When the number of messages queued is up to this value, the client will create and send MessageSet.
* **SuroClient.asyncTimeout** Even the number of messages is less than the above value, the client will send messages in the queue any way if up to this much time has elaspsed. Time unit is millisecond.
* **SuroClient.asyncQueueType** Can be either file or memory.
* **SuroClient.asyncMessageQueueCapacity** The bound of memory queue. The unit is number of messages.
* **SuroClient.asyncFileQueuePath** file queue directory path