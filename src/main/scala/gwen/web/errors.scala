/*
 * Copyright 2014-2015 Branko Juric, Brady Wood
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

    package object errors {

      def unsupportedWebDriverError(driverName: String) = throw new UnsupportedWebDriverException(driverName)
      def noSuchWindowError(msg: String) = throw new NoSuchWindowException(msg)
      def unsupportedModifierKeyError(key: String) = throw new UnsupportedModifierKeyException(key)

      /** Thrown when an unsupported web driver is detected. */
      class UnsupportedWebDriverException(driverName: String) extends RuntimeException(s"Unsupported web driver: $driverName")
      
      /** Thrown when an attempt is made to switch to a window that does not exist. */
      class NoSuchWindowException(msg: String) extends RuntimeException(msg)

      /** Thrown when an attempt is made to send an unsupported key to a field. */
      class UnsupportedModifierKeyException(key: String) extends RuntimeException(s"Unsupported modifier key '$key'. Supported modifiers include: ${Keys.values().map(_.name()).mkString(",")}")

    }
  }
}