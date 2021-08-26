Feature: Attribute in table

  Scenario Outline: Examples table with attribute
      Given attribute1 is "1"
        And attribute2 is "<AttributeValue>"
       When I capture attribute2 as attribute3
       Then attribute3 should be "1"

    Examples:
      | AttributeValue |
      | ${attribute1}  |
      