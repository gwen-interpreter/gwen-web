#
# Copyright 2020 Brady Wood, Branko Juric
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

@Lenient
Feature: Bad todo

  Scenario: Add and complete items
      Given I navigate to "http://todomvc.com/examples/angularjs"
       Then I should be on the todo page
       When I add a "Walk the dog" item
        And I add a "Get the milk" item
       Then the active item count should be "2"
       When I tick the "Get the milk" item
       Then the active item count should be "1"
       When I tick the "Walk the dog" item
       Then the active item count should be "0"
