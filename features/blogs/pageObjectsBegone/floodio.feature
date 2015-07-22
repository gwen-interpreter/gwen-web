# 
# Copyright 2015 Brady Wood, Branko Juric
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
# Test feature for blog post:
# https://warpedjavaguy.wordpress.com/2014/08/27/page-objects-begone/
#

 @floodio
 Feature: The floodio challenge
     As a gwen user
     I want to automate the floodio challenge
     So that I can verify that it works
     
 Scenario: Complete the floodio challenge
     Given I navigate to the start page
      When I click the Start button
      Then I am on the step 2 page
      When I select "21" in the how old are you dropdown
       And I click the next button
      Then I am on the step 3 page
      When I type the largest order value in the largest order input field
       And I click the largest order radio button
       And I click the next button
      Then I am on the step 4 page
      When I click the next button
      Then I am on the step 5 page
      When I wait for the one time token text
       And I type the one time token in the one time token field
       And I click the next button
      Then I am on the done page
     