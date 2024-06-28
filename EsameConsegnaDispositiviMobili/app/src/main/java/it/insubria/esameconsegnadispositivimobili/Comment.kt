package it.insubria.esameconsegnadispositivimobili

data class Comment(
    val username: String,
    val text: String
) {
    constructor() : this("", "")

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "username" to username,
            "text" to text
        )
    }
}