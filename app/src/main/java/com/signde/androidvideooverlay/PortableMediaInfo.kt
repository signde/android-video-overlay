package com.signde.androidvideooverlay

import java.util.Locale

data class PortableMediaInfo(
    val videoFormat: String = "",
    val videoResolution: String = "",
    val frameRate: String = "",
    val audioMode: String = "",
    val audioFormat: String = "",
    val videoBitrate: String = "",
    val audioBitrate: String = "",
    val audioModeIsOutput: Boolean = false,
    val hdrOutput: String = "",
    val videoHdr: String = "",
    val colorSpace: String = "",
    val audioChannels: String = "",
    val videoResolutionFromBuffer: Boolean = false
)

/** Parses live HDMI attribute on Amlogic/supported devices (e.g. 420,10bit). */
object HdmiColorSettingParser {
    /** Realtek's current format, not its EDID support list or proposed format. */
    fun parseRealtekCurrentFormat(dump: String): String {
        val current = dump.lineSequence().filter { it.trimStart().startsWith("[RTKDRM] crnt-fmt:") }.toList()
        if (current.size != 1) return ""
        val match = Regex(""",\s*(YUV420|YUV422|YUV444|RGB)/(8|10|12|16),""")
            .find(current.single()) ?: return ""
        val color = match.groupValues[1].replace("YUV", "YCbCr")
        return "$color ${match.groupValues[2]}bit"
    }

    fun parse(status: String): String {
        val trimmed = status.trim().lowercase()
        if (trimmed.isEmpty() || trimmed.contains("no such") || trimmed.contains("denied")) return ""
        val parts = trimmed.split(',')
        val format = when (parts[0].trim()) {
            "420" -> "YCbCr420"
            "422" -> "YCbCr422"
            "444" -> "YCbCr444"
            "rgb" -> "RGB"
            else -> return ""
        }
        val bit = if (parts.size > 1) parts[1].trim() else "8bit"
        return "$format $bit"
    }
}

/** Amlogic, MediaTek and live HDMI transmitter status, not EDID capabilities or codec support. */
object HdrOutputParser {
    fun parse(status: String): String = when (status.trim()) {
        "HDR10-GAMMA_ST2084", "HDR10" -> "HDR10"
        "HDR10Plus-VSIF", "HDR10Plus", "HDR10+" -> "HDR10+"
        "HDR10-GAMMA_HLG", "HLG" -> "HLG"
        "DolbyVision-Std", "DolbyVision", "DV" -> "Dolby Vision"
        "DolbyVision-Lowlatency", "DolbyVision-LL", "DV-LL" -> "Dolby Vision (LL)"
        "HDR10-others", "HDR" -> "HDR (other)"
        "SDR" -> "SDR"
        // Missing/denied/unknown status must never be mistaken for SDR.
        else -> ""
    }
}

