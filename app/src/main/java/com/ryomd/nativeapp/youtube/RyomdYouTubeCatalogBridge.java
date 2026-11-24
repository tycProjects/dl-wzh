package com.ryomd.nativeapp.youtube;

import android.app.Activity;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.schabi.newpipe.extractor.Image;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.channel.ChannelInfo;
import org.schabi.newpipe.extractor.channel.ChannelInfoItem;
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabInfo;
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabs;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandler;
import org.schabi.newpipe.extractor.search.SearchInfo;
import org.schabi.newpipe.extractor.stream.Description;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * Katalog YouTube native (tanpa backend Termux): pencarian, halaman channel, dan info video
 * lewat NewPipe Extractor. Bentuk JSON sengaja sama dengan endpoint /api/youtube/* di Termux
 * sehingga panel web dapat memakai keduanya secara bergantian.
 * Hasil dikirim ke window.ryomdYouTubeCatalog.done(callbackId, jsonText).
 */
public final class RyomdYouTubeCatalogBridge {
    private static final Pattern CALLBACK_ID = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");
    private static final Pattern VIDEO_ID = Pattern.compile("[A-Za-z0-9_-]{11}");

    private interface Job { JSONObject run() throws Exception; }

    private final Activity activity;
    private final WebView webView;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newFixedThreadPool(3);

    public RyomdYouTubeCatalogBridge(Activity activity, WebView webView) {
        this.activity = activity;
        this.webView = webView;
    }

    @JavascriptInterface
    public boolean isAvailable() { return true; }

    @JavascriptInterface
    public void search(String callbackId, String query, int limit) {
        searchPage(callbackId, query, limit, 0);
    }

    /** Fetches a bounded slice of a multi-page search result. Offset is intentionally capped. */
    @JavascriptInterface
    public void searchPage(String callbackId, String query, int limit, int offset) {
        final int max = Math.max(1, Math.min(limit <= 0 ? 40 : limit, 40));
        final int start = Math.max(0, Math.min(offset, 160));
        run(callbackId, () -> searchJson(query == null ? "" : query.trim(), max, start));
    }

    @JavascriptInterface
    public void channel(String callbackId, String channelUrl, int limit) {
        final int max = Math.max(1, Math.min(limit <= 0 ? 24 : limit, 50));
        run(callbackId, () -> channelJson(channelUrl == null ? "" : channelUrl.trim(), max));
    }

    @JavascriptInterface
    public void video(String callbackId, String idOrUrl) {
        run(callbackId, () -> videoJson(idOrUrl == null ? "" : idOrUrl.trim()));
    }

    /** Lists the progressive video heights that the native player can select. */
    @JavascriptInterface
    public void qualities(String callbackId, String idOrUrl) {
        run(callbackId, () -> qualitiesJson(idOrUrl == null ? "" : idOrUrl.trim()));
    }

    // ------------------------------------------------------------------ jobs

    private JSONObject searchJson(String query, int limit, int offset) throws Exception {
        if (query.length() < 2) throw new IllegalArgumentException("Kata pencarian terlalu pendek.");
        RyomdYouTubeExtractor.ensureInitialized();
        SearchQueryHandler queryHandler = ServiceList.YouTube.getSearchQHFactory().fromQuery(query);
        SearchInfo info = SearchInfo.getInfo(ServiceList.YouTube, queryHandler);

        // SearchInfo.getRelatedItems() is only the first HTML page (often five items).
        // Consume subsequent NewPipe pages until the requested slice is filled or exhausted.
        List<InfoItem> collected = new java.util.ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        appendUniqueItems(collected, seen, info.getRelatedItems());
        Page nextPage = info.getNextPage();
        int pageCount = 0;
        int targetCount = Math.min(offset + limit, 200);
        String paginationWarning = "";
        while (nextPage != null && countStreamItems(collected) < targetCount && pageCount < 10) {
            try {
                InfoItemsPage<InfoItem> next = SearchInfo.getMoreItems(ServiceList.YouTube, queryHandler, nextPage);
                if (next == null) { nextPage = null; break; }
                appendUniqueItems(collected, seen, next.getItems());
                nextPage = next.getNextPage();
                pageCount++;
            } catch (Exception pageError) {
                // Keep videos from already-loaded pages and expose the page error instead
                // of discarding the whole home feed because a later page failed.
                paginationWarning = safeMessage(pageError);
                nextPage = null;
                break;
            }
        }

        JSONArray videos = new JSONArray();
        JSONArray channels = new JSONArray();
        int videoIndex = 0;
        java.util.Set<String> seenChannels = new java.util.HashSet<>();
        for (InfoItem item : collected) {
            if (item instanceof StreamInfoItem) {
                JSONObject video = streamItem((StreamInfoItem) item, null, null);
                if (video == null) continue;
                if (videoIndex >= offset && videoIndex < offset + limit) videos.put(video);
                videoIndex++;
            } else if (item instanceof ChannelInfoItem && channels.length() < 8) {
                String url = item.getUrl() == null ? "" : item.getUrl();
                if (!url.isEmpty() && seenChannels.add(url)) {
                    JSONObject channel = new JSONObject();
                    channel.put("name", item.getName());
                    channel.put("url", url);
                    channel.put("thumbnail", bestImage(item.getThumbnails()));
                    channels.put(channel);
                }
            }
        }
        JSONObject result = new JSONObject();
        result.put("videos", videos);
        result.put("channels", channels);
        result.put("hasMore", nextPage != null && videoIndex >= offset + videos.length());
        result.put("nextOffset", offset + videos.length());
        result.put("totalFetched", videoIndex);
        if (!paginationWarning.isEmpty()) result.put("paginationWarning", paginationWarning);
        result.put("source", "native");
        return result;
    }

