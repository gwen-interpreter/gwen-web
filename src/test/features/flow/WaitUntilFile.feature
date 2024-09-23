Feature: Wait until file

  Scenario: Wait until filepath exists
    Given counter is "0"
     When I wait until "gwen.conf" file exists
     Then counter should be "0"

  Scenario: Wait until filepathref exists
    Given counter is "0"
      And the conf file is "gwen.conf"
     When I wait until the conf file exists
     Then counter should be "0"

  Scenario: Wait until filepath does not exist
    Given counter is "-1"
     When @Timeout('1s') I wait until "gwen.json" file not exists if "gwen.conf" file exists
     Then counter should be "-1"

  Scenario: Wait until filepathref does not exist
    Given counter is "-1"
      And the json file is "gwen.json"
     When @Timeout('1s') I wait until the json file does not exist if the json file not exists
     Then counter should be "-1"

  Scenario: Wait until filepath is not empty
    Given counter is "0"
     When I wait until "gwen.conf" file is not empty
     Then counter should be "0"

  Scenario: Wait until filepathref is not empty
    Given counter is "0"
      And the conf file is "gwen.conf"
     When I wait until the conf file is not empty
     Then counter should be "0"

  Scenario: Wait until filepath is empty
    Given counter is "-1"
     When @Try @Timeout('1s') I wait until "gwen.conf" file is empty if "gwen.conf" file exists
     Then counter should be "-1"

  Scenario: Wait until filepathref is emptyt
    Given counter is "-1"
      And the conf file is "gwen.conf"
      And the json file is "gwen.json"
     When @Try @Timeout('1s') I wait until the conf file is empty if the json file does not exist
     Then counter should be "-1"
