Feature: Select each user

  @StepDef
  @Action
  Scenario: I select all users in "<selectUser>"
    Given users is "$<selectUser>"
      And condition is defined by javascript "true"
     When I select "${selectUser}" for each selectUser in users delimited by "," if condition

  @StepDef
  @Action
  Scenario: I select "<user>"
    Given the user is "$<user>"
     Then the user should match regex "user\d"
      And my selected users is "${my selected users}$<user>"

  Scenario: Select all my users
    Given my selected users is ""
     When I select all users in "user1,user2,user3"
     Then my selected users should be "user1user2user3"
     