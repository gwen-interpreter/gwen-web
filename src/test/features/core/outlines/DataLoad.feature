Feature: Data Load
      
  @Examples('src/test/features-data/Data.csv')
  Scenario Outline: CSV examples should load
    Given the website is "${WEBSITE}"
      And @Eager the secret is defined by js "'secret'"
     When I capture the text in the website by regex "(.+?):.*" as the protocol
     Then the protocol should be "https"
      And JSON examples should load

