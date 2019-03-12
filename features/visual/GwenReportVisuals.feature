# 
# Copyright 2019 Brady Wood, Branko Juric
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

   Feature: Gwen Report
   
  Scenario: Check report visuals
      Given I start a new browser
       When I navigate to "file://${user.dir}/target/reports/sequential/index.html"
       Then I checkpoint 1200x600 visual as "Summary Report"
        And the detail report link can be located by css selector "a[class='text-success']"
       When I click the detail report link
       Then I checkpoint full page visual as "Detail Report"
        And visual checks should pass