object PortableMediaInfoParser {
    fun parse(resourceManager: String, audioFlinger: String, logcat: String): PortableMediaInfo {
        // ResourceManager's event history is not a list of currently running codecs.
        val clients = resourceManager.substringBefore("Process Pid override:").substringBefore("Events logs:")
        val videoClient = clients.split("Client:").firstOrNull { 
            "video-codec" in it && ("Name: c2." in it || "Name: OMX." in it) 
        }
        val decoder = Regex("""Name:\s+((?:c2|OMX)\.\S+)""").find(videoClient.orEmpty())?.groupValues?.get(1).orEmpty()
        val hasActiveVideoDecoder = decoder.isNotEmpty()
        val audioClient = clients.split("Client:").filter {
            "audio-codec" in it && ("Name: c2." in it || "Name: OMX." in it)
        }.singleOrNull()
        val audioDecoder = Regex("""Name:\s+((?:c2|OMX)\.\S+)""")
            .find(audioClient.orEmpty())?.groupValues?.get(1).orEmpty()
        val activeOutput = activeAudioOutput(audioFlinger)
        val videoTrack = singleSelectedTrack(logcat, "video")
        val audioTrack = singleSelectedTrack(logcat, "audio")
        val audioSource = latestAudioSource(logcat, audioTrack)
        // ESPN emits the actual adaptive selection here. [X] in a track list can
        // mark every member of an adaptive group, not just the playing rendition.
        val videoEvent = Regex("""onVideoInputFormatChanged Video Track: (\d+)x(\d+) bitrate:(-?\d+) frameRate:([\d.]+)""")
            .findAll(logcat).lastOrNull()
        val audioEvent = Regex("""onAudioInputFormatChanged Audio Track: (audio/\S+) bitrate:(-?\d+)""")
            .findAll(logcat).lastOrNull()
        val codecResolution = codecResolution(logcat, decoder)
        val eventResolution = videoEvent?.let { "${it.groupValues[1]} x ${it.groupValues[2]}" }.orEmpty()
        val trackResolution = Regex("""res=(\d+)x(\d+)""").find(videoTrack)?.let { "${it.groupValues[1]} x ${it.groupValues[2]}" }.orEmpty()
        val resolution = codecResolution.ifEmpty { eventResolution.ifEmpty { trackResolution } }
        // Do not attach a previous rendition's bitrate to a new decoder size.
        val matchingEvent = videoEvent.takeIf { eventResolution == resolution }
        val matchingTrack = videoTrack.takeIf { trackResolution == resolution }.orEmpty()
        val frameRate = matchingEvent?.groupValues?.get(4)
            ?: Regex("""(?:frame_rate|fps)=([\d.]+)""").find(matchingTrack)?.groupValues?.get(1).orEmpty()
        val audioMime = Regex("""(?:mimeType=|Audio Track: )(audio/[^,\s]+)""")
            .find(audioSource)?.groupValues?.get(1).orEmpty()
        val streamHdr = if (hasActiveVideoDecoder) detectHdrFromLogcat(logcat, decoder, matchingTrack) else ""
        return PortableMediaInfo(
            videoFormat = if (hasActiveVideoDecoder) friendlyVideoFormat(decoder) else "",
            videoResolution = if (hasActiveVideoDecoder) resolution else "",
            frameRate = if (hasActiveVideoDecoder && frameRate.toDoubleOrNull()?.let { it > 0 } == true) "${frameRate}fps" else "",
            audioMode = if (activeOutput.first.isEmpty()) "" else friendlyAudioFormat(audioMime.ifEmpty { activeOutput.first }),
            audioFormat = activeOutput.second,
            videoBitrate = if (hasActiveVideoDecoder) formatBitrate(matchingEvent?.groupValues?.get(3) ?: bitrate(matchingTrack), true) else "",
            audioBitrate = if (activeOutput.first.isNotEmpty()) formatBitrate(audioEvent?.groupValues?.get(2) ?: bitrate(audioTrack), false) else "",
            audioModeIsOutput = activeOutput.first.isNotEmpty() && audioMime.isEmpty(),
            videoHdr = streamHdr,
            hdrOutput = "",
            audioChannels = if (activeOutput.first.isNotEmpty()) sourceAudioChannels(audioSource)
                .ifEmpty { espnSelectedTrackChannels(logcat, audioTrack, audioSource) }
                .ifEmpty { codecInputChannels(logcat, audioDecoder, audioMime) } else ""
        )
    }

    private fun codecInputChannels(log: String, decoder: String, sourceMime: String): String {
        if (decoder.isEmpty()) return ""
        val lines = log.lines()
        val start = lines.indexOfLast { "allocate($decoder)" in it }
        if (start < 0) return ""
        val prefix = Regex("""^\S+\s+\S+\s+\d+\s+(\d+)\s""")
        val thread = prefix.find(lines[start])?.groupValues?.get(1) ?: return ""
        val tail = lines.drop(start + 1)
        // Initial configure data cannot describe later format changes in the
        // same decoder. Prefer Unavailable if there has been another change.
        if (tail.count { "onAudioInputFormatChanged Audio Track:" in it || "audioInputFormat [" in it } > 1) return ""
        val own = tail.filter { prefix.find(it)?.groupValues?.get(1) == thread }
            .takeWhile { "allocate(" !in it }
        val input = own.indexOfLast { "setup formats input:" in it }
        if (input < 0) return ""
        val block = own.drop(input + 1).takeWhile { !it.trimEnd().endsWith("}") && "setup formats output:" !in it }
            .joinToString("\n")
        val mime = Regex("""string mime = "(audio/[^\"]+)"""").find(block)?.groupValues?.get(1) ?: return ""
        if (mime == "audio/raw" || (sourceMime.isNotEmpty() && mime != sourceMime)) return ""
        val count = Regex("""\bint32_t channel-count = (\d+)\b""").find(block)?.groupValues?.get(1) ?: return ""
        return sourceAudioChannels("channels=$count")
    }

    private fun latestAudioSource(log: String, selectedTrack: String): String {
        // A later input-format change supersedes a track listing. Never substitute
        // the HDMI/HAL channel mask or decoded PCM output for source channels.
        val event = log.lineSequence().lastOrNull {
            "onAudioInputFormatChanged Audio Track:" in it || ("audioInputFormat [" in it && "mimeType=audio/" in it)
        }
        return if (event != null && log.lastIndexOf(event) >
            (if (selectedTrack.isEmpty()) -1 else log.lastIndexOf(selectedTrack))) event else selectedTrack
    }

    private fun sourceAudioChannels(source: String): String {
        if ("supported=NO" in source) return ""
        val count = Regex("""\b(?:channels|channelCount|channel-count)\s*[=:]\s*(\d+)\b""")
            .find(source)?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it in 1..32 } ?: return ""
        return if (count == 1) "1 channel" else "$count channels"
    }

