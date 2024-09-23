Feature: Repeat while file examples

  @StepDef
  @Action
  Scenario: I increment counter
    Given counter is defined by javascript "${counter} + 1"

  Scenario: Increment counter while filepath exists
    Given counter is "0"
     When @Try @Delay('100ms') @Timeout('1s') I increment counter while "gwen.conf" file exists
     Then counter should not be "0"

  Scenario: Increment counter while filepath exists with if condition
    Given counter is "-1"
     When @Try @Delay('100ms') @Timeout('1s') I increment counter while "gwen.conf" file exists if "gwen.conf" file exists
     Then counter should not be "-1"

  Scenario: Increment counter while filepathref not exists
    Given counter is "0"
      And the json file is "gwen.json"
     When @Try @Delay('100ms') @Timeout('1s') I increment counter while the json file not exists
     Then counter should not be "0"

  Scenario: Increment counter while filepathref not exists with if condition
    Given counter is "-1"
      And the conf file is "gwen.conf"
      And the json file is "gwen.json"
     When @Try @Delay('100ms') @Timeout('1s') I increment counter while the json file not exists if the conf file exists
     Then counter should not be "-1"

  Scenario: Increment counter while filepath not exists
    Given counter is "0"
     When @Try @Delay('100ms') @Timeout('1s') I increment counter while "gwen.json" file not exists
     Then counter should not be "0"

  Scenario: Increment counter while filepath not exists with if condition
    Given counter is "-1"
     When @Try @Delay('100ms') @Timeout('1s') I increment counter while "gwen.json" file not exists if "gwen.conf" file exists
     Then counter should not be "-1"

  Scenario: Increment counter while filepathref exists
    Given counter is "0"
      And the conf file is "gwen.conf"
     When @Try @Delay('100ms') @Timeout('1s') I increment counter while the conf file exists
     Then counter should not be "0"

  Scenario: Increment counter while filepathref exists with if condition
    Given counter is "-1"
      And the conf file is "gwen.conf"
      And the json file is "gwen.json"
     When @Try @Delay('100ms') @Timeout('1s') I increment counter while the conf file exists if the json file not exists
     Then counter should not be "-1"

  Scenario: Increment counter while filepath is not empty
    Given counter is "0"
     When @Try @Delay('100ms') @Timeout('1s') I increment counter while "gwen.conf" file is not empty
     Then counter should not be "0"

  Scenario: Increment counter while filepath is not empty with if condition
    Given counter is "-1"
     When @Try @Delay('100ms') @Timeout('1s') I increment counter while "gwen.conf" file is not empty if "gwen.conf" file is not empty
     Then counter should not be "-1"

  Scenario: Increment counter while filepathref is not empty
    Given counter is "0"
      And the conf file is "gwen.conf"
     When @Try @Delay('100ms') @Timeout('1s') I increment counter while the conf file is not empty
     Then counter should not be "0"

  Scenario: Increment counter while filepathref is not empty with if condition
    Given counter is "-1"
      And the conf file is "gwen.conf"
     When @Try @Delay('100ms') @Timeout('1s') I increment counter while the conf file is not empty if the conf file exists
     Then counter should not be "-1"

  Scenario: Increment counter while filepath is empty
    Given counter is "-1"
     When I increment counter while "gwen.conf" file is empty
     Then counter should be "-1"

  Scenario: Increment counter while filepathref is empty with if condition
    Given counter is "-1"
      And the conf file is "gwen.conf"
     When I increment counter while the conf file is empty if the conf file exists
     Then counter should be "-1"
