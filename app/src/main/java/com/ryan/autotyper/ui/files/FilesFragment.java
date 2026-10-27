package com.ryan.autotyper.ui.files;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.ryan.autotyper.AppState;
import com.ryan.autotyper.PrefsHelper;
import com.ryan.autotyper.R;
import com.ryan.autotyper.Snack;
import com.ryan.autotyper.ui.AnimKit;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Files tab v3.9 — modern Material 3 redesign + smoothness overhaul.
 *
 * What's new vs v3.4:
 *   • RecyclerView + RecentFilesAdapter for the recent list (was: per-row
 *     inflation into a LinearLayout). With <= 10 items this isn't a huge
 *     memory win, but the adapter's DiffUtil gives smooth insert/remove
 *     animations on filter/sort changes instead of full re-inflate flicker.
 *   • SwipeRefreshLayout wraps the whole scroll view → pull-to-refresh
 *     recent files (also re-runs hydrateFromAppState so the current-file
 *     card stays in sync if AppState changed elsewhere).
 *   • Material 3 filter chips (Tất cả / TXT / MD / CSV / JSON) and sort
 *     chips (Mới nhất / A→Z / Dung lượng) replace the old Spinner.
 *   • Debounced search — 180ms delay after the user stops typing, so we
 *     don't re-filter on every keystroke. The clear-search X button
 *     appears only when there's text.
 *   • onHiddenChanged refresh — when the user returns to the Files tab
 *     after editing in the editor dialog (or any other tab), the recent
 *     list + current-file card refresh automatically.
 *   • First-view-only entrance animation — staggeredEntrance runs on the
 *     very first onViewCreated, not on every tab re-show (the parent
 *     MainActivity already runs a lightweight fade when re-showing).
 *   • Shared static IO executor — avoids creating a new single-thread
 *     executor per Fragment instance (small but real GC/alloc win on
 *     low-end devices).
 */
public class FilesFragment extends Fragment {

    private static final int REQUEST_OPEN = 2001;
    private static final int REQUEST_CREATE = 2002;

    // View refs
    private Button btnChooseFile, btnNewFile, btnEmptyPick;
    private ImageButton btnEdit, btnClearSearch;
    private TextView tvFileName, tvFileMeta;
    private TextView statLines, statWords, statChars;
    private View fileStatusDot, fileIconBadge;
    private View emptyState;
    private TextView heroSubtitle, fileCountBadge, fileTypeChip, previewSnippet;

    // Loading card
    private View loadingCard;
    private ImageView loadingSpinner;
    private TextView loadingTitle, loadingSubtitle;
    private View skeleton1, skeleton2;
    private ImageButton btnCancelLoading;

    // Recent files
    private View recentSection;
    private RecyclerView recentList;
    private Button btnClearRecent;
    private EditText searchRecent;
    private ChipGroup filterChips, sortChips;
    private RecentFilesAdapter adapter;

    // Pull-to-refresh
    private SwipeRefreshLayout swipeRefresh;

    /** Cancellation flag — set true when user taps Cancel during file load. */
    private volatile boolean loadCancelled = false;

    /** Sort modes. */
    private static final int SORT_RECENT = 0;
    private static final int SORT_NAME = 1;
    private static final int SORT_SIZE = 2;
    private int currentSort = SORT_RECENT;

    /** Filter mode (file extension or "ALL"). */
    private static final String FILTER_ALL = "";
    private String currentFilter = FILTER_ALL;

    private String currentSearchQuery = "";

    /**
     * Shared single-thread executor for IO. Reused across fragment
     * instances — avoids creating a new thread pool every time the user
     * switches tabs. Shut down only when the process dies; this is safe
     * because the executor only runs short, fragment-scoped tasks and
     * the cancellation flag prevents stale callbacks from touching the UI.
     */
    private static final ExecutorService SHARED_IO = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /** Search debounce runnable (re-posted on each keystroke). */
    private final Runnable searchDebounce = this::refreshRecentList;

    /** Track whether stats have been animated at least once. */
    private boolean statsAnimated = false;

    /** Track whether this is the first onViewCreated (for entrance anim). */
    private boolean firstViewCreated = true;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_files, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindViews(view);
        wireListeners();
        setupRecentList();

