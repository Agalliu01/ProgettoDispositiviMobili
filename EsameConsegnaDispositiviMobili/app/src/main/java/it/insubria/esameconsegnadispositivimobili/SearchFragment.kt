package it.insubria.esameconsegnadispositivimobili
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*

class SearchFragment : Fragment(), AccountAdapter.OnItemClickListener {
    private lateinit var searchEditText: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var userAdapter: AccountAdapter
    private val userList = mutableListOf<User>()
    private var filteredUserList = listOf<User>()

    private val database = FirebaseDatabase.getInstance()

    private lateinit var currentUser: FirebaseUser


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false)
        searchEditText = view.findViewById(R.id.searchEditText)
        recyclerView = view.findViewById(R.id.recyclerView)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentUser = FirebaseAuth.getInstance().currentUser!!

        // Initialize RecyclerView and Adapter
        userAdapter = AccountAdapter(requireContext(), filteredUserList, this)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = userAdapter

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterUsers(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        loadUsersFromFirebase()
    }

    private fun loadUsersFromFirebase() {
        val usersRef = FirebaseDatabase.getInstance().getReference()

        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                userList.clear()
                for (snapshot in dataSnapshot.children) {
                    try {
                        val userDetails = snapshot.getValue(User::class.java)
                        // Escludi l'utente corrente
                        if (userDetails != null && userDetails.uid != currentUser.uid) {
                            userList.add(userDetails)
                        }
                    } catch (e: Exception) {
                        Log.e("FirebaseDatabase", "Error converting snapshot", e)
                    }
                }
                filteredUserList = userList
                userAdapter.updateUsers(filteredUserList)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("FirebaseDatabase", "Error getting data", databaseError.toException())
            }
        })

    }

    private fun filterUsers(query: String) {
        filteredUserList = if (query.isEmpty()) {
            userList
        } else {
            userList.filter { it.Username.contains(query, ignoreCase = true) }
        }
        userAdapter.updateUsers(filteredUserList)
    }

    override fun onItemClick(user: User, followButton: Button) {
        val userId = currentUser.uid

        val usersRef=database.getReference("users")
        usersRef.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val followedUsers = dataSnapshot.child("utentiSeguiti").getValue() as? ArrayList<String>
                    ?: ArrayList()

                if (followedUsers.contains(user.uid)) {
                    followedUsers.remove(user.uid)
                    followButton.text = "Segui"
                } else {
                    followedUsers.add(user.uid)
                    followButton.text = "Non seguire più"
                }

                // Update followedUsers in database
                usersRef.child(userId).child("utentiSeguiti").setValue(followedUsers)
                    .addOnSuccessListener {
                        // Update successful
                    }
                    .addOnFailureListener { e ->
                        Log.e("FirebaseDatabase", "Error updating followedUsers", e)
                    }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("FirebaseDatabase", "Error fetching document", databaseError.toException())
            }
        })
    }
}
