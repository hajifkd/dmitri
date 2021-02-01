package tokyo.theta.dmitri.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import tokyo.theta.dmitri.data.model.db.Document
import tokyo.theta.dmitri.databinding.ItemDocumentBinding

class DocumentAdapter(private val clickListener: (Document) -> Unit) :
    ListAdapter<Document, DocumentViewHolder>(object : DiffUtil.ItemCallback<Document>() {
        override fun areContentsTheSame(oldItem: Document, newItem: Document) =
            oldItem == newItem

        override fun areItemsTheSame(oldItem: Document, newItem: Document) =
            oldItem.id == newItem.id

    }) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = DocumentViewHolder(
        ItemDocumentBinding.inflate(LayoutInflater.from(parent.context), parent, false), clickListener
    )

    override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

}

class DocumentViewHolder(
    private val binding: ItemDocumentBinding,
    private val clickListener: (Document) -> Unit
) :
    RecyclerView.ViewHolder(binding.root) {
    fun bind(document: Document) {
        binding.document = document
        binding.executePendingBindings()

        binding.folderButton.setOnClickListener {
            clickListener(document)
        }
    }
}