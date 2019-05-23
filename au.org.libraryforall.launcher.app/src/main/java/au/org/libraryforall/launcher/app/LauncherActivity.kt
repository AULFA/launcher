package au.org.libraryforall.launcher.app

import android.animation.ObjectAnimator
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.view.View
import android.view.animation.BounceInterpolator
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import com.google.android.material.textfield.TextInputEditText

class LauncherActivity : AppCompatActivity() {

  private lateinit var lfaLayout: View
  private lateinit var lfaButton: View
  private lateinit var lfaOfflineLayout: View
  private lateinit var lfaOfflineButton: View
  private lateinit var settings: View

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    this.setContentView(R.layout.launcher_main)

    this.lfaLayout =
      this.findViewById(R.id.lfaLayout)
    this.lfaButton =
      this.findViewById<View>(R.id.lfaButton)
    this.lfaOfflineLayout =
      this.findViewById(R.id.lfaOfflineLayout)
    this.lfaOfflineButton =
      this.findViewById<View>(R.id.lfaOfflineButton)
    this.settings =
      this.findViewById<View>(R.id.settings)
  }

  override fun onBackPressed() {

  }

  override fun onStart() {
    super.onStart()

    this.lfaButton.setOnClickListener {
      this.bounceAndThen(this.lfaLayout) {
        this.launch("au.org.libraryforall.reader")
      }
    }
    this.lfaOfflineButton.setOnClickListener {
      this.bounceAndThen(this.lfaOfflineLayout) {
        this.launch("au.org.libraryforall.reader.offline")
      }
    }
    this.settings.setOnClickListener {
      this.bounceAndThen(this.settings) {
        this.showPasswordDialog()
      }
    }
  }

  private fun bounceAndThen(layout: View, andThen: () -> Unit) {
    val animY =
      ObjectAnimator.ofFloat(layout, "translationY", -16f, 0f)
    animY.duration = 250
    animY.interpolator = BounceInterpolator()
    animY.repeatCount = 0
    animY.start()
    animY.doOnEnd { andThen.invoke() }
  }

  private fun showPasswordDialog() {
    val view =
      this.layoutInflater.inflate(R.layout.launcher_password, null)
    val text =
      view.findViewById<TextInputEditText>(R.id.launcherPasswordText)

    AlertDialog.Builder(this)
      .setView(view)
      .setPositiveButton("Enter") { _, _ ->
        if (checkPassword(text.editableText.toString())) {
          this.startActivity(Intent(Settings.ACTION_SETTINGS))
        } else {
          AlertDialog.Builder(this)
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
    return text == "hello"
  }

  private fun launch(packageName: String) {
    val launchIntent =
      this.packageManager.getLaunchIntentForPackage(packageName)
    if (launchIntent != null) {
      this.startActivity(launchIntent)
    } else {
      AlertDialog.Builder(this)
        .setTitle("Error")
        .setIcon(android.R.drawable.ic_dialog_alert)
        .setMessage("The application ${packageName} is not installed.")
        .create()
        .show()
    }
  }
}
