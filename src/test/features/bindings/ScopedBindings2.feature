@Lenient
Feature: Scoped binding tests with background initializer

  Background: Init attributes
    Given a is "1"
      And b is defined by javascript "1"
      And c is "1"
      And d is defined by javascript "${c}"
      And e is "1"
     Then a should be "1"
      And b should be "1"
      And c should be "1"
      And d should be "1"
      And e should be "1"

  Scenario: System properties should resolve
    Given my java version is "${java.version}"
     Then my java version should not contain "java.version"

  Scenario: Updated binding defined should override prior value
    Given a is "2"
      And a should be "2"

  Scenario: Binding of different type should override global of same name
    Given c is defined by javascript "2"
      And d is defined by javascript "${c}1"
     Then c should be "2"
      And d should be "21"

  Scenario: Capture of dynamic attribute into static attribute should be visible
    Given e should be "1"
     When e is defined by javascript "2+1"
      And I capture the text in e by regex "(\d)" as e
      And ee is "${e}"
     Then ee should be "3"
     When e is "2"
     Then e should be "2"
     When e is defined by javascript "2+2"
     Then e should be "4"
