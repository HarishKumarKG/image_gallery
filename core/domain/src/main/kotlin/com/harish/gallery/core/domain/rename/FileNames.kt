package com.harish.gallery.core.domain.rename

/**
 * Rules for a new file name typed in the Rename box.
 * The user only types the name part ("Ooty trip"); the extension (".jpg") is kept for them.
 */
sealed interface NameCheck {
    /** Good to go: the full new name, e.g. "Ooty trip.jpg". */
    data class Ok(val fileName: String) : NameCheck

    /** Can't use it. [reason] is shown under the box; empty = just keep the button disabled. */
    data class Problem(val reason: String) : NameCheck
}

private val FORBIDDEN = Regex("[\\\\/:*?\"<>|\\u0000-\\u001F]")
private const val MAX_NAME_BYTES = 255

/** "IMG_1.jpg" -> "IMG_1", "README" -> "README" */
fun baseNameOf(fileName: String): String = fileName.substringBeforeLast('.', fileName).ifEmpty { fileName }

/** "IMG_1.jpg" -> "jpg", "README" -> "" */
fun extensionOf(fileName: String): String =
    if (baseNameOf(fileName) == fileName) "" else fileName.substringAfterLast('.')

fun checkNewFileName(typed: String, currentFileName: String, otherNamesInFolder: Collection<String>): NameCheck {
    val base = typed.trim()
    when {
        base.isEmpty() -> return NameCheck.Problem("")
        FORBIDDEN.containsMatchIn(base) -> return NameCheck.Problem("A name can't contain \\ / : * ? \" < > |")
        base.startsWith('.') -> return NameCheck.Problem("A name can't start with a dot")
        base.endsWith('.') -> return NameCheck.Problem("A name can't end with a dot")
    }
    val extension = extensionOf(currentFileName)
    val full = if (extension.isEmpty() || base.endsWith(".$extension", ignoreCase = true)) base else "$base.$extension"
    return when {
        full.toByteArray(Charsets.UTF_8).size > MAX_NAME_BYTES -> NameCheck.Problem("That name is too long")
        full == currentFileName -> NameCheck.Problem("")
        otherNamesInFolder.any { it.equals(full, ignoreCase = true) } ->
            NameCheck.Problem("Another file in this album already has this name")
        else -> NameCheck.Ok(full)
    }
}

/** Rules for a new album name ("New album" when moving / copying). */
fun checkNewAlbumName(typed: String, existingAlbumNames: Collection<String>): NameCheck {
    val name = typed.trim()
    return when {
        name.isEmpty() -> NameCheck.Problem("")
        FORBIDDEN.containsMatchIn(name) -> NameCheck.Problem("A name can't contain \\ / : * ? \" < > |")
        name.startsWith('.') -> NameCheck.Problem("A name can't start with a dot")
        name.toByteArray(Charsets.UTF_8).size > MAX_NAME_BYTES -> NameCheck.Problem("That name is too long")
        existingAlbumNames.any { it.equals(name, ignoreCase = true) } ->
            NameCheck.Problem("An album with this name already exists — pick it from the list")
        else -> NameCheck.Ok(name)
    }
}
