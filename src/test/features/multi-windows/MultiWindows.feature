@Ignore
Feature: Multi Windows

  Background: Open multiple windows
    Given window link 1 can be located by css ".multiplewindow"
      And window link 2 can be located by css ".multiplewindow2"
     When I navigate to "http://book.theautomatedtester.co.uk/chapter1"
      And I click window link 1
      And I click window link 2
     Then I should have 3 open windows

  Scenario: Close window 2 and then window 1
    Given I have an open browser
     When I switch to child window 2
      And I close child window 2
      And I switch to child window 1
      And I close child window 1
      And I switch to the root window
     Then I should have 1 open window

  Scenario: Close window 1 and then window 2
    Given I have an open browser
     When I close child window 1
      And I close child window 1
      And I switch to the parent window
     Then I should have 1 open window
