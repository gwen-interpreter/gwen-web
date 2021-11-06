@Import("src/test/features/google/Google.meta")
Feature: Google search
   
  Scenario: Perform a google search
    Given I have Google in my browser
     When I do a search for "Gwen web automation"
     Then the first result should open a Gwen page
