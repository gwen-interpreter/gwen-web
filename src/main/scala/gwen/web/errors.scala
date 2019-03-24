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
package gwen {

  package web {
    
    import org.openqa.selenium.Keys
    import org.openqa.selenium.NoSuchElementException
    import gwen.errors.GwenException

    package object errors {

      def locatorBindingError(msg: String) = throw new LocatorBindingException(msg)
      def unsupportedWebDriverError(driverName: String) = throw new UnsupportedWebDriverException(driverName)
      def noSuchWindowError(msg: String) = throw new NoSuchWindowException(msg)
      def unsupportedModifierKeyError(key: String) = throw new UnsupportedModifierKeyException(key)
      def waitTimeoutError(timeoutSecs: Long, cause: Throwable) = throw new WaitTimeoutException(timeoutSecs, cause)
      def elementNotInteractableError(elementBinding: LocatorBinding, cause: Throwable) =
        throw new WebElementNotInteractableException(elementBinding, cause)
      def elementNotFoundError(element: String, cause: Throwable = null) =
        throw new WebElementNotFoundException(element, cause)
      def invalidVisualSessionStateError(msg: String) = throw new InvalidVisualSessionStateException(msg)

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
      class WaitTimeoutException(timeoutSecs: Long, cause: Throwable)
        extends GwenException(s"Operation timed out after $timeoutSecs second(s)", cause)

      /** Thrown when a web element cannot be interacted with. */
      class WebElementNotInteractableException(elementBinding: LocatorBinding, cause: Throwable)
        extends NoSuchElementException(s"Could not interact with element: ${elementBinding.element}", cause)

      /** Thrown when a web element cannot be located. */
      class WebElementNotFoundException(element: String, cause: Throwable)
        extends NoSuchElementException(s"Could not locate element: $element", cause)

      /** Thrown when a visual checking session is in an invalid state. */
      class InvalidVisualSessionStateException(msg: String) extends AssertionError(msg)

    }
  }
}