# surometer - Suro JMeter Extension

## Install

Build the extension:

    mvn package

Install the extension into `$JMETER_HOME/lib/ext`:

    cp target/surometer-*-jar-with-dependencies.jar $JMETER_HOME/lib/ext

## Usage

### Suro Sampler

After installing `surometer`, add a Java Request Sampler and select the `SuroSampler`
class name. The following properties are required.

* **load_balancer_server**: comma-separated list of hosts in the format "hostname:port".
