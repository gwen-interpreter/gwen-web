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

   Feature: For each delimited value

  Scenario: Process each delimited value
      Given values is "One,Two,Three"
       Then x is "${value}" for each value in values delimited by ","
        And value should be absent

  @StepDef
  @DataTable(header="top")
  Scenario: I process the following user roles
      Given processed roles is ""
      Given I process user roles for each data record

  @StepDef
  Scenario: I process user roles
      Given processed roles is "${processed roles}${role}" for each role in data[Roles] delimited by ","

  Scenario: Process each delimited value for data record in table
      Given I process the following user roles
            | User | Roles       |
            | abc  | role1,role2 |
            | cbd  | role3,role4 |
       Then processed roles should be "role1role2role3role4"



