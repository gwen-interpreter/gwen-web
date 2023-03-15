@Lenient
Feature: JS function binding tests

  Scenario: ISO to DMY date single arg
    Given iso date is defined by js "new Date().toISOString().substring(0, 10)"
      And toDMY is defined by js
          """
          (function(ymd) {
            var parts = ymd.split('-');
            return parts[2] + '/' + parts[1] + '/' + parts[0];
          })(arguments[0])
          """
      And dmy date is defined by toDMY applied to "${iso date}"
     Then dmy date should match regex "^([0-2]\d|3[01])/([0]\d|1[0-2])/\d{4}$"

  Scenario: ISO to MDY date single arg
    Given toMDY is defined by js
          """
          (function(ymd) {
            var months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
            var parts = ymd.split('-');
            return months[parseInt(parts[1]) - 1] + ' ' + parts[2] + ' ' + parts[0];
          })(arguments[0])
          """
      And dmy date is defined by toMDY applied to "2022-04-05"
     Then dmy date should be "Apr 05 2022"

  Scenario: ISO to DMY date multi args
    Given iso date is defined by js "new Date().toISOString().substring(0, 10)"
      And toDMY is defined by js
          """
          (function(y,m,d) {
            return d + '/' + m + '/' + y;
          })(arguments[0], arguments[1], arguments[2])
          """
      And dmy date is defined by toDMY applied to "${iso date}" delimited by "-"
     Then dmy date should match regex "^([0-2]\d|3[01])/([0]\d|1[0-2])/\d{4}$"

  Scenario: MDY to ISO date multi args
    Given toISO is defined by js
          """
          (function(m,d,y) {
            var months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
            var mNo = months.indexOf(m) + 1;
            if (mNo < 10) mNo = '0' + mNo;
            return y + '-' + mNo + '-' + d;
          })(arguments[0], arguments[1], arguments[2])
          """
      And iso date is defined by toISO applied to "Apr 05 2022" delimited by " "
     Then iso date should be "2022-04-05"
