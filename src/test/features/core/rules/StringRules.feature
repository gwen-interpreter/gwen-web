Feature: String rules

  Rule: Joining two strings together should result in a string containing both

    Background: Reset strings
      Given string 1 is ""
        And string 2 is ""
       When I join the two strings
       Then the result should be ""
        And string 1 should not be "${gwen.eval.duration}"
        And string 2 should not be "${gwen.eval.duration.msecs}"

    Scenario Template: Joining <string 1> and <string 2> should yield <result>

      Joining <string 1> and <string 2> should yield <result>

      Given string 1 is "<string 1>"
        And string 2 is "<string 2>"
       When I join the two strings
       Then the result should be "<result>"
        And string 1 should not be "${gwen.eval.duration}"
        And string 2 should not be "${gwen.eval.duration.msecs}"

      Examples: Basic string concatenation

        The header row contains the placeholder names. The body rows that
        follow contain the data that is bound to each scenario that is evaluated.

          | string 1 | string 2 | result   |
          | howdy    | doo      | howdydoo |
          | any      | thing    | anything |

    Example: Verify that we can join two strings in meta
      Given the result is ""
       When I join two strings in meta
       Then the result should not be ""
        And the result should not be "${gwen.eval.duration}"

  Rule: Replacing a substring in a string should result in substitution of the substring

    Background: Reset strings
      Given string 1 is ""
        And string 2 is ""
        And string 3 is ""
       When I substitute string 1 for string 2 in string 3
       Then the result should be ""
        And string 1 should not be "${gwen.eval.duration}"
        And string 2 should not be "${gwen.eval.duration.msecs}"

    Scenario Template: Substituting <string 1> for <string 2> in <string 3> should yield <result>

      Substituting <string 1> for <string 2> in <string 3> should yield <result>

      Given string 1 is "<string 1>"
        And string 2 is "<string 2>"
        And string 3 is "<string 3>"
       When I substitute string 1 for string 2 in string 3
       Then the result should be "<result>"
        And string 1 should not be "${gwen.eval.duration}"
        And string 2 should not be "${gwen.eval.duration.msecs}"

      Examples: Basic string concatenation

        The header row contains the placeholder names. The body rows that
        follow contain the data that is bound to each scenario that is evaluated.

          | string 1 | string 2 | string 3         | result          |
          | cat      | dog      | feed the cat     | feed the dog    |
          | sleep    | code     | I sleep by night | I code by night |
