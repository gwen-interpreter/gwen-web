Feature: Sync 3

  Scenario: Sync 3 test
    Given x is "3"
     When I increment x
     Then x should equal 4
