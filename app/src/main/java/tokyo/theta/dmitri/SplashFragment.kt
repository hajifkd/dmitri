package tokyo.theta.dmitri

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import tokyo.theta.dmitri.data.LoginResult
import tokyo.theta.dmitri.databinding.FragmentSplashBinding

/**
 * A simple [Fragment] subclass.
 */
class SplashFragment : Fragment() {
    private lateinit var binding: FragmentSplashBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val viewModel: MendeleyViewModel by requireActivity().viewModels()
        // Inflate the layout for this fragment
        binding = FragmentSplashBinding.inflate(inflater, container, false).apply {
            button.setOnClickListener {
                viewModel.login()
                Log.e("aaaaa", "bbbbbb")
            }

            button2.setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, viewModel.buildOAuthUri()))
            }
        }

        viewModel.loginResult.observe(viewLifecycleOwner) { loginResult: LoginResult ->
            when(loginResult) {
                LoginResult.Failed -> {
                    binding.button2.visibility = View.VISIBLE
                }
                LoginResult.Offline -> {
                }
                LoginResult.OfflineWithoutData -> {
                    Toast.makeText(requireActivity(), "Offline...", Toast.LENGTH_SHORT).show()
                    requireActivity().finish()
                }
                LoginResult.Successful -> {
                    findNavController().navigate(SplashFragmentDirections.actionSplashFragmentToBrowserFragment())
                }
            }
        }

        return binding.root
    }
}