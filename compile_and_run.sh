#!/bin/bash
# compile
mvn package

# copy over new jmeter extension jar
cp target/surometer-1.0-SNAPSHOT-jar-with-dependencies.jar /Applications/apache-jmeter-2.11/lib/ext/

# get pid of any running jmeter
jmeter_pid=$(ps -ef|grep ApacheJMeter|awk '/java/{print $2}')
kill $jmeter_pid

# launch jmeter again
/Applications/apache-jmeter-2.11/bin/jmeter &
