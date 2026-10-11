package com.github.warren_bank.bookmarks.security;

import com.github.warren_bank.bookmarks.common.Constants;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * Front-door password gate for the single "Private" folder.
 *
 * The password itself is never stored. What is persisted:
 *   - a random 16-byte salt
 *   - the Base64 of SHA-256(salt + password)
 *
 * The unlocked state is session-only: it lives in memory and is cleared
 * whenever the user leaves the private folder, backgrounds the app or
 * explicitly locks it.
 *
 * Note: this is an access gate, not encryption. The bookmarks inside the
 * private folder remain stored as plaintext rows in the database (including
 * database backup files).
 */
public class PrivateLock {
  private static final String LOG_TAG = Constants.LOG_TAG;

  private static final int SALT_LENGTH = 16;

  private static PrivateLock instance = null;

  private SharedPreferences prefs;
  private SecureRandom      random;

  // session-only state
  private boolean unlocked;

  public static synchronized PrivateLock getInstance(Context context) {
    if (instance == null) {
      instance = new PrivateLock(context.getApplicationContext());
    }
    return instance;
  }

  private PrivateLock(Context context) {
    this.prefs    = context.getSharedPreferences(Constants.PRIVATE_LOCK_PREFS_NAME, 0);
    this.random   = new SecureRandom();
    this.unlocked = false;
  }

  // ---------------------------------------------------------------------------
  // state
  // ---------------------------------------------------------------------------

  public boolean isInitialized() {
    return !getSalt().equals("");
  }

  public boolean isUnlocked() {
    return unlocked;
  }

  public void lock() {
    unlocked = false;
  }

  // ---------------------------------------------------------------------------
  // password
  // ---------------------------------------------------------------------------

  // first-time setup: define the password
  public boolean setPassword(String password) {
    if (isInitialized() || (password == null) || password.isEmpty())
      return false;

    try {
      byte[] salt = new byte[SALT_LENGTH];
      random.nextBytes(salt);

      String saltBase64 = base64Encode(salt);
      String hashBase64 = base64Encode(hash(salt, password));

      SharedPreferences.Editor ed = prefs.edit();
      ed.putString(Constants.PRIVATE_LOCK_PREF_SALT, saltBase64);
      ed.putString(Constants.PRIVATE_LOCK_PREF_HASH, hashBase64);
      ed.apply();

      unlocked = true;
      return true;
    }
    catch (Exception e) {
      Log.e(LOG_TAG, e.getMessage());
      return false;
    }
  }

  // verify the password for the current session
  public boolean verify(String password) {
    if (!isInitialized() || (password == null) || password.isEmpty())
      return false;

    try {
      byte[] salt = base64Decode(getSalt());
      byte[] hash = hash(salt, password);

      String expected = getHash();
      String actual   = base64Encode(hash);

      unlocked = constantTimeEquals(expected, actual);
      return unlocked;
    }
    catch (Exception e) {
      Log.e(LOG_TAG, e.getMessage());
      return false;
    }
  }

  // change the password; the current password must be provided
  public boolean changePassword(String currentPassword, String newPassword) {
    if (!verify(currentPassword)) return false;
    if ((newPassword == null) || newPassword.isEmpty()) return false;

    try {
      byte[] salt = new byte[SALT_LENGTH];
      random.nextBytes(salt);

      SharedPreferences.Editor ed = prefs.edit();
      ed.putString(Constants.PRIVATE_LOCK_PREF_SALT, base64Encode(salt));
      ed.putString(Constants.PRIVATE_LOCK_PREF_HASH, base64Encode(hash(salt, newPassword)));
      ed.apply();

      unlocked = true;
      return true;
    }
    catch (Exception e) {
      Log.e(LOG_TAG, e.getMessage());
      return false;
    }
  }

  // ---------------------------------------------------------------------------
  // helpers
  // ---------------------------------------------------------------------------

  private String getSalt() {
    return prefs.getString(Constants.PRIVATE_LOCK_PREF_SALT, "");
  }

  private String getHash() {
    return prefs.getString(Constants.PRIVATE_LOCK_PREF_HASH, "");
  }

  private static byte[] hash(byte[] salt, String password) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    digest.update(salt);
    digest.update(password.getBytes("UTF-8"));
    return digest.digest();
  }

  private static String base64Encode(byte[] data) {
    return Base64.encodeToString(data, Base64.NO_WRAP);
  }

  private static byte[] base64Decode(String value) {
    return Base64.decode(value, Base64.NO_WRAP);
  }

  // length-checked, constant-time comparison
  private static boolean constantTimeEquals(String a, String b) {
    if ((a == null) || (b == null) || (a.length() != b.length()))
      return false;

    int result = 0;
    for (int i = 0; i < a.length(); i++) {
      result |= (a.charAt(i) ^ b.charAt(i));
    }
    return (result == 0);
  }
}
