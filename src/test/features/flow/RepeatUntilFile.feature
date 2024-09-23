Feature: Repeat until file examples

  @StepDef
  @Action
  Scenario: I increment counter
    Given counter is defined by javascript "${counter} + 1"

  Scenario: Increment counter until filepath does not exist
    Given counter is "0"
     When @Delay('100ms') @Timeout('1s') I increment counter until "gwen.json" file not exists
     Then counter should be "1"

  Scenario: Increment counter until filepath does not exist with if condition
    Given counter is "-1"
     When @Delay('100ms') @Timeout('1s') I increment counter until "gwen.json" file does not exist if "gwen.conf" file exists
     Then counter should be "0"

  Scenario: Increment counter until filepathref does not exist
    Given counter is "0"
      And the json file is "gwen.json"
     When @Delay('100ms') @Timeout('1s') I increment counter until the json file not exists
     Then counter should be "1"

  Scenario: Increment counter until filepathref does not exist with if condition
    Given counter is "-1"
      And the conf file is "gwen.conf"
      And the json file is "gwen.json"
     When @Delay('100ms') @Timeout('1s') I increment counter until the json file does not exist if the conf file exists
     Then counter should be "0"

  Scenario: Increment counter until filepath exists
    Given counter is "0"
     When @Delay('100ms') @Timeout('1s') I increment counter until "gwen.conf" file exists
     Then counter should be "1"

  Scenario: Increment counter until filepath exists with if condition
    Given counter is "-1"
     When @Delay('100ms') @Timeout('1s') I increment counter until "gwen.conf" file exists if "gwen.json" file not exists
     Then counter should be "0"

  Scenario: Increment counter until filepathref exists
    Given counter is "0"
      And the conf file is "gwen.conf"
     When @Delay('100ms') @Timeout('1s') I increment counter until the conf file exists
     Then counter should be "1"

  Scenario: Increment counter until filepathref exists with if condition
    Given counter is "-1"
      And the conf file is "gwen.conf"
      And the json file is "gwen.json"
     When @Delay('100ms') @Timeout('1s') I increment counter until the conf file exists if the json file does not exist
     Then counter should be "0"

  Scenario: Increment counter until filepath is not empty
    Given counter is "0"
     When @Delay('100ms') @Timeout('1s') I increment counter until "gwen.conf" file is not empty
     Then counter should be "1"

  Scenario: Increment counter until filepath is not empty with if condition
    Given counter is "-1"
     When @Delay('100ms') @Timeout('1s') I increment counter until "gwen.conf" file is not empty if "gwen.conf" file is not empty
     Then counter should be "0"

  Scenario: Increment counter until filepathref is not empty
    Given counter is "0"
      And the conf file is "gwen.conf"
     When @Delay('100ms') @Timeout('1s') I increment counter until the conf file is not empty
     Then counter should be "1"

  Scenario: Increment counter until filepathref is not empty with if condition
    Given counter is "-1"
      And the conf file is "gwen.conf"
     When @Delay('100ms') @Timeout('1s') I increment counter until the conf file is not empty if the conf file exists
     Then counter should be "0"
