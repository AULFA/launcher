package au.org.libraryforall.launcher.main

import android.content.Context
import au.org.libraryforall.launcher.installed.api.InstalledPackage

/**
 * A package filter that whitelists packages based on a string array declared in a resource.
 */

class LauncherPackageFilter(
  private val context: Context) : LauncherPackageFilterType {

  private val allowedPackages : Set<String> =
    this.context.resources.getStringArray(R.array.launcherPackageWhitelist).toSet()

  override fun isAllowed(installedPackage: InstalledPackage): Boolean {
    return this.allowedPackages.contains(installedPackage.id)
  }
}
