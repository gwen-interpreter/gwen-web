Feature: Formatter bindings

  Scenario: I format a date
    Given ISO date is "2026-03-14"
     When @DateTime I format ISO date from "yyyy-MM-dd" to "dd/MM/yyyy" as dmy date
     Then dmy date should be "14/03/2026"

  Scenario: I format a number
    Given number is "1234"
     When @Number I format number from "#" to "$#,##0.00" as dollars
     Then dollars should be "$1,234.00"
