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
      And js condition is defined by js "'${the target}' === 'this'"
      And cond is "${js condition}"
     When I perform this if cond
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

  Scenario: If condition with blank check
     Given the state code is blank
      When I navigate to "https://example.com/" if the state code is blank
      Then the current URL should be "https://example.com/"

  Scenario: If condition with non blank check
     Given the state code is "state"
      When I navigate to "https://example.com/?state=${the state code}" if the state code is not blank
      Then the current URL should be "https://example.com/?state=${the state code}"

  Scenario: If condition with equal value
      Given the state code is "VIC"
       When I navigate to "https://example.com/?state=Victoria" if the state code is "VIC"
       Then the current URL should be "https://example.com/?state=Victoria"

  Scenario: If condition with non equal value
      Given the state code is "NSW"
       When I navigate to "https://example.com/" if the state code is not "VIC"
       Then the current URL should be "https://example.com/"

  Scenario: If condition with non matching value
      Given the state code is "VIC"
       When I navigate to "https://example.com/?state=Victoria" if the state code matches regex "(VIC|NSW)"
       Then the current URL should be "https://example.com/?state=Victoria"

  Scenario: If condition with non matching value
      Given the state code is "QLD"
       When I navigate to "https://example.com/?state=Other" if the state code does not match regex "(VIC|NSW)"
       Then the current URL should be "https://example.com/?state=Other"

  Scenario: Perform this if filepath exists
    Given the target is "this"
     When I perform this if "gwen.conf" file exists
      And I perform that if "gwen.conf" file does not exist
     Then the called step should be "this step"
      And the called step should not be "that step"

  Scenario: Perform this if fileref exists
    Given the target is "that"
      And the file is "gwen.conf"
     When I perform that if the file exists
      And I perform this if the file not exists
     Then the called step should be "that step"
      And the called step should not be "this step"

  Scenario: Perform this if file empty or not
    Given the target is "this"
     When I perform this if "gwen.conf" file is not empty
      And I perform that if "gwen.conf" file is empty
     Then the called step should be "this step"
      And the called step should not be "that step"

  Scenario: Perform this if fileref emtpy or not
    Given the target is "that"
      And the file is "gwen.conf"
     When I perform that if the file is not empty
      And I perform this if the file is empty
     Then the called step should be "that step"
      And the called step should not be "this step"
