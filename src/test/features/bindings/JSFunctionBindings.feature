@Lenient
Feature: JS function binding tests
	    
  Scenario: ISO to DMY date single arg
      Given toDMY is defined by js
        """
        (function(ymd) {
          var parts = ymd.split('-');
          return parts[2] + '/' + parts[1] + '/' + parts[0];
        })(arguments[0])
        """
    And dmy date is defined by toDMY applied to "2022-04-05"
   Then dmy date should be "05/04/2022"
	
  Scenario: MDY date multi args
      Given toMDY is defined by js
        """
        (function(d,m,y) {
          return m + ' ' + d + ', ' + y;
        })(arguments[0], arguments[1], arguments[2])
        """
    And mdy date is defined by toMDY applied to "05 April 2022" delimited by " "
   Then mdy date should be "April 05, 2022"

  Scenario: DMY date multi args custom delimiter
    Given toDMY is defined by js
        """
        (function(y,m,d) {
          return d + '/' + m + '/' + y;
        })(arguments[0], arguments[1], arguments[2])
        """
    And dmy date is defined by toDMY applied to "2022-04-06" delimited by "-"
   Then dmy date should be "06/04/2022"
   