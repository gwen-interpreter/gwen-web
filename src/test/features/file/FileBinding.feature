Feature: File binding test

  Scenario: Read attribute from file
    Given the file value is defined by file "src/test/features/file/file.txt"
     When I capture the file value
     Then the file value should be "gwen"
