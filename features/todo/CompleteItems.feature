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

  Feature: Create and complete Todo items

Scenario: Add items in my Todo list
    Given I launch the Todo app
     When I add a "Walk the dog" item
      And I add a "Get the milk" item
     Then the number of active items should be "2"

Scenario: Complete one item
    Given I am on the Todo page
     When I tick the "Get the milk" item
     Then the number of active items should be "1"

Scenario: Complete another item
    Given I am on the Todo page
     When I tick the "Walk the dog" item
     Then the number of active items should be "0"

Scenario: Clear my completed items
   Given I am on the Todo page
    When I click the clear completed button
    Then the number of active items should not be displayed