Feature: Match and extract Text templates

  Scenario: Text mismatch
    Given my text content is
          """
          1 May 2025
          GWEN STACEY
          U 123
          1 TORCHWOOD AVE
          TORCHWOOD VIC 3058
          Account number: 123456
          Summary
          Active
          """
     When I capture my text content as contents
     Then contents should not match template 
          """
          @{**}
          GWEN STACEY
          @{**}
          Account number
          is
          @{Account number}
          Summary
          @{**}
          """
      And Account number should not be defined

  Scenario: Text match
    Given my text content is
          """
          1 May 2025
          GWEN STACEY
          U 123
          1 TORCHWOOD AVE
          TORCHWOOD VIC 3058
          Account number: 123456
          Summary
          Active
          """
     When I capture my text content as contents
     Then contents should match template 
          """
          @{**}
          GWEN STACEY
          @{**}
          Account number: @{Account number}
          Summary
          @{**}
          """
      And Account number should be "123456"
