package tokyo.theta.dmitri

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import tokyo.theta.dmitri.data.model.db.FolderContent
import tokyo.theta.dmitri.databinding.FragmentBrowserBinding
import tokyo.theta.dmitri.view.FolderAdapter

/**
 * A simple [Fragment] subclass.
 */
class BrowserFragment : Fragment() {
    private lateinit var binding: FragmentBrowserBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        // return inflater.inflate(R.layout.fragment_browser, container, false)
        binding = FragmentBrowserBinding.inflate(inflater, container, false)
        val viewModel: MendeleyViewModel by requireActivity().viewModels()


        binding.recyclerView.adapter = FolderAdapter(viewLifecycleOwner, viewModel.isSyncing) {
            findNavController().navigate(
                BrowserFragmentDirections.actionBrowserFragmentToFolderFragment(
                    it.id
                )
            )
        }.apply {
            viewModel.folders.observe(viewLifecycleOwner) {
                submitList(it)
            }
        }

        return binding.root
    }
}