# 
# Copyright 2014-2015 Brady Wood, Branko Juric
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

 @floodio
 Feature: Complete the floodio challenge
     As a gwen user
     I want to automate the floodio challenge
     So that I can verify that it works
  
  @Robot
  Scenario: Initialise user agent
      Given my gwen.web.useragent setting is "${my user agent}" 
      
  Scenario: Launch the challenge
      Given I launch the floodio challenge
       Then I should be on the start page
       
  Scenario: Complete step 1
      Given I am on the start page
       When I click the Start button
       Then I should be on the step 2 page

  Scenario: Complete step 2
      Given I am on the step 2 page
       When I select "${my age}" in the how old are you dropdown
       Then the how old are you dropdown text should be "${my age}"
        And the how old are you dropdown value should be "${my age}"
       When I click the next button
       Then I should be on the step 3 page
       
  Scenario: Complete step 3
      Given I am on the step 3 page
       When I select and enter the largest order value
        And I click the next button
       Then I should be on the step 4 page
   
  Scenario: Complete step 4
      Given I am on the step 4 page
       When I click the next button
       Then I should be on the step 5 page

  Scenario: Complete step 5
      Given I am on the step 5 page
       When I enter the one time token
        And I click the next button
       Then I should be on the challenge completed page
       
  @Robot
  Scenario: Verify robot completion
      Given I am on the challenge completed page
       Then the lead paragraph should contain "Congratulations, your scripting skills are impressive"
       