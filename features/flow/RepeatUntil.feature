# 
# Copyright 2017 Brady Wood, Branko Juric
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

   Feature: Repeat until example

  @StepDef
  Scenario: I increment counter
      Given counter is defined by javascript "${counter} + 1"
        And counter > 3 is defined by javascript "${counter} > 3"

  Scenario: Increment counter
      Given counter is "0"
       When I increment counter until counter > 3 using no delay
       Then counter should be "4"



