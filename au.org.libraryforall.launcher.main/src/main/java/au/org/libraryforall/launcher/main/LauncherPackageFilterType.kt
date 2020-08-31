package au.org.libraryforall.launcher.main

import au.org.libraryforall.launcher.installed.api.InstalledPackage

/**
 * An interface to whitelist installed packages.
 */

interface LauncherPackageFilterType {

  /**
   * @return `true` if the given package is allowed in the launcher
   */

  fun isAllowed(installedPackage: InstalledPackage): Boolean

}
