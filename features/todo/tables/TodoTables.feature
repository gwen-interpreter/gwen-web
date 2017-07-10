#
# Copyright 2017 Brady Wood, Branko Juric
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

Feature: Managing todo items with data tables

  Scenario: Complete a todo item

    Given I have the following active items
          | Walk the dog |
          | Get the milk |
          | Feed the cat |

     When I complete the "Get the milk" item

     Then the status of my items should be
          | Item         | Status    |
          | Walk the dog | active    |
          | Get the milk | completed |
          | Feed the cat | active    |