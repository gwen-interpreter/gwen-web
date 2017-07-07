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
import org.mockito.Mockito.verify
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.scalatest.mockito.MockitoSugar
import gwen.dsl.Step
import gwen.dsl.StepKeyword

class WebEngineTest extends FlatSpec with Matchers with MockitoSugar with WebEngine {
  
  val mockEnv: WebEnvContext = mock[WebEnvContext]
  val mockLocatorBinding: LocatorBinding = mock[LocatorBinding]
  val mockWebContext: WebContext = mock[WebContext]
  
  "Performing action on context sensitive web element" should "perform that action" in {
    val step = Step(StepKeyword.When, "I click the menu item of the menu")

    when(mockEnv.webContext).thenReturn(mockWebContext)

    evaluate(step, mockEnv)

    verify(mockWebContext).performActionInContext("click", "the menu item", "the menu")

  }
  
  "Performing action on web element bound to name not containing 'of' literal" should "peform that action (issue #20 fix)" in {
    val step = Step(StepKeyword.When, "I click the submit button")

    when(mockEnv.webContext).thenReturn(mockWebContext)
    when(mockEnv.getLocatorBinding("the submit button")).thenReturn(mockLocatorBinding)
    
    evaluate(step, mockEnv)
    
    verify(mockEnv).getLocatorBinding("the submit button")
    verify(mockWebContext).performAction("click", mockLocatorBinding)
        
  }
  
}