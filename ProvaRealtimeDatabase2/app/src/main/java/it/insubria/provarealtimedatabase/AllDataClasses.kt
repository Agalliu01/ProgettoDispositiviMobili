package it.insubria.provarealtimedatabase

data class Comment(
    val username: String,
    val text: String
){
    constructor() : this("", "")






}

data class Post(
    val uid: String,
    val username: String,
    val description: String,
    val imageUrl: String,
    val link: String,
    var likedBy: MutableList<String> = mutableListOf(), // Lista degli utenti che hanno messo "mi piace"
    val comments: MutableList<Comment> = mutableListOf() // Lista dei commenti relativi al post
){
    constructor() : this("", "", "", "", "", mutableListOf(), mutableListOf())


    // Funzione per rimuovere un "mi piace" da parte di un utente



}





data class User(
    var Nome: String,
    var Cognome: String,
    var Username: String,
    val email: String,
    var password: String,
    val utentiSeguiti: MutableList<String>,
    val follower: MutableList<String>,
    val listaPost: MutableList<Post>
) {
    constructor() : this("", "", "", "", "", mutableListOf(), mutableListOf(), mutableListOf())
    // Metodo per impostare il nome dell'utente



    // Altri metodi utili possono essere aggiunti qui per manipolare o gestire gli utenti, post, ecc.
}
