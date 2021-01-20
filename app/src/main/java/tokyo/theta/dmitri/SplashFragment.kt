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
                findNavController().navigate(SplashFragmentDirections.actionSplashFragmentToBrowserFragment())
            }

            button2.setOnClickListener {
                // e.g. https://api.mendeley.com/oauth/authorize?client_id=773&redirect_uri=http:%2F%2Flocalhost%2Fmendeley%2Fserver_sample.php&response_type=code&scope=all&state=213653957730.97845
                viewModel.authState =
                    (0..MendeleyViewModel.AUTH_STATE_LENGTH).map { ('a'..'z').random() }
                        .joinToString("")
                Uri.Builder().scheme("https").authority("api.mendeley.com").appendPath("oauth")
                    .appendPath("authorize")
                    .appendQueryParameter("client_id", getString(R.string.mendeley_client_id))
                    .appendQueryParameter(
                        "redirect_uri",
                        "${getString(R.string.app_name)}://${getString(R.string.mendeley_auth)}/"
                    )
                    .appendQueryParameter("response_type", "code")
                    .appendQueryParameter("scope", "all")
                    .appendQueryParameter("state", viewModel.authState).build().let {
                        startActivity(
                            Intent(Intent.ACTION_VIEW, it)
                        )
                    }

            }
        }.root
    }
}