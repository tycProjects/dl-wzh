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
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage;
import org.schabi.newpipe.extractor.Page;
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
import java.util.HashSet;
import java.util.Set;
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
        final int max = Math.max(1, Math.min(limit <= 0 ? 100 : limit, 100));
        run(callbackId, () -> searchJson(query == null ? "" : query.trim(), max));
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

    // ------------------------------------------------------------------ jobs

    private JSONObject searchJson(String query, int limit) throws Exception {
        if (query.length() < 2) throw new IllegalArgumentException("Kata pencarian terlalu pendek.");
        RyomdYouTubeExtractor.ensureInitialized();
        SearchQueryHandler queryHandler = ServiceList.YouTube.getSearchQHFactory().fromQuery(query);
        SearchInfo info = SearchInfo.getInfo(ServiceList.YouTube, queryHandler);
        JSONArray videos = new JSONArray();
        JSONArray channels = new JSONArray();
        Set<String> seenVideoIds = new HashSet<>();
        Set<String> seenChannelUrls = new HashSet<>();
        appendSearchItems(info.getRelatedItems(), videos, channels, seenVideoIds, seenChannelUrls, limit);

        // NewPipe returns a Page cursor. Walk subsequent pages instead of silently
        // stopping at the first 20 results; keep already collected items on partial errors.
        Page nextPage = info.getNextPage();
        int pageCount = 0;
        while (videos.length() < limit && Page.isValid(nextPage) && pageCount < 12) {
            try {
                InfoItemsPage<InfoItem> page = SearchInfo.getMoreItems(ServiceList.YouTube, queryHandler, nextPage);
                appendSearchItems(page.getItems(), videos, channels, seenVideoIds, seenChannelUrls, limit);
                nextPage = page.getNextPage();
                pageCount++;
            } catch (Exception pageError) {
                android.util.Log.w("RyomdYouTubeCatalog", "Pagination stopped after " + videos.length() + " videos", pageError);
                break;
            }
        }
        JSONObject result = new JSONObject();
        result.put("videos", videos);
        result.put("channels", channels);
        result.put("source", "native");
        result.put("requested", limit);
        result.put("hasMore", Page.isValid(nextPage));
        return result;
    }

    private static void appendSearchItems(List<InfoItem> items, JSONArray videos, JSONArray channels,
                                          Set<String> seenVideoIds, Set<String> seenChannelUrls,
                                          int limit) throws JSONException {
        if (items == null) return;
        for (InfoItem item : items) {
            if (item instanceof StreamInfoItem) {
                if (videos.length() >= limit) continue;
                JSONObject video = streamItem((StreamInfoItem) item, null, null);
                if (video == null) continue;
                String id = video.optString("id", "");
                if (!id.isEmpty() && seenVideoIds.add(id)) videos.put(video);
            } else if (item instanceof ChannelInfoItem && channels.length() < 8) {
                String url = item.getUrl();
                if (url == null || !seenChannelUrls.add(url)) continue;
                JSONObject channel = new JSONObject();
                channel.put("name", item.getName());
                channel.put("url", url);
                channel.put("thumbnail", bestImage(item.getThumbnails()));
                channels.put(channel);
            }
        }
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
