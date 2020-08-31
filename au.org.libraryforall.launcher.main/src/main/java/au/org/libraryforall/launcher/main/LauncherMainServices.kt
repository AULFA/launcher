package au.org.libraryforall.launcher.main

import android.annotation.SuppressLint
import android.content.Context
import au.org.libraryforall.launcher.installed.InstalledPackages
import au.org.libraryforall.launcher.installed.api.InstalledPackagesType
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

@SuppressLint("StaticFieldLeak")
object LauncherMainServices {

  private val logger = LoggerFactory.getLogger(LauncherMainServices::class.java)

  private class AtomicService<T>(private val init: () -> (T)) {
    private val initialized = AtomicBoolean(false)

    @Volatile
    private var service: T? = null

    fun get(): T {
      return if (this.initialized.compareAndSet(false, true)) {
        try {
          this.service = this.init.invoke()
          this.service!!
        } catch (e: Exception) {
          this.initialized.set(false)
          throw e
        }
      } else {
        this.service!!
      }
    }
  }

  private lateinit var context: Context

  private val installedPackagesReference: AtomicService<InstalledPackagesType> =
    AtomicService { InstalledPackages.create(context) }

  private val packageFilterReference: AtomicService<LauncherPackageFilterType> =
    AtomicService { LauncherPackageFilter(context) }

  fun packageFilter(): LauncherPackageFilterType =
    packageFilterReference.get()

  fun installedPackages(): InstalledPackagesType =
    installedPackagesReference.get()

  fun initialize(context: Context) {
    LauncherMainServices.context = context
  }
}
