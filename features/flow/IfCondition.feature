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

   Feature: If Conditionals

  @StepDef
  @Action
  Scenario: I perform this
      Given the called step is "this step"

  @StepDef
  @Action
  Scenario: I perform that
      Given the called step is "that step"

  Scenario: Perform this
      Given the target is "this"
        And this condition is defined by javascript ""${the target}" == "this""
       When I perform this if this condition
       Then the called step should be "this step"

  Scenario: Perform that
      Given the target is "that"
        And that condition is defined by javascript ""${the target}" == "that""
       When I perform that if that condition
       Then the called step should be "that step"

   Scenario: Do not perform this
      Given the target is "that"
        And the called step is "none"
        And this condition is defined by javascript ""${the target}" == "this""
       When I perform this if this condition
       Then the called step should be "none"
