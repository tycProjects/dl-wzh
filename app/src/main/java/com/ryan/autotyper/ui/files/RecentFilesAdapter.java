package com.ryan.autotyper.ui.files;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.ryan.autotyper.PrefsHelper;
import com.ryan.autotyper.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for the Files tab's "Recent Files" list.
 *
 * Design notes:
 *   • Stable IDs (uri.hashCode()) so RecyclerView can preserve scroll position
 *     and avoid unnecessary rebinds when the list is reordered/filtered.
 *   • DiffUtil dispatch updates granularly (insert/remove/change) instead of
 *     notifyDataSetChanged(), so individual rows animate in/out cleanly.
 *   • File-type icon tinted by extension (TXT/MD/CSV/JSON/XML/default) so
 *     the user can scan the list at a glance.
 *   • Per-row callbacks (open / edit / forget / long-press) so the fragment
 *     stays in charge of file loading + dialog management.
 *   • The "no matches" empty row is a separate ViewType so it doesn't get
 *     confused with a real file row.
 */
public class RecentFilesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_FILE = 1;
    private static final int TYPE_EMPTY = 2;

    public interface Callbacks {
        void onOpenFile(@NonNull PrefsHelper.RecentFile rf);
        void onEditFile(@NonNull PrefsHelper.RecentFile rf);
        void onForgetFile(@NonNull PrefsHelper.RecentFile rf);
        void onLongPress(@NonNull PrefsHelper.RecentFile rf, @NonNull View anchor);
    }

    private final List<PrefsHelper.RecentFile> items = new ArrayList<>();
    private String activeUriString = "";
    private String emptyMessage = "";

    @Nullable
    private Callbacks callbacks;

    public void setCallbacks(@Nullable Callbacks cb) { this.callbacks = cb; }

    public void submit(@NonNull List<PrefsHelper.RecentFile> newItems,
                       @Nullable String activeUriString,
                       @Nullable String emptyMessage) {
        // DiffUtil gives us smooth insert/remove/change animations for free.
        // Snapshot old list, swap, dispatch diff on a background thread (cheap
        // for <= 10 items, but still off the UI thread to be safe).
        final List<PrefsHelper.RecentFile> old = new ArrayList<>(items);
        final String oldActive = this.activeUriString;
        final String oldEmpty = this.emptyMessage;
        this.items.clear();
        this.items.addAll(newItems);
        this.activeUriString = activeUriString != null ? activeUriString : "";
        this.emptyMessage = emptyMessage != null ? emptyMessage : "";

        // If only the empty-message changed (rare), just notify item 0.
        boolean structureSame = old.size() == items.size();
        boolean activeSame = oldActive.equals(this.activeUriString);
        boolean emptySame = oldEmpty.equals(this.emptyMessage);

        if (!structureSame || !activeSame || !emptySame) {
            // Cheap path: dispatch diff in-place (lists are tiny).
            DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                @Override
                public int getOldListSize() { return old.size(); }
                @Override
                public int getNewListSize() { return items.size(); }
                @Override
                public boolean areItemsTheSame(int o, int n) {
                    return old.get(o).uri.equals(items.get(n).uri);
                }
                @Override
                public boolean areContentsTheSame(int o, int n) {
                    PrefsHelper.RecentFile a = old.get(o);
                    PrefsHelper.RecentFile b = items.get(n);
                    return a.uri.equals(b.uri)
                            && stringEquals(a.name, b.name)
                            && a.sizeBytes == b.sizeBytes
                            && a.openedAt == b.openedAt;
                }
            });
            diff.dispatchUpdatesTo(this);
        }
    }

    private static boolean stringEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    @Override
    public int getItemViewType(int position) {
        // If the list contains a single placeholder entry with an empty uri,
        // render the empty row instead of a file row.
        if (items.size() == 1 && items.get(0).uri.isEmpty()) {
            return TYPE_EMPTY;
        }
        return TYPE_FILE;
    }

    @Override
    public long getItemId(int position) {
        if (getItemViewType(position) == TYPE_EMPTY) return -1L;
        return items.get(position).uri.hashCode() & 0xFFFFFFFFL;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_EMPTY) {
            View v = inf.inflate(R.layout.item_recent_empty, parent, false);
            return new EmptyVH(v);
        }
        View v = inf.inflate(R.layout.item_recent_file, parent, false);
        return new RowVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof EmptyVH) {
            ((EmptyVH) holder).bind(emptyMessage);
            return;
        }
        RowVH vh = (RowVH) holder;
        PrefsHelper.RecentFile rf = items.get(position);
        boolean isActive = rf.uri.equals(activeUriString);
        vh.bind(rf, isActive, callbacks);
    }

    @Override
    public int getItemCount() {
        // If we're displaying the empty row (1 item with empty uri), it counts as 1.
        return items.size();
    }

    /** Helper: push an empty placeholder row so the adapter shows the
     *  "no matches" message inside the RecyclerView itself. */
    public void showEmpty(@NonNull String message) {
        items.clear();
        items.add(new PrefsHelper.RecentFile("", "", 0L, 0L));
        emptyMessage = message;
        notifyDataSetChanged();
    }

    // ============================================================
    // ViewHolders
    // ============================================================

    static final class RowVH extends RecyclerView.ViewHolder {
        final View root;
        final View fileBadgeBg;
        final ImageView fileBadgeIcon;
        final TextView fileBadgeText;
        final TextView tvName;
        final TextView tvMeta;
        final TextView tvSizeChip;
        final TextView tvTypeChip;
        final View activeDot;
        final ImageButton btnEdit;
        final ImageButton btnMore;

        RowVH(@NonNull View v) {
            super(v);
            root = v;
            fileBadgeBg = v.findViewById(R.id.fileBadgeBg);
            fileBadgeIcon = v.findViewById(R.id.fileBadgeIcon);
            fileBadgeText = v.findViewById(R.id.fileBadgeText);
            tvName = v.findViewById(R.id.recentName);
            tvMeta = v.findViewById(R.id.recentMeta);
            tvSizeChip = v.findViewById(R.id.recentSizeChip);
            tvTypeChip = v.findViewById(R.id.recentTypeChip);
            activeDot = v.findViewById(R.id.recentActiveDot);
            btnEdit = v.findViewById(R.id.btnEdit);
            btnMore = v.findViewById(R.id.btnMore);
        }

        void bind(@NonNull PrefsHelper.RecentFile rf, boolean isActive,
                  @Nullable Callbacks cb) {
            Context ctx = itemView.getContext();

            // Active state — card border + dot + alpha
            itemView.setBackgroundResource(isActive
                    ? R.drawable.bg_recent_card_active_v2
                    : R.drawable.bg_recent_card_v2);
            if (activeDot != null) activeDot.setVisibility(isActive ? View.VISIBLE : View.GONE);
            itemView.setAlpha(isActive ? 1f : 0.97f);

            // Name + meta
            String displayName = (rf.name != null && !rf.name.isEmpty()) ? rf.name : "(file)";
            tvName.setText(displayName);
            tvMeta.setText(formatRelativeTime(ctx, rf.openedAt));

            // File-type badge tint + label
            String ext = extractExtension(rf.name);
            int tint = colorForExt(ctx, ext);
            tintBadge(fileBadgeBg, tint);
            if (fileBadgeText != null) {
                if (ext.isEmpty()) {
                    fileBadgeText.setVisibility(View.GONE);
                } else {
                    fileBadgeText.setText(ext);
                    fileBadgeText.setVisibility(View.VISIBLE);
                }
            }

            // Size chip
            if (rf.sizeBytes > 0) {
                tvSizeChip.setText(formatFileSize(rf.sizeBytes));
                tvSizeChip.setVisibility(View.VISIBLE);
            } else {
                tvSizeChip.setVisibility(View.GONE);
            }

            // Type chip
            if (ext.isEmpty()) {
                tvTypeChip.setVisibility(View.GONE);
            } else {
                tvTypeChip.setText(ext);
                tvTypeChip.setVisibility(View.VISIBLE);
            }

            // Click handlers
            itemView.setOnClickListener(v -> {
                if (cb != null) cb.onOpenFile(rf);
            });
            itemView.setOnLongClickListener(v -> {
                if (cb != null) {
                    cb.onLongPress(rf, v);
                    return true;
                }
                return false;
            });
            if (btnEdit != null) {
                btnEdit.setOnClickListener(v -> {
                    if (cb != null) cb.onEditFile(rf);
                });
            }
            if (btnMore != null) {
                btnMore.setOnClickListener(v -> {
                    if (cb != null) cb.onLongPress(rf, v);
                });
            }
        }

        private static String extractExtension(String name) {
            if (name == null) return "";
            int dot = name.lastIndexOf('.');
            if (dot < 0 || dot == name.length() - 1) return "";
            return name.substring(dot + 1).toUpperCase(Locale.ROOT);
        }

        private static int colorForExt(Context ctx, String ext) {
            int res;
            switch (ext) {
                case "TXT": res = R.color.brand_cyan; break;
                case "MD":  res = R.color.brand_violet; break;
                case "CSV": res = R.color.brand_emerald; break;
                case "JSON": res = R.color.brand_amber; break;
                case "XML": res = R.color.brand_pink; break;
                case "LOG": res = R.color.text_muted; break;
                default:    res = R.color.brand_violet; break;
            }
            return ContextCompat.getColor(ctx, res);
        }

        private static void tintBadge(View badge, int color) {
            // Darken the color slightly for the badge background, then set as tint.
            Drawable d = badge.getBackground();
            if (d != null) {
                int bg = darken(color, 0.55f);
                d.setColorFilter(bg, PorterDuff.Mode.SRC_IN);
                d.invalidateSelf();
            }
        }

        private static int darken(int color, float factor) {
            int r = (int) (Color.red(color) * factor);
            int g = (int) (Color.green(color) * factor);
            int b = (int) (Color.blue(color) * factor);
            return Color.argb(255, Math.max(0, Math.min(255, r)),
                    Math.max(0, Math.min(255, g)),
                    Math.max(0, Math.min(255, b)));
        }

        private static String formatFileSize(long bytes) {
            if (bytes < 1024) return bytes + " B";
            if (bytes < 1024 * 1024) return String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0);
            return String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0));
        }

        private static String formatRelativeTime(Context ctx, long ts) {
            if (ts <= 0) return "";
            long now = System.currentTimeMillis();
            long diff = now - ts;
            if (diff < 60_000L) return "Vừa xong";
            if (diff < 3_600_000L) return (diff / 60_000L) + " phút trước";
            if (diff < 86_400_000L) return (diff / 3_600_000L) + " giờ trước";
            if (diff < 7L * 86_400_000L) return (diff / 86_400_000L) + " ngày trước";
            // Fallback to absolute date
            return DateUtils.formatDateTime(ctx, ts,
                    DateUtils.FORMAT_ABBREV_MONTH | DateUtils.FORMAT_SHOW_DATE);
        }
    }

    static final class EmptyVH extends RecyclerView.ViewHolder {
        final TextView tvMessage;
        EmptyVH(@NonNull View v) {
            super(v);
            tvMessage = v.findViewById(R.id.emptyMessage);
        }
        void bind(@NonNull String message) {
            if (tvMessage != null) tvMessage.setText(message);
        }
    }
}
