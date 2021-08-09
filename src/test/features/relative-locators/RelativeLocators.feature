# 
# Copyright 2021 Brady Wood, Branko Juric
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

   Feature: Relative locators

  Scenario: Locate input fields relative to a button
      Given the button can be located by tag "button"
        And field 1 can be located by tag name "input" above the button 
        And field 2 can be located by css "input" to left of the button 
        And field 3 can be located by xpath "//input" to right of the button 
        And field 4 can be located by tag name "input" below the button 
        And field 5 can be located by tag name "input" near the button 
        And field 6 can be located by tag name "input" near the button within 60 pixels
       When I navigate to "https://chercher.tech/practice/relative-locators"
        And I type "above button" in field 1
        And I type "to left of button" in field 2
        And I type "to right of button" in field 3
        And I type "below button" in field 4
        And I append " & near" to field 5
       Then field 1 should contain "above button"
        And field 2 should contain "to left of button"
        And field 3 should contain "to right of button"
        And field 4 should contain "below button"
        And field 5 should contain "& near"
        And field 6 should contain "& near"
