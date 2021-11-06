Feature: If Conditionals

  @StepDef
  @Action
  Scenario: I perform this
    Given the called step is "this step"

  @StepDef
  @Action
  Scenario: I perform that
    Given the called step is "that step"

  Scenario: Perform this
    Given the target is "this"
      And this condition is defined by javascript ""${the target}" == "this""
     When I perform this if this condition
     Then the called step should be "this step"

  Scenario: Perform that
    Given the target is "that"
      And that condition is defined by javascript ""${the target}" == "that""
     When I perform that if that condition
     Then the called step should be "that step"

   Scenario: Do not perform this
    Given the target is "that"
      And the called step is "none"
      And this condition is defined by javascript ""${the target}" == "this""
     When I perform this if this condition
     Then the called step should be "none"
