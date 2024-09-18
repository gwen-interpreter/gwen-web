@Ignore
Feature: Relative locators

  Scenario: Locate input fields relative to a button
    Given the button can be located by tag "button"
      And field 1 can be located by tag name "input" above the button
      And field 2 can be located by css "input" to left of the button
      And @Timeout('5s') field 3 can be located by xpath "//input" to right of the button
      And field 4 can be located by tag "input" below the button
      And field 5 can be located by tag "input" near the button
      And field 6 can be located by tag "input" near and within 60 pixels of the button
     When I navigate to "https://chercher.tech/practice/relative-locators"
      And I type "above button" in field 1
      And I type "to left of button" in field 2
      And I type "to right of button" in field 3
      And I type "below button" in field 4
      And I append " & near" to field 5
     Then field 1 should contain "above button"
      And field 2 should contain "to left of button"
      And field 3 should contain "to right of button"
      And field 4 should contain "below button"
      And field 5 should contain "& near"
      And field 6 should contain "& near"
