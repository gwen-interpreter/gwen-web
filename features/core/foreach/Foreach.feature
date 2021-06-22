#
# Copyright 2021 Branko Juric, Brady Wood
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

 Feature: Select each user

   @StepDef
   @Action
   Scenario: I select all users in "<selectUser>"
       Given users is "$<selectUser>"
         And condition is defined by javascript "true"
        When I select "${selectUser}" for each selectUser in users delimited by "," if condition

   @StepDef
   @Action
   Scenario: I select "<user>"
       Given the user is "$<user>"
        Then the user should match regex "user\d"
         And my selected users is "${my selected users}$<user>"

   Scenario: Select all my users
       Given my selected users is ""
        When I select all users in "user1,user2,user3"
        Then my selected users should be "user1user2user3"
       
          