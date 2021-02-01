package tokyo.theta.dmitri.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import tokyo.theta.dmitri.data.model.db.Folder
import tokyo.theta.dmitri.data.model.db.FolderContent
import tokyo.theta.dmitri.databinding.ItemFolderBinding

class FolderAdapter(private val clickListener: (Folder) -> Unit) :
    ListAdapter<Folder, FolderViewHolder>(object : DiffUtil.ItemCallback<Folder>() {
        override fun areContentsTheSame(oldItem: Folder, newItem: Folder) =
            oldItem == newItem

        override fun areItemsTheSame(oldItem: Folder, newItem: Folder) =
            oldItem.id == newItem.id

    }) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = FolderViewHolder(
        ItemFolderBinding.inflate(LayoutInflater.from(parent.context), parent, false), clickListener
    )

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

}

class FolderViewHolder(
    private val binding: ItemFolderBinding,
    private val clickListener: (Folder) -> Unit
) :
    RecyclerView.ViewHolder(binding.root) {
    fun bind(folder: Folder) {
        binding.folder = folder
        binding.executePendingBindings()

        binding.folderButton.setOnClickListener {
            clickListener(folder)
        }
    }
}