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
# Set your workspace. Here all the services files get copied.
WORKSPACE=~/Desktop/temp/orion-workspace
cd $WORKSPACE

kill `cat ./active-processes/worker-1-pid.txt`
kill `cat ./active-processes/worker-2-pid.txt`
kill `cat ./active-processes/master-pid.txt`