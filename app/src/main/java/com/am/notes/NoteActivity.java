package com.am.notes;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.CheckBox;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.am.notes.model.Note;
import com.am.notes.viewmodel.NoteViewModel;
import android.widget.ImageButton;
import java.util.Objects;

/*
 * Copyright (c) 2025-Present, Anton Muzhytskyi
 * All rights reserved.
 *
 * This code is developed and owned by Anton Muzhytskyi.
 * Unauthorized copying of this file, via any medium, is strictly prohibited.
 */

public class NoteActivity extends AppCompatActivity {
    private EditText editText;
    private NoteViewModel noteViewModel;
    private Note note;
    private CheckBox protectedCheckBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Objects.requireNonNull(getSupportActionBar()).hide();
        setContentView(R.layout.activity_note);

        ImageButton backButton = findViewById(R.id.button_back);
        editText = findViewById(R.id.edit_text);
        protectedCheckBox = findViewById(R.id.checkbox_protected);
        noteViewModel = new ViewModelProvider(this).get(NoteViewModel.class);

        String noteText = getIntent().getStringExtra("NOTE_TEXT");
        long noteId = getIntent().getLongExtra("NOTE_ID", -1);
        boolean isProtected = getIntent().getBooleanExtra("NOTE_PROTECTED", false);

        if (noteText != null) {
            editText.setText(noteText);
            if (noteId != -1) {
                note = new Note(noteText);
                note.setId(noteId);
                note.setProtected(isProtected);
                note.setLastUsedTimestamp(System.currentTimeMillis());
                noteViewModel.update(note);
                protectedCheckBox.setChecked(isProtected);
            }
        }

        editText.addTextChangedListener(new TextWatcher() {
            private final Handler handler = new Handler(Looper.getMainLooper());
            private Runnable saveRunnable;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                handler.removeCallbacks(saveRunnable);
                if (s != null && !s.toString().trim().isEmpty()) {
                    saveRunnable = () -> saveNote(s.toString());
                    handler.postDelayed(saveRunnable, 1000);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        backButton.setOnClickListener(v -> {
            saveNote(editText.getText().toString());
            Intent intent = new Intent(NoteActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveNote(editText.getText().toString());
    }

    private void saveNote(String text) {
        if (text != null && !text.trim().isEmpty()) {
            if (note == null) {
                note = new Note(text);
                note.setProtected(protectedCheckBox.isChecked());
                noteViewModel.insert(note);
            } else {
                note.setText(text);
                note.setProtected(protectedCheckBox.isChecked());
                note.setLastUsedTimestamp(System.currentTimeMillis());
                noteViewModel.update(note);
            }
        } else if (note != null) {
            noteViewModel.delete(note);
            note = null;
        }
    }
}
