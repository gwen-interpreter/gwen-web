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

 @jkvine
 Feature: Information is available for the user to find out about jkvine
    As a person who wishes to find IT Specialists on demand
    I want to find out more information
    So that I can focus my attention on my business

 Scenario: verify that a user is able to find out more information about jkvine
    Given I am a first time user who wants to know more about jkvine
     When I am presented with about information
     Then I should be able to read what jkvine is about

 Scenario: determine the range of services on offer
    Given I am a business user who requires some assistance with executive mentoring
     When I find information on services
     Then I can read more information on capability assurance
