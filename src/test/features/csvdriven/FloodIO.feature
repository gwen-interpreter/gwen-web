@floodio
@Ignore
Feature: Complete the Floodio challenge ${my age}
     
  As a gwen user aged ${my age}
  I want to automate the floodio challenge
  So that I can be on the hall of fame
      
  Scenario: Launch the challenge
    Given the start page url is "https://challengers.flood.io/start"  
     When I navigate to the start page
     Then I should be on the start page
       
  Scenario: Complete step 1
    Given I am on the start page
     When I click the Start button
     Then I should be on the step 2 page

  Scenario: Complete step 2
    Given I am on the step 2 page
     When I select "${my age}" in the how old are you dropdown
      And I click the next button
     Then I should be on the step 3 page
       
  Scenario: Complete step 3
    Given I am on the step 3 page
     When I select and enter the largest order value
      And I click the next button
     Then I should be on the step 4 page
   
  Scenario: Complete step 4
    Given I am on the step 4 page
     When I click the next button
     Then I should be on the step 5 page

  Scenario: Complete step 5
    Given I am on the step 5 page
     When I enter the one time token
      And I click the next button
     Then I should be on the challenge completed page
