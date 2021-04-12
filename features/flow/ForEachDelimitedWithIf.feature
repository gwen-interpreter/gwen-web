# 
# Copyright 2021 Brady Wood, Branko Juric
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

   Feature: For each delimited value with if condition

  @StepDef
  @Action
  Scenario: I click option "<option>" in "<type>"
      Given the option is "$<option>"
        And the type is "$<type>"

  @StepDef
  @Action
  Scenario: I click checbox in "<type>"
      Given options is "option1,option2,option3"
        And condition is defined by javascript "true"
       When I click option "${opt}" in "$<type>" for each opt in options delimited by "," if condition

  Scenario: I process options
      Given the type is "my type"
       When I click checbox in "group"
       Then the option should be "option3"
        And the type should be "group"

  
