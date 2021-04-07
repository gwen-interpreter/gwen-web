/*
 * Copyright 2014-2019 Branko Juric, Brady Wood
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

/**
 * Defines methods for raising various kinds of errors (exceptions).
 */
package gwen.web

import gwen.Errors.GwenException

import org.openqa.selenium.Keys

object Errors {
  
    def locatorBindingError(msg: String) = throw new LocatorBindingException(msg)
    def unsupportedWebDriverError(driverName: String) = throw new UnsupportedWebDriverException(driverName)
    def noSuchWindowError(msg: String) = throw new NoSuchWindowException(msg)
    def unsupportedModifierKeyError(key: String) = throw new UnsupportedModifierKeyException(key)
    def waitTimeoutError(timeoutSecs: Long, reason: String, cause: Throwable) = throw new WaitTimeoutException(timeoutSecs, reason, cause)
    def waitTimeoutError(timeoutSecs: Long, reason: String) = throw new WaitTimeoutException(timeoutSecs, reason, null)
    def elementNotInteractableError(elementBinding: LocatorBinding, cause: Throwable) =
      throw new WebElementNotInteractableException(elementBinding, cause)
    def elementNotFoundError(elementBinding: LocatorBinding, cause: Throwable = null) =
      throw new WebElementNotFoundException(elementBinding, cause)
    def invalidVisualSessionStateError(msg: String) = throw new InvalidVisualSessionStateException(msg)
    def visualAssertionError(msg: String) = throw new VisualAssertionException(msg)

    /** Thrown when a locator binding error is detected . */
    class LocatorBindingException(msg: String) extends GwenException(msg)

    /** Thrown when an unsupported web driver is detected. */
    class UnsupportedWebDriverException(driverName: String)
      extends GwenException(s"Unsupported web driver: $driverName")
    
    /** Thrown when an attempt is made to switch to a window that does not exist. */
    class NoSuchWindowException(msg: String) extends GwenException(msg)

    /** Thrown when an attempt is made to send an unsupported key to a field. */
    class UnsupportedModifierKeyException(key: String)
      extends GwenException(s"Unsupported modifier key '$key'. Supported modifiers include: ${Keys.values().map(_.name()).mkString(",")}")

    /** Thrown when a timeout error occurs. */
    class WaitTimeoutException(timeoutSecs: Long, reason: String, cause: Throwable)
      extends GwenException(s"Timed out after $timeoutSecs second(s) $reason", cause)

    /** Thrown when a web element is not found or interactable. */
    class NotFoundOrInteractableException(msg: String, cause: Throwable)
      extends GwenException(msg, cause)

    /** Thrown when a web element cannot be interacted with. */
    class WebElementNotInteractableException(elementBinding: LocatorBinding, cause: Throwable)
      extends NotFoundOrInteractableException(s"Could not interact with element: $elementBinding", cause)

    /** Thrown when a web element cannot be located. */
    class WebElementNotFoundException(elementBinding: LocatorBinding, cause: Throwable)
      extends NotFoundOrInteractableException(s"Could not locate element: $elementBinding", cause)
    
    /** Thrown when a visual checking session is in an invalid state. */
    class InvalidVisualSessionStateException(msg: String) extends AssertionError(msg)

    /** Thrown when a visual assertion fails. */
    class VisualAssertionException(msg: String) extends AssertionError(msg)

}