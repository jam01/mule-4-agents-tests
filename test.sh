#!/usr/bin/env bash

set -o errexit

if [ ! -d "./mule-enterprise-standalone-4.2.2" ]
then
echo "downloading and unzipping mule ee 4.2.2 ..."
curl -OJ https://s3.amazonaws.com/new-mule-artifacts/mule-ee-distribution-standalone-4.2.2.zip \
  && unzip -uoq mule-ee-distribution-standalone-4.2.2.zip
fi

rm $PWD/mule-enterprise-standalone-4.2.2/logs/*

echo "building artifacts ..."
mvn clean package

# Setting up env variables that the wrapper.conf file will use to set the javaagent
export BM_HOME=$PWD/byteman-agent/target/
export BB_HOME=$PWD/bytebuddy-agent/target/

# Copying a simple app to be started with mule, this has an http listener that triggers an http requester (which uses ahc)
cp $PWD/java-specialagent-test-mule-4.2.2/target/java-specialagent-test-mule-4.2.2-1.0.0-SNAPSHOT-mule-application.jar \
    $PWD/mule-enterprise-standalone-4.2.2/apps


# BYTEBUDDY
echo -e "\n\n\ntesting instrumentation with bytebuddy agent ..."
# Copying a wrapper.conf configured to use bytebuddy-agent.jar
rm $PWD/mule-enterprise-standalone-4.2.2/conf/wrapper.conf
cp $PWD/wrapper-bytebuddy.conf $PWD/mule-enterprise-standalone-4.2.2/conf/wrapper.conf

$PWD/mule-enterprise-standalone-4.2.2/bin/mule start
# waiting for the app to be started within mule
sleep 15

# Triggering the requester
curl localhost:8081/
# Looking for the logs that the agent should've printed
echo -e "\n\nsearching logs for agent messages ..."
cat $PWD/mule-enterprise-standalone-4.2.2/logs/* | grep "from bytebuddy"
echo -e "...\n"

$PWD/mule-enterprise-standalone-4.2.2/bin/mule stop


# BYTEMAN
echo -e "\n\n\ntesting instrumentation with byteman agent ..."
rm $PWD/mule-enterprise-standalone-4.2.2/conf/wrapper.conf
cp $PWD/wrapper-byteman.conf $PWD/mule-enterprise-standalone-4.2.2/conf/wrapper.conf

$PWD/mule-enterprise-standalone-4.2.2/bin/mule start
sleep 15

curl localhost:8081/

echo -e "\n\nsearching logs for agent messages ..."
cat $PWD/mule-enterprise-standalone-4.2.2/logs/* | grep "from byteman"
echo -e "...\n"

$PWD/mule-enterprise-standalone-4.2.2/bin/mule stop
