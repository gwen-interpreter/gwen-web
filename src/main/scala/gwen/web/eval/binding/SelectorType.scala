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

import gwen.web.eval.WebErrors

import scala.util.Failure
import scala.util.Success
import scala.util.Try

enum SelectorType:
  case id, name, xpath, javascript, cache, `tag name`, `css selector`, `class name`, `link text`, `partial link text`

object SelectorType {
  
  def parse(sType: String): SelectorType = {
    Try(valueOf(sType)) match {
      case Success(value) => value
      case Failure(error) => sType match {
        case "tag" => `tag name`
        case "class" => `class name`
        case "css" => `css selector`
        case "js" => javascript
        case _ => WebErrors.invalidSelectorTypeError(sType)
      }
    }
  }

}

