Feature: Lookup state codes from CSV file
   
  Scenario: Lookup file and bind to given name
    Given mapping file is "src/test/features-data/StateCodes.csv"
     When I lookup Code in mapping file as state code where "'${csv.record.Name}' == 'Victoria'"
     Then state code should be "VIC"

  Scenario: Lookup file reference and bind to given name
    Given state name is ""
     When I lookup Name in the "src/test/features-data/StateCodes.csv" file as state name where "'${csv.record.Code}' == 'NSW'"
     Then state name should be "New South Wales"

  Scenario: Lookup file and bind to column name
    Given mapping file is "src/test/features-data/StateCodes.csv"
     When I lookup Code in mapping file where "'${csv.record.Name}' == 'Queensland'"
     Then Code should be "QLD"

  Scenario: Lookup file reference and bind to column name
    Given Name is ""
     When I lookup Name in the "src/test/features-data/StateCodes.csv" file where "'${csv.record.Code}' == 'WA'"
     Then Name should be "Western Australia"
