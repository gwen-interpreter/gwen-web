Feature: Formatter bindings

  Scenario: I format a date
    Given ISO date is "2026-03-14"
     When @DateTime I format ISO date from "yyyy-MM-dd" to "dd/MM/yyyy" as dmy date
     Then dmy date should be "14/03/2026"

  Scenario: I format an ISO date to a display date with oridnal day suffix
    Given ISO date is "2026-03-02"
     When @DateTime I format ISO date from "yyyy-MM-dd" to "d(st|nd|rd|th) MMMM yyyy" as display date
     Then display date should be "2nd March 2026"

  Scenario: I format a display date with oridnal day suffix to an ISO date
    Given display date is "23rd March 2026"
     When @DateTime I format display date from "d(st|nd|rd|th) MMMM yyyy" to "yyyy-MM-dd" as ISO date
     Then ISO date should be "2026-03-23"

  Scenario: I format a number
    Given number is "1234"
     When @Number I format number from "#" to "$#,##0.00" as dollars
     Then dollars should be "$1,234.00"

  Scenario: I format a number to a number with an ordinal suffix
    Given number is "22"
     When @Number I format number from "#" to "#(st|nd|rd|th)" as ordingal number
     Then ordingal number should be "22nd"

  Scenario: I format a number with an ordinal suffix to a leading zero number
    Given ordingal number is "1st"
     When @Number I format ordingal number from "#(st|nd|rd|th)" to "00" as leading zero number
     Then leading zero number should be "01"
