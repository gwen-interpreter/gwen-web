/*
 * Copyright 2019-2021 Brady Wood, Branko Juric
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

import gwen.core.Settings
import gwen.core.GwenSettings

import com.typesafe.config.ConfigFactory
import org.scalatest.prop.TableDrivenPropertyChecks.Table
import org.scalatest.flatspec.AnyFlatSpec

abstract class BaseTest extends AnyFlatSpec {

  Settings.exclusively {
    Settings.init()
  }

  val levels = Table ( ("level"), ("feature"), ("scenario") )

  def withSetting[T](name: String, value: String)(body: => T):T = {
    Settings.exclusively {
      val original = Settings.getOpt(name)
      try {
        Settings.set(name, value)
        body
      } finally {
        original.fold(Settings.clear(name)) { v =>
          Settings.set(name, v)
        }
      }
    }
  }
  
}