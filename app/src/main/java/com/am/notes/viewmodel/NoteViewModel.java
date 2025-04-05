package com.am.notes.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.am.notes.model.Note;
import com.am.notes.repository.NoteRepository;
import java.util.List;
import java.util.concurrent.Executors;

/*
 * Copyright (c) 2025-Present, Anton Muzhytskyi
 * All rights reserved.
 *
 * This code is developed and owned by Anton Muzhytskyi.
 * Unauthorized copying of this file, via any medium, is strictly prohibited.
 */

public class NoteViewModel extends AndroidViewModel {
    private final NoteRepository repository;
    private final LiveData<List<Note>> allNotes;

    public NoteViewModel(@NonNull Application application) {
        super(application);
        repository = new NoteRepository(application);
        allNotes = repository.getAllNotes();
    }

    public void insert(Note note) {
        Executors.newSingleThreadExecutor().execute(() -> {
            long id = repository.insert(note);
            note.setId((int) id);
        });
    }

    public void update(Note note) {
        Executors.newSingleThreadExecutor().execute(() -> {
            repository.update(note);
        });
    }

    public void delete(Note note) {
        Executors.newSingleThreadExecutor().execute(() -> {
            repository.delete(note);
        });
    }

    public LiveData<List<Note>> getAllNotes() {
        return allNotes;
    }
}
