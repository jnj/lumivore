package org.joshjoyce.lumivore.util

import org.jetlang.core.{DisposingExecutor, Callback}
import org.jetlang.channels.Channel

object Implicits {
  implicit def lambdaToJetlangCallback[A, Nothing](f: (A) => Any): Callback[A] = {
    new Callback[A] {
      override def onMessage(p1: A) = f(p1)
    }
  }

  implicit def enrichChannel[A](c: Channel[A]) = new RichMemoryChannel[A](c)
}

class RichMemoryChannel[A](val underlying: Channel[A]) {

  def subscribe(e: DisposingExecutor)(f: (A) => Any) = {
    underlying.subscribe(e, Implicits.lambdaToJetlangCallback(f))
  }
}
