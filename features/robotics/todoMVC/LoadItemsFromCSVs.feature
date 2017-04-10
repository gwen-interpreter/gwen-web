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

Feature: Load Todo items from CSV files

  Scenario: Launch the Todo application
    Given I launch the Todo app
     When I load items from CSV files
     Then the "Walk the dog" item should be unticked
      And the "Get the milk" item should be ticked
      And the "Take out trash" item should be unticked
      And the "Feed the cat" item should be unticked
      And the "Wash the car" item should be ticked
      And the "Write some code" item should be unticked
      And the number of active items should be "4"