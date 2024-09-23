Feature: File assertions

  Scenario: Assert file exists
    Given the file is defined by js "'src/test/features/file/file.txt'"
     When I capture the file
     Then "src/test/features/file" file should exist
      And the file should exist

  Scenario: Assert file does not exist
    Given the file is defined by js "'src/test/features/file/missing.txt'"
     When I capture the file
     Then "src/test/features/file/missing.txt" file should not exist
      And the file should not exist

  Scenario: Assert file shoud be empty
    Given the file is defined by js "'src/test/features/file/file2.txt'"
     When I capture the file
     Then "src/test/features/file/file2.txt" file should be empty
      And the file should be empty

  Scenario: Assert file shoud not be empty
    Given the file is defined by js "'src/test/features/file/file.txt'"
     When I capture the file
     Then "src/test/features/file/file.txt" file should not be empty
      And the file should not be empty
