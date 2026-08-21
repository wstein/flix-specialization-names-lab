package dev.wstein.flixlab.allconstructs

import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.selects.select

/**
 * Section 13: Concurrency, Channels, Select & Parallelism (par yield).
 *
 * Unlike Scala and Java, this section needs no approximation: `kotlinx.coroutines`' [Channel],
 * `select { }`, and `async`/`await` map almost 1:1 onto Flix's own channel, `select`, and
 * `par`/`yield` constructs -- the one port where this section is a native fit rather than a
 * best-effort stand-in.
 */
fun concurrencyDemo(): Int =
    runBlocking {
        val channel = Channel<Int>(1)
        launch { channel.send(42) }
        channel.receive()
    }

fun selectDemo(): String =
    runBlocking {
        val channel1 = Channel<String>(1)
        val channel2 = Channel<String>(1)
        launch { channel1.send("Hello Select") }
        select {
            channel1.onReceive { it }
            channel2.onReceive { it }
        }
    }

fun parYieldDemo(): Pair<Int, String> =
    runBlocking {
        val x = async { 1 + 2 }
        val y = async { "hello world" }
        x.await() to y.await()
    }