    private static void appendUniqueItems(List<InfoItem> target, java.util.Set<String> seen,
                                          List<InfoItem> source) {
        if (source == null) return;
        for (InfoItem item : source) {
            if (item == null) continue;
            String key = item.getUrl();
            if (key == null || key.isEmpty()) continue;
            if (item instanceof StreamInfoItem) {
                String videoId = extractVideoId(key);
                key = "video:" + (videoId == null ? key : videoId);
            } else if (item instanceof ChannelInfoItem) {
                key = "channel:" + key;
            }
            if (seen.add(key)) target.add(item);
        }
    }

    private static int countStreamItems(List<InfoItem> items) {
        int count = 0;
        for (InfoItem item : items) if (item instanceof StreamInfoItem) count++;
        return count;
    }

    private JSONObject channelJson(String url, int limit) throws Exception {
        if (url.isEmpty()) throw new IllegalArgumentException("URL channel kosong.");
        RyomdYouTubeExtractor.ensureInitialized();
        ChannelInfo info = ChannelInfo.getInfo(ServiceList.YouTube, url);
        JSONObject channel = new JSONObject();
        channel.put("url", info.getUrl());
        channel.put("title", info.getName());
        channel.put("description", info.getDescription() == null ? "" : info.getDescription());
        channel.put("thumbnail", bestImage(info.getAvatars()));
        if (info.getSubscriberCount() >= 0) channel.put("subscriberCount", info.getSubscriberCount());

        JSONArray videos = new JSONArray();
        ListLinkHandler videosTab = null;
        List<ListLinkHandler> tabs = info.getTabs();
        if (tabs != null) {
            for (ListLinkHandler tab : tabs) {
                List<String> filters = tab.getContentFilters();
                if (filters != null && filters.contains(ChannelTabs.VIDEOS)) { videosTab = tab; break; }
            }
        }
        if (videosTab != null) {
            ChannelTabInfo tabInfo = ChannelTabInfo.getInfo(ServiceList.YouTube, videosTab);
            for (InfoItem item : tabInfo.getRelatedItems()) {
                if (videos.length() >= limit) break;
                if (!(item instanceof StreamInfoItem)) continue;
                JSONObject video = streamItem((StreamInfoItem) item, info.getName(), info.getUrl());
                if (video != null) videos.put(video);
            }
        }
        JSONObject result = new JSONObject();
        result.put("channel", channel);
        result.put("videos", videos);
        result.put("source", "native");
        return result;
    }

    private JSONObject qualitiesJson(String idOrUrl) throws Exception {
        String id = extractVideoId(idOrUrl);
        if (id == null) throw new IllegalArgumentException("ID video YouTube tidak valid.");
        JSONArray qualities = new JSONArray();
        for (RyomdYouTubeExtractor.VideoQuality quality :
                RyomdYouTubeExtractor.availableVideoQualities(id)) {
            JSONObject row = new JSONObject();
            row.put("height", quality.height);
            row.put("fps", quality.fps);
            row.put("bitrate", quality.bitrate);
            row.put("label", quality.label());
            qualities.put(row);
        }
        JSONObject result = new JSONObject();
        result.put("qualities", qualities);
        result.put("source", "native-newpipe");
        result.put("maxHeight", 1080);
        return result;
    }

    private JSONObject videoJson(String idOrUrl) throws Exception {
        String id = extractVideoId(idOrUrl);
        if (id == null) throw new IllegalArgumentException("ID video YouTube tidak valid.");
        RyomdYouTubeExtractor.ensureInitialized();
        StreamInfo info = StreamInfo.getInfo(ServiceList.YouTube, "https://www.youtube.com/watch?v=" + id);
        JSONObject video = new JSONObject();
        video.put("id", id);
        video.put("title", info.getName());
        video.put("channel", info.getUploaderName());
        video.put("channelUrl", info.getUploaderUrl());
        video.put("description", descriptionText(info.getDescription()));
        video.put("thumbnail", thumbnailFor(id));
        if (info.getViewCount() >= 0) video.put("viewCount", info.getViewCount());
        if (info.getDuration() > 0) video.put("duration", info.getDuration());
        JSONObject result = new JSONObject();
        result.put("video", video);
        result.put("source", "native");
        return result;
    }

