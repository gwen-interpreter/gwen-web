/*
 * Copyright 2021-2024 Branko Juric, Brady Wood
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

import gwen.web.eval.WebContext
import gwen.web.eval.WebErrors

import gwen.core.Errors
import gwen.core.Formatting
import gwen.core.eval.binding.BindingType
import gwen.core.eval.binding.JSBinding
import gwen.core.eval.EvalContext
import gwen.core.eval.binding.DryValueBinding
import gwen.core.state.Environment
import gwen.core.state.SensitiveData

import scala.util.chaining._
import scala.util.Success
import scala.util.Try

object ElementFunctionBinding {

  private val bindingType = "elemFunction"
  
  def baseKey(name: String) = s"$name/$bindingType"
  def jsRefKey(name: String) = s"${baseKey(name)}/jsRef"
  def elemKey(name: String) = s"${baseKey(name)}/elem"
  def maskedKey(name: String) = s"${baseKey(name)}/masked"

  def bind(name: String, javascriptRef: String, element: String, masked: Boolean, env: Environment): Unit = {
    env.topScope.clear(name)
    env.topScope.set(jsRefKey(name), javascriptRef)
    env.topScope.set(elemKey(name), element)
    if (masked) {
      env.topScope.set(maskedKey(name), true.toString)
    }
  }

  def find[T <: EvalContext](name: String, ctx: T): Try[ElementFunctionBinding[T]] = Try {
    ctx.topScope.get(elemKey(name))
    new ElementFunctionBinding(name, ctx)
  }

}

class ElementFunctionBinding[T <: EvalContext](name: String, ctx: T) extends JSBinding[T](name, Nil, ctx) {

  val jsRefKey = ElementFunctionBinding.jsRefKey(name)
  val elemKey = ElementFunctionBinding.elemKey(name)
  override val maskedKey = ElementFunctionBinding.maskedKey(name)

  override def resolve(): String = {
    bindIfLazy(
      resolveValue(jsRefKey) { jsRef =>
        resolveValue(elemKey) { element =>
          val javascript = ctx.topScope.get(JSBinding.key(jsRef))
          val value = Try(ctx.parseArrowFunction(javascript)) match {
            case Success(Some(func)) if func.argNames.size == 1 =>
              val jsWrapper = func.argsBindingWrapper(List("arguments[0]"))
              val webCtx = ctx.asInstanceOf[WebContext]
              ctx.evaluate(resolveDryValue(BindingType.javascript.toString)) {
                webCtx.evaluateElementFunction(jsWrapper, webCtx.getLocatorBinding(element)).map(_.toString).getOrElse("")
              }
            case _ =>
              WebErrors.functionSignatureError(jsRef, "Single argument arrow function expected")
          }
          val masked = ctx.topScope.getOpt(maskedKey).map(_.toBoolean).getOrElse(false)
          if (masked) SensitiveData.mask(name, value) else value
        }
      }
    )
  }

  private def parseParams(jsRef: String, javascript: String, params: List[String]): List[String] = {
    params tap { _ =>
      if (!params.contains(DryValueBinding.unresolved(BindingType.javascript))) {
        0 to (params.size - 1) foreach { idx =>
          if (!javascript.contains(s"arguments[$idx]")) {
            Errors.missingJSArgumentError(jsRef, idx)
          }
        }
      }
    }
  }

  override def toString: String = Try {
    resolveValue(jsRefKey) { jsRef =>
      resolveValue(elemKey) { element =>
        s"$name [${ElementFunctionBinding.bindingType}: $jsRef, elem: $element]"
      }
    }
  } getOrElse name

}
