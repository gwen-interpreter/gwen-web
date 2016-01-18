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

   Feature: Google search
   
  Scenario: Perform a google search
            
            This scenario calls the a step definition defined in the Goolge.meta
            file to perform a google search for "gwen-web". It then clicks the 
            first link in the returned results and checks the URL of the page 
            that is loaded.
            
      Given I do a google search for "gwen-web"
       When I click the first result
       Then the current URL should be "https://github.com/gwen-interpreter/gwen-web"
