Feature: Lookup items from CSV file
   
  Scenario: Lookup and bind to given names
    Given mapping file is "src/test/features-data/TodoItems1.csv"
     When I lookup Item in mapping file as done item where "'${csv.record.Status}' == 'done'"
      And I lookup Item in the "src/test/features-data/TodoItems1.csv" file as pending item where "'${csv.record.Status}' == 'pending'"
     Then done item should be "Get the milk"
      And pending item should be "Walk the dog"

  Scenario: Lookup done item and bind to column name
    Given mapping file is "src/test/features-data/TodoItems1.csv"
     When I lookup Item in mapping file where "'${csv.record.Status}' == 'done'"
     Then Item should be "Get the milk"

  Scenario: Lookup pending item and bind to column name
    Given mapping file is "src/test/features-data/TodoItems1.csv"
     When I lookup Item in the "src/test/features-data/TodoItems1.csv" file where "'${csv.record.Status}' == 'pending'"
     Then Item should be "Walk the dog"