        hydrateFromAppState();
        refreshRecentList();
        updateEmptyState();

        // Staggered entrance — only on the very first creation. Subsequent
        // tab re-shows get a lightweight fade-in from MainActivity.
        if (firstViewCreated) {
            firstViewCreated = false;
            View rootLinear = view.findViewById(R.id.files_root);
            if (rootLinear != null) {
                AnimKit.staggeredEntrance(rootLinear, 45L);
            }
        }
    }

    private void bindViews(@NonNull View view) {
        btnChooseFile = view.findViewById(R.id.btnChooseFile);
        btnNewFile    = view.findViewById(R.id.btnNewFile);
        btnEmptyPick  = view.findViewById(R.id.btnEmptyPick);
        btnEdit       = view.findViewById(R.id.btnEdit);
        btnClearSearch = view.findViewById(R.id.btnClearSearch);
        tvFileName    = view.findViewById(R.id.tvFileName);
        tvFileMeta    = view.findViewById(R.id.tvFileMeta);
        statLines     = view.findViewById(R.id.statLines);
        statWords     = view.findViewById(R.id.statWords);
        statChars     = view.findViewById(R.id.statChars);
        fileStatusDot = view.findViewById(R.id.fileStatusDot);
        fileIconBadge = view.findViewById(R.id.fileIconBadge);
        emptyState    = view.findViewById(R.id.emptyState);
        heroSubtitle  = view.findViewById(R.id.heroSubtitle);
        fileCountBadge = view.findViewById(R.id.fileCountBadge);
        fileTypeChip  = view.findViewById(R.id.fileTypeChip);
        previewSnippet = view.findViewById(R.id.previewSnippet);

        loadingCard     = view.findViewById(R.id.loadingCard);
        loadingSpinner  = view.findViewById(R.id.loadingSpinner);
        loadingTitle    = view.findViewById(R.id.loadingTitle);
        loadingSubtitle = view.findViewById(R.id.loadingSubtitle);
        skeleton1       = view.findViewById(R.id.skeleton1);
        skeleton2       = view.findViewById(R.id.skeleton2);
        btnCancelLoading = view.findViewById(R.id.btnCancelLoading);

        recentSection   = view.findViewById(R.id.recentSection);
        recentList      = view.findViewById(R.id.recentList);
        btnClearRecent  = view.findViewById(R.id.btnClearRecent);
        searchRecent    = view.findViewById(R.id.searchRecent);
        filterChips     = view.findViewById(R.id.filterChips);
        sortChips       = view.findViewById(R.id.sortChips);

        swipeRefresh    = view.findViewById(R.id.swipeRefresh);
    }

    private void wireListeners() {
        btnCancelLoading.setOnClickListener(v -> {
            haptic();
            loadCancelled = true;
            showLoading(false, null, null);
            setBusy(false);
            hydrateFromAppState();
            if (getView() != null) Snack.show(getView(), "🚫 Đã huỷ load file");
        });

        btnChooseFile.setOnClickListener(v -> { haptic(); openFilePicker(); });
        btnNewFile.setOnClickListener(v -> { haptic(); createNewFile(); });
        btnEdit.setOnClickListener(v -> { haptic(); openEditor(); });
        if (btnEmptyPick != null) {
            btnEmptyPick.setOnClickListener(v -> { haptic(); openFilePicker(); });
        }

        btnClearRecent.setOnClickListener(v -> {
            haptic();
            PrefsHelper.clearRecentFiles(requireContext());
            refreshRecentList();
            updateEmptyState();
            if (getView() != null) Snack.show(getView(), "🧹 Đã xoá danh sách file gần đây");
        });

        // Search field — debounced 180ms
        searchRecent.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String q = s != null ? s.toString().trim().toLowerCase(Locale.ROOT) : "";
                currentSearchQuery = q;
                // Toggle clear-search button visibility
                if (btnClearSearch != null) {
                    btnClearSearch.setVisibility(q.isEmpty() ? View.GONE : View.VISIBLE);
                }
                // Debounce
                mainHandler.removeCallbacks(searchDebounce);
                mainHandler.postDelayed(searchDebounce, 180L);
            }
        });
        if (btnClearSearch != null) {
            btnClearSearch.setOnClickListener(v -> {
                haptic();
                searchRecent.setText("");
                currentSearchQuery = "";
                btnClearSearch.setVisibility(View.GONE);
                refreshRecentList();
            });
        }

        // Filter chips
        filterChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            Chip checked = group.findViewById(checkedIds.get(0));
            if (checked == null) return;
            int id = checked.getId();
            if (id == R.id.chipAll) currentFilter = FILTER_ALL;
            else if (id == R.id.chipTxt) currentFilter = "TXT";
            else if (id == R.id.chipMd) currentFilter = "MD";
            else if (id == R.id.chipCsv) currentFilter = "CSV";
            else if (id == R.id.chipJson) currentFilter = "JSON";
            haptic();
            refreshRecentList();
        });

        // Sort chips
        sortChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            Chip checked = group.findViewById(checkedIds.get(0));
            if (checked == null) return;
            int id = checked.getId();
            if (id == R.id.chipSortRecent) currentSort = SORT_RECENT;
            else if (id == R.id.chipSortName) currentSort = SORT_NAME;
            else if (id == R.id.chipSortSize) currentSort = SORT_SIZE;
            haptic();
            refreshRecentList();
        });

        // Swipe-to-refresh
        swipeRefresh.setOnRefreshListener(this::onPullToRefresh);
        // Modern violet accent color for the spinner
        swipeRefresh.setColorSchemeResources(
                R.color.brand_violet, R.color.brand_pink, R.color.brand_cyan);
    }

    /** Pull-to-refresh: re-hydrate from AppState and refresh recent list. */
    private void onPullToRefresh() {
        hydrateFromAppState();
        refreshRecentList();
        updateEmptyState();
        // Stop the spinner after a short delay so the user sees the refresh happen
        swipeRefresh.postDelayed(() -> {
            if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
            if (getView() != null) Snack.show(getView(), "Đã làm mới");
        }, 600L);
    }

    private void setupRecentList() {
        adapter = new RecentFilesAdapter();
        adapter.setHasStableIds(true);
        adapter.setCallbacks(new RecentFilesAdapter.Callbacks() {
            @Override public void onOpenFile(@NonNull PrefsHelper.RecentFile rf) { openRecent(rf); }
            @Override public void onEditFile(@NonNull PrefsHelper.RecentFile rf) { editRecent(rf); }
            @Override public void onForgetFile(@NonNull PrefsHelper.RecentFile rf) { forgetRecent(rf); }
            @Override public void onLongPress(@NonNull PrefsHelper.RecentFile rf, @NonNull View anchor) {
                haptic();
                showRecentContextMenu(rf);
            }
        });
        recentList.setLayoutManager(new LinearLayoutManager(requireContext()));
        recentList.setHasFixedSize(false);
        recentList.setAdapter(adapter);
        // Run a one-shot layout animation on first items
        recentList.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(
                requireContext(), R.anim.recycler_layout_anim));
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshRecentList();
        refreshStatsFromAppState();
        updateEmptyState();
    }

    /**
     * Called by the host Activity when this fragment is shown again after
     * being hidden (show/hide fragment pattern). Used to refresh the recent
     * list + current file card so the user sees the latest state when they
     * come back to the Files tab.
     */
    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && getContext() != null) {
            hydrateFromAppState();
            refreshRecentList();
            updateEmptyState();
        }
    }

    // ============================================================
    // Recent file actions
    // ============================================================

    private void openRecent(@NonNull PrefsHelper.RecentFile rf) {
        haptic();
        Uri uri = Uri.parse(rf.uri);
        if (uri == null) return;
        try {
            requireContext().getContentResolver().takePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                            | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } catch (SecurityException ignored) { /* may still be readable */ }
        loadFile(uri);
    }

    private void editRecent(@NonNull PrefsHelper.RecentFile rf) {
        haptic();
        Uri uri = Uri.parse(rf.uri);
        if (uri == null) return;
        AppState s = AppState.get();
        boolean isActive = s.getFileUri() != null
                && s.getFileUri().toString().equals(rf.uri);
        if (!isActive) {
            try {
                requireContext().getContentResolver().takePersistableUriPermission(uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            } catch (SecurityException ignored) { }
            loadFileAndThen(uri, this::openEditor);
        } else {
            openEditor();
        }
    }

    private void forgetRecent(@NonNull PrefsHelper.RecentFile rf) {
        haptic();
        PrefsHelper.forgetRecentFile(requireContext(), rf.uri);
        refreshRecentList();
        updateEmptyState();
        if (getView() != null) Snack.show(getView(), "Đã bỏ " + rf.name);
    }

    // ============================================================
    // Editor dialog
    // ============================================================

    private void openEditor() {
        AppState s = AppState.get();
        if (!s.hasFile()) {
            if (getView() != null) Snack.show(getView(), "Chọn file trước khi chỉnh sửa");
            return;
        }
        EditorDialogFragment dialog = EditorDialogFragment.newInstance(s.getFileName());
        dialog.show(getChildFragmentManager(), "editor_dialog");
    }

    // ============================================================
    // Recent Files — filter + sort + render
    // ============================================================

    private void refreshRecentList() {
        if (getContext() == null) return;
        List<PrefsHelper.RecentFile> recent = new ArrayList<>(PrefsHelper.getRecentFiles(requireContext()));
        int totalRecent = recent.size();

        // Update hero subtitle + count badge (use unfiltered count for badge)
        if (fileCountBadge != null) {
            fileCountBadge.setText(String.valueOf(totalRecent));
        }
        if (heroSubtitle != null) {
            if (totalRecent == 0) {
                heroSubtitle.setText("Chọn · Tạo · Xem · Chỉnh sửa");
            } else {
                heroSubtitle.setText(totalRecent + " file gần đây · bấm để mở nhanh");
            }
        }
        // Hide section if no recents at all
        if (totalRecent == 0) {
            recentSection.setVisibility(View.GONE);
            return;
        }
        recentSection.setVisibility(View.VISIBLE);

        // === Apply extension filter ===
        if (!FILTER_ALL.equals(currentFilter)) {
            List<PrefsHelper.RecentFile> filtered = new ArrayList<>();
            for (PrefsHelper.RecentFile rf : recent) {
                String ext = extractExtension(rf.name);
                if (currentFilter.equals(ext)) filtered.add(rf);
            }
            recent = filtered;
        }

        // === Apply search filter ===
        if (!currentSearchQuery.isEmpty()) {
            List<PrefsHelper.RecentFile> filtered = new ArrayList<>();
            for (PrefsHelper.RecentFile rf : recent) {
                if (rf.name != null && rf.name.toLowerCase(Locale.ROOT).contains(currentSearchQuery)) {
                    filtered.add(rf);
                }
            }
            recent = filtered;
        }

        // === Apply sort ===
        switch (currentSort) {
            case SORT_NAME:
                Collections.sort(recent, (a, b) -> {
                    String an = a.name != null ? a.name.toLowerCase(Locale.ROOT) : "";
                    String bn = b.name != null ? b.name.toLowerCase(Locale.ROOT) : "";
                    return an.compareTo(bn);
                });
                break;
            case SORT_SIZE:
                Collections.sort(recent, (a, b) -> Long.compare(b.sizeBytes, a.sizeBytes));
                break;
            case SORT_RECENT:
            default:
                // Already sorted newest first (pushRecentFile inserts at top)
                break;
        }

        // === Active URI for highlighting ===
        AppState s = AppState.get();
        String activeUri = s.getFileUri() != null ? s.getFileUri().toString() : "";

        // === Submit to adapter (with empty placeholder if no matches) ===
        if (recent.isEmpty()) {
            String msg = !currentSearchQuery.isEmpty()
                    ? "Không tìm thấy file khớp \"" + currentSearchQuery + "\""
                    : "Không có file " + currentFilter;
            adapter.showEmpty(msg);
        } else {
            adapter.submit(recent, activeUri, null);
        }
    }

    /** Extract file extension uppercase ("TXT", "CSV", "MD", ...) or "" if none. */
    private static String extractExtension(String name) {
        if (name == null) return "";
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) return "";
        return name.substring(dot + 1).toUpperCase(Locale.ROOT);
    }

    private void showRecentContextMenu(PrefsHelper.RecentFile rf) {
        BottomSheetDialog sheet = new BottomSheetDialog(requireContext());
        View sheetView = getLayoutInflater().inflate(R.layout.sheet_recent_context, null);

        View btnRename = sheetView.findViewById(R.id.action_rename);
        View btnShare  = sheetView.findViewById(R.id.action_share);
        View btnDelete = sheetView.findViewById(R.id.action_delete);

        btnRename.setOnClickListener(v -> {
            sheet.dismiss();
            showRenameDialog(rf);
        });

        btnShare.setOnClickListener(v -> {
            sheet.dismiss();
            try {
                Uri uri = Uri.parse(rf.uri);
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("text/plain");
                share.putExtra(Intent.EXTRA_STREAM, uri);
                share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(share, "Chia sẻ " + rf.name));
            } catch (Exception e) {
                if (getView() != null) {
                    Snack.show(getView(), "Không chia sẻ được: " + e.getMessage());
                }
            }
        });

        btnDelete.setOnClickListener(v -> {
            sheet.dismiss();
            showDeleteConfirmDialog(rf);
        });

        sheet.setContentView(sheetView);
        sheet.show();
    }

    /** Rename a recent file: shows a dialog with the current name, then writes the new name
     *  to SAF (DocumentsContract.renameDocument) and updates the recent entry. */
    private void showRenameDialog(PrefsHelper.RecentFile rf) {
        final Uri uri = Uri.parse(rf.uri);
        if (uri == null) {
            if (getView() != null) Snack.show(getView(), "Không lấy được file");
            return;
        }
        final android.app.AlertDialog.Builder ab = new android.app.AlertDialog.Builder(requireContext());
        ab.setTitle("Đổi tên file");
        ab.setMessage("Tên mới (giữ đuôi .txt):");

        final android.widget.EditText input = new android.widget.EditText(requireContext());
        String currentName = rf.name != null ? rf.name : "file.txt";
        input.setText(currentName);
        input.setSelection(currentName.length());
        ab.setView(input);

        ab.setPositiveButton("Đổi tên", (d, w) -> {
            String newName = input.getText().toString().trim();
            if (newName.isEmpty()) {
                if (getView() != null) Snack.show(getView(), "Tên không được để trống");
                return;
            }
            renameFile(uri, newName, rf);
        });
        ab.setNegativeButton("Huỷ", null);
        ab.show();
    }

    /** Rename via DocumentsContract.renameDocument (requires API 19+). */
    private void renameFile(Uri uri, String newName, PrefsHelper.RecentFile rf) {
        SHARED_IO.execute(() -> {
            String err = null;
            Uri newUri = null;
            try {
                newUri = android.provider.DocumentsContract.renameDocument(
                        requireContext().getContentResolver(), uri, newName);
            } catch (Exception e) {
                err = e.getMessage() != null ? e.getMessage() : "Không đổi tên được";
            }
            final String error = err;
            final Uri finalNewUri = newUri;
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (error != null) {
                        if (getView() != null) Snack.show(getView(), "❌ " + error);
                        return;
                    }
                    PrefsHelper.forgetRecentFile(requireContext(), rf.uri);
                    String newUriStr = finalNewUri != null ? finalNewUri.toString() : rf.uri;
                    PrefsHelper.pushRecentFile(requireContext(), Uri.parse(newUriStr), newName);

                    AppState s = AppState.get();
                    if (s.getFileUri() != null && s.getFileUri().toString().equals(rf.uri)) {
                        if (finalNewUri != null) {
                            loadFile(finalNewUri);
                        }
                    }
                    refreshRecentList();
                    updateEmptyState();
                    if (getView() != null) {
                        Snack.show(getView(), "✅ Đã đổi tên thành " + newName);
                    }
                });
            }
        });
    }

    /** Show delete confirmation dialog. */
    private void showDeleteConfirmDialog(PrefsHelper.RecentFile rf) {
        View sheetView = getLayoutInflater().inflate(R.layout.dialog_delete_confirm, null);
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(requireContext())
                .setView(sheetView)
                .create();

        TextView tvName = sheetView.findViewById(R.id.delFileName);
        tvName.setText(rf.name != null ? rf.name : "(file)");

        sheetView.findViewById(R.id.btnDeleteFromList).setOnClickListener(v -> {
            dialog.dismiss();
            PrefsHelper.forgetRecentFile(requireContext(), rf.uri);
            refreshRecentList();
            updateEmptyState();
            if (getView() != null) {
                Snack.show(getView(), "Đã xoá " + rf.name + " khỏi danh sách");
            }
        });

        sheetView.findViewById(R.id.btnDeletePermanently).setOnClickListener(v -> {
            dialog.dismiss();
            deleteFilePermanently(rf);
        });

        sheetView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    /** Delete the actual file via DocumentsContract.deleteDocument(). */
    private void deleteFilePermanently(PrefsHelper.RecentFile rf) {
        final Uri uri = Uri.parse(rf.uri);
        if (uri == null) {
            if (getView() != null) Snack.show(getView(), "Không lấy được file");
            return;
        }
        SHARED_IO.execute(() -> {
            String err = null;
            try {
                boolean ok = android.provider.DocumentsContract.deleteDocument(
                        requireContext().getContentResolver(), uri);
                if (!ok) err = "Không xoá được file";
            } catch (Exception e) {
                err = e.getMessage() != null ? e.getMessage() : "Không xoá được file";
            }
            final String error = err;
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (error != null) {
                        if (getView() != null) Snack.show(getView(), "❌ " + error);
                        return;
                    }
                    PrefsHelper.forgetRecentFile(requireContext(), rf.uri);
                    AppState s = AppState.get();
                    if (s.getFileUri() != null && s.getFileUri().toString().equals(rf.uri)) {
                        s.setLines(new ArrayList<>(), "", null);
                        s.setEditedText("");
                    }
                    refreshRecentList();
                    updateEmptyState();
                    hydrateFromAppState();
                    if (getView() != null) {
                        Snack.show(getView(), "🗑 Đã xoá file vĩnh viễn");
                    }
                });
            }
        });
    }

    // ============================================================
    // Empty state
    // ============================================================

    private void updateEmptyState() {
        AppState s = AppState.get();
        boolean hasRecent = !PrefsHelper.getRecentFiles(requireContext()).isEmpty();
        boolean empty = !s.hasFile() && !hasRecent;
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty && emptyState != null) {
            View illu = emptyState.findViewWithTag("empty_illu");
            if (illu != null) {
                Animation floating = AnimationUtils.loadAnimation(requireContext(), R.anim.floating);
                illu.startAnimation(floating);
            }
        }
    }

    // ============================================================
    // Pick / New
    // ============================================================

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_OPEN);
    }

    private void createNewFile() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, "untitled.txt");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_CREATE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != android.app.Activity.RESULT_OK || data == null) return;
        Uri uri = data.getData();
        if (uri == null) return;

        try {
            requireContext().getContentResolver().takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } catch (SecurityException ignored) { /* not granted — fallback to read-only */ }

        if (requestCode == REQUEST_OPEN) {
            loadFile(uri);
        } else if (requestCode == REQUEST_CREATE) {
            originalTextSnapshot = "";
            AppState.get().setLines(new ArrayList<>(), "untitled.txt", uri);
            AppState.get().setEditedText("");
            showFileInfo("untitled.txt", 0, 0, true);
            refreshStatsFromAppState();
            PrefsHelper.pushRecentFile(requireContext(), uri, "untitled.txt");
            refreshRecentList();
            updateEmptyState();
            if (getView() != null) {
                Snack.show(getView(), "✨ Đã tạo file mới — gõ nội dung trong editor");
            }
            openEditor();
        }
    }

    // ============================================================
    // Load
    // ============================================================

    private String originalTextSnapshot = "";

    private void loadFile(final Uri uri) {
        loadFileAndThen(uri, null);
    }

    private interface AfterLoad { void run(); }

    private void loadFileAndThen(final Uri uri, @Nullable AfterLoad after) {
        loadCancelled = false;
        showLoading(true, "Đang đọc file…", "Vui lòng đợi");
        tvFileName.setText("Đang đọc…");
        tvFileMeta.setText("");
        setBusy(true);

        SHARED_IO.execute(() -> {
            final List<String> loaded = new ArrayList<>();
            String error = null;
            long fileSize = 0;
            try {
                try {
                    android.database.Cursor cursor = requireContext().getContentResolver()
                            .query(uri, new String[]{android.provider.OpenableColumns.SIZE,
                                    android.provider.OpenableColumns.DISPLAY_NAME},
                                    null, null, null);
                    if (cursor != null) {
                        if (cursor.moveToFirst()) {
                            int idx = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE);
                            if (idx >= 0 && !cursor.isNull(idx)) {
                                fileSize = cursor.getLong(idx);
                            }
                        }
                        cursor.close();
                    }
                } catch (Exception ignored) { /* size optional */ }

                InputStream is = requireContext().getContentResolver().openInputStream(uri);
                if (is == null) {
                    error = "Không mở được file";
                } else {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is), 64 * 1024);
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (loadCancelled) {
                            reader.close();
                            is.close();
                            return;
                        }
                        loaded.add(line);
                    }
                    reader.close();
                    is.close();
                }
            } catch (OutOfMemoryError oom) {
                error = "File quá lớn, hết bộ nhớ";
                loaded.clear();
            } catch (Exception e) {
                if (loadCancelled) return;
                error = e.getMessage() != null ? e.getMessage() : "Lỗi đọc file";
            }

            if (loadCancelled) return;

            final String err = error;
            final long finalFileSize = fileSize;
            mainHandler.post(() -> {
                setBusy(false);
                showLoading(false, null, null);
                if (err != null) {
                    if (getView() != null) Snack.show(getView(), "❌ " + err);
                    hydrateFromAppState();
                    return;
                }
                String displayName = extractDisplayName(uri);
                originalTextSnapshot = joinLines(loaded);
                AppState.get().setLines(loaded, displayName, uri);
                AppState.get().setEditedText(originalTextSnapshot);
                int charCount = originalTextSnapshot.length();
                showFileInfo(displayName, loaded.size(), charCount, true);
                animateStats(loaded.size(), countWords(originalTextSnapshot), charCount);
                PrefsHelper.pushRecentFile(requireContext(), uri, displayName, finalFileSize);
                refreshRecentList();
                updateEmptyState();
                if (getView() != null) {
                    Snack.show(getView(), "✅ Đã load " + loaded.size() + " dòng");
                }
                if (after != null) after.run();
            });
        });
    }

    /** Toggle the file loading card with spinner + skeleton shimmer. */
    private void showLoading(boolean show, String title, String subtitle) {
        if (show) {
            loadingCard.setVisibility(View.VISIBLE);
            if (title != null) loadingTitle.setText(title);
            if (subtitle != null) loadingSubtitle.setText(subtitle);
            Animation rotate = AnimationUtils.loadAnimation(requireContext(), R.anim.rotate_progress);
            loadingSpinner.startAnimation(rotate);
            Animation sk = AnimationUtils.loadAnimation(requireContext(), R.anim.skeleton_shimmer);
            skeleton1.startAnimation(sk);
            Animation sk2 = AnimationUtils.loadAnimation(requireContext(), R.anim.skeleton_shimmer);
            sk2.setStartOffset(180);
            skeleton2.startAnimation(sk2);
            loadingCard.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.pop_in));
        } else {
            loadingCard.setVisibility(View.GONE);
            loadingSpinner.clearAnimation();
            skeleton1.clearAnimation();
            skeleton2.clearAnimation();
        }
    }

    // ============================================================
    // Helpers
    // ============================================================

    private void hydrateFromAppState() {
        AppState s = AppState.get();
        if (s.hasFile()) {
            int chars = s.getEditedText().length();
            int lines = s.getLineCount();
            showFileInfo(s.getFileName(), lines, chars, true);
        } else {
            showFileInfo(getString(R.string.file_none), 0, 0, false);
        }
        refreshStatsFromAppState();
    }

    private void refreshStatsFromAppState() {
        AppState s = AppState.get();
        String text = s.getEditedText();
        int lines = text.isEmpty() ? 0 : text.split("\n", -1).length;
        int chars = text.length();
        int words = countWords(text);
        if (statsAnimated) {
            statLines.setText(String.valueOf(lines));
            statWords.setText(String.valueOf(words));
            statChars.setText(String.valueOf(chars));
        } else {
            animateStats(lines, words, chars);
            statsAnimated = true;
        }
    }

    private void animateStats(int lines, int words, int chars) {
        FilesUi.animateCount(statLines, lines);
        FilesUi.animateCount(statWords, words);
        FilesUi.animateCount(statChars, chars);
    }

    private void setBusy(boolean busy) {
        btnChooseFile.setEnabled(!busy);
        btnNewFile.setEnabled(!busy);
    }

    private void showFileInfo(String name, int lines, int chars, boolean hasFile) {
        tvFileName.setText(name);
        if (hasFile) {
            tvFileMeta.setText(lines + " dòng · " + chars + " ký tự");
            tvFileName.setTextColor(getColor(R.color.text_primary));
            fileStatusDot.setBackgroundResource(R.drawable.bg_status_ok);
            fileStatusDot.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFF34D399));
            btnEdit.setEnabled(true);
            btnEdit.setAlpha(1f);
            String ext = extractExtension(name);
            if (fileTypeChip != null) {
                fileTypeChip.setText(ext);
                fileTypeChip.setVisibility(ext.isEmpty() ? View.GONE : View.VISIBLE);
            }
            // Tint the file icon badge with the file-type color
            if (fileIconBadge != null) {
                int tint = colorForExt(ext);
                int alpha = (tint >>> 24) == 0xFF ? 0x1A000000 | (tint & 0xFFFFFF) : 0x1A8B5CF6;
                fileIconBadge.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(alpha));
            }
            if (previewSnippet != null) {
                String text = AppState.get().getEditedText();
                String preview = buildPreview(text, 3);
                if (preview.isEmpty()) {
                    previewSnippet.setVisibility(View.GONE);
                } else {
                    previewSnippet.setText(preview);
                    previewSnippet.setVisibility(View.VISIBLE);
                }
            }
        } else {
            tvFileMeta.setText("—");
            tvFileName.setTextColor(getColor(R.color.text_muted));
            fileStatusDot.setBackgroundResource(R.drawable.bg_status_warn);
            fileStatusDot.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFFF59E0B));
            btnEdit.setEnabled(false);
            btnEdit.setAlpha(0.4f);
            if (fileTypeChip != null) fileTypeChip.setVisibility(View.GONE);
            if (previewSnippet != null) previewSnippet.setVisibility(View.GONE);
            if (fileIconBadge != null) {
                fileIconBadge.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(0x1A8B5CF6));
            }
        }
    }

    private static int colorForExt(String ext) {
        if (ext == null) return 0xFF8B5CF6;
        switch (ext) {
            case "TXT": return 0xFF06B6D4;
            case "MD":  return 0xFF8B5CF6;
            case "CSV": return 0xFF10B981;
            case "JSON": return 0xFFF59E0B;
            case "XML": return 0xFFEC4899;
            default:    return 0xFF8B5CF6;
        }
    }

    /** Build a 3-line preview from the edited text. */
    private String buildPreview(String text, int maxLines) {
        if (text == null || text.isEmpty()) return "";
        String[] all = text.split("\n", -1);
        StringBuilder sb = new StringBuilder();
        int shown = 0;
        for (String line : all) {
            if (line.trim().isEmpty()) continue;
            if (shown > 0) sb.append('\n');
            sb.append(line.length() > 60 ? line.substring(0, 60) + "…" : line);
            shown++;
            if (shown >= maxLines) break;
        }
        return sb.toString();
    }

    private int getColor(int res) { return getResources().getColor(res); }

    private static int countWords(String text) {
        if (text == null || text.isEmpty()) return 0;
        String trimmed = text.trim();
        if (trimmed.isEmpty()) return 0;
        return trimmed.split("\\s+").length;
    }

    private String extractDisplayName(Uri uri) {
        String name = uri.getLastPathSegment();
        if (name != null && name.contains(":")) {
            name = name.substring(name.lastIndexOf(":") + 1);
        }
        return name != null ? name : "file.txt";
    }

    private static String joinLines(List<String> ls) {
        if (ls == null || ls.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ls.size(); i++) {
            if (i > 0) sb.append('\n');
            sb.append(ls.get(i));
        }
        return sb.toString();
    }

    private void haptic() {
        if (getView() == null) return;
        getView().performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY,
                android.view.HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Cancel any pending search debounce so it doesn't fire after destroy.
        mainHandler.removeCallbacks(searchDebounce);
        // NOTE: SHARED_IO is intentionally NOT shut down here — it's shared
        // across fragment instances. It will be cleaned up when the process
        // dies. The loadCancelled flag prevents stale file loads from
        // touching the destroyed UI.
    }
}
