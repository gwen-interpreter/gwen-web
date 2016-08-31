Feature: Scoped binding tests
      
Scenario: Init globals
	Given a is "1"
	  And b is defined by javascript "1"
	  And c is "1"
      And d is defined by javascript "${c}"
     Then a should be "1"
      And b should be "1"
      And c should be "1"
      And d should be "1" 
	
Scenario: Global binding defined in local scope should override that in feature scope
    Given I am on the A page
     Then a is "2"
      And a should be "2"
      
Scenario: Global binding defined in local scope should override javascript binding in feature scope
     When I am on the B page
     Then b is "2"
      And b should be "2"
      
Scenario: Binding of different type in local scope should override global of same name
     When I am on the CD page
     Then c is defined by javascript "2"
      And d is defined by javascript "${c}1"
      And c should be "2"
      And d should be "21"