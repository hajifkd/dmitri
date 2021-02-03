package tokyo.theta.dmitri

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observe
import androidx.navigation.fragment.navArgs
import kotlinx.coroutines.launch
import tokyo.theta.dmitri.data.model.db.Document
import tokyo.theta.dmitri.databinding.FragmentFolderBinding
import tokyo.theta.dmitri.view.DocumentAdapter


class FolderFragment : Fragment() {
    private lateinit var binding: FragmentFolderBinding
    private val args: FolderFragmentArgs by navArgs()
    private val viewModel: MendeleyViewModel by activityViewModels()

    private suspend fun clickDocument(document: Document) {
        val files = viewModel.getFiles(document)

        if (files.isEmpty()) {
            Toast.makeText(
                requireActivity(),
                "No file is associated with ${document.title}",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val file = files[0] // TODO UI?

        Log.d("file", "$file")

        if (!file.isDownloaded) {
            Toast.makeText(requireActivity(), "Downloading ${file.name}", Toast.LENGTH_SHORT).show()
            val result = viewModel.downloadFile(file)
            Toast.makeText(requireActivity(), "Downloaded ${file.name}", Toast.LENGTH_SHORT).show()
        }

        val mimeTypeMap = MimeTypeMap.getSingleton()
        val mime = mimeTypeMap.getMimeTypeFromExtension(file.name.split('.').last())

        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(viewModel.localFileUri(file), mime)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        Log.d("Open file intent", "$intent")
        startActivity(intent)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        // return inflater.inflate(R.layout.fragment_browser, container, false)
        binding = FragmentFolderBinding.inflate(inflater, container, false)

        binding.recyclerView.adapter = DocumentAdapter {
            lifecycleScope.launch {
                clickDocument(it)
            }
        }.apply {
            viewModel.folderContent(args.folderId).observe(viewLifecycleOwner) {
                submitList(it.documents)
            }

        }

        return binding.root
    }
}