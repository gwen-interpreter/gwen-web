Feature: Dry Run Annotations

  @StepDef
  @Action
  Scenario: I process step one
    Given step is "one"

  @StepDef
  @Action
  Scenario: I process step two
    Given step is "two"

  @StepDef
  @Action
  Scenario: I process step three
    Given step is "three"

  @StepDef
  @Action
  Scenario: I process step four
    Given step is "four"

  @StepDef
  @Action
  Scenario: I process step five
    Given step is "five"

  @StepDef
  @Action
  Scenario: I process step
    Given I process step ${name}

  Scenario: Process multi steps
    Given names is defined by js    @DryRun(name='names',value='one two three')
          """
          ['one' ,'two' ,'three'].join(' ')
          """
     When I process step for each name in names delimited by " "
     Then step should be "three"

  Scenario: Process single step
    Given number is "five"
     When I process step ${number}    @DryRun(name='number',value={'one','two','three','four'})
     Then step should be "${number}"
