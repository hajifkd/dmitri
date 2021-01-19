package tokyo.theta.dmitri

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import tokyo.theta.dmitri.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)
            .apply {
                (supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment).apply {
                    NavigationUI.setupWithNavController(
                        toolbar,
                        navController,
                        AppBarConfiguration(
                            setOf(
                                R.id.browserFragment,
                                R.id.loginFragment,
                                R.id.splashFragment
                            ), drawer
                        )
                    )
                    navController.addOnDestinationChangedListener { _controller, destination, _arguments ->
                        appbar.visibility = if (destination.id in arrayOf(
                                R.id.splashFragment,
                                R.id.loginFragment
                            )
                        ) View.GONE else View.VISIBLE
                    }
                }
            }

        intent?.data?.let {
            if (it.scheme != getString(R.string.app_name)) {
                return
            }
            if (it.host == getString(R.string.mendeley_auth)) {
                // return from OAuth
                val viewModel by viewModels<MendeleyViewModel>()
                if (it.getQueryParameter("state")?.equals(viewModel.authState) == true) {
                    // initiated by this app
                    it.getQueryParameter("code")?.let { viewModel.authCode.value = it }
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
                R.id.browserFragment,
                R.id.loginFragment
            )
        ) {
            finish()
            return
        }
        super.onBackPressed()
    }
}