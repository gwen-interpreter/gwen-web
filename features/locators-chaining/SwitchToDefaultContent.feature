#
# Copyright 2014-2016 Brady Wood, Branko Juric
# Copyright 2018 Alexandru Cuciureanu
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

Feature: Switch to default content

  @Import("features/locators-chaining/W3Schools.meta")
  Scenario: Switch to default content
    Given I have W3Schools HTML Iframes Page in my browser
    Then I switch to default content from within an iframe