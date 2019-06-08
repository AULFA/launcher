package au.org.libraryforall.launcher.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
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
    private fun bundleArguments(parameters: LauncherViewControllerParameters): Bundle {
      val bundle = Bundle()
      bundle.putSerializable("parameters", parameters)
      return bundle
    }
  }

  private var installedPackageEvents: Disposable? = null
  private val applicationList: MutableList<InstalledPackage> = mutableListOf()

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
  private lateinit var version: TextView

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup
  ): View {
    val view = inflater.inflate(R.layout.launcher_main, container, false)
    this.title = view.findViewById(R.id.launcherTitle)
    this.recyclerView = view.findViewById(R.id.launcherListView)
    this.version = view.findViewById(R.id.launcherVersion)
    this.settings = view.findViewById(R.id.launcherSettings)
    return view
  }

  override fun onAttach(view: View) {
    super.onAttach(view)

    this.version.text =
      String.format("LFA Launcher %s (%d)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)

    this.listAdapter =
      LauncherAppListAdapter(
        context = this.activity!!,
        applications = this.applicationList,
        onSelect = this::onSelectedPackage,
        showPackageId = this.parameters.unlocked)

    this.recyclerView.setHasFixedSize(true)
    this.recyclerView.layoutManager = LinearLayoutManager(view.context);
    this.recyclerView.adapter = this.listAdapter
    (this.recyclerView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

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

  private fun onPackageEvent() {
    UIThread.execute {
      val packageList = this.installed.packages()
        .values
        .filter { installedPackage -> this.isAllowed(installedPackage) }
        .sortedBy { installedPackage -> installedPackage.name }

      this.applicationList.clear()
      this.applicationList.addAll(packageList)
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
}
