@Sample
Feature: Google search

  Scenario: Lucky Google search
    Given I have Google in my browser
     When I do a search for "gwen-web automation"
     Then I should find a Gwen page
