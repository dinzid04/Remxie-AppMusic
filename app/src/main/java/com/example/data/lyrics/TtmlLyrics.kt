package com.example.data.lyrics

import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.roundToLong

object TtmlLyrics {
    fun parse(ttml: String): List<LyricLine> {
        val trimmed = ttml.trim()
        if (trimmed.isEmpty() || !trimmed.contains("<")) return emptyList()
        return try {
            val factory = DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = false
                isValidating = false
                try {
                    setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                } catch (_: Exception) {
                }
            }
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(ByteArrayInputStream(trimmed.toByteArray(Charsets.UTF_8)))
            val root = doc.documentElement ?: return emptyList()

            val agentTypes = HashMap<String, String>()
            val agentElements = root.getElementsByTagName("*")
            for (i in 0 until agentElements.length) {
                val el = agentElements.item(i) as? Element ?: continue
                val tag = el.tagName.substringAfterLast(':').lowercase()
                if (tag == "agent") {
                    val id = el.attrAny("xml:id", "id")
                    val type = el.attrAny("type", "ttm:type")
                    if (id.isNotEmpty() && type.isNotEmpty()) {
                        agentTypes[id] = type
                    }
                }
            }

            val pList = ArrayList<Element>()
            val allNodes = root.getElementsByTagName("*")
            for (i in 0 until allNodes.length) {
                val el = allNodes.item(i) as? Element ?: continue
                val tag = el.tagName.substringAfterLast(':').lowercase()
                if (tag == "p") {
                    pList.add(el)
                }
            }

            data class PendingLine(
                val timeMs: Long,
                val text: String,
                val words: List<LyricWord>,
                val sungUntilMs: Long?,
                val background: LyricLine?,
                val agent: String?,
            )

            val pending = ArrayList<PendingLine>(pList.size)
            val singers = ArrayList<String?>(pList.size)

            for (p in pList) {
                val pBegin = time(p.attrAny("begin"))
                val pEnd = time(p.attrAny("end"))
                val pDur = time(p.attrAny("dur"))
                val agent = p.attrAny("ttm:agent", "agent").ifEmpty { null }

                val mainRuns = ArrayList<TimedRun>()
                val bgRuns = ArrayList<TimedRun>()
                val plainMain = StringBuilder()
                val plainBg = StringBuilder()

                fun collectSpan(node: Node, inBackground: Boolean) {
                    when (node.nodeType) {
                        Node.TEXT_NODE -> {
                            val txt = node.nodeValue ?: ""
                            if (inBackground) plainBg.append(txt) else plainMain.append(txt)
                        }
                        Node.ELEMENT_NODE -> {
                            val el = node as Element
                            val role = el.attrAny("ttm:role", "role").lowercase()
                            if (role.contains("x-translation") || role.contains("x-roman")) {
                                return
                            }
                            val isBg = inBackground || role.contains("x-bg")
                            val sBegin = time(el.attrAny("begin"))
                            val sEnd = time(el.attrAny("end"))
                            val sDur = time(el.attrAny("dur"))
                            val childElements = (0 until el.childNodes.length)
                                .mapNotNull { el.childNodes.item(it) as? Element }
                                .filter {
                                    val r = it.attrAny("ttm:role", "role").lowercase()
                                    !r.contains("x-translation") && !r.contains("x-roman")
                                }

                            if (sBegin != null && childElements.isEmpty()) {
                                val rawText = buildString {
                                    for (cIdx in 0 until el.childNodes.length) {
                                        val c = el.childNodes.item(cIdx)
                                        if (c.nodeType == Node.TEXT_NODE) append(c.nodeValue ?: "")
                                    }
                                    val next = el.nextSibling
                                    if (next != null && next.nodeType == Node.TEXT_NODE) {
                                        val trailing = next.nodeValue ?: ""
                                        if (trailing.isNotEmpty() && trailing.all { it.isWhitespace() }) {
                                            if (!endsWith(' ')) append(' ')
                                        }
                                    }
                                }
                                val endMs = sEnd ?: (if (sDur != null) sBegin + sDur else (pEnd ?: (sBegin + 350L)))
                                if (rawText.isNotEmpty()) {
                                    val run = TimedRun(sBegin, endMs.coerceAtLeast(sBegin), rawText)
                                    if (isBg) bgRuns.add(run) else mainRuns.add(run)
                                }
                            } else {
                                for (cIdx in 0 until el.childNodes.length) {
                                    collectSpan(el.childNodes.item(cIdx), isBg)
                                }
                            }
                        }
                    }
                }

                for (cIdx in 0 until p.childNodes.length) {
                    collectSpan(p.childNodes.item(cIdx), false)
                }

                val words = wordsFromRuns(mainRuns, spacingIsExplicit = true)
                val lineText = if (words.isNotEmpty()) {
                    words.joinToString(" ") { it.text }
                } else {
                    plainMain.toString().replace(Regex("\\s+"), " ").trim()
                }
                if (lineText.isEmpty() || isGeniusSectionHeader(lineText)) continue

                val startMs = words.firstOrNull()?.startMs ?: pBegin ?: continue
                val endMs = pEnd
                    ?: (if (pBegin != null && pDur != null) pBegin + pDur else null)
                    ?: words.lastOrNull()?.endMs

                val bgWords = wordsFromRuns(bgRuns, spacingIsExplicit = true)
                val bgTextRaw = if (bgWords.isNotEmpty()) {
                    bgWords.joinToString(" ") { it.text }
                } else {
                    plainBg.toString().replace(Regex("\\s+"), " ").trim()
                }
                val bgTextClean = bgTextRaw.trim().removeSurrounding("(", ")").trim()
                val bgLine = if (bgTextClean.isNotEmpty()) {
                    val cleanedBgWords = bgWords.mapNotNull { w ->
                        w.withoutChars { it == '(' || it == ')' }
                    }
                    LyricLine(
                        timeMs = cleanedBgWords.firstOrNull()?.startMs ?: startMs,
                        text = bgTextClean,
                        words = cleanedBgWords,
                        sungUntilMs = cleanedBgWords.lastOrNull()?.endMs ?: endMs,
                    )
                } else null

                singers.add(agent)
                pending.add(
                    PendingLine(
                        timeMs = startMs,
                        text = lineText,
                        words = words,
                        sungUntilMs = endMs,
                        background = bgLine,
                        agent = agent,
                    )
                )
            }

            if (pending.isEmpty()) return emptyList()
            val alignments = lineAlignments(singers, agentTypes)
            pending.mapIndexed { index, item ->
                val align = alignments.getOrElse(index) { LyricAlignment.Start }
                LyricLine(
                    timeMs = item.timeMs,
                    text = item.text,
                    words = item.words,
                    sungUntilMs = item.sungUntilMs,
                    background = item.background?.copy(alignment = align),
                    alignment = align,
                )
            }
                .sortedBy { it.timeMs }
                .withBackgroundVocals()
                .withInstrumentalGaps()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun time(value: String?): Long? {
        val raw = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return try {
            when {
                raw.endsWith("ms", ignoreCase = true) ->
                    raw.dropLast(2).trim().toDoubleOrNull()?.roundToLong()
                raw.endsWith("s", ignoreCase = true) ->
                    raw.dropLast(1).trim().toDoubleOrNull()?.let { (it * 1000.0).roundToLong() }
                raw.endsWith("m", ignoreCase = true) ->
                    raw.dropLast(1).trim().toDoubleOrNull()?.let { (it * 60_000.0).roundToLong() }
                raw.endsWith("h", ignoreCase = true) ->
                    raw.dropLast(1).trim().toDoubleOrNull()?.let { (it * 3_600_000.0).roundToLong() }
                raw.contains(':') -> {
                    val parts = raw.split(':')
                    when (parts.size) {
                        2 -> {
                            val min = parts[0].toLongOrNull() ?: return null
                            val sec = parts[1].toDoubleOrNull() ?: return null
                            (min * 60_000L) + (sec * 1000.0).roundToLong()
                        }
                        3 -> {
                            val hr = parts[0].toLongOrNull() ?: return null
                            val min = parts[1].toLongOrNull() ?: return null
                            val sec = parts[2].toDoubleOrNull() ?: return null
                            (hr * 3_600_000L) + (min * 60_000L) + (sec * 1000.0).roundToLong()
                        }
                        else -> null
                    }
                }
                else -> raw.toDoubleOrNull()?.let { (it * 1000.0).roundToLong() }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun Element.attrAny(vararg names: String): String {
        for (name in names) {
            val v = getAttribute(name)
            if (!v.isNullOrBlank()) return v.trim()
        }
        return ""
    }
}
