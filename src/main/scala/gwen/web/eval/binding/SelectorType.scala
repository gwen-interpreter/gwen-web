/*
 * Copyright 2021 Branko Juric, Brady Wood
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

 package gwen.web.eval.binding

import scala.util.Failure
import scala.util.Success
import scala.util.Try

object SelectorType extends Enumeration {
  
  type SelectorType = Value
  val id, name, xpath, javascript, cache = Value
  val `tag name` = Value("tag name")
  val `css selector` = Value("css selector")
  val `class name` = Value("class name")
  val `link text` = Value("link text")
  val `partial link text` = Value("partial link text")

  def parse(sType: String): SelectorType.Value = {
    Try(withName(sType)) match {
      case Success(value) => value
      case Failure(error) => sType match {
        case "tag" => `tag name`
        case "class" => `class name`
        case "css" => `css selector`
        case "js" => javascript
        case _ => throw error
      }
    }
  }

}

