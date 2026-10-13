package com.signde.androidvideooverlay

/** Conservative fallback for the Realtek SurfaceFlinger format verified on wayne. */
object VideoBufferFallback {
    fun apply(
        info: PortableMediaInfo,
        manufacturer: String,
        resources: String,
        playerPackage: String?,
        readSurface: () -> String
    ): PortableMediaInfo {
        // Do not change existing detection or add compositor polling on other SoCs.
        if (info.videoResolution.isNotEmpty() || info.videoFormat.isEmpty() ||
            !manufacturer.equals("Realtek", ignoreCase = true) || playerPackage == null ||
            !Regex("""[a-zA-Z][\w]*(?:\.[\w]+)+""").matches(playerPackage)) return info
        val clients = resources.substringBefore("Process Pid override:").substringBefore("Events logs:")
            .split("Client:").filter { "video-codec" in it }
        if (clients.size != 1) return info
        // A missing/denied/changed dump must not erase otherwise valid AV details.
        val resolution = runCatching { parse(readSurface(), playerPackage) }.getOrDefault("")
        return if (resolution.isEmpty()) info else info.copy(
            videoResolution = resolution, videoResolutionFromBuffer = true,
            // Do not associate old track rates with a newly observed buffer size.
            frameRate = "", videoBitrate = ""
        )
    }

    fun parse(dump: String, playerPackage: String): String {
        val candidates = Regex("""(?m)^\+ Layer \(""").split(dump).drop(1).mapNotNull { block ->
            val header = block.lineSequence().firstOrNull().orEmpty()
            if (!header.startsWith("SurfaceView[$playerPackage/")) return@mapNotNull null
            // Only the actual live YUV buffer, never the SurfaceView parent,
            // background, RGB app window, display frame, or buffer-cache entries.
            if (!Regex("""defaultPixelFormat=Unknown 0x0*77(?:,|\s)""").containsMatchIn(block)) return@mapNotNull null
            val visible = Regex("""Region VisibleRegion[^\n]*count=[1-9]\d*\)[ \t]*\n\s*\[\s*(-?\d+),\s*(-?\d+),\s*(-?\d+),\s*(-?\d+)\]""")
                .find(block) ?: return@mapNotNull null
            val rect = visible.groupValues.drop(1).map { it.toIntOrNull() ?: return@mapNotNull null }
            if (rect[2] <= rect[0] || rect[3] <= rect[1]) return@mapNotNull null
            val buffers = Regex("""\bactiveBuffer=\[\s*(\d+)x\s*(\d+):""").findAll(block).toList()
            val buffer = buffers.singleOrNull() ?: return@mapNotNull null
            val width = buffer.groupValues[1].toIntOrNull() ?: return@mapNotNull null
            val height = buffer.groupValues[2].toIntOrNull() ?: return@mapNotNull null
            if (width !in 16..16384 || height !in 16..16384) return@mapNotNull null
            "$width x $height"
        }
        // Two visible video surfaces are ambiguous even if their sizes match.
        return candidates.singleOrNull().orEmpty()
    }
}
