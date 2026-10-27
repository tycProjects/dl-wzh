package com.ryan.autotyper.ui.files;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.ryan.autotyper.AppState;
import com.ryan.autotyper.Snack;
import com.ryan.autotyper.R;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Full-screen editor dialog opened from the Files tab.
 *
 *   • Title = file name
 *   • Search bar with prev/next nav + highlight
 *   • Live stats: dòng / từ / ký tự
 *   • Save button writes back to the persisted Uri
 *   • Cancel button discards changes (does NOT touch AppState's edited text)
 *
 * While editing, every change is pushed into AppState so AutoTyperService
 * uses the latest content when the user hits START in the Home tab.
 */
public class EditorDialogFragment extends DialogFragment {

    private static final String ARG_FILE_NAME = "file_name";

    private EditText editor, searchBox;
    private TextView tvTitle, tvStats, searchCount;
    private Button btnSearchPrev, btnSearchNext, btnSave, btnCancel;

    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    /** Search state */
    private final List<Integer> matchPositions = new ArrayList<>();
    private int currentMatchIdx = -1;
    private boolean suppressWatcher = false;

    /** Snapshot of edited text when dialog opened — used to detect "dirty" on save. */
    private String originalText = "";

    public static EditorDialogFragment newInstance(@NonNull String fileName) {
        EditorDialogFragment f = new EditorDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_FILE_NAME, fileName);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Full-screen dialog
        setStyle(STYLE_NORMAL, R.style.Theme_YeuEmMoiVuTru_EditorDialog);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Dialog d = getDialog();
        if (d != null && d.getWindow() != null) {
            d.getWindow().setWindowAnimations(R.style.EditorDialogAnimation);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_editor, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String fileName = "file.txt";
        if (getArguments() != null) fileName = getArguments().getString(ARG_FILE_NAME, "file.txt");

        tvTitle       = view.findViewById(R.id.editorTitle);
        editor        = view.findViewById(R.id.editor);
        searchBox     = view.findViewById(R.id.searchBox);
        searchCount   = view.findViewById(R.id.searchCount);
        btnSearchPrev = view.findViewById(R.id.btnSearchPrev);
        btnSearchNext = view.findViewById(R.id.btnSearchNext);
        btnSave       = view.findViewById(R.id.btnSave);
        btnCancel     = view.findViewById(R.id.btnCancel);
        tvStats       = view.findViewById(R.id.editorStats);

        tvTitle.setText(fileName);

        // Seed editor with current edited text from AppState
        String text = AppState.get().getEditedText();
        originalText = text;
        suppressWatcher = true;
        editor.setText(text);
        suppressWatcher = false;
        updateStats(text);

        editor.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (suppressWatcher) return;
                String t = s != null ? s.toString() : "";
                AppState.get().setEditedText(t);
                updateStats(t);
                if (searchBox.getText().length() > 0) {
                    runSearch(searchBox.getText().toString());
                }
            }
        });

        searchBox.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                runSearch(s != null ? s.toString() : "");
            }
        });

        btnSearchPrev.setOnClickListener(v -> navMatch(-1));
        btnSearchNext.setOnClickListener(v -> navMatch(+1));

        btnCancel.setOnClickListener(v -> {
            // Restore original text in AppState (discard edits made in this dialog)
            AppState.get().setEditedText(originalText);
            dismiss();
        });

        btnSave.setOnClickListener(v -> saveAndDismiss());
    }

    private void updateStats(String text) {
        int lines = text.isEmpty() ? 0 : text.split("\n", -1).length;
        int chars = text.length();
        int words = countWords(text);
        tvStats.setText(String.format(Locale.ROOT, "%d dòng · %d từ · %d ký tự", lines, words, chars));
    }

    private static int countWords(String text) {
        if (text == null || text.isEmpty()) return 0;
        String trimmed = text.trim();
        if (trimmed.isEmpty()) return 0;
        return trimmed.split("\\s+").length;
    }

    // ============================================================
    // Search (mirrors FilesFragment's old logic)
    // ============================================================

    private void runSearch(@NonNull String query) {
        String text = editor.getText().toString();
        matchPositions.clear();
        currentMatchIdx = -1;

        if (query.isEmpty()) {
            SuppressRestore(text);
            searchCount.setVisibility(View.GONE);
            btnSearchPrev.setVisibility(View.GONE);
            btnSearchNext.setVisibility(View.GONE);
            return;
        }

        String lowerText = text.toLowerCase(Locale.ROOT);
        String lowerQuery = query.toLowerCase(Locale.ROOT);
        int from = 0;
        while (true) {
            int idx = lowerText.indexOf(lowerQuery, from);
            if (idx < 0) break;
            matchPositions.add(idx);
            from = idx + lowerQuery.length();
        }

        if (matchPositions.isEmpty()) {
            SuppressRestore(text);
            searchCount.setVisibility(View.VISIBLE);
            searchCount.setText("0/0");
            btnSearchPrev.setVisibility(View.GONE);
            btnSearchNext.setVisibility(View.GONE);
            return;
        }

        SpannableStringBuilder ssb = new SpannableStringBuilder(text);
        int hiBg = getResources().getColor(R.color.search_highlight);
        int hiFg = getResources().getColor(R.color.search_highlight_text);
        for (int pos : matchPositions) {
            int e = Math.min(pos + query.length(), text.length());
            ssb.setSpan(new BackgroundColorSpan(hiBg), pos, e, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb.setSpan(new ForegroundColorSpan(hiFg), pos, e, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        SuppressRestore(ssb);

        searchCount.setVisibility(View.VISIBLE);
        btnSearchPrev.setVisibility(View.VISIBLE);
        btnSearchNext.setVisibility(View.VISIBLE);
        currentMatchIdx = 0;
        highlightCurrentMatch(query);
        updateSearchCountLabel();
    }

    /** Replaces editor text without re-triggering the watcher. */
    private void SuppressRestore(CharSequence text) {
        suppressWatcher = true;
        int sel = editor.getSelectionStart();
        editor.setText(text);
        suppressWatcher = false;
        if (sel >= 0 && sel <= editor.getText().length()) editor.setSelection(sel);
    }

    private void navMatch(int delta) {
        if (matchPositions.isEmpty()) return;
        currentMatchIdx = (currentMatchIdx + delta + matchPositions.size()) % matchPositions.size();
        highlightCurrentMatch(searchBox.getText().toString());
        updateSearchCountLabel();
    }

    private void highlightCurrentMatch(@NonNull String query) {
        if (currentMatchIdx < 0 || currentMatchIdx >= matchPositions.size()) return;
        int start = matchPositions.get(currentMatchIdx);
        int end = Math.min(start + query.length(), editor.getText().length());
        editor.requestFocus();
        editor.setSelection(end);
        try {
            int line = editor.getLayout().getLineForOffset(start);
            int y = (int) editor.getLayout().getLineTop(line)
                    - editor.getHeight() / 2 + editor.getLayout().getLineBottom(line) / 2;
            editor.scrollTo(0, Math.max(0, y));
        } catch (Exception ignored) { /* layout not ready yet */ }
    }

    private void updateSearchCountLabel() {
        if (matchPositions.isEmpty()) {
            searchCount.setText("0/0");
        } else {
            searchCount.setText(String.format(Locale.ROOT, "%d/%d",
                    currentMatchIdx + 1, matchPositions.size()));
        }
    }

    // ============================================================
    // Save
    // ============================================================

    private void saveAndDismiss() {
        AppState s = AppState.get();
        if (s.getFileUri() == null) {
            if (getView() != null) Snack.show(getView(), "Chưa có file để lưu");
            dismiss();
            return;
        }
        final String text = editor.getText().toString();
        final android.net.Uri uri = s.getFileUri();
        btnSave.setEnabled(false);
        btnCancel.setEnabled(false);

        ioExecutor.execute(() -> {
            String error = null;
            try {
                OutputStream os = requireContext().getContentResolver().openOutputStream(uri, "wt");
                if (os == null) {
                    error = "Không ghi được file (chỉ đọc?)";
                } else {
                    os.write(text.getBytes("UTF-8"));
                    os.flush();
                    os.close();
                }
            } catch (Exception e) {
                error = e.getMessage() != null ? e.getMessage() : "Lỗi ghi file";
            }
            final String err = error;
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (err != null) {
                        if (getView() != null) Snack.show(getView(), "❌ " + err);
                        btnSave.setEnabled(true);
                        btnCancel.setEnabled(true);
                        return;
                    }
                    originalText = text;
                    if (getView() != null) {
                        Snack.show(getView(), "💾 Đã lưu " + text.length() + " ký tự");
                    }
                    dismiss();
                });
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog d = getDialog();
        if (d != null && d.getWindow() != null) {
            d.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ioExecutor.shutdownNow();
    }
}
