package com.signde.androidvideooverlay

import org.junit.Assert.assertEquals
import org.junit.Test

class AvDetailParserTest {
    private val decoder = "c2.realtek.hevc.main10.decoder.secure"
    private val resources = "Client:\nName: $decoder\nResources: secure-codec/video-codec:[]:1"
    private val audio = "Output thread 0x1:\nStandby: no\nHAL format: 0x1 (AUDIO_FORMAT_PCM_16_BIT)"
    private val allocation = "10-13 10:00:00.000 1000 1002 D CCodec: allocate($decoder)"
    private fun transfer(value: Int, thread: Int = 1002) =
        "10-13 10:00:01.000 1000 $thread D CCodecConfig: coded.vui.color.transfer = $value"
    private fun parse(log: String) = PortableMediaInfoParser.parse(resources, audio, log)

    @Test fun fullDecoderNameRetainsHevcDespiteProfileAndSecureSuffix() {
        assertEquals("H.265 / HEVC", parse("").videoFormat)
        assertEquals("", parse("").videoHdr)
        assertEquals("H.265 / HEVC", PortableMediaInfoParser.parse(
            resources.replace(decoder, "OMX.realtek.video.decoder.hevc.main10"), audio, "").videoFormat)
    }

    @Test fun realtekColorUsesOnlyUnambiguousCurrentOutput() {
        val current = "[RTKDRM] crnt-fmt: [MODE]: 2, VIC_3840_2160P_60HZ(97), YUV420/10, HDR_MODE_INPUT_FW_AUTO(6)"
        assertEquals("YCbCr420 10bit", HdmiColorSettingParser.parseRealtekCurrentFormat(current))
        assertEquals("", HdmiColorSettingParser.parseRealtekCurrentFormat(current.replace("crnt-fmt:", "gpt-fmt:")))
        assertEquals("", HdmiColorSettingParser.parseRealtekCurrentFormat("$current\n$current"))
        assertEquals("", HdmiColorSettingParser.parseRealtekCurrentFormat("Permission denied"))
        assertEquals("", HdrOutputParser.parse("HDR_MODE_INPUT_FW_AUTO(6)"))
    }

    @Test fun activeTransferIsSeparateFromUnknownHdmiOutput() {
        val hdr = parse("$allocation\n${transfer(6)}")
        assertEquals("HDR10", hdr.videoHdr)
        assertEquals("", hdr.hdrOutput)
        assertEquals("HLG", parse("$allocation\n${transfer(7)}").videoHdr)
        assertEquals("SDR", parse("$allocation\n${transfer(6)}\n${transfer(3)}").videoHdr)
    }

    @Test fun unrelatedCodecThreadCannotOverrideTransfer() {
        assertEquals("HDR10", parse("$allocation\n${transfer(6)}\n${transfer(3, 2000)}").videoHdr)
        assertEquals("", parse("$allocation\n${transfer(6, 2000)}").videoHdr)
    }

    @Test fun missingAllocationDoesNotScanUnscopedHistory() {
        assertEquals("", parse(transfer(6)).videoHdr)
    }

    @Test fun newAllocationClearsPreviousTransfer() {
        assertEquals("", parse("$allocation\n${transfer(6)}\n$allocation").videoHdr)
    }

    @Test fun stoppedVideoCannotReportHdrFromOldTrack() {
        assertEquals("", PortableMediaInfoParser.parse("", audio,
            "[X] Track:0, mimeType=video/hevc, res=3840x2160, color-transfer=6").videoHdr)
    }

    @Test fun selectedTrackSuppliesHdrOnlyUntilNewFormatEvent() {
        val track = "[X] Track:0, mimeType=video/hevc, res=3840x2160, color-transfer=7"
        assertEquals("HLG", parse(track).videoHdr)
        assertEquals("", parse("$track\nonVideoInputFormatChanged Video Track: 3840x2160 bitrate:10000000 frameRate:60").videoHdr)
    }

    @Test fun sourceChannelsDoNotUseHalChannelCount() {
        assertEquals("", PortableMediaInfoParser.parse(resources,
            "$audio\nChannel count: 8\nChannel mask: 0x63f", "").audioChannels)
        assertEquals("6 channels", parse("[X] Track:0, mimeType=audio/eac3, channels=6").audioChannels)
        assertEquals("1 channel", parse("[X] Track:0, mimeType=audio/aac, channels=1").audioChannels)
    }

    @Test fun latestAudioEventSupersedesOldTrackChannels() {
        val track = "[X] Track:0, mimeType=audio/eac3, channels=6"
        val event = "onAudioInputFormatChanged Audio Track: audio/aac bitrate:128000"
        assertEquals("", parse("$track\n$event").audioChannels)
        assertEquals("2 channels", parse("$track\n$event channelCount:2").audioChannels)
    }

    private val espnTrack = "[X] Track:0, id=eac-3:English, mimeType=audio/eac3, channels=6, language=en, supported=true"
    private val espnEvent = "onAudioInputFormatChanged Audio Track: audio/eac3 bitrate:256000 language:en"
    private val espnSelection = "$espnTrack\n[ ] Track:1, mimeType=audio/mp4a-latm, channels=2, language=en, supported=true"

