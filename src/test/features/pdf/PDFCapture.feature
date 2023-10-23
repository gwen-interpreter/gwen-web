Feature: PDF Capture

  Scenario: Capture PDF from the current URL
    Given the pdf URL is "https://gweninterpreter.org/assets/docs/gwen-reportportal-userguide.pdf"
      And the PDF text is ""
     When I navigate to "${the pdf URL}"
      And I capture the PDF text from the current URL
      And I capture the PDF text from the current URL as pdf text 1
     Then pdf text 1 should contain "Gwen"
      And the PDF text should not be blank
      And pdf text 1 should be
          """
          ${the PDF text}
          """

  Scenario: Capture PDF from a given URL
    Given the pdf URL is "https://gweninterpreter.org/assets/docs/gwen-reportportal-userguide.pdf"
      And the PDF text is ""
     When I capture the PDF text from url "${the pdf URL}"
      And I capture the PDF text from url "${the pdf URL}" as pdf text 2
      And I download "https://gweninterpreter.org/assets/docs/gwen-reportportal-userguide.pdf" to "target/download/gwen-reportportal-userguide.pdf"
     Then pdf text 2 should contain "Gwen"
      And the PDF text should not be blank
      And pdf text 2 should be
          """
          ${the PDF text}
          """

  Scenario: Capture PDF from a given file
    Given the pdf file is "src/test/resources/docs/gwen-reportportal-userguide.pdf"
      And the PDF text is ""
     When I capture the PDF text from file "${the pdf file}" as pdf text 3
      And I capture the PDF text from file "${the pdf file}"
     Then pdf text 3 should contain "Gwen"
      And the PDF text should not be blank
      And pdf text 3 should be
          """
          ${the PDF text}
          """
