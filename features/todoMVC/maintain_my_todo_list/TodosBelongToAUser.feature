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
#   https://github.com/RiverGlide/serenity-web-todomvc-journey/blob/master/src/test/java/net/serenitybdd/demos/todos/features/maintain_my_todo_list/TodosBelongToAUser.java

   Feature: Todos belong to a user
  
Background: Open two browser sessions for two users
      Given I start a browser for James
        And I start a browser for Jane
        
  Scenario: I should not affect todos belonging to another user 
      Given I switch to James
        And I browse to the application home page
        And I add a "Walk the dog" item
        And I add a "Put out the garbage" item
        And I switch to Jane
        And I browse to the application home page
        And I add a "Walk the dog" item
        And I add a "Walk the cat" item
        And I switch to James
        And I complete the "Walk the dog" item
       When I switch to Jane
       Then the displayed items should be "Walk the dog,Walk the cat"