    @Test fun espnBitrateOnlyEventRetainsMatchingSelectedChannels() {
        val result = parse("$espnSelection\n$espnEvent")
        assertEquals("6 channels", result.audioChannels)
        assertEquals("256 kbps", result.audioBitrate)
        assertEquals("Dolby Digital+", result.audioMode)
        assertEquals("", result.hdrOutput)
    }

    @Test fun espnEventChannelsTakePrecedenceIncludingExplicitUnknown() {
        assertEquals("2 channels", parse("$espnSelection\n$espnEvent channelCount:2").audioChannels)
        assertEquals("", parse("$espnSelection\n$espnEvent channelCount:-1").audioChannels)
    }

    @Test fun espnDoesNotReuseSelectionAcrossCodecLanguageOrLaterFormatChanges() {
        assertEquals("", parse("$espnSelection\n${espnEvent.replace("audio/eac3", "audio/mp4a-latm")}").audioChannels)
        assertEquals("", parse("$espnSelection\n${espnEvent.replace("language:en", "language:es")}").audioChannels)
        assertEquals("", parse("$espnSelection\n${espnEvent.replace(" language:en", "")}").audioChannels)
        assertEquals("", parse("$espnSelection\n$espnEvent\n$espnEvent").audioChannels)
        assertEquals("6 channels", parse("$espnSelection\n$espnEvent\n$espnSelection\n$espnEvent").audioChannels)
    }

    @Test fun espnSameCodecAlternativesAndMultipleSelectionsAreAmbiguous() {
        val alternative = "[ ] Track:1, mimeType=audio/eac3, channels=2, language=en, supported=true"
        assertEquals("", parse("$espnTrack\n$alternative\n$espnEvent").audioChannels)
        assertEquals("", parse("$espnTrack\n${alternative.replace("[ ]", "[X]")}\n$espnEvent").audioChannels)
    }

    @Test fun espnMissingUnsupportedOrInactiveSelectionDoesNotSupplyChannels() {
        assertEquals("", parse(espnEvent).audioChannels)
        assertEquals("", parse("${espnSelection.replace("supported=true", "supported=false")}\n$espnEvent").audioChannels)
        assertEquals("", parse("${espnSelection.replace("channels=6", "channels=-1")}\n$espnEvent").audioChannels)
        assertEquals("", PortableMediaInfoParser.parse(resources, "", "$espnSelection\n$espnEvent").audioChannels)
    }

    @Test fun embyInputFormatIdentifiesTrueHdAndEightChannels() {
        val log = """
            [X] Track:0, mimeType=audio/true-hd, channels=8
            [ ] Track:0, mimeType=audio/ac3, channels=6
            EventLogger: audioInputFormat [eventTime=1.87, mimeType=audio/true-hd, channels=8, sample_rate=48000]
        """.trimIndent()
        val result = parse(log)
        assertEquals("Dolby TrueHD", result.audioMode)
        assertEquals("8 channels", result.audioChannels)
    }

    @Test fun activeAudioCodecInputChannelsNeverUseOutputOrStaleConfiguration() {
        val resource = "$resources\nClient:\nName: c2.android.aac.decoder\nResources: non-secure-codec/audio-codec:[]:1"
        val log = """
            10-13 13:15:29.792 8283 10712 D CCodec: allocate(c2.android.aac.decoder)
            10-13 13:15:29.830 8283 10712 D CCodec: setup formats input: AMessage = {
            10-13 13:15:29.830 8283 10712 D CCodec: int32_t channel-count = 2
            10-13 13:15:29.830 8283 10712 D CCodec: string mime = "audio/mp4a-latm"
            10-13 13:15:29.830 8283 10712 D CCodec: }
            10-13 13:15:29.830 8283 10712 D CCodec: setup formats output: AMessage = {
            10-13 13:15:29.830 8283 10712 D CCodec: int32_t channel-count = 8
            10-13 13:15:29.830 8283 10712 D CCodec: string mime = "audio/raw"
            10-13 13:15:29.830 8283 10712 D CCodec: }
            10-13 13:15:29.864 8283 8283 I PlayerEventListener: onAudioInputFormatChanged Audio Track: audio/mp4a-latm bitrate:128000
        """.trimIndent()
        assertEquals("2 channels", PortableMediaInfoParser.parse(resource, audio, log).audioChannels)
        assertEquals("", parse(log).audioChannels)
        assertEquals("", PortableMediaInfoParser.parse(resource, audio,
            "$log\nonAudioInputFormatChanged Audio Track: audio/mp4a-latm bitrate:96000").audioChannels)
        assertEquals("", PortableMediaInfoParser.parse(resource, audio,
            log.replace("setup formats input:", "not input:")).audioChannels)
    }

    @Test fun missingInvalidAmbiguousOrInactiveChannelsAreUnknown() {
        for (count in listOf("-1", "0", "99")) {
            assertEquals("", parse("[X] Track:0, mimeType=audio/aac, channels=$count").audioChannels)
        }
        assertEquals("", parse("[X] Track:0, mimeType=audio/aac, channels=2, supported=NO").audioChannels)
        val tracks = "[X] Track:0, mimeType=audio/aac, channels=2\n[X] Track:1, mimeType=audio/aac, channels=6"
        assertEquals("", parse(tracks).audioChannels)
        assertEquals("", PortableMediaInfoParser.parse(resources, "",
            "[X] Track:0, mimeType=audio/aac, channels=2").audioChannels)
    }
}
