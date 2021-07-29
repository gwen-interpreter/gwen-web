#
# Copyright 2017-2021 Branko Juric, Brady Wood
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

 Feature: Join Strings

   Scenario Outline: Joining <string 1> and <string 2> should yield <result>

     This scenario is evaluated at the point where the outline is declared.
     Joining <string 1> and <string 2> should yield <result>

     Given string 1 is "<string 1>"
       And string 2 is "<string 2>"
      When I join the two strings
      Then the result should be "<result>"

     Examples: Basic string concatenation

       The header row contains the placeholder names. The body rows that
       follow contain the data that is bound to each scenario that is evaluated.

       | string 1 | string 2 | result   |
       | howdy    | doo      | howdydoo |
       | any      | thing    | anything |

   Scenario: Verify that we can join two strings in meta
      Given result is ""
       When I join two strings in meta
       Then the result should not be ""
