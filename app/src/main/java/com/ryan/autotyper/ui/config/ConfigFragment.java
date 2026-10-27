package com.ryan.autotyper.ui.config;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.ryan.autotyper.AppState;
import com.ryan.autotyper.PrefsHelper;
import com.ryan.autotyper.R;
import com.ryan.autotyper.Snack;
import com.ryan.autotyper.ui.AnimKit;

/**
 * Config tab v3.5 — typing settings with reset + about card.
 *
 *   • rows         (1–5)
 *   • typingMode   (Gõ từng chữ / Dán từng dòng)
 *   • delayMs      (≥ 0)
 *   • suffixName   (free text, optional)
 *
 * Plus:
 *   • Reset to defaults button (with confirm dialog)
 *   • Live rows badge next to the title
 *
 * Haptic feedback on Apply / Reset.
 */
public class ConfigFragment extends Fragment {

    private Spinner spinnerRows, spinnerTypingMode;
    private EditText etDelay, etSuffixName;
    private Button btnApply, btnReset;
    private TextView rowsBadge;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_config, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        spinnerRows = view.findViewById(R.id.spinnerRows);
        spinnerTypingMode = view.findViewById(R.id.spinnerTypingMode);
        etDelay = view.findViewById(R.id.etDelay);
        etSuffixName = view.findViewById(R.id.etSuffixName);
        btnApply = view.findViewById(R.id.btnApply);
        btnReset = view.findViewById(R.id.btnReset);
        rowsBadge = view.findViewById(R.id.rowsBadge);

        setupSpinners();
        wireLiveBindings();
        hydrateFromAppState();

        btnApply.setOnClickListener(v -> {
            haptic();
            flushInputsToAppState();
            if (getView() != null) Snack.show(getView(), "✅ Đã áp dụng cài đặt");
        });

        btnReset.setOnClickListener(v -> {
            haptic();
            new AlertDialog.Builder(requireContext())
                    .setTitle("Khôi phục mặc định?")
                    .setMessage("Số dòng = 1\nCách gõ = Gõ từng chữ\nDelay = 1000ms\nSuffix = (rỗng)")
                    .setPositiveButton("Khôi phục", (d, w) -> {
                        PrefsHelper.resetConfig(requireContext(), AppState.get());
                        hydrateFromAppState();
                        if (getView() != null) Snack.show(getView(), "🔄 Đã khôi phục mặc định");
                    })
                    .setNegativeButton("Huỷ", null)
                    .show();
        });

        // Staggered entrance on root LinearLayout
        View rootLinear = view.findViewById(R.id.config_root);
        if (rootLinear != null) {
            AnimKit.staggeredEntrance(rootLinear, 60L);
        }
    }

    private void setupSpinners() {
        ArrayAdapter<String> rowsAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"1", "2", "3", "4", "5"});
        rowsAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerRows.setAdapter(rowsAdapter);

        ArrayAdapter<String> modeAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                new String[]{getString(R.string.mode_type_char), getString(R.string.mode_paste_line)});
        modeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerTypingMode.setAdapter(modeAdapter);
    }

    private void wireLiveBindings() {
        spinnerRows.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View v, int position, long id) {
                AppState s = AppState.get();
                s.setRows(position + 1);
                if (rowsBadge != null) rowsBadge.setText(String.valueOf(position + 1));
                persistConfig();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        spinnerTypingMode.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View v, int position, long id) {
                AppState s = AppState.get();
                s.setTypingMode(position);
                persistConfig();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        etDelay.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                int v = 1000;
                try { v = Integer.parseInt(s.toString()); } catch (Exception ignored) {}
                AppState.get().setDelayMs(v);
                persistConfig();
            }
        });

        etSuffixName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                AppState.get().setSuffixName(s != null ? s.toString().trim() : "");
                persistConfig();
            }
        });
    }

    private void hydrateFromAppState() {
        AppState s = AppState.get();
        int rowsIdx = Math.max(0, Math.min(4, s.getRows() - 1));
        spinnerRows.setSelection(rowsIdx, false);
        spinnerTypingMode.setSelection(s.getTypingMode(), false);
        etDelay.setText(String.valueOf(s.getDelayMs()));
        etSuffixName.setText(s.getSuffixName());
        if (rowsBadge != null) rowsBadge.setText(String.valueOf(s.getRows()));
    }

    private void flushInputsToAppState() {
        try {
            AppState.get().setDelayMs(Integer.parseInt(etDelay.getText().toString()));
        } catch (Exception ignored) {}
        AppState.get().setSuffixName(etSuffixName.getText().toString().trim());
        AppState.get().setRows(spinnerRows.getSelectedItemPosition() + 1);
        AppState.get().setTypingMode(spinnerTypingMode.getSelectedItemPosition());
        persistConfig();
    }

    private void persistConfig() {
        Context c = getContext();
        if (c == null) return;
        PrefsHelper.saveConfig(c, AppState.get());
    }

    private void haptic() {
        if (getView() == null) return;
        getView().performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY,
                android.view.HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
    }
}
