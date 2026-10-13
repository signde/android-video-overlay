package com.signde.androidvideooverlay

import org.junit.Assert.assertEquals
import org.junit.Test

class PortableMediaInfoParserTest {
    @Test
    fun parsesHevcAndEac3Passthrough() {
        val resources = """
            Pid: 21229
              Client:
                Name: c2.amlogic.hevc.decoder
                Resources:
                  non-secure-codec/video-codec:[]:1
        """.trimIndent()
        val logs = """
            D/EventLogger: [X] Track:0, id=1, mimeType=video/hevc, container=video/mp2t, res=1280x720, supported=YES
            D/EventLogger: [X] Track:0, id=2, mimeType=audio/eac3, container=video/mp2t, channels=6, sample_rate=48000, supported=YES
        """.trimIndent()
        val audio = """
            Output thread 0x1, type 4 (OFFLOAD):
              Standby: no
              HAL format: 0xa000000 (AUDIO_FORMAT_E_AC3)
              AudioStreamOut: 0x2 flags 0x11 (AUDIO_OUTPUT_FLAG_DIRECT|AUDIO_OUTPUT_FLAG_COMPRESS_OFFLOAD)
        """.trimIndent()

        assertEquals(
            PortableMediaInfo("H.265 / HEVC", "1280 x 720", "", "Dolby Digital+", "Direct / offload", audioChannels = "6 channels"),
            PortableMediaInfoParser.parse(resources, audio, logs)
        )
    }

    @Test
    fun distinguishesDecodedPcmFromDtsHdSource() {
        val resources = """
            Pid: 21229
              Client:
                Name: c2.amlogic.av1.decoder
                Resources:
                  non-secure-codec/video-codec:[]:1
        """.trimIndent()
        val logs = """
            D/EventLogger: [X] Track:0, id=1, mimeType=video/av01, container=video/x-matroska, res=3840x1612, supported=YES
            D/EventLogger: [X] Track:0, id=2, mimeType=audio/vnd.dts.hd, container=video/x-matroska, channels=8, sample_rate=48000, supported=YES
        """.trimIndent()
        val audio = """
            Output thread 0x1, type 0 (MIXER):
              Standby: no
              HAL format: 0x1 (AUDIO_FORMAT_PCM_16_BIT)
              AudioStreamOut: 0x2 flags 0x8 (AUDIO_OUTPUT_FLAG_DEEP_BUFFER)
        """.trimIndent()

        assertEquals(
            PortableMediaInfo("AV1", "3840 x 1612", "", "DTS-HD", "Decoded PCM", audioChannels = "8 channels"),
            PortableMediaInfoParser.parse(resources, audio, logs)
        )
    }

    private val avcResources = """
        Client:
          Name: c2.amlogic.avc.decoder.secure
          Resources: secure-codec/video-codec:[]:1
    """.trimIndent()

    private val pcmOutput = """
        Output thread 0x1:
          Standby: no
          HAL format: 0x1 (AUDIO_FORMAT_PCM_16_BIT)
    """.trimIndent()

    @Test
    fun espnUsesPlayingRenditionNotLastAdaptiveTrack() {
        val logs = """
            [X] Track:0, mimeType=video/avc, bitrate=2478000, res=960x540, fps=29.97
            [X] Track:1, mimeType=video/avc, bitrate=11548000, res=1920x1080, fps=59.94
            onVideoInputFormatChanged Video Track: 960x540 bitrate:2478000 frameRate:29.97
            onAudioInputFormatChanged Audio Track: audio/eac3 bitrate:256000 language:en
        """.trimIndent()
        val result = PortableMediaInfoParser.parse(avcResources, pcmOutput, logs)
        assertEquals("960 x 540", result.videoResolution)
        assertEquals("2.48 Mbps", result.videoBitrate)
        assertEquals("256 kbps", result.audioBitrate)
        assertEquals("29.97fps", result.frameRate)
        assertEquals("Dolby Digital+", result.audioMode)
    }

    @Test
    fun ambiguousAdaptiveTrackListDoesNotInventSelectedBitrate() {
        val logs = """
            [X] Track:0, mimeType=video/avc, bitrate=2478000, res=960x540
            [X] Track:1, mimeType=video/avc, bitrate=11548000, res=1920x1080
        """.trimIndent()
        val result = PortableMediaInfoParser.parse(avcResources, pcmOutput, logs)
        assertEquals("", result.videoBitrate)
        assertEquals("", result.videoResolution)
    }

