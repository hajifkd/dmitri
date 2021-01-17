package tokyo.theta.dmitri

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import tokyo.theta.dmitri.databinding.FragmentBrowserBinding

/**
 * A simple [Fragment] subclass.
 */
class BrowserFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        // return inflater.inflate(R.layout.fragment_browser, container, false)
        return FragmentBrowserBinding.inflate(inflater, container, false).apply {
            Toast.makeText(activity, "Hoge", Toast.LENGTH_SHORT).show()
        }.root
    }
}