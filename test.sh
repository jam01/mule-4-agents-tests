#!/usr/bin/env bash

set -o errexit

PWD=`dirname $0`

#curl -OJ https://s3.amazonaws.com/new-mule-artifacts/mule-ee-distribution-standalone-4.2.2.zip \
#  && unzip -uoq mule-ee-distribution-standalone-4.2.2.zip
rm $PWD/mule-enterprise-standalone-4.2.2/logs/*

mvn clean package

# Setting up env variables that the wrapper.conf file will use to set the javaagent
export BM_HOME=$PWD/byteman-agent/target/
export BB_HOME=$PWD/bytebuddy-agent/target/

# Copying a simple app to be started with mule, this has an http listener that triggers an http requester (which uses ahc)
cp $PWD/java-specialagent-test-mule-4.2.2/target/java-specialagent-test-mule-4.2.2-1.0.0-SNAPSHOT-mule-application.jar \
    $PWD/mule-enterprise-standalone-4.2.2/apps


# BYTEBUDDY
# Copying a wrapper.conf configured to use bytebuddy-agent.jar
rm $PWD/mule-enterprise-standalone-4.2.2/conf/wrapper.conf
cp $PWD/wrapper-bytebuddy.conf $PWD/mule-enterprise-standalone-4.2.2/conf/wrapper.conf

$PWD/mule-enterprise-standalone-4.2.2/bin/mule start
# waiting for the app to be started within mule
sleep 15

# Triggering the requester
curl localhost:8081/
# Looking for the logs that the agent should've printed
echo -e "\n"
cat $PWD/mule-enterprise-standalone-4.2.2/logs/* | grep "from bytebuddy"

$PWD/mule-enterprise-standalone-4.2.2/bin/mule stop


# BYTEMAN
rm $PWD/mule-enterprise-standalone-4.2.2/conf/wrapper.conf
cp $PWD/wrapper-byteman.conf $PWD/mule-enterprise-standalone-4.2.2/conf/wrapper.conf

$PWD/mule-enterprise-standalone-4.2.2/bin/mule start
sleep 15

curl localhost:8081/

echo -e "\n"
cat $PWD/mule-enterprise-standalone-4.2.2/logs/* | grep "from byteman"

$PWD/mule-enterprise-standalone-4.2.2/bin/mule stop
