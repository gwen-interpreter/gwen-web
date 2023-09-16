Feature: Dry Run Annotations - Delimited

  Scenario: Perform all steps
    Given numbers is defined by js    @DryRun(name='numbers',value={'1 2 3'})
          """
          ['1', '2', '3'].join(' ')
          """
     When I perform step ${number} for each number in numbers delimited by " "
     Then numbers should be "1 2 3"
      And sequence should be "112233"

