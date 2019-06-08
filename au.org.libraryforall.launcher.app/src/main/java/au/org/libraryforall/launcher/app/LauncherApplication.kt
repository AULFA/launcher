package au.org.libraryforall.launcher.app

import android.app.Application

class LauncherApplication : Application() {

  override fun onCreate() {
    super.onCreate()
    LauncherMainServices.initialize(this)
  }

}