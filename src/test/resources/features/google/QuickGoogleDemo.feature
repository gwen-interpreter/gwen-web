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

   Feature: Quick google demo
   
  Scenario: Submit a simple google search
      Given I navigate to "http://google.com"
        And the search field can be located by name "q"
        And the search results can be located by id "ires"
       When I enter "gwen interpreter" in the search field
        And I submit the search field
       Then the search results should be displayed
        And the page title should be "gwen interpreter - Google Search" 
       
