# 
# Copyright 2014-2021 Brady Wood, Branko Juric
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

 # website unavailable: http://book.theautomatedtester.co.uk/chapter1
 @Ignore
 Feature: Multi Windows

Background: Open multiple windows
    Given window link 1 can be located by css ".multiplewindow"
      And window link 2 can be located by css ".multiplewindow2"
     When I navigate to "http://book.theautomatedtester.co.uk/chapter1"
      And I click window link 1
      And I click window link 2
     Then I should have 3 open windows

Scenario: Close window 2 and then window 1
    Given I have an open browser
     When I switch to child window 2
      And I close child window 2
      And I switch to child window 1
      And I close child window 1
      And I switch to the root window
     Then I should have 1 open window

Scenario: Close window 1 and then window 2
    Given I have an open browser
     When I close child window 1
      And I close child window 1
      And I switch to the parent window
     Then I should have 1 open window
