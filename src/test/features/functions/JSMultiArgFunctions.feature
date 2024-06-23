Feature: Multi Argument Functions

  @StepDef
  @Assertion
  Scenario: Multi argument functions should work
     Then desc should be "${ (t1 = text1, t2 = text2) => t1 + ' - ' + t2 }"
      And desc should be "${ (t1 = text1, t2 = text2) => t1 + '${spacer}' + t2 }"
      And desc should be "${ (t1 = text1, s = spacer, t2 = text2) => { return t1 + s +  t2 } }"
      And uppr should be "${ (t1 = text1, t2 = text2) => { return (t1 + ' - ' + t2).toUpperCase() } }"
      And uppr should be "${ (t1 = text1, s = spacer, t2 = text2) => (t1 + s + t2).toUpperCase() }"
      And desc should be "${ (t1 = text 1, t2 = text 2) => t1 + ' - ' + t2 }"
      And desc should be "${ (t1 = text 1, t2 = text 2) => t1 + '${spacer}' + t2 }"
      And desc should be "${ (t1 = text 1, s = spacer, t2 = text 2) => { return t1 + s +  t2 } }"
      And uppr should be "${ (t1 = text 1, t2 = text 2) => { return (t1 + ' - ' + t2).toUpperCase() } }"
      And uppr should be "${ (t1 = text 1, s = spacer, t2 = text 2) => (t1 + s + t2).toUpperCase() }"
      And desc should be "${ (text1, text2) => text1 + ' - ' + text2 }"
      And desc should be "${ (text1, text2) => text1 + '${spacer}' + text2 }"
      And desc should be "${ (text1, spacer, text2) => { return text1 + spacer +  text2 } }"
      And uppr should be "${ (text1, text2) => { return (text1 + ' - ' + text2).toUpperCase() } }"
      And uppr should be "${ (text1, spacer, text2) => (text1 + spacer + text2).toUpperCase() }"
      And desc should be "${ t1 = text1, t2 = text2 => t1 + ' - ' + t2 }"
      And desc should be "${ t1 = text1, t2 = text2 => t1 + '${spacer}' + t2 }"
      And desc should be "${ t1 = text1, s = spacer, t2 = text2 => { return t1 + s +  t2 } }"
      And uppr should be "${ t1 = text1, t2 = text2 => { return (t1 + ' - ' + t2).toUpperCase() } }"
      And uppr should be "${ t1 = text1, s = spacer, t2 = text2 => (t1 + s + t2).toUpperCase() }"
      And desc should be "${ t1 = text 1, t2 = text 2 => t1 + ' - ' + t2 }"
      And desc should be "${ t1 = text 1, t2 = text 2 => t1 + '${spacer}' + t2 }"
      And desc should be "${ t1 = text 1, s = spacer, t2 = text 2 => { return t1 + s +  t2 } }"
      And uppr should be "${ t1 = text 1, t2 = text 2 => { return (t1 + ' - ' + t2).toUpperCase() } }"
      And uppr should be "${ t1 = text 1, s = spacer, t2 = text 2 => (t1 + s + t2).toUpperCase() }"
      And desc should be "${ text1, text2 => text1 + ' - ' + text2 }"
      And desc should be "${ text1, text2 => text1 + '${spacer}' + text2 }"
      And desc should be "${ text1, spacer, text2 => { return text1 + spacer +  text2 } }"
      And uppr should be "${ text1, text2 => { return (text1 + ' - ' + text2).toUpperCase() } }"
      And uppr should be "${ text1, spacer, text2 => (text1 + spacer + text2).toUpperCase() }"

  @StepDef
  @Assertion
  Scenario: Multi argument functions in DocStrings should work
     Then desc should be
          """
          ${ (t1 = text1, t2 = text2) => t1 + ' - ' + t2 }
          """
      And desc should be
          """
          ${ (t1 = text1, t2 = text2) => t1 + '${spacer}' + t2 }
          """
      And desc should be
          """
          ${ (t1 = text1, s = spacer, t2 = text2) => {
            return t1 + s +  t2 
          }}
          """
      And uppr should be
          """
          ${ (t1 = text1, t2 = text2) => { 
            return (t1 + ' - ' + t2).toUpperCase()
          }}
          """
      And uppr should be
          """
          ${ (t1 = text1, s = spacer, t2 = text2) => (t1 + s + t2).toUpperCase() }
          """
      And desc should be
          """
          ${ (t1 = text 1, t2 = text 2) => t1 + ' - ' + t2 }
          """
      And desc should be
          """
          ${ (t1 = text 1, t2 = text 2) => t1 + '${spacer}' + t2 }
          """
      And desc should be
          """
          ${ (t1 = text 1, s = spacer, t2 = text 2) => { 
            return t1 + s +  t2 
          }}
          """
      And uppr should be
          """
          ${ (t1 = text 1, t2 = text 2) => { 
            return (t1 + ' - ' + t2).toUpperCase() 
          }}
          """
      And uppr should be
          """
          ${ (t1 = text 1, s = spacer, t2 = text 2) => (t1 + s + t2).toUpperCase() }
          """
      And desc should be
          """
          ${ (text1, text2) => text1 + ' - ' + text2 }
          """
      And desc should be
          """
          ${ (text1, text2) => text1 + '${spacer}' + text2 }
          """
      And desc should be
          """
          ${ (text1, spacer, text2) => { 
            return text1 + spacer +  text2 
          }}
          """
      And uppr should be
          """
          ${ (text1, text2) => { 
              return (text1 + ' - ' + text2).toUpperCase()
          }}
          """
      And uppr should be
          """
          ${ (text1, spacer, text2) => (text1 + spacer + text2).toUpperCase() }
          """
      And desc should be
          """
          ${ t1 = text1, t2 = text2 => t1 + ' - ' + t2 }
          """
      And desc should be
          """
          ${ t1 = text1, t2 = text2 => t1 + '${spacer}' + t2 }
          """
      And desc should be
          """
          ${ t1 = text1, s = spacer, t2 = text2 => {
            return t1 + s +  t2 
          }}
          """
      And uppr should be
          """
          ${ t1 = text1, t2 = text2 => { 
            return (t1 + ' - ' + t2).toUpperCase()
          }}
          """
      And uppr should be
          """
          ${ t1 = text1, s = spacer, t2 = text2 => (t1 + s + t2).toUpperCase() }
          """
      And desc should be
          """
          ${ t1 = text 1, t2 = text 2 => t1 + ' - ' + t2 }
          """
      And desc should be
          """
          ${ t1 = text 1, t2 = text 2 => t1 + '${spacer}' + t2 }
          """
      And desc should be
          """
          ${ t1 = text 1, s = spacer, t2 = text 2 => { 
            return t1 + s +  t2 
          }}
          """
      And uppr should be
          """
          ${ t1 = text 1, t2 = text 2 => { 
            return (t1 + ' - ' + t2).toUpperCase() 
          }}
          """
      And uppr should be
          """
          ${ t1 = text 1, s = spacer, t2 = text 2 => (t1 + s + t2).toUpperCase() }
          """
      And desc should be
          """
          ${ text1, text2 => text1 + ' - ' + text2 }
          """
      And desc should be
          """
          ${ text1, text2 => text1 + '${spacer}' + text2 }
          """
      And desc should be
          """
          ${ text1, spacer, text2 => { 
            return text1 + spacer +  text2 
          }}
          """
      And uppr should be
          """
          ${ text1, text2 => { 
              return (text1 + ' - ' + text2).toUpperCase()
          }}
          """
      And uppr should be
          """
          ${ text1, spacer, text2 => (text1 + spacer + text2).toUpperCase() }
          """

  @StepDef
  @Assertion
  Scenario: Multi argument functions in documented examples should work
    Given name is "gwen"
      And surname is "stacey"
      And first name is "${name}"
      And last name is "${surname}"
      And full name is defined by js "(name, surname) => name + ' ' + surname"
      And full name 2 is defined by js
          """
          (name = first name, surname = last name) => name + ' ' + surname
          """
      And capitalise is defined by js
          """
          (name) => { 
              const head = name.charAt(0).toUpperCase()
              const tail = name.slice(1).toLowerCase()
              return head + tail
          }
          """
      And proper first name is defined by capitalise applied to "${first name}"
      And proper last name is defined by capitalise applied to "${last name}"
      And proper full name is defined by full name applied to "${proper first name},${proper last name}" delimited by ","
     Then full name should be "gwen stacey"
      And full name 2 should be "gwen stacey"
      And proper full name should be "Gwen Stacey"

  Scenario: Invoke the multi argument functions
    Given text1 is "Gwen"
      And text2 is "Automation"
      And text 1 is "Gwen"
      And text 2 is "Automation"
      And spacer is " - "
      And desc is "Gwen - Automation"
      And uppr is "GWEN - AUTOMATION"
     When I capture desc
     Then Multi argument functions should work
      And Multi argument functions in DocStrings should work
      And Multi argument functions in documented examples should work
      And text1 should not be "${gwen.eval.duration}"
      And text2 should not be "${gwen.eval.duration.msecs}"
      And text2 should not be "${gwen.eval.duration.secs}"
