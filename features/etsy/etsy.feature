# 
# Copyright 2014-2015 Brady Wood, Branko Juric
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

 @etsy
 Feature: Information is available for the user to find out about Etsy
    As an anonymous user who is not registered with Etsy
    I want to be able to find out information
    So that I can view handmade items, vintage goods and craft supplies
  
  Scenario: verify a user is able to find out information about Etsy
      Given I am a first time user on Etsy
       When I go to find out more about Etsy
       Then I should be presented with About information

