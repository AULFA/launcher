package au.org.libraryforall.launcher.installed

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import au.org.libraryforall.launcher.installed.api.InstalledPackage
import au.org.libraryforall.launcher.installed.api.InstalledPackageEvent
import au.org.libraryforall.launcher.installed.api.InstalledPackageEvent.InstalledPackagesChanged.InstalledPackageAdded
import au.org.libraryforall.launcher.installed.api.InstalledPackageEvent.InstalledPackagesChanged.InstalledPackageRemoved
import au.org.libraryforall.launcher.installed.api.InstalledPackageEvent.InstalledPackagesChanged.InstalledPackageUpdated
import au.org.libraryforall.launcher.installed.api.InstalledPackagesType
import com.google.common.util.concurrent.MoreExecutors
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors

class InstalledPackages private constructor(
  private val context: Context) : InstalledPackagesType {

  private val logger = LoggerFactory.getLogger(InstalledPackages::class.java)

  private val executor =
    MoreExecutors.listeningDecorator(
      Executors.newFixedThreadPool(1) { runnable ->
        val th = Thread(runnable)
        th.name = "au.org.libraryforall.launcher.installed.InstalledPackages.poll[${th.id}]"
        android.os.Process.setThreadPriority(19)
        th
      })

  init {
    this.logger.debug("initialized")
    this.executor.execute { this.pollingTask() }
  }

  companion object {
    fun create(context: Context): InstalledPackagesType =
      InstalledPackages(context)
  }

  private val eventSubject: PublishSubject<InstalledPackageEvent> = PublishSubject.create()
  private val packageManager = this.context.packageManager

  override fun packages(): Map<String, InstalledPackage> {
    val intent = Intent(Intent.ACTION_MAIN, null)
    intent.addCategory(Intent.CATEGORY_LAUNCHER)
    return this.packageManager.queryIntentActivities(intent, 0)
      .mapNotNull(this::resolveInfoToPackage)
      .map { pkg -> Pair(pkg.id, pkg) }
      .toMap()
  }

  private fun pollingTask() {
    var installedThen =
      try {
        this.packages()
      } catch (e: Exception) {
        this.logger.error("polling task initial: ", e)
        mapOf<String, InstalledPackage>()
      }

    while (true) {
      try {
        val installedNow =
          this.packages()

        val added =
          installedNow.filter { p -> !installedThen.containsKey(p.key) }
        val removed =
          installedThen.filter { p -> !installedNow.containsKey(p.key) }

        val updated =
          installedNow.filter { now ->
            val then = installedThen[now.key]
            if (then != null) {
              then.versionCode != now.value.versionCode
            } else {
              false
            }
          }

        this.logger.trace("{} packages added", added.size)
        for (p in added) {
          this.eventSubject.onNext(InstalledPackageAdded(p.value))
        }

        this.logger.trace("{} packages removed", removed.size)
        for (p in removed) {
          this.eventSubject.onNext(InstalledPackageRemoved(p.value))
        }

        this.logger.trace("{} packages updated", updated.size)
        for (p in updated) {
          this.eventSubject.onNext(InstalledPackageUpdated(p.value))
        }

        installedThen = installedNow
      } catch (e: Exception) {
        this.logger.error("polling task: ", e)
      } finally {
        try {
          Thread.sleep(5_000L)
        } catch (e: InterruptedException) {
          Thread.currentThread().interrupt()
        }
      }
    }
  }

  private fun resolveInfoToPackage(info: ResolveInfo): InstalledPackage? {
    return try {
      val packageInfo =
        this.packageManager.getPackageInfo(info.activityInfo.packageName, 0)

      val packageId =
        packageInfo.packageName
      val packageVersionCode =
        packageInfo.versionCode
      val packageVersionName =
        packageInfo.versionName ?: packageVersionCode.toString()
      val packageName =
        this.packageManager.getApplicationLabel(packageInfo.applicationInfo).toString()

      InstalledPackage(
        id = packageId,
        versionName = packageVersionName,
        versionCode = packageVersionCode,
        name = packageName)
    } catch (e: Exception) {
      null
    }
  }

  override val events: Observable<InstalledPackageEvent>
    get() = this.eventSubject

}