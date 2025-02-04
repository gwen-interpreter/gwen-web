Feature: Zero Argument Functions

  @StepDef
  @Assertion
  Scenario: Zero argument functions should work
     Then name should be "${ () => 'Gwen'}"
      And desc should be "${ () => { return '${name} Automation' } }"
      And name should be "${ => 'Gwen'}"
      And desc should be "${ => { return '${name} Automation' } }"

  @StepDef
  @Assertion
  Scenario: Zero argument functions in DocStrings should work
     Then desc should be
          """
          ${ () => '${name} Automation' }
          """
      And desc should be
          """
          ${ () => {
            return '${name} Automation'
          }}
          """
      And desc should be
          """
          ${ => '${name} Automation' }
          """
      And desc should be
          """
          ${ => {
            return '${name} Automation'
          }}
          """

  @StepDef
  @Assertion
  Scenario: Zero argument functions in documented examples should work
    Given today is defined by js "() => new Date().toISOString().substring(0, 10)"
      And tomorrow is defined by js
          """
          () => { 
              var today = new Date()
              var tomorrow = new Date()
              tomorrow.setDate(today.getDate() + 1)
              return tomorrow.toISOString().substring(0, 10)
          }
          """
     Then today should not be "${tomorrow}"

  @StepDef
  @Assertion
  Scenario: Standard JS arrows should not be impacted
    Given result is defined by js
          """
          ['gwen'].some(n => n == 'gwen')
          """
     Then result should be true

  Scenario: Invoke the zero argument functions
    Given name is "Gwen"
      And desc is "Gwen Automation"
     When I capture name
     Then Zero argument functions should work
      And Zero argument functions in DocStrings should work
      And Zero argument functions in documented examples should work
      And Standard JS arrows should not be impacted
      And name should not be "${gwen.feature.eval.duration}"
      And desc should not be "${gwen.feature.eval.duration.msecs}"
      And desc should not be "${gwen.feature.eval.duration.secs}"
