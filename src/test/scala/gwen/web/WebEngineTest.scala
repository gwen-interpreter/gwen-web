/*
 * Copyright 2016 Brady Wood, Branko Juric
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gwen.web

import org.mockito.Mockito.when
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Matchers.any
import org.openqa.selenium.WebDriver
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.scalatest.mockito.MockitoSugar
import gwen.eval.ScopedDataStack
import gwen.eval.GwenOptions
import gwen.dsl.Step
import gwen.dsl.StepKeyword

class WebEngineTest extends FlatSpec with Matchers with MockitoSugar with WebEngine {
  
  val mockEnv = mock[WebEnvContext]
  val mockLocatorBinding = mock[LocatorBinding]
  
  "Performing action on context sensitive web element" should "peform that action" in {
    val step = Step(StepKeyword.When, "I click the menu item of the menu")
 
    when(mockEnv.getLocatorBinding("the menu")).thenReturn(mockLocatorBinding)
    when(mockEnv.getLocatorBinding("the menu item")).thenReturn(mockLocatorBinding)
    
    evaluate(step, mockEnv)
    
    verify(mockEnv).getLocatorBinding("the menu")
    verify(mockEnv).getLocatorBinding("the menu item")
        
  }
  
  "Performing action on web element bound to name containing 'of' literal" should "peform that action (issue #20 fix)" in {
    val step = Step(StepKeyword.When, "I click the end of page button")
 
    when(mockEnv.getLocatorBinding("page button")).thenThrow(new LocatorBindingException("page button", "not bound"))
    when(mockEnv.getLocatorBinding("the end of page button")).thenReturn(mockLocatorBinding)
    
    evaluate(step, mockEnv)
    
    verify(mockEnv).getLocatorBinding("the end of page button")
        
  }
  
  "Performing action on web element bound to name not containing 'of' literal" should "peform that action (issue #20 fix)" in {
    val step = Step(StepKeyword.When, "I click the submit button")
 
    when(mockEnv.getLocatorBinding("the submit button")).thenReturn(mockLocatorBinding)
    
    evaluate(step, mockEnv)
    
    verify(mockEnv).getLocatorBinding("the submit button")
        
  }
  
}