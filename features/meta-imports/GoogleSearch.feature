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

   @Import("features/google/Google.meta")
   Feature: Google search
   
  Scenario: Perform a google search
      Given I have Google in my browser
       When I do a search for "Gwen web automation"
       Then the first result should open a Gwen page
