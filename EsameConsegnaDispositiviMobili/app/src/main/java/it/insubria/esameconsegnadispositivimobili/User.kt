package it.insubria.esameconsegnadispositivimobili

import java.io.Serializable

data class User(
    val uid:String,
    var nome: String,
    var cognome: String,
    var username: String,
    val email: String,
    var password: String,
    val utentiSeguiti: MutableList<String>,
    val listaPost: MutableList<Post>, // Lista di oggetti Post
    var profileImageUrl: String = "" // Aggiunto campo per l'URL dell'immagine del profilo
) :Serializable{
    constructor() : this("","", "", "", "", "", mutableListOf(), mutableListOf(), "")


}
