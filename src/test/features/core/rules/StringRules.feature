Feature: String rules

  Rule: Joining two strings together should result in a string containing both

    Background: Reset strings

      Howdy doo

      Given the result is blank
       When I join the two strings
       Then the result should not be blank

    Scenario Template: Joining <string 1> and <string 2> should yield <result>

      Joining <string 1> and <string 2> should yield <result>

      Given the result is blank
       When I join the two strings
       Then the result should be "<result>"

      Examples: Basic string concatenation

        The header row contains the placeholder names. The body rows that
        follow contain the data that is bound to each scenario that is evaluated.

          | string 1 | string 2 | result   |
          | howdy    | doo      | howdydoo |
          | any      | thing    | anything |

    Example: Verify that we can join two strings in meta
      Given the result is blank
       When I join two strings in meta
       Then the result should not be blank if gwen.state.level is "feature"
        And gwen.scenario.name should be "Verify that we can join two strings in meta"
        And gwen.rule.name should be "Joining two strings together should result in a string containing both"

  Rule: Replacing a substring in a string should result in substitution of the substring

    Background: Reset strings
      Given the result is blank
       When I substitute string 1 for string 2 in string 3
       Then the result should not be blank
        And gwen.rule.name should be "Replacing a substring in a string should result in substitution of the substring"

    Scenario Template: Substituting <string 1> for <string 2> in <string 3> should yield <result>

      Substituting <string 1> for <string 2> in <string 3> should yield <result>

      Given the result is blank
       When I substitute string 1 for string 2 in string 3
       Then the result should be "<result>"
        And gwen.rule.name should be "Replacing a substring in a string should result in substitution of the substring"
        And gwen.scenario.name should contain "-- More basic string concatenation"

      Examples: More basic string concatenation

        The header row contains the placeholder names. The body rows that
        follow contain the data that is bound to each scenario that is evaluated.

          | string 1 | string 2 | string 3         | result          |
          | cat      | dog      | feed the cat     | feed the dog    |
          | sleep    | code     | I sleep by night | I code by night |
