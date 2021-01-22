package tokyo.theta.dmitri

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.navigation.fragment.findNavController
import tokyo.theta.dmitri.databinding.FragmentSplashBinding

/**
 * A simple [Fragment] subclass.
 */
class SplashFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val viewModel: MendeleyViewModel by requireActivity().viewModels()
        // Inflate the layout for this fragment
        return FragmentSplashBinding.inflate(inflater, container, false).apply {
            button.setOnClickListener {
                viewModel.getToken()
                findNavController().navigate(SplashFragmentDirections.actionSplashFragmentToBrowserFragment())
            }

            button2.setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, viewModel.buildOAuthUri()))
            }
        }.root
    }
}