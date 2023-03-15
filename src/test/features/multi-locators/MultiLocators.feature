Feature: Multi locators

  Background: Open browser to google
    Given the current URL is ""
     When I navigate to "http://google.com"
     Then the current URL should contain "google"

  Scenario: Last locator of three should find the search field

    Defines three locators for the search field.
    The 1st and 2nd will fail and the 3rd will succeed.

    Given the search field can be located by
          | selector   | expression    |
          | id         | search        |
          | class name | .search-field |
          | name       | q             |
     When I locate the search field
     Then the search field should be displayed
