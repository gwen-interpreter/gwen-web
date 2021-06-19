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

enum ElementEvent:
  case clicked, submitted, checked, ticked, unchecked, unticked, selected, deselected, typed, entered, tabbed, cleared, `right clicked`, `double clicked`, `moved to`

object ElementEvent {
  
  def actionOf(event: ElementEvent): ElementAction = {
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

enum ElementAction:
  case click, submit, check, tick, uncheck, untick, select, deselect, enter, tab, clear, `right click`, `double click`, `type`, `move to`

enum ElementState:
  case displayed, hidden, checked, ticked, unchecked, unticked, enabled, disabled

enum ScrollTo:
 case top, bottom

enum DropdownSelection:
  case text, value, index

enum PopupAction:
  case accept, dismiss
