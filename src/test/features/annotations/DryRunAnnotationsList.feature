Feature: Dry Run Annotations - List

  Scenario: Perform all steps
    Given sequence is ""
      And number is "1"
     When I perform step ${number}    @DryRun(name='number',value=['1','2','3'])
     Then number should be "1"
      And sequence should start with "11"
