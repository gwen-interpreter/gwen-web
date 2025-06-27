Feature: Dry Run Annotations - Single

  Scenario: Perform one step
    Given sequence is ""
      And number is defined by js "'2'"    @DryRun(name='number',value='2')
     When I perform step ${number}
     Then number should be "2"
      And sequence should be "22"
