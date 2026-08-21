package dev.wstein.flixlab.allconstructs

/** Section 1: Modules, Visibility & Imports.
  *
  * Flix's `mod` gives a nested, independently importable namespace; Scala's `object` is the native
  * equivalent -- a singleton module whose members are reached by qualified access, same as `use
  * HelperModule.helper` on the Flix side.
  */
object HelperModule:
  def helper(): String = "HelperModule.helper"
  def moduleValue(): Int = 42
