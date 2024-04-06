Feature: Accumulated errors

  Scenario: No accumulated errors
    Given x is "1"
      And z is true
     When I capture x as y
      And I reset accumulated errors
     Then x should not be blank
      And y should be "1"
      And z should be true
      And there should be no accumulated errors

  Scenario: Two accumulated errors
    Given w is "1"
      And x is blank
      And z is false
     When I capture w as y
      And I reset accumulated errors
     Then @Sustained x should not be blank
      And y should be "1"
      And @Sustained z should be true
      And gwen.accumulated.errors should be 
         """
         2 errors:
         (1) x should not be blank
         (2) z should be true but got false
         """
      And gwen.accumulated.errors:JSONArray should be 
         """
         ["x should not be blank","z should be true but got false"]
         """
