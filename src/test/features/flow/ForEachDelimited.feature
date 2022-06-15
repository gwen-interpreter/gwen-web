Feature: For each delimited value

  Scenario: Process each delimited value
    Given values is "One,Two,Three"
      And x is "${value}" for each value in values delimited by ","
     When I capture values
     Then value should be absent

  @StepDef
  @DataTable
  @Context
  Scenario: I process the following user roles
    Given processed roles is ""
      And I process user roles for each data record

  @StepDef
  Scenario: I process user roles
    Given processed roles is "${processed roles}${role}" for each role in data[Roles] delimited by ","

  Scenario: Process each delimited value for data record in table
    Given I process the following user roles
          | User | Roles       |
          | abc  | role1,role2 |
          | cbd  | role3,role4 |
     When I capture processed roles
     Then processed roles should be "role1role2role3role4"

  Scenario: For-each on empty iteration should do nothing
    Given items is ""
     When z is "${item}" for each item in items delimited by ","
     Then z should be absent
