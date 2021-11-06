Feature: Join Strings

  Scenario Outline: Joining <string 1> and <string 2> should yield <result>

    This scenario is evaluated at the point where the outline is declared.
    Joining <string 1> and <string 2> should yield <result>

    Given string 1 is "<string 1>"
      And string 2 is "<string 2>"
     When I join the two strings
     Then the result should be "<result>"

    Examples: Basic string concatenation

      The header row contains the placeholder names. The body rows that
      follow contain the data that is bound to each scenario that is evaluated.

        | string 1 | string 2 | result   |
        | howdy    | doo      | howdydoo |
        | any      | thing    | anything |

  Scenario: Verify that we can join two strings in meta
    Given result is ""
     When I join two strings in meta
     Then the result should not be ""
