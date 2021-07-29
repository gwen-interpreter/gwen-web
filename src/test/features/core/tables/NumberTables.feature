#
# Copyright 2017-2021 Branko Juric, Brady Wood
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

 Feature: Number tables

   Scenario: Various table forms
       Given single row without header contains a number in decimal and binary form
             | 3 | 11 |
         And single row with header contains a number in decimal and binary form
             | decimal | binary |
             | 3       | 11     |
         And single column without header contains a number in decimal and binary form
             | 3  |
             | 11 |
         And single column with header contains a number in decimal and binary form
             | decimal | 3  |
             | binary  | 11 |
         And each row contains a number and its square and cube
             | number | square | cube |
             | 1      | 1      | 1    |
             | 2      | 4      | 8    |
             | 3      | 9      | 27   |
         And each row contains a number in decimal and binary form
             | decimal | binary |
             | 1       | 1      |
             | 2       | 10     |
             | 3       | 11     |
         And each column contains a number and its square and cube
             | number | 1 | 2 | 3  |
             | square | 1 | 4 | 9  |
             | cube   | 1 | 8 | 27 |
         And each column contains a number in decimal and binary form
             | 4   | 5    | 6   |
             | 100 | 101  | 110 |
         And the top and left numbers yield the product in the matrix
             | x | 1 | 2 | 3 |
             | 1 | 1 | 2 | 3 |
             | 2 | 2 | 4 | 6 |
             | 3 | 3 | 6 | 9 |
         And each row contains two numbers that sum to a Fibonacci number in the third
             | 0 | 1 | 1 |
             | 1 | 1 | 2 |
             | 1 | 2 | 3 |
             | 2 | 3 | 5 |
             | 3 | 5 | 8 |
         And four in decimal is "4"
         And five in decimal is "5"
         And six in decimal is "6"
         And four in binary is "100"
         And five in binary is "101"
         And six in binary is "110"
         And each column contains a number in decimal and binary form
             | ${four in decimal} | ${five in decimal} | ${six in decimal} |
             | ${four in binary}  | ${five in binary}  | ${six in binary}  |
        When tables are nested in stepdefs
        Then everything should be "ok"

