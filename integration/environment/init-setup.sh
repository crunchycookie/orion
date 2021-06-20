#!/bin/bash
#
# Copyright 2021 crunchycookie
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

CURRENT_DIRECTORY=`pwd`
WORKER_JAR_PATH=$CURRENT_DIRECTORY/../../worker/target/worker-0.1-SNAPSHOT-jar-with-dependencies.jar
MASTER_JAR_PATH=$CURRENT_DIRECTORY/../../master/target/master-0.1-SNAPSHOT.jar
MASTER_CONFIG_PATH=$CURRENT_DIRECTORY/orion-master.properties

echo $MASTER_JAR_PATH


# Set your workspace. Here all the services files get copied.
WORKSPACE=~/Desktop/temp/orion-workspace
cd $WORKSPACE

rm -rf *
chmod -R ug+rw $WORKSPACE

mkdir logs
mkdir active-processes

# Configure and start Workers.
mkdir worker-1
cp $WORKER_JAR_PATH ./worker-1/worker-service-1.jar

mkdir worker-2
cp $WORKER_JAR_PATH ./worker-2/worker-service-2.jar

java -jar ./worker-1/worker-service-1.jar 9090 > ./logs/worker-1.log 2>&1 &
echo $! > ./active-processes/worker-1-pid.txt

java -jar ./worker-2/worker-service-2.jar 9091 > ./logs/worker-2.log 2>&1 &
echo $! > ./active-processes/worker-2-pid.txt

# Configure and start the Master.
mkdir master
cp $MASTER_CONFIG_PATH ./master/orion-master.properties
cp $MASTER_JAR_PATH ./master/master-service.jar

HERE=`pwd`
cd master
java -jar master-service.jar run orion-master.properties > ../logs/master.log 2>&1 &
echo $! > ../active-processes/master-pid.txt
cd ../
