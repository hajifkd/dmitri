package tokyo.theta.dmitri

import android.content.DialogInterface
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observe
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import kotlinx.coroutines.launch
import tokyo.theta.dmitri.databinding.ActivityMainBinding
import tokyo.theta.dmitri.databinding.NavigationHeaderBinding
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MendeleyViewModel by viewModels()

    private fun initNavHost() {
        (supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment).apply {
            NavigationUI.setupWithNavController(
                binding.toolbar,
                navController,
                AppBarConfiguration(
                    setOf(R.id.browserFragment, R.id.splashFragment),
                    binding.drawer
                )
            )
            navController.addOnDestinationChangedListener { _, destination, _ ->
                binding.appbar.visibility = if (destination.id in arrayOf(
                        R.id.splashFragment
                    )
                ) View.GONE else View.VISIBLE

                binding.drawer.setDrawerLockMode(
                    if (destination.id in arrayOf(
                            R.id.splashFragment
                        )
                    ) DrawerLayout.LOCK_MODE_LOCKED_CLOSED else DrawerLayout.LOCK_MODE_UNLOCKED
                )
            }
        }
    }

    private suspend fun askSyncDirtyFile(): Int = suspendCoroutine {
        val callback = DialogInterface.OnClickListener { _, r -> it.resume(r) }
        AlertDialog.Builder(this).setMessage(R.string.file_upload_dialog)
            .setPositiveButton(R.string.overwrite, callback)
            .setNeutralButton(R.string.upload_as, callback) // TODO not implemented
            .setNegativeButton(R.string.cancel, callback)
            .setOnCancelListener { _ -> it.resume(AlertDialog.BUTTON_NEGATIVE) }
            .create().show()
    }

    private suspend fun syncData() {
        val dirtyFiles = viewModel.dataRepository.dirtyFiles()

        Log.d("dirtyFiles", "${dirtyFiles}")

        Toast.makeText(
            this@MainActivity,
            "Synchronizing data...",
            Toast.LENGTH_SHORT
        ).show()

        if (dirtyFiles.isNotEmpty()) {
            val dirtySync = askSyncDirtyFile()

            if (dirtySync == AlertDialog.BUTTON_NEGATIVE) {
                Log.d("sync", "cancel")
                return
            }

            viewModel.refreshTokenOrShowToast() ?: return

            dirtyFiles.forEach {
                if (!when (dirtySync) {
                        AlertDialog.BUTTON_POSITIVE -> viewModel.overwriteFile(it)
                        AlertDialog.BUTTON_NEUTRAL -> viewModel.saveAsNewFile(it)
                        else -> return@forEach
                    }
                ) {
                    Toast.makeText(
                        this,
                        "Failed to save files. Try again later...",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@forEach
                }
            }
        } else {
            viewModel.refreshTokenOrShowToast() ?: return
        }

        viewModel.updateData()
    }

    private fun initToolbar() {
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_sync -> {
                    Log.d("appbar", "sync clicked")
                    findNavController(R.id.navHostFragment).navigate(R.id.browserFragment)
                    lifecycleScope.launch {
                        viewModel.isSyncing.postValue(true)
                        syncData()
                        viewModel.isSyncing.postValue(false)
                    }
                    true
                }
                else -> {
                    false
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var callbacked = false

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
                        callbacked = true
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
                val header = NavigationHeaderBinding.bind(navigation.getHeaderView(0))
                viewModel.profilePhoto.observe(this@MainActivity) {
                    header.profileImage.setImageBitmap(BitmapFactory.decodeFile(it.absolutePath))
                }
                viewModel.userName.observe(this@MainActivity) {
                    header.userName.text = it
                }
            }
        initToolbar()
        initNavHost()

        if (callbacked) {
            lifecycleScope.launch {
                viewModel.login()
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