    // --------------------------------------------------------------- helpers

    private static JSONObject streamItem(StreamInfoItem item, String fallbackChannel,
                                         String fallbackChannelUrl) throws JSONException {
        String id = extractVideoId(item.getUrl());
        if (id == null) return null;
        JSONObject video = new JSONObject();
        video.put("id", id);
        video.put("title", item.getName());
        String channel = item.getUploaderName();
        if (channel == null || channel.trim().isEmpty()) channel = fallbackChannel;
        String channelUrl = item.getUploaderUrl();
        if (channelUrl == null || channelUrl.trim().isEmpty()) channelUrl = fallbackChannelUrl;
        video.put("channel", channel == null ? "" : channel);
        video.put("channelUrl", channelUrl == null ? "" : channelUrl);
        if (item.getDuration() > 0) video.put("duration", item.getDuration());
        if (item.getViewCount() >= 0) video.put("viewCount", item.getViewCount());
        String uploaded = item.getTextualUploadDate();
        if (uploaded != null && !uploaded.isEmpty()) video.put("published", uploaded);
        video.put("thumbnail", thumbnailFor(id));
        return video;
    }

    private static String thumbnailFor(String id) {
        return "https://i.ytimg.com/vi/" + id + "/hqdefault.jpg";
    }

    private static String bestImage(List<Image> images) {
        if (images == null || images.isEmpty()) return "";
        Image best = null;
        for (Image image : images) {
            if (image == null || image.getUrl() == null) continue;
            if (best == null || image.getWidth() > best.getWidth()) best = image;
        }
        return best == null ? "" : best.getUrl();
    }

    private static String descriptionText(Description description) {
        if (description == null) return "";
        String text = description.content();
        if (text == null) return "";
        if (description.type() == Description.Type.HTML) {
            text = text.replaceAll("(?i)<br\\s*/?>", "\n").replaceAll("<[^>]+>", "");
        }
        return text.length() > 6000 ? text.substring(0, 6000) : text;
    }

    static String extractVideoId(String value) {
        if (value == null) return null;
        String text = value.trim();
        if (VIDEO_ID.matcher(text).matches()) return text;
        try {
            Uri uri = Uri.parse(text);
            String v = uri.getQueryParameter("v");
            if (v != null && VIDEO_ID.matcher(v).matches()) return v;
            List<String> segments = uri.getPathSegments();
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();
            if (host.endsWith("youtu.be") && !segments.isEmpty()
                    && VIDEO_ID.matcher(segments.get(0)).matches()) return segments.get(0);
            if (segments.size() >= 2 && (segments.get(0).equals("shorts")
                    || segments.get(0).equals("embed") || segments.get(0).equals("live"))
                    && VIDEO_ID.matcher(segments.get(1)).matches()) return segments.get(1);
        } catch (Exception ignored) { }
        return null;
    }

    private void run(String callbackId, Job job) {
        if (callbackId == null || !CALLBACK_ID.matcher(callbackId).matches()) return;
        try {
            executor.execute(() -> {
                JSONObject payload;
                try {
                    payload = job.run();
                    payload.put("ok", true);
                } catch (Throwable error) {
                    payload = new JSONObject();
                    try {
                        payload.put("ok", false);
                        payload.put("error", safeMessage(error));
                    } catch (JSONException ignored) { }
                }
                deliver(callbackId, payload);
            });
        } catch (RuntimeException rejected) {
            // executor sudah dimatikan (Activity dihancurkan) -> abaikan
        }
    }

    private void deliver(String callbackId, JSONObject payload) {
        final String script = "(function(){var c=window.ryomdYouTubeCatalog;if(c&&typeof c.done==='function')c.done("
                + JSONObject.quote(callbackId) + "," + JSONObject.quote(payload.toString()) + ");})();";
        main.post(() -> {
            if (webView != null && !activity.isFinishing() && !activity.isDestroyed()) {
                webView.evaluateJavascript(script, null);
            }
        });
    }

    public void shutdown() { executor.shutdownNow(); }

    private static String safeMessage(Throwable error) {
        String message = error == null ? "" : error.getMessage();
        if (message == null || message.trim().isEmpty()) {
            message = error == null ? "Kesalahan tidak diketahui" : error.getClass().getSimpleName();
        }
        message = message.replace('\n', ' ').replace('\r', ' ');
        return message.length() > 220 ? message.substring(0, 220) : message;
    }
}
