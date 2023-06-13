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

import gwen.web.eval.binding.LocatorBinding

import gwen.core.GwenSettings
import gwen.core.Errors.GwenException

import org.openqa.selenium.Keys

import java.io.File

/**
 * Defines methods for raising various kinds of errors (exceptions).
 */
object WebErrors {
  
    def locatorBindingError(msg: String) = throw new LocatorBindingException(msg)
    def unsupportedWebBrowserError(browser: String) = throw new UnsupportedWebBrowserException(browser)
    def unsupportedWebDriverError(driverName: String) = throw new UnsupportedWebDriverException(driverName)
    def noSuchWindowError(msg: String) = throw new NoSuchWindowException(msg)
    def unsupportedModifierKeyError(key: String) = throw new UnsupportedModifierKeyException(key)
    def elementNotInteractableError(binding: LocatorBinding, cause: Throwable) =
      throw new WebElementNotInteractableException(binding, cause)
    def elementNotFoundError(binding: LocatorBinding, cause: Throwable = null) =
      throw new WebElementNotFoundException(binding, cause)
    def invalidClickActionError(action: ElementAction) = throw new InvalidClickActionException(action)
    def invalidContextActionError(action: ElementAction) = throw new InvalidContextActionException(action)
    def invalidActionError(action: ElementAction) = throw new InvalidActionException(action)
    def invalidSelectorTypeError(selectorType: String) = throw new InvalidSelectorTypeException(selectorType)
    def multipleBrowserSettingsError(settingsFiles: List[File]) = throw new MultipleBrowserSettingsException(settingsFiles)

    /** Thrown when a locator binding error is detected . */
    class LocatorBindingException(msg: String) extends GwenException(msg)

     /** Thrown when an unsupported browser is detected. */
    class UnsupportedWebBrowserException(browser: String)
      extends GwenException(s"Unsupported web browser: $browser")

    /** Thrown when an unsupported web driver is detected. */
    class UnsupportedWebDriverException(driverName: String)
      extends GwenException(s"Unsupported web driver: $driverName")
    
    /** Thrown when an attempt is made to switch to a window that does not exist. */
    class NoSuchWindowException(msg: String) extends GwenException(msg)

    /** Thrown when an attempt is made to send an unsupported key to a field. */
    class UnsupportedModifierKeyException(key: String)
      extends GwenException(s"Unsupported modifier key '$key'. Supported modifiers include: ${Keys.values().map(_.name()).mkString(",")}")

    /** Thrown when a web element is not found or interactable. */
    class NotFoundOrInteractableException(msg: String, cause: Throwable)
      extends GwenException(msg, cause)

    /** Thrown when a web element cannot be interacted with. */
    class WebElementNotInteractableException(binding: LocatorBinding, cause: Throwable)
      extends NotFoundOrInteractableException(s"Could not interact with ${binding.displayName}", cause)

    /** Thrown when a web element cannot be located. */
    class WebElementNotFoundException(binding: LocatorBinding, cause: Throwable)
      extends NotFoundOrInteractableException(s"Could not locate${if (GwenSettings.`gwen.error.messages.inline.locators`) " element:" else ""} ${binding.displayName}", cause)
    
    /** Thrown when an invalid click action is detected. */
    class InvalidClickActionException(action: ElementAction) extends GwenException(s"Invalid click action: $action (only ${ElementAction.click}, ${ElementAction.`right click`}, or ${ElementAction.`double click`} supported)")

    /** Thrown when an invalid context action is detected. */
    class InvalidContextActionException(action: ElementAction) extends GwenException(s"Invalid context action: $action (only ${ElementAction.click}, ${ElementAction.`right click`}, ${ElementAction.`double click`}, ${ElementAction.`check`}, ${ElementAction.`tick`}, ${ElementAction.`uncheck`}, ${ElementAction.`untick`} or ${ElementAction.`move to`} supported)")

    /** Thrown when an invalid action is detected. */
    class InvalidActionException(action: ElementAction) extends GwenException(s"Invalid action: $action (only ${ElementAction.click}, ${ElementAction.`right click`}, ${ElementAction.`double click`}, ${ElementAction.`clear`}, ${ElementAction.`submit`}, ${ElementAction.`check`}, ${ElementAction.`tick`}, ${ElementAction.`uncheck`}, ${ElementAction.`untick`} or ${ElementAction.`move to`} supported)")

    /** Thrown when an invalid selector is detected. */
    class InvalidSelectorTypeException(selectorType: String) extends GwenException(s"Invalid selector type: $selectorType")

    /** Thrown when more than one browser setting file is active.*/
    class MultipleBrowserSettingsException(settingsFiles: List[File]) extends GwenException(s"Multiple browser settings provided (only 1 expected): ${settingsFiles.mkString(", ")}")

}