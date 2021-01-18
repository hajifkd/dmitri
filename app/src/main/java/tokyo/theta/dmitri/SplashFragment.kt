package tokyo.theta.dmitri

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        // Inflate the layout for this fragment
        return FragmentSplashBinding.inflate(inflater, container, false).apply {
            button.setOnClickListener {
                findNavController().navigate(SplashFragmentDirections.actionSplashFragmentToBrowserFragment())
            }

            button2.setOnClickListener {
                findNavController().navigate(SplashFragmentDirections.actionSplashFragmentToLoginFragment())
            }
        }.root
    }
}