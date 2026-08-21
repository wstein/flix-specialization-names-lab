(ns dev.wstein.flixlab.allconstructs.modules
  "Section 1: Modules, Visibility & Imports.

  A Clojure namespace *is* the native equivalent of Flix's `mod` -- a nested,
  independently requirable unit whose members are reached by qualified
  access, same as `use HelperModule.helper` on the Flix side. No
  approximation needed.")

(defn helper []
  "HelperModule.helper")

(defn module-value []
  42)
