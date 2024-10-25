Feature: Join Strings

  Scenario Outline: Joining <string 1> and <string 2> should yield <result> 1

    This scenario is evaluated at the point where the outline is declared.
    Joining <string 1> and <string 2> should yield <result>

    Given the result is blank
     When I join the two strings
     Then the result should be "<result>"

    @Parallel
    Examples: Basic string concatenation

      The header row contains the placeholder names. The body rows that
      follow contain the data that is bound to each scenario that is evaluated.

        | string 1 | string 2 | result   |
        | howdy    | doo      | howdydoo |
        | any      | thing    | anything |

  Scenario: Verify that we can join two strings in meta
    Given the result is blank
     When I join two strings in meta
     Then the result should be defined

  Scenario Outline: Joining <text 1> and <text 2> should yield <result> with impilictly bound cells

    This scenario is evaluated at the point where the outline is declared.
    Joining <text 1> and <text 2> should yield <result>

    Given the result is "${text 1}${text 2}"
     When I capture the result
     Then the result should be "<result>"
      And the result should be "${result}"

    Examples: Basic string concatenation

      The header row contains the placeholder names. The body rows that
      follow contain the data that is bound to each scenario that is evaluated.

        | text 1 | text 2 | result     |
        | butter | fly    | butterfly  |
        | basket | ball   | basketball |

  Scenario Outline: Empty examples tables should do nothing
    Given the result is "${text 1}${text 2}"
     When I capture the result
     Then the result should be "<result>"
      And the result should be "${result}"

    Examples: Empty examples table

      The header row contains the placeholder names. The body is empty.

        | text 1 | text 2 | result |
