package it.insubria.esameconsegnadispositivimobili

data class User(
    var Nome: String,
    var Cognome: String,
    var Username: String,
    val email: String,
    var password: String,
    val utentiSeguiti: MutableList<String>,
    val follower: MutableList<String>,
    val listaPost: MutableList<Post>, // Lista di oggetti Post
    var profileImageUrl: String = "" // Aggiunto campo per l'URL dell'immagine del profilo
) {
    constructor() : this("", "", "", "", "", mutableListOf(), mutableListOf(), mutableListOf(), "")

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "Nome" to Nome,
            "Cognome" to Cognome,
            "Username" to Username,
            "email" to email,
            "password" to password,
            "utentiSeguiti" to utentiSeguiti,
            "follower" to follower,
            "listaPost" to listaPost.map { it.toMap() },
            "profileImageUrl" to profileImageUrl // Aggiunto campo per l'URL dell'immagine del profilo
        )
    }
}
