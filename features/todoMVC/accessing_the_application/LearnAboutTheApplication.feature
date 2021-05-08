#
# Copyright 2017-2021 Brady Wood, Branko Juric
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
#   https://github.com/RiverGlide/serenity-web-todomvc-journey/blob/master/src/test/java/net/serenitybdd/demos/todos/features/accessing_the_application/LearnAboutTheApplication.java

   Feature: Learn about the application
  
Background: Open a new browser
      Given I have no open browser
       When I start a browser for James
       Then I should have 1 open browser
      
  Scenario: I should be able to identify the application
      Given I have an open browser
       When I browse to the application home page
       Then the page title should contain "Todo"
        And the heading should be "todos"
        And the info panel should contain "todo"
      
  Scenario: I should see how to begin
      Given I have an open browser
       When I browse to the application home page
       Then the placeholder string should be "What needs to be done?"