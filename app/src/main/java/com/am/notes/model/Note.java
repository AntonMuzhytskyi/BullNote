package com.am.notes.model;

import android.util.Log;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.am.notes.utils.EncryptionHelper;
import com.am.notes.utils.KeyStoreHelper;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;

/*
 * Copyright (c) 2025-Present, Anton Muzhytskyi
 * All rights reserved.
 *
 * This code is developed and owned by Anton Muzhytskyi.
 * Unauthorized copying of this file, via any medium, is strictly prohibited.
 */

@Entity(tableName = "notes")
public class Note {

    @PrimaryKey(autoGenerate = true)
    private long id;
    private byte[] encryptedText;
    private boolean protectedNote;
    private long lastUsedTimestamp;
    private static final String TAG = "Note";

    //for Room
    public Note() {}

    public Note(String text) {
        this.protectedNote = false;
        this.lastUsedTimestamp = System.currentTimeMillis();
        setText(text);
    }

    public long getId() { return id; }

    public void setId(long id) {
        this.id = id;
        try {
            SecretKey key = KeyStoreHelper.getKey(id);
            if (key == null && encryptedText != null) {
                key = KeyStoreHelper.generateKey(id);
                this.encryptedText = EncryptionHelper.encrypt(new String(encryptedText, StandardCharsets.UTF_8), key);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting ID: " + id, e);
        }
    }

    public String getText() {
        try {
            SecretKey key = KeyStoreHelper.getKey(id);
            if (key != null && encryptedText != null) {
                return EncryptionHelper.decrypt(encryptedText, key);
            }
            return "";
        } catch (Exception e) {
            Log.e(TAG, "Error getting text for ID: " + id, e);
            return "";
        }
    }

    public void setText(String text) {
        try {
            if (text != null && !text.trim().isEmpty()) {
                SecretKey key = id == 0 ? KeyStoreHelper.generateKey(0) : KeyStoreHelper.getKey(id);
                if (key == null) key = KeyStoreHelper.generateKey(id);
                this.encryptedText = EncryptionHelper.encrypt(text, key);
            } else {
                this.encryptedText = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting text: " + text, e);
        }
    }

    public byte[] getEncryptedText() { return encryptedText; }
    public void setEncryptedText(byte[] encryptedText) { this.encryptedText = encryptedText; }

    public boolean isProtected() { return protectedNote; }
    public void setProtected(boolean protectedNote) { this.protectedNote = protectedNote; }

    public boolean isProtectedNote() {return protectedNote;}
    public void setProtectedNote(boolean protectedNote) {this.protectedNote = protectedNote;}

    public long getLastUsedTimestamp() { return lastUsedTimestamp; }
    public void setLastUsedTimestamp(long lastUsedTimestamp) { this.lastUsedTimestamp = lastUsedTimestamp; }
}
