package it.insubria.esamedispositivimobili

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PersonalnfoAccountAdapter(
    private val context: Context,
    private var userDetails: UserDetails,
    private val onSaveClickListener: OnSaveClickListener
) : RecyclerView.Adapter<PersonalnfoAccountAdapter.ViewHolder>() {

    private val db = FirebaseFirestore.getInstance()
    private val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid

    interface OnSaveClickListener {
        fun onSaveChanges(userDetails: UserDetails)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.fragment_personalnfo_account_adapter, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(userDetails)
    }

    override fun getItemCount(): Int {
        // Considera il numero di campi da visualizzare/modificare (es. nome, cognome, email, username, password)
        return 1
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nomeEditText: EditText = itemView.findViewById(R.id.nomeEditText)
        private val cognomeEditText: EditText = itemView.findViewById(R.id.cognomeEditText)
        private val emailEditText: EditText = itemView.findViewById(R.id.emailEditText)
        private val usernameEditText: EditText = itemView.findViewById(R.id.usernameEditText)
        private val passwordEditText: EditText = itemView.findViewById(R.id.passwordEditText)
        private val saveChangesButton: Button = itemView.findViewById(R.id.validateChangesButton)

        init {
            saveChangesButton.setOnClickListener {
                val nome = nomeEditText.text.toString().trim()
                val cognome = cognomeEditText.text.toString().trim()
                val email = emailEditText.text.toString().trim()
                val username = usernameEditText.text.toString().trim()
                val password = passwordEditText.text.toString().trim()

                // Crea un oggetto UserDetails con le modifiche
                val updatedUserDetails = UserDetails(
                    nome = nome,
                    cognome = cognome,
                    email = email,
                    username = username,
                    password = password
                )

                // Chiamata al listener per salvare le modifiche
                onSaveClickListener.onSaveChanges(updatedUserDetails)
            }
        }

        fun bind(userDetails: UserDetails) {
            nomeEditText.setText(userDetails.nome)
            cognomeEditText.setText(userDetails.cognome)
            emailEditText.setText(userDetails.email)
            usernameEditText.setText(userDetails.username)
            passwordEditText.setText(userDetails.password)
        }
    }
}
