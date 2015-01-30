/*
 * Copyright 2014 Brady Wood, Branko Juric
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

import java.io.File

import org.scalatest.FlatSpec

import gwen.dsl.Failed
import gwen.dsl.Passed
import gwen.eval.GwenOptions

trait WebEngineTestCase extends FlatSpec {
  
  def interpret(featureFilepath: String, metaFilepaths: List[String] = Nil) {
    evaluate(featureFilepath, true, metaFilepaths)
  }
  
  def interpretNoReport(featureFilepath: String, metaFilepaths: List[String] = Nil) {
    evaluate(featureFilepath, false, metaFilepaths)
  }
  
  private def evaluate(featureFilepath: String, withReport: Boolean, metaFilepaths: List[String]) {
    
    val options = GwenOptions(
      true,
      false,
      if (withReport) Some(new File("target/report")) else None, 
      Nil, 
      Nil,
      metaFilepaths map { filepath => new File(getClass().getResource(filepath).getFile()) },
      List(new File(featureFilepath)))

      
    val intepreter = new GwenWebInterpreter
    intepreter.execute(options, None) match {
      case Passed(_) => // woo hoo
      case Failed(_, error) => error.printStackTrace(); fail(error.getMessage())
      case _ => fail("evaluation expected but got noop")
    }
  }
  
}