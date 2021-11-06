Feature: Sync 1

  Scenario: Sync 1 test
    Given x is "1"
     When I increment x
     Then x should equal 2
