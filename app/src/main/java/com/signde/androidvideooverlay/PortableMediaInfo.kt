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
    val colorSpace: String = ""
)

/** Parses live HDMI attribute on Amlogic/supported devices (e.g. 420,10bit). */
object HdmiColorSettingParser {
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
        val activeOutput = activeAudioOutput(audioFlinger)
        val videoTrack = singleSelectedTrack(logcat, "video")
        val audioTrack = singleSelectedTrack(logcat, "audio")
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
        val audioMime = audioEvent?.groupValues?.get(1)
            ?: Regex("""mimeType=([^, ]+)""").find(audioTrack)?.groupValues?.get(1).orEmpty()
        val streamHdr = detectHdrFromLogcat(logcat, decoder, videoTrack)
        return PortableMediaInfo(
            videoFormat = if (hasActiveVideoDecoder) friendlyVideoFormat(decoder.substringBefore(".decoder").substringAfterLast('.')) else "",
            videoResolution = if (hasActiveVideoDecoder) resolution else "",
            frameRate = if (hasActiveVideoDecoder && frameRate.toDoubleOrNull()?.let { it > 0 } == true) "${frameRate}fps" else "",
            audioMode = if (activeOutput.first.isEmpty()) "" else friendlyAudioFormat(audioMime.ifEmpty { activeOutput.first }),
            audioFormat = activeOutput.second,
            videoBitrate = if (hasActiveVideoDecoder) formatBitrate(matchingEvent?.groupValues?.get(3) ?: bitrate(matchingTrack), true) else "",
            audioBitrate = if (activeOutput.first.isNotEmpty()) formatBitrate(audioEvent?.groupValues?.get(2) ?: bitrate(audioTrack), false) else "",
            audioModeIsOutput = activeOutput.first.isNotEmpty() && audioMime.isEmpty(),
            videoHdr = streamHdr,
            hdrOutput = ""
        )
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
        // 1. Only inspect videoTrack if it's selected and supported
        if (videoTrack.isNotEmpty() && !videoTrack.contains("supported=NO")) {
            val lowerTrack = videoTrack.lowercase()
            when {
                "dolby-vision" in lowerTrack || "dvhe" in lowerTrack || "dvh1" in lowerTrack -> return "Dolby Vision"
                "hdr10plus" in lowerTrack || "hdr10+" in lowerTrack -> return "HDR10+"
                "color-transfer=6" in lowerTrack -> return "HDR10"
                "color-transfer=7" in lowerTrack -> return "HLG"
            }
        }

        // 2. Scope to current active decoder instance in logcat
        if (decoder.isNotEmpty()) {
            val lines = log.lines()
            val start = lines.indexOfLast { "allocate($decoder)" in it }
            val scanLines = if (start >= 0) lines.drop(start) else lines
            val prefix = Regex("""^\S+\s+\S+\s+(\d+)\s+(\d+)\s""")
            val thread = if (start >= 0) prefix.find(lines[start])?.groupValues?.get(2) else null

            var activeTransfer: String? = null
            for (line in scanLines) {
                if (thread != null && prefix.find(line)?.groupValues?.get(2) != thread) {
                    if (!line.contains("CCodecConfig") && !line.contains("CCodec")) continue
                }
                val transferMatch = Regex("""coded\.vui\.color\.transfer\s*=\s*(\d+)""").find(line)
                    ?: Regex("""(?:color[.-])?transfer\s*[=:]\s*(\d+)""").find(line)
                if (transferMatch != null) {
                    activeTransfer = transferMatch.groupValues[1]
                }
            }
            when (activeTransfer) {
                "6" -> return "HDR10"
                "7" -> return "HLG"
                "1", "2", "3" -> return ""
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
        "truehd" in value.lowercase() -> "Dolby TrueHD"
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
