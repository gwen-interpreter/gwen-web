@Ignore
Feature: Drag Drop

  Scenario: Drag box to cell
	Given the box can be located by xpath "//*[@id='credit2']/a"
      And the cell can be located by xpath "//*[@id='bank']/li"
	 When I navigate to "http://demo.guru99.com/test/drag_drop.html"
      And I capture the box as the box text
      And I drag and drop the box to the cell
	 Then the cell should contain "${the box text}"
