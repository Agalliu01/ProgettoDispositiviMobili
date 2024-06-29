package it.insubria.esameconsegnadispositivimobili

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class CommentAdapter(
    private val context: Context,
    private val commentList: List<Comment>
) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val itemView = LayoutInflater.from(context)
            .inflate(R.layout.fragment_comment_adapter, parent, false)
        return CommentViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val currentComment = commentList[position]
        holder.usernameTextView.text = currentComment.username
        holder.commentTextView.text = currentComment.text

        // Caricamento dell'immagine del profilo
        Glide.with(context)
            .load(currentComment.imageProfile)
            .into(holder.profileImageView)
    }

    override fun getItemCount(): Int {
        return commentList.size
    }

    inner class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val usernameTextView: TextView = itemView.findViewById(R.id.text_comment_username)
        val commentTextView: TextView = itemView.findViewById(R.id.text_comment_text)
        val profileImageView: ImageView = itemView.findViewById(R.id.image_profile)
    }
}
