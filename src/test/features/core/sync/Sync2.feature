Feature: Sync 2

  Scenario: Sync 2 test
    Given x is "2"
     When I increment x
     Then x should equal 3
