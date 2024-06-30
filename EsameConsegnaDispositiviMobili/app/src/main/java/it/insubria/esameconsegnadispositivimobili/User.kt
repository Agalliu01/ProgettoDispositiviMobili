package it.insubria.esameconsegnadispositivimobili

import java.io.Serializable

data class User(
    val uid: String,
    var nome: String,
    var cognome: String,
    var username: String,
    val email: String,
    var password: String,
    val utentiSeguiti: MutableList<String>? = null, // Lista di utenti seguiti inizializzata a null
    var profileImageUrl: String = "" // Aggiunto campo per l'URL dell'immagine del profilo
) : Serializable {
    constructor() : this("", "", "", "", "", "", null, "")
}
