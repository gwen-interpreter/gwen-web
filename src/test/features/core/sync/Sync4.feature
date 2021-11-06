Feature: Sync 4

  Scenario: Sync 4 test
    Given x is "4"
     When I increment x
     Then x should equal 5
