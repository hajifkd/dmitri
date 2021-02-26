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
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observe
import androidx.navigation.fragment.navArgs
import kotlinx.coroutines.launch
import tokyo.theta.dmitri.data.model.db.Document
import tokyo.theta.dmitri.data.model.db.File
import tokyo.theta.dmitri.databinding.FragmentFolderBinding
import tokyo.theta.dmitri.view.DocumentAdapter
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class FolderFragment : Fragment() {
    private lateinit var binding: FragmentFolderBinding
    private val args: FolderFragmentArgs by navArgs()
    private val viewModel: MendeleyViewModel by activityViewModels()

    private suspend fun selectFile(files: List<File>): File? = suspendCoroutine {
        AlertDialog.Builder(requireContext()).setTitle(R.string.multiple_file_found)
            .setItems(files.map { it.name }.toTypedArray()) { _, i -> it.resume(files[i]) }
            .setOnCancelListener { _ -> it.resume(null) }
            .create().show()
    }

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

        val file = if (files.size == 1) files[0] else {
            selectFile(files) ?: return
        }

        Log.d("file", "$file")

        if (!file.isDownloaded) {
            Toast.makeText(requireActivity(), "Downloading ${file.name}", Toast.LENGTH_SHORT).show()
            viewModel.refreshTokenOrShowToast()?:return
            viewModel.downloadFile(file)
            Toast.makeText(requireActivity(), "Downloaded ${file.name}", Toast.LENGTH_SHORT).show()
        }

        val mimeTypeMap = MimeTypeMap.getSingleton()
        val mime = mimeTypeMap.getMimeTypeFromExtension(file.name.split('.').last())

        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(viewModel.localFileUri(file), mime)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
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
            lifecycleScope.launch {
                submitList(viewModel.folderContent(args.folderId).documents)
            }

        }

        return binding.root
    }
}