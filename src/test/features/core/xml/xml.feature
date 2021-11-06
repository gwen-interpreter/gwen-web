Feature: XML feature

  Scenario: Pass XML to StepDef
    Given content is "<?xml version="1.0" encoding="UTF-8"?><Test>test</Test>"
     When I process unquoted XML ${content}
      And I process quoted XML "${content}"
      And I process XML in content
     Then xml1 should not be ""
      And xml2 should not be ""
      And xml3 should not be ""
