Feature: Data Load

  @StepDef
  @Examples('src/test/features/core/outlines/Data.json')
  @Parallel
  @Assertion
  Scenario Outline: JSON examples should load
    Given the website is "${website}"
     When I capture the text in the website by regex "(.+?):.*" as the protocol
     Then the protocol should be "https"
      And the secret should not be blank
      
  @Examples('src/test/features/core/outlines/Data.csv')
  Scenario Outline: CSV examples should load
    Given the website is "${WEBSITE}"
      And @Eager the secret is defined by js "'secret'"
     When I capture the text in the website by regex "(.+?):.*" as the protocol
     Then the protocol should be "https"
      And JSON examples should load

