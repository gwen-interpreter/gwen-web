@Lenient
Feature: JS function binding tests
	    
  Scenario: ISO to DMY date single arg
    Given iso date is "2022-04-05"
      And iso-to-dmy is defined by js
          """
          (function(ymd) {
            var parts = ymd.split('-');
            return parts[2] + '/' + parts[1] + '/' + parts[0];
          })(arguments[0])
          """
      And dmy date is defined by iso-to-dmy applied to "${iso date}"
     Then dmy date should be "05/04/2022"
	
  Scenario: MDY date multi args
    Given day is "05"
      And month is "Apr"
      And year is "2022"
      And to-mdy is defined by js
          """
          (function(d,m,y) {
            return m + ' ' + d + ', ' + y;
          })(arguments[0], arguments[1], arguments[2])
          """
      And mdy date is defined by to-mdy applied to "${day},${month},${year}"
     Then mdy date should be "Apr 05, 2022"

  Scenario: DMY date multi args custom delimiter
    Given iso date is "2022-04-06"
      And to-dmy is defined by js
          """
          (function(y,m,d) {
            return d + '/' + m + '/' + y;
          })(arguments[0], arguments[1], arguments[2])
          """
      And dmy date is defined by to-dmy applied to "${iso date}" delimited by "-"
     Then dmy date should be "06/04/2022"