package au.org.libraryforall.launcher.app

import android.app.Activity
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import au.org.libraryforall.launcher.installed.api.InstalledPackage
import com.google.common.cache.CacheBuilder
import java.util.concurrent.Callable

class LauncherAppListAdapter(
  private val context: Activity,
  private val applications: List<InstalledPackage>,
  private val onSelect: (InstalledPackage) -> Unit,
  private val showPackageId: Boolean)
  : RecyclerView.Adapter<LauncherAppListAdapter.ViewHolder>() {

  private val packageManager = this.context.packageManager
  private val iconCache =
    CacheBuilder.newBuilder()
      .concurrencyLevel(2)
      .maximumSize(10)
      .build<String, Drawable>()

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): ViewHolder {
    val inflater =
      LayoutInflater.from(parent.context)
    val item =
      inflater.inflate(R.layout.launcher_item, parent, false)
    return this.ViewHolder(item)
  }

  override fun getItemCount(): Int =
    this.applications.size

  override fun onBindViewHolder(
    holder: ViewHolder,
    position: Int
  ) {
    val item = this.applications[position]

    holder.parent.setOnClickListener { this.onSelect.invoke(item) }
    holder.title.text = item.name
    holder.icon.setImageDrawable(this.iconCache.get(item.id) { loadIcon(item) })

    if (this.showPackageId) {
      holder.packageId.text = item.id
      holder.packageId.visibility = View.VISIBLE
    } else {
      holder.packageId.visibility = View.INVISIBLE
    }
  }

  private fun loadIcon(item: InstalledPackage): Drawable? {
    return try {
      return this.packageManager.getApplicationIcon(item.id)
    } catch (e: Exception) {
      null
    }
  }

  inner class ViewHolder(val parent: View) : RecyclerView.ViewHolder(parent) {
    val icon =
      this.parent.findViewById<ImageView>(R.id.launcherAppIcon)
    val title =
      this.parent.findViewById<TextView>(R.id.launcherAppTitle)
    val packageId =
      this.parent.findViewById<TextView>(R.id.launcherAppPackageId)
  }
}