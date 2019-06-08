package au.org.libraryforall.launcher.app

import android.os.Handler
import android.os.Looper

object UIThread {

  fun execute(runnable: () -> Unit) {
    Handler(Looper.getMainLooper()).post(runnable)
  }

}