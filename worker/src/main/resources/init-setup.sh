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

# Run this within the workspace folder.

WORKER_JAR_PATH=/Users/tharindu/repositories/orion/worker/target/worker-0.1-SNAPSHOT-jar-with-dependencies.jar
TEMP_FOLDER=../orion-temp-folder

rm -rf $TEMP_FOLDER
mkdir $TEMP_FOLDER

cp init-setup.sh $TEMP_FOLDER
cp stop-setup.sh $TEMP_FOLDER

rm -rf ./*

cp $TEMP_FOLDER/init-setup.sh .
cp $TEMP_FOLDER/stop-setup.sh .
rm -rf $TEMP_FOLDER

mkdir worker-1
cp $WORKER_JAR_PATH ./worker-1/worker-service-1.jar

mkdir worker-2
cp $WORKER_JAR_PATH ./worker-2/worker-service-2.jar

java -jar ./worker-1/worker-service-1.jar 9090 > worker-1.log 2>&1 &
echo $! > worker-1-pid.txt

java -jar ./worker-2/worker-service-2.jar 9091 > worker-2.log 2>&1 &
echo $! > worker-2-pid.txt
