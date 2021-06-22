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

   Feature: Repeat while example

  @StepDef
  @Action
  Scenario: I increment counter<counterNo>
      Given counter$<counterNo> is defined by javascript "${counter$<counterNo>} + 1"
        And counter$<counterNo> < 4 is defined by javascript "${counter$<counterNo>} < 4"

  Scenario: Increment counter1
      Given counter1 is "-1"
       When I increment counter1
        And I increment counter1 while counter1 < 4 using no delay
       Then counter1 should be "4"

  Scenario: Increment counter2 with if condition
      Given counter2 is "-1"
        And condition is defined by javascript "true"
       When I increment counter2
        And I increment counter2 while counter2 < 4 using no delay if condition
       Then counter2 should be "4"
