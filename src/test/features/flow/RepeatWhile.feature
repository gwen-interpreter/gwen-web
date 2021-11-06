Feature: Repeat while example

  @StepDef
  @Action
  Scenario: I increment counter1
    Given counter1 is defined by javascript "${counter1} + 1"
      And counter1 < 4 is defined by javascript "${counter1} < 4"

  @StepDef
  @Action
  Scenario: I increment counter2
    Given counter2 is defined by javascript "${counter2} + 1"
      And counter2 < 4 is defined by javascript "${counter2} < 4"

  Scenario: Increment counter1
    Given counter1 is "-1"
     When I increment counter1
      And I increment counter1 while counter1 < 4 using no delay
     Then counter1 should be "4"

  Scenario: Increment counter2 with if condition
    Given counter2 is "-1"
      And condition is defined by javascript "true"
     When I increment counter2
      And I increment counter2 while counter2 < 4 using no delay if condition
     Then counter2 should be "4"
