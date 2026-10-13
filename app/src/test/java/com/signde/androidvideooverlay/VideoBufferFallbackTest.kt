package com.signde.androidvideooverlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoBufferFallbackTest {
    private val pkg = "com.peacocktv.peacockandroid"
    private val resources = "Pid: 19726\nClient:\nName: c2.realtek.video.avc.decoder.secure\nResources: secure-codec/video-codec:[]:1"
    private val info = PortableMediaInfo(videoFormat = "H.264 / AVC", audioChannels = "2 channels", hdrOutput = "HDR10")
    private fun layer(owner: String = pkg, width: Int = 1920, height: Int = 1080) = """
        + Layer (SurfaceView[$owner/com.peacock.peacocktv.GoogleMainActivity](BLAST)#278) uid=10114
          Region VisibleRegion (this=0 count=1)
            [  0,   0, 3840, 2160]
              size=(0,0), crop=[0,0,-1,-1], isProtected=1, defaultPixelFormat=Unknown 0x000077,
              activeBuffer=[$width x $height:1614825776,Unknown 0x000077], queued-frames=0
    """.trimIndent().replace("$width x $height", "${width}x$height")

    @Test fun readsBufferRatherThanUpscaledDisplayRectangle() {
        assertEquals("1920 x 1080", VideoBufferFallback.parse(layer(), pkg))
    }

    @Test fun fillsOnlyMissingResolutionAndPreservesOtherDetails() {
        val result = VideoBufferFallback.apply(info, "Realtek", resources, pkg) { layer() }
        assertEquals("1920 x 1080", result.videoResolution)
        assertTrue(result.videoResolutionFromBuffer)
        assertEquals(info.audioChannels, result.audioChannels)
        assertEquals(info.hdrOutput, result.hdrOutput)
    }

    @Test fun existingResolutionNeverInvokesFallbackOnAnySoc() {
        for (manufacturer in listOf("Realtek", "Amlogic", "MediaTek", "Qualcomm", "")) {
            val original = info.copy(videoResolution = "1280 x 720", frameRate = "60fps", videoBitrate = "5 Mbps")
            assertEquals(original, VideoBufferFallback.apply(original, manufacturer, resources, pkg) { error("must not query") })
        }
    }

    @Test fun otherSocsNeverInvokeFallbackEvenWithMissingResolution() {
        for (manufacturer in listOf("Amlogic", "MediaTek", "Qualcomm", "")) {
            assertEquals(info, VideoBufferFallback.apply(info, manufacturer, resources, pkg) { error("must not query") })
        }
    }

    @Test fun requiresExactlyOneActiveDecoderAndCurrentPackage() {
        for (r in listOf("", "Events logs:\n$resources", "$resources\n$resources")) {
            assertEquals(info, VideoBufferFallback.apply(info, "Realtek", r, pkg) { error("must not query") })
        }
        for (p in listOf(null, "", "com.peacocktv.peacockandroid:worker", "invalid/name")) {
            assertEquals(info, VideoBufferFallback.apply(info, "Realtek", resources, p) { error("must not query") })
        }
        assertEquals(PortableMediaInfo(), VideoBufferFallback.apply(PortableMediaInfo(), "Realtek", resources, pkg) { error("must not query") })
    }

    @Test fun unrelatedAppAndPrefixCollisionAreIgnored() {
        assertEquals("", VideoBufferFallback.parse(layer("$pkg.other"), pkg))
        assertEquals("", VideoBufferFallback.parse(layer("com.other.player"), pkg))
        assertEquals("1920 x 1080", VideoBufferFallback.parse(layer("com.other.player") + "\n" + layer(), pkg))
    }

    @Test fun rejectsParentBackgroundRgbAndCachedBuffers() {
        for (dump in listOf(
            layer().replace("SurfaceView[", "Background for SurfaceView["),
            layer().replace("SurfaceView[", "AppWindow["),
            layer().replace("defaultPixelFormat=Unknown 0x000077", "defaultPixelFormat=RGBA_8888"),
            layer().replace("activeBuffer=", "cachedBuffer="),
            layer(width = 0, height = 0)
        )) assertEquals("", VideoBufferFallback.parse(dump, pkg))
    }

    @Test fun hiddenAndAmbiguousSurfacesAreRejected() {
        assertEquals("", VideoBufferFallback.parse(layer().replace("count=1", "count=0"), pkg))
        assertEquals("", VideoBufferFallback.parse(layer().replace("3840, 2160", "0, 0"), pkg))
        assertEquals("", VideoBufferFallback.parse(layer() + "\n" + layer(), pkg))
        assertEquals("", VideoBufferFallback.parse(layer() + "\n" + layer(width = 1280, height = 720), pkg))
    }

    @Test fun failureDoesNotEraseOtherAvData() {
        assertEquals(info, VideoBufferFallback.apply(info, "Realtek", resources, pkg) { error("ADB read failed") })
        assertEquals(info, VideoBufferFallback.apply(info, "Realtek", resources, pkg) { "Permission denied" })
        assertFalse(info.videoResolutionFromBuffer)
    }

    @Test fun bufferSizeCannotInheritStaleFrameRateOrBitrate() {
        val result = VideoBufferFallback.apply(info.copy(frameRate = "60fps", videoBitrate = "5 Mbps"), "Realtek", resources, pkg) { layer() }
        assertEquals("", result.frameRate)
        assertEquals("", result.videoBitrate)
    }
}
