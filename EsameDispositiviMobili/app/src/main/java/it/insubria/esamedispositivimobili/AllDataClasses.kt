package it.insubria.esamedispositivimobili
data class Comment(
    val username: String,
    val text: String
) {
    constructor() : this("", "")

    fun getUsernameCommento(): String {
        return username
    }

    fun getTextCommento(): String {
        return text
    }

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "username" to username,
            "text" to text
        )
    }
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



    fun toMap(): Map<String, Any?> {
        return mapOf(
            "uid" to uid,
            "username" to username,
            "description" to description,
            "imageUrl" to imageUrl,
            "link" to link,
            "likedBy" to likedBy,
            "comments" to comments.map { it.toMap() } // Converti ogni commento in mappa
        )
    }

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


    fun toMap(): Map<String, Any?> {
        return mapOf(
            "Nome" to Nome,
            "Cognome" to Cognome,
            "Username" to Username,
            "email" to email,
            "password" to password,
            "utentiSeguiti" to utentiSeguiti,
            "follower" to follower,
            "listaPost" to listaPost.map { it.toMap() } // Converti ogni post in mappa
        )
    }
}