Feature: If Conditionals

  @StepDef
  @Action
  Scenario: no operation

  @StepDef
  @Action
  Scenario: I perform this
    Given the called step is "this step"
     When no operation

  @StepDef
  @Action
  Scenario: I conditionally perform this
    Given I perform this if this condition
      And I perform that if not this condition

  @StepDef
  @Action
  Scenario: I perform that
    Given the called step is "that step"

  Scenario: Perform this
    Given the target is "this"
      And this condition is defined by javascript ""${the target}" === "this""
     When I perform this if this condition
      And I perform that if not this condition
     Then the called step should be "this step"

  Scenario: Perform this 2
    Given the target is "this"
      And this condition is defined by javascript ""${the target}" === "this""
     When I conditionally perform this
     Then the called step should be "this step"

  Scenario: Perform that
    Given the target is "that"
      And that condition is defined by javascript ""${the target}" === "that""
     When I perform that if that condition
     Then the called step should be "that step"

  Scenario: Do not perform this
    Given the target is "that"
      And the called step is "none"
      And this condition is defined by javascript ""${the target}" === "this""
     When I perform this if this condition
     Then the called step should be "none"

  Scenario: Do perform this (negated)
    Given the called step is "none"
      And this condition is "true"
     When I perform this if not this condition
     Then the called step should be "none"

  Scenario: Do not perform this (negated)
    Given the called step is "none"
      And this condition is "false"
     When I perform this if not this condition
     Then the called step should be "this step"

  Scenario: Perform this using applied function as conditional
    Given the target is "this"
      And toBoolean is defined by js "arguments[0]"
      And this condition is defined by toBoolean applied to "true"
     When I perform this if this condition
      And I perform that if not this condition
     Then the called step should be "this step"  

  Scenario: Perform this using applied function as conditional (negated)
    Given the target is "this"
      And toBoolean is defined by js "arguments[0]"
      And this condition is defined by toBoolean applied to "false"
     When I perform this if not this condition
      And I perform that if this condition
     Then the called step should be "this step" 
