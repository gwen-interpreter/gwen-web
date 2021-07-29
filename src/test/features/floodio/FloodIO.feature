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
 @TestCaseId("TC-1234")
 Feature: Complete the floodio challenge
     As a gwen user
     I want to automate the floodio challenge
     So that I can be on the hall of fame

  Scenario: Launch the challenge
      Given the start page url is "https://challengers.flood.io/start"  
       When I navigate to the start page
       Then I should be on the start page
       
  Scenario: Complete step 1
      Given I am on the start page
       When I click the Start button
       Then I should be on the step 2 page

  Scenario: Complete step 2
      Given I am on the step 2 page
       When I select "21" in the how old are you dropdown
        And I click the next button
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
        And I capture the one time token
        And I click the next button
       Then I should be on the challenge completed page
            