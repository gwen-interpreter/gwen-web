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

# This is a Gwen executable feature that mimics the Serenity feature test here:
#   https://github.com/RiverGlide/serenity-web-todomvc-journey/blob/master/src/test/java/net/serenitybdd/demos/todos/features/completing_todos/ToggleAllTodos.java

   Feature: Toggle all todos
  
Background: Open a new browser
      Given I start a browser for James
      
  Scenario: I should be able to quickly complete all todos
      Given I browse to the application home page
       When I add a "Walk the dog" item
        And I add a "Put out the garbage" item
        And I toggle all items
       Then the "Walk the dog" item should be completed
        And the "Put out the garbage" item should be completed

  Scenario: I should be able to toggle status of all todos
      Given I browse to the application home page
       When I add a "Walk the dog" item
        And I add a "Put out the garbage" item
        And I toggle all items
        And I toggle all items
       Then the "Walk the dog" item should be active
        And the "Put out the garbage" item should be active