package it.insubria.esameconsegnadispositivimobili

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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

        database = FirebaseDatabase.getInstance().reference

        val buttonViewAllPosts: Button = view.findViewById(R.id.btn_tutti)
        val buttonViewFollowedPosts: Button = view.findViewById(R.id.btn_seguiti)
        val buttonViewPersonalPosts: Button = view.findViewById(R.id.btn_personali)

        buttonViewAllPosts.setOnClickListener { loadAllPosts() }
        buttonViewFollowedPosts.setOnClickListener { loadFollowedPosts() }
        buttonViewPersonalPosts.setOnClickListener { loadPersonalPosts() }

        return view
    }

    private fun loadAllPosts() {
        database.child("posts").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                postList.clear()
                for (postSnapshot in snapshot.children) {
                    val post = postSnapshot.getValue(Post::class.java)
                    if (post != null && post.uid != currentUserUid && !followedUserIds.contains(post.uid)) {
                        postList.add(post)
                    }
                }
                postAdapter = PostAdapter(postList, false)
                recyclerView.adapter = postAdapter
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("HomeFragment", "Error loading all posts", error.toException())
            }
        })
    }

    private fun loadFollowedPosts() {
        currentUserUid?.let { uid ->
            database.child("users").child(uid).child("utentiSeguiti")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        followedUserIds.clear()
                        for (userSnapshot in snapshot.children) {
                            val userId = userSnapshot.key
                            if (userId != null) {
                                followedUserIds.add(userId)
                            }
                        }
                        database.child("posts").addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                postList.clear()
                                for (postSnapshot in snapshot.children) {
                                    val post = postSnapshot.getValue(Post::class.java)
                                    if (post != null && followedUserIds.contains(post.uid)) {
                                        postList.add(post)
                                    }
                                }
                                postAdapter = PostAdapter(postList, false)
                                recyclerView.adapter = postAdapter
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.e("HomeFragment", "Error loading followed posts", error.toException())
                            }
                        })
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("HomeFragment", "Error loading followed users", error.toException())
                    }
                })
        }
    }

    private fun loadPersonalPosts() {
        currentUserUid?.let { uid ->
            database.child("users").child(uid).child("listaPost")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        postList.clear()
                        for (postSnapshot in snapshot.children) {
                            val post = postSnapshot.getValue(Post::class.java)
                            if (post != null) {
                                postList.add(post)
                            }
                        }
                        postAdapter = PostAdapter(postList, true) // pass true for isPersonalSection
                        recyclerView.adapter = postAdapter
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("HomeFragment", "Error loading personal posts", error.toException())
                    }
                })
        }
    }
}