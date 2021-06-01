/*
 * Copyright 2014-2021 Branko Juric, Brady Wood
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
package gwen.web.eval

object ElementEvent extends Enumeration {
  
  type ElementEvent = Value
  
  val clicked, submitted, checked, ticked, unchecked, unticked, selected, deselected, typed, entered, tabbed, cleared = Value
  val `right clicked` = Value("right clicked")
  val `double clicked` = Value("double clicked")
  val `moved to` = Value("moved to")

  def actionOf(event: ElementEvent.Value): ElementAction.Value = {
    event match {
      case ElementEvent.clicked          => ElementAction.click
      case ElementEvent.`right clicked`  => ElementAction.`right click`
      case ElementEvent.`double clicked` => ElementAction.`double click`
      case ElementEvent.submitted        => ElementAction.submit
      case ElementEvent.checked          => ElementAction.check
      case ElementEvent.ticked           => ElementAction.tick
      case ElementEvent.unchecked        => ElementAction.uncheck
      case ElementEvent.unticked         => ElementAction.untick
      case ElementEvent.selected         => ElementAction.select
      case ElementEvent.deselected       => ElementAction.deselect
      case ElementEvent.typed            => ElementAction.`type`
      case ElementEvent.entered          => ElementAction.enter
      case ElementEvent.tabbed           => ElementAction.tab
      case ElementEvent.cleared          => ElementAction.clear
      case ElementEvent.`moved to`       => ElementAction.`move to`
    }
  }

}

object ElementAction extends Enumeration {
  
  type ElementAction = Value
  
  val click, submit, check, tick, uncheck, untick, select, deselect, enter, tab, clear = Value
  val `right click` = Value("right click")
  val `double click` = Value("double click")
  val `type` = Value("type")
  val `move to` = Value("move to")

}

object ElementState extends Enumeration {
  
  type ElementState = Value

  val displayed, hidden, checked, ticked, unchecked, unticked, enabled, disabled = Value

}

object ScrollTo extends Enumeration {

  type ScrollTo = Value

  val top, bottom = Value

}

object DropdownSelection extends Enumeration {

  type DropdownSelection = Value

  val text, value, index = Value

}

object PopupAction extends Enumeration {

  type PopupAction = Value

  val accept, dismiss = Value

}


