package com.github.ratoshniuk.izumi.distage.sample.plugins

import com.github.pshirshov.izumi.distage.model.providers.ProviderMagnet
import com.github.pshirshov.izumi.distage.model.reflection.macros.TrivialMacroLogger
import com.github.pshirshov.izumi.distage.model.reflection.universe.StaticDIUniverse
import com.github.pshirshov.izumi.distage.reflection.{DependencyKeyProviderDefaultImpl, ReflectionProviderDefaultImpl, SymbolIntrospectorDefaultImpl}
import com.github.pshirshov.izumi.fundamentals.reflection.AnnotationTools

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

// FIXME: added intersection type support (val parents)
final case class TraitConstructor[T](provider: ProviderMagnet[T])

object TraitConstructor {
  def apply[A: TraitConstructor]: TraitConstructor[A] = implicitly

  implicit def derive[T]: TraitConstructor[T] = macro TraitConstructorMacro.mkTraitConstructor[T]
}

object TraitConstructorMacro {

  def mkTraitConstructor[T: c.WeakTypeTag](c: blackbox.Context): c.Expr[TraitConstructor[T]] = mkTraitConstructorImpl[T](c, generateUnsafeWeakSafeTypes = false)

  def mkTraitConstructorImpl[T: c.WeakTypeTag](c: blackbox.Context, generateUnsafeWeakSafeTypes: Boolean): c.Expr[TraitConstructor[T]] = {
    import c.universe._

    val macroUniverse = StaticDIUniverse(c)
    import macroUniverse.Association._
    import macroUniverse.Wiring._
    import macroUniverse._

    val symbolIntrospector = SymbolIntrospectorDefaultImpl.Static(macroUniverse)
    val keyProvider = DependencyKeyProviderDefaultImpl.Static(macroUniverse)(symbolIntrospector)
    val reflectionProvider = ReflectionProviderDefaultImpl.Static(macroUniverse)(keyProvider, symbolIntrospector)
    val logger = TrivialMacroLogger[this.type](c)

    val targetType = weakTypeOf[T]

    val SingletonWiring.AbstractSymbol(_, wireables, _) = reflectionProvider.symbolToWiring(SafeType(targetType))

    val (wireArgs, wireMethods) = wireables.map {
      case AbstractMethod(ctx, name, _, key) =>
        val tpe = key.tpe.tpe
        val methodName: TermName = TermName(name)
        val argName: TermName = c.freshName(methodName)

        val mods = AnnotationTools.mkModifiers(u)(ctx.methodSymbol.annotations)

        q"$mods val $argName: $tpe" -> q"override val $methodName: $tpe = $argName"
    }.unzip

    val parents = targetType match {
      case RefinedType(tpes, _) => tpes.map(t => q"$t")
      case t => List(q"$t")
    }

    val instantiate = if (wireMethods.isEmpty)
      q"final class x extends $targetType {}; new x();"
    else
      q"final class x extends ..$parents { ..$wireMethods }; new x();"

    val constructorDef = q"""
      ${if (wireArgs.nonEmpty)
          q"def constructor(..$wireArgs): $targetType = ($instantiate)"
        else
          q"def constructor: $targetType = ($instantiate): $targetType"
      }
      """

    val providerMagnet = symbolOf[ProviderMagnet.type].asClass.module

    val provided =
      if (generateUnsafeWeakSafeTypes)
        q"{ $providerMagnet.generateUnsafeWeakSafeTypes[$targetType](constructor _) }"
      else
        q"{ $providerMagnet.apply[$targetType](constructor _) }"

    val res = c.Expr[TraitConstructor[T]] {
      q"""
          {
          $constructorDef

          new ${weakTypeOf[TraitConstructor[T]]}($provided)
          }
       """
    }
    logger.log(s"Final syntax tree of trait $targetType:\n$res")

    res
  }
}
