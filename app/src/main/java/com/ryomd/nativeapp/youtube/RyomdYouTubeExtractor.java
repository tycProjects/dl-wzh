package com.ryomd.nativeapp.youtube;

import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Request;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.localization.ContentCountry;
import org.schabi.newpipe.extractor.localization.Localization;
import org.schabi.newpipe.extractor.MediaFormat;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.DeliveryMethod;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.VideoStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;

/**
 * Ryomd adapter around NewPipe Extractor, following the extraction architecture
 * used by PlayTube. Extracts stream URLs only; it does not bypass DRM, private
 * video permissions, sign-in walls, or access-control restrictions.
 */
final class RyomdYouTubeExtractor {
    private static volatile boolean initialized;
    private static final Object INIT_LOCK = new Object();
    private static final String USER_AGENT =
            "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36";
    private static final String CONSENT_COOKIE =
            "CONSENT=YES+cb.20210328-17-p0.en+FX+456; " +
            "SOCS=CAISNQgDEitib3FfaWRlbnRpdHlmcm9udGVuZHVpc2VydmVyXzIwMjMwNjE4LjA2X3AwGgJlbiACGgYIgLCspAY";

    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(40, TimeUnit.SECONDS)
            .callTimeout(50, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .build();

    private RyomdYouTubeExtractor() { }

    static final class ResolvedMedia {
        final String videoId;
        final String title;
        final String artist;
        final String streamUrl;
        final String companionAudioUrl;
        final String thumbnailUrl;
        final boolean audioOnly;
        /** MIME eksplisit hanya untuk manifest DASH/HLS; null = biarkan ExoPlayer mendeteksi. */
        final String mimeType;
        final int selectedHeight;
        final int requestedHeight;

        ResolvedMedia(String videoId, String title, String artist, String streamUrl,
                      String companionAudioUrl, String thumbnailUrl, boolean audioOnly,
                      String mimeType, int selectedHeight, int requestedHeight) {
            this.mimeType = mimeType;
            this.videoId = videoId;
            this.title = title;
            this.artist = artist;
            this.streamUrl = streamUrl;
            this.companionAudioUrl = companionAudioUrl;
            this.thumbnailUrl = thumbnailUrl;
            this.audioOnly = audioOnly;
            this.selectedHeight = selectedHeight;
            this.requestedHeight = requestedHeight;
        }
    }

    static final class VideoQuality {
        final int height;
        final int fps;
        final int bitrate;

        VideoQuality(int height, int fps, int bitrate) {
            this.height = height;
            this.fps = fps;
            this.bitrate = bitrate;
        }

        String label() {
            return height + "p" + (fps > 30 ? " · " + fps + " fps" : "");
        }
    }

    // Batas konservatif untuk kompatibilitas decoder dan konsumsi data perangkat Android.
    private static final int MAX_HEIGHT = 1080;
    private static final String MIME_DASH = "application/dash+xml";
    private static final String MIME_HLS = "application/x-mpegURL";

    private static boolean progressive(org.schabi.newpipe.extractor.stream.Stream stream) {
        return stream != null && nonBlank(stream.getContent())
                && stream.getDeliveryMethod() == DeliveryMethod.PROGRESSIVE_HTTP;
    }

    /** AV1 sering tidak punya decoder hardware pada HP lama -> hindari agar tidak layar hitam. */
    private static boolean avoidCodec(VideoStream stream) {
        String codec = stream.getCodec();
        return codec != null && codec.toLowerCase(Locale.ROOT).startsWith("av01");
    }

    static ResolvedMedia resolve(String videoId, boolean audioOnly) throws Exception {
        return resolve(videoId, audioOnly, 0);
    }

    /** requestedHeight=0 means automatic / highest compatible quality. */
    static ResolvedMedia resolve(String videoId, boolean audioOnly, int requestedHeight) throws Exception {
        ensureInitialized();
        String watchUrl = "https://www.youtube.com/watch?v=" + videoId;
        StreamInfo info = StreamInfo.getInfo(ServiceList.YouTube, watchUrl);
        String title = clean(info.getName(), "YouTube video " + videoId);
        String artist = clean(info.getUploaderName(), "YouTube");
        String thumbnail = null;
        if (info.getThumbnails() != null && !info.getThumbnails().isEmpty()) {
            thumbnail = info.getThumbnails().get(info.getThumbnails().size() - 1).getUrl();
        }

        AudioStream bestAudio = bestAudio(info);
        if (audioOnly) {
            if (bestAudio != null) {
                return new ResolvedMedia(videoId, title, artist, bestAudio.getContent(), null,
                        thumbnail, true, null, 0, 0);
            }
            return manifestFallback(info, videoId, title, artist, thumbnail, true,
                    "NewPipe tidak menemukan stream audio yang tersedia.", 0);
        }

        Map<Integer, VideoStream> muxed = collectVideoStreams(info.getVideoStreams(), false);
        Map<Integer, VideoStream> videoOnly = bestAudio == null
                ? Collections.emptyMap() : collectVideoStreams(info.getVideoOnlyStreams(), true);
        int selectedHeight = chooseHeight(muxed, videoOnly, requestedHeight);
        if (selectedHeight > 0) {
            VideoStream selected = videoOnly.get(selectedHeight);
            if (selected != null) {
                return new ResolvedMedia(videoId, title, artist, selected.getContent(),
                        bestAudio.getContent(), thumbnail, false, null, selectedHeight,
                        requestedHeight > 0 ? selectedHeight : 0);
            }
            selected = muxed.get(selectedHeight);
            if (selected != null) {
                return new ResolvedMedia(videoId, title, artist, selected.getContent(), null,
                        thumbnail, false, null, selectedHeight, requestedHeight > 0 ? selectedHeight : 0);
            }
        }

        return manifestFallback(info, videoId, title, artist, thumbnail, false,
                "NewPipe tidak menemukan stream video yang dapat diputar.", requestedHeight);
    }

    /** Returns only qualities that this adapter can actually play as progressive HTTP streams. */
    static List<VideoQuality> availableVideoQualities(String videoId) throws Exception {
        ensureInitialized();
        StreamInfo info = StreamInfo.getInfo(ServiceList.YouTube,
                "https://www.youtube.com/watch?v=" + videoId);
        AudioStream audio = bestAudio(info);
        Map<Integer, VideoStream> muxed = collectVideoStreams(info.getVideoStreams(), false);
        Map<Integer, VideoStream> videoOnly = audio == null
                ? Collections.emptyMap() : collectVideoStreams(info.getVideoOnlyStreams(), true);
        TreeMap<Integer, VideoQuality> result = new TreeMap<>(Collections.reverseOrder());
        for (Map.Entry<Integer, VideoStream> entry : muxed.entrySet()) {
            VideoStream stream = entry.getValue();
            result.put(entry.getKey(), new VideoQuality(entry.getKey(), stream.getFps(), stream.getBitrate()));
        }
        for (Map.Entry<Integer, VideoStream> entry : videoOnly.entrySet()) {
            VideoStream stream = entry.getValue();
            VideoQuality previous = result.get(entry.getKey());
            int fps = stream.getFps() > 0 ? stream.getFps() : (previous == null ? 0 : previous.fps);
            int bitrate = Math.max(stream.getBitrate(), previous == null ? 0 : previous.bitrate);
            result.put(entry.getKey(), new VideoQuality(entry.getKey(), fps, bitrate));
        }
        return new ArrayList<>(result.values());
    }

    private static AudioStream bestAudio(StreamInfo info) {
        AudioStream bestAudio = null;
        List<AudioStream> audioStreams = info.getAudioStreams();
        if (audioStreams != null) {
            for (AudioStream stream : audioStreams) {
                if (!progressive(stream)) continue;
                if (bestAudio == null || stream.getAverageBitrate() > bestAudio.getAverageBitrate()) {
                    bestAudio = stream;
                }
            }
        }
        return bestAudio;
    }

    private static Map<Integer, VideoStream> collectVideoStreams(List<VideoStream> streams,
                                                                  boolean requireVideoOnly) {
        TreeMap<Integer, VideoStream> result = new TreeMap<>();
        if (streams == null) return result;
        for (VideoStream stream : streams) {
            if (!progressive(stream) || avoidCodec(stream)) continue;
            if (stream.isVideoOnly() != requireVideoOnly) continue;
            int height = stream.getHeight();
            if (height <= 0 || height > MAX_HEIGHT) continue;
            VideoStream current = result.get(height);
            if (current == null || stream.getBitrate() > current.getBitrate()
                    || (stream.getBitrate() == current.getBitrate()
                    && stream.getFormat() == MediaFormat.MPEG_4
                    && current.getFormat() != MediaFormat.MPEG_4)) {
                result.put(height, stream);
            }
        }
        return result;
    }

    private static int chooseHeight(Map<Integer, VideoStream> muxed,
                                    Map<Integer, VideoStream> videoOnly, int requestedHeight) {
        TreeMap<Integer, Boolean> heights = new TreeMap<>();
        for (Integer h : muxed.keySet()) heights.put(h, Boolean.TRUE);
        for (Integer h : videoOnly.keySet()) heights.put(h, Boolean.TRUE);
        if (heights.isEmpty()) return 0;
        if (requestedHeight <= 0) return heights.lastKey();
        Integer exactOrLower = heights.floorKey(Math.min(requestedHeight, MAX_HEIGHT));
        return exactOrLower != null ? exactOrLower : heights.firstKey();
    }

    private static ResolvedMedia manifestFallback(StreamInfo info, String videoId, String title,
                                                  String artist, String thumbnail,
                                                  boolean audioOnly, String failure, int requestedHeight)
            throws IOException {
        if (nonBlank(info.getHlsUrl())) {
            return new ResolvedMedia(videoId, title, artist, info.getHlsUrl(), null, thumbnail,
                    audioOnly, MIME_HLS, 0, Math.max(0, requestedHeight));
        }
        if (nonBlank(info.getDashMpdUrl())) {
            return new ResolvedMedia(videoId, title, artist, info.getDashMpdUrl(), null, thumbnail,
                    audioOnly, MIME_DASH, 0, Math.max(0, requestedHeight));
        }
        throw new IOException(failure);
    }

    static void ensureInitialized() throws IOException {
        if (initialized) return;
        synchronized (INIT_LOCK) {
            if (initialized) return;
            NewPipe.init(new Downloader() {
                @Override
                public Response execute(Request request) throws IOException {
                    okhttp3.Request.Builder builder = new okhttp3.Request.Builder().url(request.url());
                    String method = request.httpMethod().toUpperCase(Locale.ROOT);
                    byte[] payload = request.dataToSend();
                    if ("GET".equals(method)) {
                        builder.get();
                    } else if ("HEAD".equals(method)) {
                        builder.head();
                    } else {
                        RequestBody body = payload == null ? null
                                : RequestBody.create((MediaType) null, payload);
                        if (body == null && !"DELETE".equals(method)) {
                            body = RequestBody.create((MediaType) null, new byte[0]);
                        }
                        builder.method(method, body);
                    }

                    boolean hasUserAgent = false;
                    boolean hasLanguage = false;
                    boolean hasCookie = false;
                    Map<String, List<String>> headers = request.headers();
                    if (headers != null) {
                        for (Map.Entry<String, List<String>> header : headers.entrySet()) {
                            String key = header.getKey();
                            if (key == null || header.getValue() == null) continue;
                            if ("user-agent".equalsIgnoreCase(key)) hasUserAgent = true;
                            if ("accept-language".equalsIgnoreCase(key)) hasLanguage = true;
                            if ("cookie".equalsIgnoreCase(key)) hasCookie = true;
                            for (String value : header.getValue()) {
                                if (value != null) builder.addHeader(key, value);
                            }
                        }
                    }
                    if (!hasUserAgent) builder.header("User-Agent", USER_AGENT);
                    if (!hasLanguage) builder.header("Accept-Language", "id-ID,id;q=0.9,en-US;q=0.8,en;q=0.7");
                    if (!hasCookie && request.url().contains("youtube.com")) {
                        builder.header("Cookie", CONSENT_COOKIE);
                    }

                    try (okhttp3.Response response = HTTP.newCall(builder.build()).execute()) {
                        String body = response.body() == null ? "" : response.body().string();
                        return new Response(response.code(), response.message(),
                                response.headers().toMultimap(), body,
                                response.request().url().toString());
                    }
                }
            }, new Localization("id", "ID"), new ContentCountry("ID"));
            initialized = true;
        }
    }

    private static boolean nonBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String clean(String value, String fallback) {
        if (!nonBlank(value)) return fallback;
        String trimmed = value.trim();
        return trimmed.length() > 220 ? trimmed.substring(0, 220) : trimmed;
    }
}
