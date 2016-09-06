# 
# Copyright 2016 Brady Wood, Branko Juric
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

 Feature: Scoped binding tests
      
Scenario: Init globals
	Given a is "1"
	  And b is defined by javascript "1"
	  And c is "1"
      And d is defined by javascript "${c}"
      And e is "1"
     Then a should be "1"
      And b should be "1"
      And c should be "1"
      And d should be "1"
      And e should be "1" 
      
Scenario: System properties should resolve
    Given my java version is "${java.version}"
     Then my java version should not contain "java.version"
	
Scenario: Global binding defined in local scope should override that in feature scope
    Given I am on the A page
     Then a is "2"
      And a should be "2"
      
Scenario: Global binding defined in local scope should override javascript binding in feature scope
     When I am on the B page
     Then b is "2"
      And b should be "2"
      
Scenario: Binding of different type in local scope should override global of same name
     When I am on the CD page
     Then c is defined by javascript "2"
      And d is defined by javascript "${c}1"
      And c should be "2"
      And d should be "21"
      
Scenario: Capture of dynamic attribute into static attribute should be visible in current scope 
    Given I am on the E page
     Then e should be "1" 
     When e is defined by javascript "2+1"
      And I capture the text in e by regex "(\d)" as e
      And ee is "${e}"
     Then ee should be "3"
     When e is "2"
     Then e should be "2"
     When e is defined by javascript "2+2"
     Then e should be "4"
