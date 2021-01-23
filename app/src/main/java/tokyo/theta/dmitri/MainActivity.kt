package tokyo.theta.dmitri

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import tokyo.theta.dmitri.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel: MendeleyViewModel by viewModels()

        intent?.data?.let {
            if (it.scheme != getString(R.string.app_name)) {
                return
            }
            if (it.host == getString(R.string.mendeley_auth)) {
                // return from OAuth
                if (it.getQueryParameter("state")?.equals(viewModel.authState) == true) {
                    // initiated by this app
                    it.getQueryParameter("code")?.let {
                        viewModel.authCode = it
                        Toast.makeText(this, getString(R.string.auth_succeeded), Toast.LENGTH_SHORT)
                            .show()
                    }
                } else if (viewModel.authCode == null) {
                    // unauthorized but wrong code
                    Toast.makeText(this, getString(R.string.auth_failed), Toast.LENGTH_SHORT).show()
                } else {
                    // authorized and wrong code
                    Toast.makeText(this, getString(R.string.already_logged_in), Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

        viewModel.authState = null

        binding = DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)
            .apply {
                (supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment).apply {
                    NavigationUI.setupWithNavController(
                        toolbar,
                        navController,
                        AppBarConfiguration(
                            setOf(
                                R.id.browserFragment,
                                R.id.splashFragment
                            ), drawer
                        )
                    )
                    navController.addOnDestinationChangedListener { _, destination, _ ->
                        appbar.visibility = if (destination.id in arrayOf(
                                R.id.splashFragment
                            )
                        ) View.GONE else View.VISIBLE

                        drawer.setDrawerLockMode(
                            if (destination.id in arrayOf(
                                    R.id.splashFragment
                                )
                            ) DrawerLayout.LOCK_MODE_LOCKED_CLOSED else DrawerLayout.LOCK_MODE_UNLOCKED
                        )
                    }
                }
            }
    }

    override fun onBackPressed() {
        if (binding.drawer.isDrawerOpen(GravityCompat.START)) {
            binding.drawer.close()
            return
        }

        if (findNavController(R.id.navHostFragment).currentDestination?.id in arrayOf(
                R.id.browserFragment
            )
        ) {
            finish()
            return
        }
        super.onBackPressed()
    }
}