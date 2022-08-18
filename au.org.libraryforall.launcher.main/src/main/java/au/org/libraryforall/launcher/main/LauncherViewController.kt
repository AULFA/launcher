package au.org.libraryforall.launcher.main

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import au.org.libraryforall.launcher.installed.api.InstalledPackage
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.RouterTransaction
import com.google.android.material.textfield.TextInputEditText
import io.reactivex.disposables.Disposable

class LauncherViewController(arguments: Bundle) : Controller(arguments) {

  constructor(parameters: LauncherViewControllerParameters)
    : this(bundleArguments(parameters))

  companion object {
    private const val UNINSTALL_REQUEST_CODE = 1234

    private fun bundleArguments(parameters: LauncherViewControllerParameters): Bundle {
      val bundle = Bundle()
      bundle.putSerializable("parameters", parameters)
      return bundle
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    if (requestCode == UNINSTALL_REQUEST_CODE) {
      when (resultCode) {
        Activity.RESULT_OK -> Toast.makeText(activity, R.string.launcherUninstallSuccess, Toast.LENGTH_SHORT).show()
        Activity.RESULT_CANCELED -> Toast.makeText(activity, R.string.launcherUninstallCanceled, Toast.LENGTH_SHORT).show()
        else -> Toast.makeText(activity, R.string.launcherUninstallFail, Toast.LENGTH_SHORT).show()
      }
    }
  }

  override fun handleBack(): Boolean {
    if (this.parameters.unlocked) {
      return false
    }
    return true
  }

  private var installedPackageEvents: Disposable? = null
  private val applicationList: MutableList<InstalledPackage> = mutableListOf()
  private val visibleAppList: MutableList<InstalledPackage> = mutableListOf()

  private val parameters: LauncherViewControllerParameters =
    arguments.getSerializable("parameters") as LauncherViewControllerParameters
  private val installed =
    LauncherMainServices.installedPackages()
  private val packageFilter =
    LauncherMainServices.packageFilter()

  private lateinit var title: TextView
  private lateinit var listAdapter: LauncherAppListAdapter
  private lateinit var recyclerView: RecyclerView
  private lateinit var settings: ImageView
  private lateinit var updater: ImageView
  private lateinit var version: TextView

  private var showUpdaterIcon = false

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup
  ): View {
    val view = inflater.inflate(R.layout.launcher_main, container, false)
    this.title = view.findViewById(R.id.launcherTitle)
    this.recyclerView = view.findViewById(R.id.launcherListView)
    this.version = view.findViewById(R.id.launcherVersion)
    this.settings = view.findViewById(R.id.launcherSettings)
    this.updater = view.findViewById(R.id.launcherUpdater)

    if (this.updater.visibility == View.VISIBLE) {
      showUpdaterIcon = true
    }

    return view
  }

  override fun onAttach(view: View) {
    super.onAttach(view)

    this.version.text =
      String.format("LFA Launcher %s (%d)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)

    this.listAdapter =
      LauncherAppListAdapter(
        context = this.activity!!,
        applications = this.visibleAppList,
        onSelect = this::onSelectedPackage,
        onDelete = this::onDeletePackage,
        showPopupMenu = this.parameters.unlocked,
        showPackageId = this.parameters.unlocked)

    this.recyclerView.setHasFixedSize(true)
    this.recyclerView.layoutManager = GridLayoutManager(view.context, 2)
    this.recyclerView.adapter = this.listAdapter
    (this.recyclerView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

    this.updater.setOnClickListener { this.launchUpdater() }

    if (!this.parameters.unlocked) {
      this.title.text = this.resources!!.getString(R.string.launcherTitleLocked)
      this.settings.setOnClickListener { this.showPasswordDialog() }
    } else {
      this.title.text = this.resources!!.getString(R.string.launcherTitleUnlocked)
      this.settings.visibility = View.INVISIBLE
    }

    this.installedPackageEvents = this.installed.events.subscribe { this.onPackageEvent() }
    this.onPackageEvent()
  }

  private fun onSelectedPackage(installedPackage: InstalledPackage) {
    this.launch(installedPackage.id)
  }

  private fun onDeletePackage(installedPackage: InstalledPackage) {
    this.delete(installedPackage.id)
  }

  private fun onPackageEvent() {
    UIThread.execute {
      // Filter the installed packages using the whitelist for this app version.
      // Hide the LFA Updater app if this version of the app shows the "update" button on the bottom right.
      val packageList = this.installed.packages()
        .values
        .filter { installedPackage -> this.isAllowed(installedPackage) }
        .filter { installedPackage -> !showUpdaterIcon || !installedPackage.id.contains("au.org.libraryforall.updater") }
        .sortedBy { installedPackage -> installedPackage.name }

      this.applicationList.clear()
      this.applicationList.addAll(this.installed.packages().values)

      this.visibleAppList.clear()
      this.visibleAppList.addAll(packageList)
      this.listAdapter.notifyDataSetChanged()
    }
  }

  private fun isAllowed(installedPackage: InstalledPackage): Boolean {
    return if (this.parameters.unlocked) {
      true
    } else {
      this.packageFilter.isAllowed(installedPackage)
    }
  }

  override fun onDetach(view: View) {
    super.onDetach(view)

    this.installedPackageEvents?.dispose()
  }

  private fun launchUpdater() {
    // Launch the LFA Updater app if it's installed
    this.applicationList.firstOrNull { installedPackage -> installedPackage.id.contains("au.org.libraryforall.updater") }
      ?.let { launch(it.id) }
  }

  private fun showPasswordDialog() {
    val activity =
      this.activity!!
    val layoutInflater =
      activity.layoutInflater
    val view =
      layoutInflater.inflate(R.layout.launcher_password, null)
    val text =
      view.findViewById<TextInputEditText>(R.id.launcherPasswordText)

    AlertDialog.Builder(activity)
      .setView(view)
      .setPositiveButton("Enter") { _, _ ->
        if (checkPassword(text.editableText.toString())) {
          this.router.pushController(RouterTransaction.with(
            LauncherViewController(LauncherViewControllerParameters(unlocked = true))))
        } else {
          AlertDialog.Builder(activity)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle("Wrong!")
            .setMessage("Incorrect password")
            .create()
            .show()
        }
      }
      .create()
      .show()
  }

  private fun checkPassword(text: String): Boolean {
    return text == BuildConfig.SETTINGS_PASSWORD
  }

  private fun launch(packageName: String) {
    val activity =
      this.activity!!
    val launchIntent =
      activity.packageManager.getLaunchIntentForPackage(packageName)
    if (launchIntent != null) {
      this.startActivity(launchIntent)
    } else {
      AlertDialog.Builder(activity)
        .setTitle("Error")
        .setIcon(android.R.drawable.ic_dialog_alert)
        .setMessage("The application ${packageName} is not installed or cannot be launched directly.")
        .create()
        .show()
    }
  }

  private fun delete(packageName: String) {
    val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE)
    intent.data = Uri.parse("package:${packageName}")
    intent.putExtra(Intent.EXTRA_RETURN_RESULT, true)
    startActivityForResult(intent, UNINSTALL_REQUEST_CODE)
  }
}
