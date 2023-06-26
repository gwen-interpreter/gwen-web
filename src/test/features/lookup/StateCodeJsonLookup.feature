Feature: Lookup state codes from JSON file

  Scenario: Lookup file and bind to given name
    Given mapping file is "src/test/features-data/StateCodes.json"
     When I lookup Code in mapping file as state code 1 where "'${json.record.Name}' == 'Victoria'"
      And I lookup Code in mapping file as state code 2 where "'${data.record.Name}' == 'Victoria'"
     Then state code 1 should be "VIC"
      And state code 2 should be "VIC"

  Scenario: Lookup file reference and bind to given name
    Given state name is ""
     When I lookup Name in the "src/test/features-data/StateCodes.json" file as state name 1 where "'${json.record.Code}' == 'NSW'"
      And I lookup Name in the "src/test/features-data/StateCodes.json" file as state name 2 where "'${data.record.Code}' == 'NSW'"
     Then state name 1 should be "New South Wales"
      And state name 2 should be "New South Wales"

  Scenario: Lookup file and bind to column name by json.record filter
    Given mapping file is "src/test/features-data/StateCodes.json"
      And target state is "Queensland"
     When I lookup Code in mapping file where "'${json.record.Name}' == '${target state}'"
     Then Code should be "QLD"

  Scenario: Lookup file and bind to column name by data.record filter
    Given mapping file is "src/test/features-data/StateCodes.json"
      And target state is "Queensland"
     When I lookup Code in mapping file where "'${data.record.Name}' == '${target state}'"
     Then Code should be "QLD"

  Scenario: Lookup file reference and bind to column name by json.record filter
    Given Name is ""
     When I lookup Name in the "src/test/features-data/StateCodes.json" file where "'${json.record.Code}' == 'WA'"
     Then Name should be "Western Australia"

  Scenario: Lookup file reference and bind to column name by data.record filter
    Given Name is ""
     When I lookup Name in the "src/test/features-data/StateCodes.json" file where "'${data.record.Code}' == 'WA'"
     Then Name should be "Western Australia"
