package au.org.libraryforall.launcher.main

import android.app.Activity
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import au.org.libraryforall.launcher.installed.api.InstalledPackage
import com.google.common.cache.CacheBuilder

class LauncherAppListAdapter(
  private val context: Activity,
  private val applications: List<InstalledPackage>,
  private val onSelect: (InstalledPackage) -> Unit,
  private val onDelete: (InstalledPackage) -> Unit,
  private val showPopupMenu: Boolean,
  private val showPackageId: Boolean)
  : RecyclerView.Adapter<LauncherAppListAdapter.ViewHolder>() {

  private val packageManager = this.context.packageManager
  private val iconCache =
    CacheBuilder.newBuilder()
      .concurrencyLevel(2)
      .maximumSize(10)
      .build<String, Drawable>()

  private val fallback =
    ContextCompat.getDrawable(this.context, R.drawable.lfa_unknown)

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
    holder.parent.setOnLongClickListener {
      if (! this.showPopupMenu) {
        return@setOnLongClickListener false
      }

      val pop= PopupMenu(context, it)
      pop.inflate(R.menu.launcher_item_menu)

      pop.setOnMenuItemClickListener { menuItem ->
        when(menuItem.itemId) {
          R.id.launcher_item_menu_delete -> {
            this.onDelete.invoke(item)
          }
        }
        true
      }
      pop.show()
      true
    }
    holder.title.text = item.name
    holder.icon.setImageDrawable(this.iconCache.get(item.id) { loadIcon(item) })

    if (this.showPackageId) {
      holder.packageId.text =
        String.format("%s %s (%d)", item.id, item.versionName, item.versionCode)
      holder.packageId.visibility = View.VISIBLE
    } else {
      holder.packageId.visibility = View.INVISIBLE
    }
  }

  private fun loadIcon(item: InstalledPackage): Drawable? {
    return try {
      this.packageManager.getApplicationIcon(item.id)
    } catch (e: Exception) {
      this.fallback
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