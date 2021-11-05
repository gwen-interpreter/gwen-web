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

   Feature: Multi locators

Background: Open browser to google
     Given the current URL is "" 
      When I navigate to "http://google.com"
      Then the current URL should contain "google"
   
  Scenario: Last locator of three should find the search field
            Defines three locators for the search field.
            The 1st and 2nd will fail and the 3rd will succeed.
      Given the search field can be located by
            | selector   | expression    |
            | id         | search        |
            | class name | .search-field |
            | name       | q             |
       When I locate the search field
       Then the search field should be displayed
