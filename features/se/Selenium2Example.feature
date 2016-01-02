# 
# Copyright 2014-2016 Brady Wood, Branko Juric
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

   Feature: Selenium 2 Example
 
    This feature does the same thing as the selenium code example found here: 
    http://www.seleniumhq.org/docs/03_webdriver.jsp#introducing-the-selenium-webdriver-api-by-example
      
  Scenario: Google search
      Given I navigate to "http://www.google.com"
        And the search field can be located by name "q"
       When I enter "Cheese!" in the search field
       Then the page title should start with "Cheese!"

