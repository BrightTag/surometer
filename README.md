# surometer - Suro JMeter Extension

## License

Copyright 2014 Signal.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.


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
