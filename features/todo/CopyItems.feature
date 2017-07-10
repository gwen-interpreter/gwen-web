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

  Feature: Copy Todo items

Scenario: Load items into session 1
    Given I start a browser for session 1
      And I launch the Todo app
     When I add a "Walk the dog" item
      And I add a "Get the milk" item
      And I add a "Take out trash" item
     Then the number of active items should be "3"

Scenario: Copy items from session 1 to session 2
    Given I start a browser for session 2
      And I launch the Todo app
     When I copy all items from session 1 to session 2
     Then the number of active items should be "3"