    private fun espnSelectedTrackChannels(log: String, track: String, event: String): String {
        // ESPN's first input-format notification omits channels. Fill only that
        // missing field from an unambiguous matching selection in the same log.
        // A second format change requires a fresh selection; do not cache tracks.
        if (track.isEmpty() || "supported=true" !in track ||
            "onAudioInputFormatChanged Audio Track:" !in event ||
            Regex("""\b(?:channels|channelCount|channel-count)\s*[=:]""").containsMatchIn(event)) return ""
        val trackIndex = log.lastIndexOf(track)
        val eventIndex = log.lastIndexOf(event)
        if (eventIndex <= trackIndex) return ""
        val interval = log.substring(trackIndex, eventIndex + event.length)
        if (interval.lineSequence().count {
                "onAudioInputFormatChanged Audio Track:" in it || "audioInputFormat [" in it
            } != 1) return ""
        fun mime(line: String) = Regex("""(?:mimeType=|Audio Track: )(audio/[^,\s]+)""")
            .find(line)?.groupValues?.get(1).orEmpty()
        fun language(line: String) = Regex("""\blanguage[=:]([^,\s]+)""")
            .find(line)?.groupValues?.get(1).orEmpty()
        if (mime(event).isEmpty() || mime(event) != mime(track) ||
            language(event).isEmpty() || language(event) != language(track)) return ""
        // Same-codec alternatives may have different channel counts. MIME alone
        // cannot identify which rendition was selected in a later notification.
        val tracks = log.substring(0, eventIndex).lineSequence()
            .filter { "Track:" in it && "mimeType=audio/" in it }.toList()
        val group = tracks.drop(tracks.indexOfLast { "Track:0," in it }.coerceAtLeast(0))
        if (group.count { mime(it) == mime(event) && language(it) == language(event) } != 1) return ""
        return sourceAudioChannels(track)
    }

    private fun singleSelectedTrack(log: String, type: String): String {
        val tracks = log.lineSequence().filter { "Track:" in it && "mimeType=$type/" in it }.toList()
        val latestGroup = tracks.drop(tracks.indexOfLast { "Track:0," in it }.coerceAtLeast(0))
        return latestGroup.filter { "[X]" in it }.singleOrNull().orEmpty()
    }

    private fun bitrate(line: String): String = Regex("""\bbitrate=(-?\d+)""").find(line)?.groupValues?.get(1).orEmpty()

    private fun formatBitrate(value: String, video: Boolean): String {
        val bps = value.toLongOrNull()?.takeIf { it > 0 } ?: return ""
        return if (video) String.format(Locale.US, "%.2f Mbps", bps / 1_000_000.0)
        else String.format(Locale.US, "%.0f kbps", bps / 1000.0)
    }

    private fun codecResolution(log: String, decoder: String): String {
        if (decoder.isEmpty()) return ""
        val lines = log.lines()
        val start = lines.indexOfLast { "allocate($decoder)" in it }
        if (start < 0) return ""
        // Thread IDs prevent interleaved audio or another video codec from
        // overwriting this decoder's dimensions. Only threadtime logs are used.
        val prefix = Regex("""^\S+\s+\S+\s+(\d+)\s+(\d+)\s""")
        val thread = prefix.find(lines[start])?.groupValues?.get(2) ?: return ""
        var width = ""
        var height = ""
        var cropWidth = ""
        var cropHeight = ""
        for (line in lines.drop(start)) {
            if (prefix.find(line)?.groupValues?.get(2) != thread) continue
            Regex("""raw\.size\.width = (\d+)""").find(line)?.let { width = it.groupValues[1]; cropWidth = ""; cropHeight = "" }
            Regex("""raw\.size\.height = (\d+)""").find(line)?.let { height = it.groupValues[1]; cropWidth = ""; cropHeight = "" }
            Regex("""raw\.crop\.width = (\d+)""").find(line)?.let { cropWidth = it.groupValues[1] }
            Regex("""raw\.crop\.height = (\d+)""").find(line)?.let { cropHeight = it.groupValues[1] }
        }
        if (cropWidth.toIntOrNull()?.let { it > 0 } == true && cropHeight.toIntOrNull()?.let { it > 0 } == true) return "$cropWidth x $cropHeight"
        return if (width.toIntOrNull()?.let { it > 0 } == true && height.toIntOrNull()?.let { it > 0 } == true) "$width x $height" else ""
    }

