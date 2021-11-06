Feature: For each delimited value with if condition

  @StepDef
  @Action
  Scenario: I click option "<option>" in "<type>"
    Given the option is "$<option>"
      And the type is "$<type>"
      And the spaces is "[          ]"
      And the tildes is "[~~~~~~~~~~]"

  @StepDef
  @Action
  Scenario: I click checbox in "<type>"
    Given options is "~option1,option2,option3"
      And condition is defined by javascript "true"
     When I click option "${opt}" in "$<type>" for each opt in options delimited by "," if condition

  Scenario: I process options
    Given the type is "my type"
      And the spaces is "[          ]"
      And the tildes is "[~~~~~~~~~~]"
     When I click checbox in "group"
     Then the option should be "option3"
      And the type should be "group"
