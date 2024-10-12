Feature: Single Argument Functions

  @StepDef
  @Assertion
  Scenario: Single argument functions should work with single word name binding
     Then name should be "${ (n = name) => n }"
      And desc should be "${ (n = name) => n + ' Automation' }"
      And desc should be "${ (n = name) => { return n + ' Automation' } }"
      And uppr should be "${ (n = name) => { return n.toUpperCase() } }"
      And name should be "${ (name) => name }"
      And desc should be "${ (name) => name + ' Automation' }"
      And uppr should be "${ (name) => name.toUpperCase() }"
      And desc should be "${ (name) => { return '${name} Automation' } }"
      And uppr should be "${ (name) => { return name.toUpperCase() } }"
      And name should be "${ n = name => n }"
      And desc should be "${ n = name => n + ' Automation' }"
      And desc should be "${ n = name => { return n + ' Automation' } }"
      And uppr should be "${ n = name => { return n.toUpperCase() } }"
      And uppr should be "${ n = name => n.toUpperCase() }"
      And name should be "${ name => name }"
      And desc should be "${ name => name + ' Automation' }"
      And uppr should be "${ name => name.toUpperCase() }"
      And desc should be "${ name => { return '${name} Automation' } }"
      And uppr should be "${ name => { return name.toUpperCase() } }"

  @StepDef
  @Assertion
  Scenario: Single argument functions in DocStrings should work with single word name binding
     Then name should be
          """
          ${ (n = name) => n }
          """
      And desc should be
          """
          ${ (n = name) => n + ' Automation' }
          """
      And desc should be
          """
          ${ (n = name) => { return n + ' Automation' } }
          """
      And uppr should be
          """
          ${ (n = name) => { return n.toUpperCase() } }
          """
      And uppr should be
          """
          ${ (n = name) => n.toUpperCase() }
          """
      And name should be
          """
          ${ (name) => name }
          """
      And desc should be
          """
          ${ (name) => name + ' Automation' }
          """
      And uppr should be
          """
          ${ (name) => name.toUpperCase() }
          """
      And desc should be
          """
          ${ (name) => { 
            return '${name} Automation' 
          }}
          """
      And uppr should be
          """
          ${ (name) => { 
            return name.toUpperCase() 
          }}
          """
      And name should be
          """
          ${ name => name }
          """
      And desc should be
          """
          ${ name => name + ' Automation' }
          """
      And uppr should be
          """
          ${ name => name.toUpperCase() }
          """
      And desc should be
          """
          ${ name => { 
            return '${name} Automation' 
          }}
          """
      And uppr should be
          """
          ${ name => { 
            return name.toUpperCase() 
          }}
          """
      And name should be
          """
          ${ name => name }
          """
      And desc should be
          """
          ${ name => name + ' Automation' }
          """
      And uppr should be
          """
          ${ name => name.toUpperCase() }
          """
      And desc should be
          """
          ${ name => { 
            return '${name} Automation' 
          }}
          """
      And uppr should be
          """
          ${ name => { 
            return name.toUpperCase() 
          }}
          """

  @StepDef
  @Assertion
  Scenario: Single argument functions should work with multi word name binding
     Then name should be "${ (n = the name) => n }"
      And desc should be "${ (n = the name) => n + ' Automation' }"
      And desc should be "${ (n = the name) => { return n + ' Automation' } }"
      And uppr should be "${ (n = the name) => { return n.toUpperCase() } }"
      And uppr should be "${ (n = the name) => n.toUpperCase() }"
      And name should be "${ n = the name => n }"
      And desc should be "${ n = the name => n + ' Automation' }"
      And desc should be "${ n = the name => { return n + ' Automation' } }"
      And uppr should be "${ n = the name => { return n.toUpperCase() } }"
      And uppr should be "${ n = the name => n.toUpperCase() }"

  @StepDef
  @Assertion
  Scenario: Single argument functions in DocStrings should work with multi word name binding
     Then name should be
          """
          ${ (n = the name) => n }
          """
      And desc should be
          """
          ${ (n = the name) => n + ' Automation' }
          """
      And desc should be
          """
          ${ (n = the name) => { return n + ' Automation' } }
          """
      And uppr should be
          """
          ${ (n = the name) => { return n.toUpperCase() } }
          """
      And uppr should be
          """
          ${ (n = the name) => n.toUpperCase() }
          """
      And name should be
          """
          ${ n = the name => n }
          """
      And desc should be
          """
          ${ n = the name => n + ' Automation' }
          """
      And desc should be
          """
          ${ n = the name => { return n + ' Automation' } }
          """
      And uppr should be
          """
          ${ n = the name => { return n.toUpperCase() } }
          """
      And uppr should be
          """
          ${ n = the name => n.toUpperCase() }
          """

  @StepDef
  @Assertion
  Scenario: Single argument functions in documented examples should work
    Given first name is "gwen"
      And last name is "stacey"
      And number is "1"
      And user.001.username is "gwen.stacey"
      And uppercased name is defined by js "(name) => name.toUpperCase()"
      And proper name is defined by js
          """
          (name = first name) => { 
              const head = name.charAt(0).toUpperCase()
              const tail = name.slice(1).toLowerCase()
              return head + tail
          }
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
      And username is    @DryRun(name=`number => ('00' + number).slice(-3)`,value='001')
          """
          ${user.${ number => ('00' + number).slice(-3) }.username}
          """
     Then uppercased name should be "GWEN"
      And proper name should be "Gwen"
      And proper first name should be "Gwen"
      And proper last name should be "Stacey"
      And username should be "gwen.stacey"

  Scenario: Invoke the single argument functions
    Given name is "Gwen"
      And the name is "${name}"
      And desc is "Gwen Automation"
      And uppr is "GWEN"
     When I capture desc
     Then Single argument functions should work with single word name binding
      And Single argument functions in DocStrings should work with single word name binding
      And Single argument functions should work with multi word name binding
      And Single argument functions in DocStrings should work with multi word name binding
      And Single argument functions in documented examples should work
      And name should not be "${gwen.feature.eval.duration}"
      And desc should not be "${gwen.feature.eval.duration.msecs}"
      And desc should not be "${gwen.feature.eval.duration.secs}"
