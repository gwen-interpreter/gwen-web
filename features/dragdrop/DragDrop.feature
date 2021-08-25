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

 Feature: Drag Drop
      
Scenario: Drag box to cell
	Given the box can be located by xpath "//*[@id='credit2']/a"
    And the cell can be located by xpath "//*[@id='bank']/li"
	 When I navigate to "http://demo.guru99.com/test/drag_drop.html"
    And I capture the box as the box text
    And I drag and drop the box to the cell
	 Then the cell should contain "${the box text}"
