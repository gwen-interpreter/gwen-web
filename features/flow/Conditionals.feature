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

   Feature: Conditionals

  @StepDef
  Scenario: I perform this scenario
      Given the called step is "this step"

  @StepDef
  Scenario: I perform that scenario
      Given the called step is "that step"

  Scenario: Perform this or that

     Given the variable is "this"
      When I perform ${the variable} scenario
      Then the called step should be "this step"

     Given the variable is "that"
      When I perform ${the variable} scenario
      Then the called step should be "that step"
