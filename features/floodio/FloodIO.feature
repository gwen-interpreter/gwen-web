/*
 * Copyright 2014 Brady Wood, Branko Juric
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 Feature: Complete the flood io challenge
  
  @Robot
  Scenario: Initialise user agent
      Given my gwen.web.useragent setting is "I AM ROBOT" 
      
  Scenario: Complete step 1
      Given I navigate to the start page
       Then the heading text should be "Welcome"
       When I click the Start button
       Then the heading text should be "Step 2"

  Scenario: Complete step 2
      Given I am on the step 2 page
       When I select "21" in the how old are you dropdown
        And I click the next button
       Then the heading text should be "Step 3"
       
  Scenario: Complete step 3
      Given I am on the step 3 page
       When I select and enter the largest order value
        And I click the next button
       Then the heading text should be "Step 4"
   
  Scenario: Complete step 4
      Given I am on the step 4 page
       When I click the next button
       Then the heading text should be "Step 5"

  Scenario: Complete step 5
      Given I am on the step 5 page
       When I enter the one time token
        And I click the next button
       Then the heading text should be "You're Done!"

  @Robot
  Scenario: Verify robot completion
      Given I am on the challenge completed page
       Then the lead paragraph text should contain "Congratulations, your scripting skills are impressive"
       