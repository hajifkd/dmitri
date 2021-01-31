package tokyo.theta.dmitri

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.observe
import tokyo.theta.dmitri.data.model.db.FolderContent
import tokyo.theta.dmitri.databinding.FragmentBrowserBinding

/**
 * A simple [Fragment] subclass.
 */
class BrowserFragment : Fragment() {
    private lateinit var binding: FragmentBrowserBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        // return inflater.inflate(R.layout.fragment_browser, container, false)
        binding = FragmentBrowserBinding.inflate(inflater, container, false).apply {
            Toast.makeText(activity, "Hoge", Toast.LENGTH_SHORT).show()
        }

        val viewModel: MendeleyViewModel by requireActivity().viewModels()

        viewModel.folders.observe(viewLifecycleOwner) { folders: List<FolderContent> ->
            // add views
        }

        return binding.root
    }
}