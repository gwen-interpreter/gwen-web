Feature: Conditionals

  @StepDef
  @Action
  Scenario: I perform this scenario
    Given the called step is "this step"

  @StepDef
  @Action
  Scenario: I perform that scenario
    Given the called step is "that step"

  Scenario: Perform this
    Given the variable is "this"
     When I perform ${the variable} scenario
     Then the called step should be "this step"

  Scenario: Perform that
    Given the variable is "that"
     When I perform ${the variable} scenario
     Then the called step should be "that step"
