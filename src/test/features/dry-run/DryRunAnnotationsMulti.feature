Feature: Dry Run Annotations (Multi)

  @StepDef
  @Action
  Scenario: I perform step 1
     Then number should be "1"

  @StepDef
  @Action
  Scenario: I perform step 2
     Then number should not be "1"

  @StepDef
  @Action
  Scenario: I perform step 3
     Then number should not be "1"

  @StepDef
  @Action
  Scenario: I perform step 4
     Then number should not be "1"

  Scenario: Perform all steps (multiple values)
    Given numbers is defined by js "['1', '2'].join(' ')"
     When I perform step ${number} for each number in numbers delimited by " "    @DryRun(name='number',value={'1','2'})
     Then numbers should be "1 2"

  Scenario: Perform all steps (delimited values)
    Given numbers is defined by js    @DryRun(name='numbers',value='1 2')
          """
          ['1', '2'].join(' ')
          """
     When I perform step ${number} for each number in numbers delimited by " "
     Then numbers should be "1 2"

  Scenario: Perform single step
    Given number is "3"
     When I perform step ${number}    @DryRun(name='number',value={'1','2','3','4'})
     Then number should be "3"
