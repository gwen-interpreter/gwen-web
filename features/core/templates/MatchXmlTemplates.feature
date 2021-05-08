#
# Copyright 2018-2021 Branko Juric, Brady Wood
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

  Feature: Match and extract XML templates


 Background: Init
       Given my pet status is "available"
        When I capture my pet status
        Then my pet status should be "available"

 Scenario: Match static single line XML template
     Given my value is
           """
           <result><id>42</id><category><name>pet</name></category><name>tiger</name><status>available</status></result>
           """
      When I capture my value
      Then my value should match template "<result><id>42</id><category><name>pet</name></category><name>tiger</name><status>available</status></result>"
       And my value should match template
           """
           <result><id>42</id><category><name>pet</name></category><name>tiger</name><status>available</status></result>
           """
       And my value should match template file "features/core/templates/xml/StaticSingleLineTemplate.xml"


 Scenario: Match static multi line XML template
     Given my value is
           """
           <result>
               <id>42</id>
               <category>
                   <name>pet</name>
               </category>
               <name>tiger</name>
               <status>available</status>
           </result>
           """
      When I capture my value
      Then my value should not match template "<result><id>42</id><category><name>pet</name></category><name>tiger</name><status>available</status></result>"
       And my value should match template
           """
           <result>
               <id>42</id>
               <category>
                   <name>pet</name>
               </category>
               <name>tiger</name>
               <status>available</status>
           </result>
           """
       And my value should match template file "features/core/templates/xml/StaticMultiLineTemplate.xml"


 Scenario: Match dynamic single line XML template (1 ignore, 1 extract, 1 inject)
     Given my value is
           """
           <result><id>42</id><category><name>pet</name></category><name>tiger</name><status>available</status></result>
           """
      When I capture my value
      Then my value should match template "<result><id>!{}</id><category><name>pet</name></category><name>@{pet name}</name><status>${my pet status}</status></result>"
       And category name should be absent
       And pet id should be absent
       And pet name should be "tiger"


 Scenario: Match dynamic multi line XML template (2 extracts, 1 ignore, 1 inject)
     Given my value is
           """
           <result>
               <id>42</id>
               <category>
                   <name>pet</name>
               </category>
               <name>tiger</name>
               <status>available</status>
           </result>
           """
       And the pet name is defined by the text in my value by xpath "result/name"
       And the pet status is defined by the text in my value by xpath "result/status"
      When I capture my value
      Then my value should match template
           """
           <result>
               <id>@{pet id}</id>
               <category>
                   <name>@{category name}</name>
               </category>
               <name>!{}</name>
               <status>${my pet status}</status>
           </result>
           """
       And pet id should be "42"
       And category name should be "pet"
       And the pet name should be "tiger"
       And the pet status should be "available"


 Scenario: Match dynamic single line XML template file (1 ignore, 1 extract, 1 inject)
     Given my value is
           """
           <result><id>42</id><category><name>pet</name></category><name>tiger</name><status>available</status></result>
           """
      When I capture my value
      Then my value should match template file "features/core/templates/xml/DynamicSingleLineTemplate.xml"
       And category name 1 should be absent
       And pet id 1 should be absent
       And pet name 1 should be "tiger"

  Scenario: Match dynamic multi line XML template file (2 extracts, 1 ignore, 1 inject)
     Given my value is
           """
           <result>
               <id>42</id>
               <category>
                   <name>pet</name>
               </category>
               <name>tiger</name>
               <status>available</status>
           </result>
           """
       And the pet name is defined by the text in my value by xpath "result/name"
       And the pet status is defined by the text in my value by xpath "result/status"
      When I capture my value
      Then my value should match template file "features/core/templates/xml/DynamicMultiLineTemplate.xml"
       And pet id 2 should be "42"
       And category name 2 should be "pet"
       And the pet name should be "tiger"
       And the pet status should be "available"