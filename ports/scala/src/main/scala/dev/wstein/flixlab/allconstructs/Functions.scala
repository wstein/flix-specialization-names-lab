package dev.wstein.flixlab.allconstructs

import scala.annotation.tailrec
import scala.util.chaining.*

/** Section 9: Functions, Currying, Pipelines, Lambdas & Tail Recursion. All native --
  * `scala.util.chaining`'s `.pipe` is the direct counterpart of Flix's `|>`, and `.tupled` covers
  * the binary pipeline operator `||>`.
  */
def add(x: Int, y: Int): Int = x + y

def curriedMultiply(x: Int): Int => Int = y => x * y

def genericIdentity[A](x: A): A = x

def higherOrderDemo(f: Int => Int, x: Int): Int = f(x)

def pipelineDemo(x: Int): Int =
  x.pipe(n => n + 5).pipe(n => n * 2)

def binaryPipelineDemo(a: Int, b: Int): Int =
  (a, b).pipe(add.tupled)

@tailrec
def factorial(n: Long, acc: Long): Long =
  if n <= 1L then acc
  else factorial(n - 1L, n * acc)
