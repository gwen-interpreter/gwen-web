@Sample
Feature: Google search

  Scenario: Lucky Google search
    Given I have Google in my browser
     When I do a search for "automation"
     Then at least one result should be displayed
