package com.am.notes;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.am.notes.model.Note;
import com.am.notes.viewmodel.NoteViewModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/*
 * Copyright (c) 2025-Present, Anton Muzhytskyi
 * All rights reserved.
 *
 * This code is developed and owned by Anton Muzhytskyi.
 * Unauthorized copying of this file, via any medium, is strictly prohibited.
 */

public class MainActivity extends AppCompatActivity {
    private NoteViewModel noteViewModel;
    private SwitchCompat blockedSwitch;
    private boolean isUnblocked = false;
    private NoteAdapter adapter;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;
    private Note noteToOpen = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Objects.requireNonNull(getSupportActionBar()).hide();
        setContentView(R.layout.activity_main);

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new NoteAdapter();
        recyclerView.setAdapter(adapter);

        noteViewModel = new ViewModelProvider(this).get(NoteViewModel.class);
        noteViewModel.getAllNotes().observe(this, this::filterAndSortNotes);

        blockedSwitch = findViewById(R.id.switch_blocked);
        blockedSwitch.setChecked(true);
        isUnblocked = false;

        setupBiometricPrompt();

        blockedSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            blockedSwitch.setText(isChecked ? getString(R.string.regular) : getString(R.string.protected_label));
            if (!isChecked) {
                noteToOpen = null;
                biometricPrompt.authenticate(promptInfo);
            } else {
                isUnblocked = false;
                filterAndSortNotes(noteViewModel.getAllNotes().getValue());
            }
        });

        adapter.setOnItemClickListener(note -> {
            if (!note.isProtected() || isUnblocked) {
                openNote(note);
            } else {
                noteToOpen = note;
                biometricPrompt.authenticate(promptInfo);
            }
        });

        adapter.setOnItemLongClickListener(this::showDeleteConfirmationDialog);

        ImageButton buttonAddNote = findViewById(R.id.button_add_note);
        buttonAddNote.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, NoteActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        isUnblocked = false;
        blockedSwitch.setChecked(true);
        filterAndSortNotes(noteViewModel.getAllNotes().getValue());
    }

    private void setupBiometricPrompt() {
        Executor executor = Executors.newSingleThreadExecutor();
        biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                runOnUiThread(() -> {
                    if (noteToOpen != null) {
                        openNote(noteToOpen);
                        noteToOpen = null;
                    } else {
                        isUnblocked = true;
                        blockedSwitch.setChecked(false);
                        filterAndSortNotes(noteViewModel.getAllNotes().getValue());
                    }
                });
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, R.string.authentication_failed, Toast.LENGTH_SHORT).show();
                    if (noteToOpen == null) {
                        blockedSwitch.setChecked(true);
                    }
                });
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, getString(R.string.authentication_error) + errString, Toast.LENGTH_SHORT).show();
                    if (noteToOpen == null) {
                        blockedSwitch.setChecked(true);
                    }
                });
            }
        });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.unlock_notes))
                .setSubtitle(getString(R.string.use_your_fingerprint_to_unlock_protected_notes))
                .setNegativeButtonText(getString(R.string.cancel))
                .build();

        BiometricManager biometricManager = BiometricManager.from(this);
        switch (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            case BiometricManager.BIOMETRIC_SUCCESS:
            case BiometricManager.BIOMETRIC_STATUS_UNKNOWN:
            case BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED:
            case BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED:
                break;
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                Toast.makeText(this, R.string.biometric_authentication_not_available, Toast.LENGTH_LONG).show();
                blockedSwitch.setEnabled(false);
                break;
        }
    }

    private void filterAndSortNotes(List<Note> notes) {
        if (notes == null) return;
        List<Note> filteredNotes = new ArrayList<>();
        for (Note note : notes) {
            if (!note.isProtected() || isUnblocked) {
                filteredNotes.add(note);
            }
        }
        filteredNotes.sort((note1, note2) -> Long.compare(note2.getLastUsedTimestamp(), note1.getLastUsedTimestamp()));
        adapter.setNotes(filteredNotes);
    }

    private void openNote(Note note) {
        Intent intent = new Intent(MainActivity.this, NoteActivity.class);
        intent.putExtra("NOTE_TEXT", note.getText());
        intent.putExtra("NOTE_ID", note.getId());
        intent.putExtra("NOTE_PROTECTED", note.isProtected());
        startActivity(intent);
    }

    private void showDeleteConfirmationDialog(Note note, Context context) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.delete_note)
                .setMessage(R.string.are_you_sure_you_want_to_delete_this_note)
                .setPositiveButton(R.string.delete, (dialog, which) -> noteViewModel.delete(note))
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }
}
