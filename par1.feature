
Feature: Reconciliation Module

Scenario: Login and setup unique time variables...
	
	
	And I acquire a lock for 15 seconds
	And I wait 16 seconds
	And I release a lock
	And I wait 10 seconds
	And I acquire a lock for 30 seconds
	And I wait 100 seconds
	And I release a lock
		And I wait 10 seconds
	And I acquire a lock for 45 seconds
	And I wait 18 seconds
	And I release a lock
	
	