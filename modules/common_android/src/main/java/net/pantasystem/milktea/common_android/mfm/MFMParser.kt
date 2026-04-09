package net.pantasystem.milktea.common_android.mfm

import dev.misskey.mfm.Mfm
import dev.misskey.mfm.node.MfmNode
import java.util.regex.Pattern

object MFMParser {

    private val idPattern = Pattern.compile("""^([a-zA-Z0-9]+)$""")

    fun parse(text: String?): List<MfmNode>? {
        text ?: return null
        return Mfm.parse(text)
    }

    fun convertAppNoteUriIfGiveNoteUrl(accountHost: String?, url: String): String? {
        accountHost ?: return null
        val startsPattern = "https://$accountHost/notes/"
        if (url.startsWith(startsPattern)) {
            val id = url.substring(startsPattern.length)
            val matcher = idPattern.matcher(id)
            if (matcher.find()) {
                return "milktea://$accountHost/notes/${matcher.group()}"
            }
        }
        return null
    }

    fun convertAppChannelUriIfGiveChannelUrl(accountHost: String?, url: String): String? {
        accountHost ?: return null
        val startsPattern = "https://$accountHost/channels/"
        if (url.startsWith(startsPattern)) {
            val id = url.substring(startsPattern.length)
            val matcher = idPattern.matcher(id)
            if (matcher.find()) {
                return "milktea://channels/${matcher.group()}"
            }
        }
        return null
    }
}
