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

   Feature: Current URL checks

  Scenario: Current URL should change when page changes
      Given I navigate to "https://github.com/gwen-interpreter/gwen-web"
       Then the current URL should contain "gwen"
        And the current URL should not contain "wiki"
       When I navigate to "https://github.com/gwen-interpreter/gwen-web/wiki"
       Then the current URL should contain "wiki"