    private fun activeAudioOutput(dump: String): Pair<String, String> {
        val activeThread = dump.split("\nOutput thread")
            .lastOrNull { Regex("""(?m)^\s*Standby:\s+no\s*$""").containsMatchIn(it) }
            ?: return "" to ""
        val format = Regex("""HAL format:\s+[^\n]*\(AUDIO_FORMAT_([^\)]+)\)""")
            .find(activeThread)?.groupValues?.get(1).orEmpty()
        val flags = Regex("""AudioStreamOut:\s+[^\n]*\(([^\)]+)\)""")
            .find(activeThread)?.groupValues?.get(1).orEmpty()
        val mode = when {
            format.startsWith("PCM_") -> "Decoded PCM"
            "DIRECT" in flags || "OFFLOAD" in flags -> "Direct / offload"
            else -> "Compressed"
        }
        return format to mode
    }

    private fun detectHdrFromLogcat(log: String, decoder: String, videoTrack: String): String {
        // Prefer current-decoder metadata. Do not scan unrelated codec threads
        // or old log history when the active allocation has rolled out of logcat.
        val lines = log.lines()
        val start = lines.indexOfLast { "allocate($decoder)" in it }
        val prefix = Regex("""^\S+\s+\S+\s+(\d+)\s+(\d+)\s""")
        val thread = if (start >= 0) prefix.find(lines[start])?.groupValues?.get(2) else null
        if (thread != null) {
            var transfer: String? = null
            for (line in lines.drop(start + 1)) {
                if (prefix.find(line)?.groupValues?.get(2) != thread) continue
                if ("allocate(" in line) break
                if ("CCodecConfig" !in line) continue
                Regex("""\bcoded\.vui\.color\.transfer\s*=\s*(\d+)\b""")
                    .find(line)?.let { transfer = it.groupValues[1] }
            }
            when (transfer) {
                "6" -> return "HDR10"
                "7" -> return "HLG"
                "1", "2", "3" -> return "SDR"
            }
        }
        // A selected track is a fallback only if not superseded by a newer
        // allocation or rendition change. Main 10 / bit depth alone is not HDR.
        val trackAt = if (videoTrack.isEmpty()) -1 else log.lastIndexOf(videoTrack)
        val allocationAt = log.lastIndexOf("allocate($decoder)")
        val formatAt = log.lastIndexOf("onVideoInputFormatChanged Video Track:")
        if (trackAt >= 0 && trackAt > allocationAt && trackAt > formatAt &&
            !videoTrack.contains("supported=NO")) {
            val lowerTrack = videoTrack.lowercase()
            when {
                "dolby-vision" in lowerTrack || "dvhe" in lowerTrack || "dvh1" in lowerTrack -> return "Dolby Vision"
                "hdr10plus" in lowerTrack || "hdr10+" in lowerTrack -> return "HDR10+"
                "color-transfer=6" in lowerTrack -> return "HDR10"
                "color-transfer=7" in lowerTrack -> return "HLG"
            }
        }

        return ""
    }

    private fun friendlyVideoFormat(value: String): String {
        val lower = value.lowercase()
        return when {
            "av01" in lower || "av1" in lower -> "AV1"
            "hevc" in lower || "h265" in lower -> "H.265 / HEVC"
            "avc" in lower || "h264" in lower -> "H.264 / AVC"
            "vp9" in lower -> "VP9"
            "vp8" in lower -> "VP8"
            "mpeg2" in lower || "mpeg-2" in lower -> "MPEG-2"
            "mpeg4" in lower || "mpeg-4" in lower -> "MPEG-4"
            "vc1" in lower || "vc-1" in lower -> "VC-1"
            "dvhe" in lower || "dvh1" in lower || "dolby" in lower -> "Dolby Vision"
            value.isNotEmpty() -> value.substringAfterLast('/')
            else -> ""
        }
    }

    private fun friendlyAudioFormat(value: String): String = when {
        "eac3-joc" in value.lowercase() || "e_ac3_joc" in value.lowercase() -> "Dolby Digital+ (Atmos)"
        "eac3" in value.lowercase() || "e_ac3" in value.lowercase() -> "Dolby Digital+"
        "ac3" in value.lowercase() -> "Dolby Digital"
        "ac4" in value.lowercase() -> "Dolby AC-4"
        "truehd" in value.lowercase() || "true-hd" in value.lowercase() -> "Dolby TrueHD"
        "dts.hd" in value.lowercase() || "dts_hd" in value.lowercase() || "dtshd" in value.lowercase() -> "DTS-HD"
        "dts" in value.lowercase() -> "DTS"
        "aac" in value.lowercase() || "mp4a-latm" in value.lowercase() -> "AAC"
        "opus" in value.lowercase() -> "Opus"
        "vorbis" in value.lowercase() -> "Vorbis"
        "pcm" in value.lowercase() || value == "audio/raw" -> "PCM (source unknown)"
        value.isNotEmpty() -> value.substringAfterLast('/')
        else -> ""
    }
}
