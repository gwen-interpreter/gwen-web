Feature: Dry Run Annotations (Single)

  @StepDef
  @Action
  Scenario: I perform step 1
     Then number should be "1"

  Scenario: Perform one step (single value)
    Given number is defined by js "'1'"    @DryRun(name='number',value='1')
     When I perform step ${number}
     Then number should be "1"
