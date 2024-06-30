package it.insubria.esameconsegnadispositivimobili

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.OrientationEventListener
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HomeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var postAdapter: PostAdapter
    private val postList = mutableListOf<Post>()
    private val followedUserIds = mutableListOf<String>()
    private val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid

    private lateinit var database: DatabaseReference

    override fun onCreateView(

        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {



        val view = inflater.inflate(R.layout.fragment_home, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewHome)
        recyclerView.layoutManager = LinearLayoutManager(context)

        // Initialize RecyclerView Adapter
        postAdapter = PostAdapter(postList, false) // false because not in "My Posts" section
        recyclerView.adapter = postAdapter

        database = FirebaseDatabase.getInstance().reference

        // Load all posts immediately when fragment is created
        loadAllPosts()

        return view
    }

    private fun loadAllPosts() {
        database.child("posts").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                postList.clear()
                for (postSnapshot in snapshot.children) {
                    try {
                        val post = postSnapshot.getValue(Post::class.java)
                        post?.let {
                            postList.add(it)
                        }
                    } catch (e: Exception) {
                        Log.e("HomeFragment", "Error deserializing post: ${e.message}", e)
                    }
                }
                updatePostAdapter(postList, true) // true because loading all posts
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("HomeFragment", "Error loading all posts", error.toException())
            }
        })
    }

    private fun updatePostAdapter(posts: List<Post>, isPersonalSection: Boolean) {
        // Create a new adapter with the updated list and the isPersonalSection flag
        postAdapter = PostAdapter(posts.toMutableList(), isPersonalSection)
        recyclerView.adapter = postAdapter
        postAdapter.notifyDataSetChanged()
    }
}