    @Test
    fun directvFollowsDecoderChangesWithoutUsingAudioCodecDefaults() {
        val logs = """
            10-09 17:10:42.000 29383 30368 D CCodec: allocate(c2.amlogic.avc.decoder.secure)
            10-09 17:10:42.100 29383 30368 D CCodecConfig: raw.size.height = 540
            10-09 17:10:42.100 29383 30368 D CCodecConfig: raw.size.width = 960
            10-09 17:10:42.200 29383 30385 D CCodec: int32_t bitrate = 64000
            10-09 17:10:42.200 29383 30385 D CCodec: string mime = "audio/raw"
            10-09 17:11:00.321 29383 30368 D CCodecConfig: raw.crop.height = 720
            10-09 17:11:00.321 29383 30368 D CCodecConfig: raw.crop.width = 1280
        """.trimIndent()
        val result = PortableMediaInfoParser.parse(avcResources, pcmOutput, logs)
        assertEquals("1280 x 720", result.videoResolution)
        assertEquals("H.264 / AVC", result.videoFormat)
        assertEquals("", result.audioBitrate)
        assertEquals("", result.videoBitrate)
        assertEquals("PCM (source unknown)", result.audioMode)
    }

    @Test
    fun stoppedDecoderDoesNotReuseHistoricalVideo() {
        val result = PortableMediaInfoParser.parse("Process Pid override:\nEvents logs:\n$avcResources", "",
            "[X] Track:0, mimeType=video/avc, bitrate=5000000, res=1920x1080")
        assertEquals(PortableMediaInfo(), result)
    }

    @Test
    fun singleEmbyTracksSupplyReportedBitrates() {
        val logs = """
            [X] Track:0, mimeType=video/avc, bitrate=5000000, res=1920x1080
            [ ] Track:0, mimeType=audio/ac3, bitrate=640000
            [X] Track:1, mimeType=audio/mp4a-latm, bitrate=192000
        """.trimIndent()
        val result = PortableMediaInfoParser.parse(avcResources, pcmOutput, logs)
        assertEquals("5.00 Mbps", result.videoBitrate)
        assertEquals("192 kbps", result.audioBitrate)
        assertEquals("AAC", result.audioMode)
    }

    @Test
    fun oldHevcHdrDoesNotLeakIntoActiveAvcSdrDecoder() {
        val logs = """
            10-10 10:00:00.000 1000 1001 D CCodec: allocate(c2.amlogic.hevc.decoder)
            10-10 10:00:00.100 1000 1001 D CCodecConfig: coded.vui.color.transfer = 6
            10-10 10:00:05.000 1000 1002 D CCodec: allocate(c2.amlogic.avc.decoder.secure)
            10-10 10:00:05.100 1000 1002 D CCodecConfig: coded.vui.color.transfer = 3
        """.trimIndent()
        val result = PortableMediaInfoParser.parse(avcResources, pcmOutput, logs)
        assertEquals("SDR", result.videoHdr)
        assertEquals("", result.hdrOutput)
    }

    @Test
    fun unselectedUnsupportedTrackDoesNotTriggerDolbyVision() {
        val logs = """
            [X] Track:0, mimeType=video/avc, bitrate=5000000, res=1920x1080
            [ ] Track:1, mimeType=video/dolby-vision, supported=NO
        """.trimIndent()
        val result = PortableMediaInfoParser.parse(avcResources, pcmOutput, logs)
        assertEquals("", result.videoHdr)
        assertEquals("", result.hdrOutput)
    }

    @Test
    fun activeDecoderWithTransfer6ReportsHdr10() {
        val logs = """
            10-10 10:00:05.000 1000 1002 D CCodec: allocate(c2.amlogic.avc.decoder.secure)
            10-10 10:00:05.100 1000 1002 D CCodecConfig: coded.vui.color.transfer = 6
        """.trimIndent()
        val result = PortableMediaInfoParser.parse(avcResources, pcmOutput, logs)
        assertEquals("HDR10", result.videoHdr)
    }

    @Test
    fun parsesHdmiColorAttributes() {
        assertEquals("YCbCr420 10bit", HdmiColorSettingParser.parse("420,10bit"))
        assertEquals("YCbCr422 12bit", HdmiColorSettingParser.parse("422,12bit"))
        assertEquals("RGB 8bit", HdmiColorSettingParser.parse("rgb,8bit"))
        assertEquals("YCbCr444 8bit", HdmiColorSettingParser.parse("444,8bit\n"))
        assertEquals("", HdmiColorSettingParser.parse("Permission denied"))
        assertEquals("", HdmiColorSettingParser.parse(""))
    }
}
