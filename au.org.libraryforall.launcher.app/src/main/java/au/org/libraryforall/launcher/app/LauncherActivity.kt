package au.org.libraryforall.launcher.app

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.bluelinelabs.conductor.Conductor
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction

class LauncherActivity : AppCompatActivity() {

  private lateinit var router: Router

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    this.setContentView(R.layout.main_activity)

    val container =
      this.findViewById<View>(R.id.main_container) as ViewGroup

    this.router = Conductor.attachRouter(this, container, savedInstanceState)
    if (!this.router.hasRootController()) {
      this.router.setRoot(RouterTransaction.with(LauncherViewController(
        LauncherViewControllerParameters(unlocked = false))))
    }
  }

  override fun onBackPressed() {
    if (!this.router.handleBack()) {
      super.onBackPressed()
    }
  }
}
