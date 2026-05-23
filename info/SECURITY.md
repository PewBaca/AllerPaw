# AllerNutri – Sicherheitsanalyse & Roadmap

> Stand: 2026-05-22 · Version: v0.11.0012  
> Erstellt auf Basis vollständiger Code-Analyse des GitHub-Repositories.

---

## Übersicht: Implementierungsstand

| ID | Problem | Priorität | Status |
|----|---------|-----------|--------|
| S1 | Auth-Token unverschlüsselt im DataStore | 🔴 KRITISCH | 🔲 offen |
| S2 | Hardcoded WEB_CLIENT_ID | 🔴 KRITISCH | ✅ behoben in v0.11.0012 |
| S3 | HTTP-Logging im Release-Build aktiv | 🟠 HOCH | ✅ behoben in v0.11.0012 |
| S4 | API-Keys unverschlüsselt im DataStore | 🟠 HOCH | 🔲 offen |
| S5 | ProGuard falscher Paketname (allerpaw) | 🟡 MITTEL | ✅ behoben in v0.11.0012 |
| S6 | allowBackup ohne Ausschlussregeln | 🟡 MITTEL | 🔲 offen |
| S7 | play-services-auth ungenutzte Dependency | 🟡 MITTEL | 🔲 offen |

---

## Bereits behobene Probleme (v0.11.0012)

### ✅ S2 – WEB_CLIENT_ID aus Code entfernt

**War:** `const val WEB_CLIENT_ID = "YOUR_WEB_CLIENT_ID.apps.googleusercontent.com"` direkt in `AuthRepository.kt`

**Ist:** Wert kommt aus `local.properties` → `BuildConfig.GOOGLE_WEB_CLIENT_ID`

**Setup für Entwickler:**
```
# local.properties (nie committen — bereits in .gitignore)
google.web.client.id=DEINE_ID.apps.googleusercontent.com
```

Die Datei `local.properties` ist in `.gitignore` eingetragen. Ohne gültige ID funktioniert Google Sign-In nicht — der Demo-Modus ist davon unabhängig.

---

### ✅ S3 – HTTP-Logging nur im Debug-Build

**War:** `HttpLoggingInterceptor` mit `Level.BASIC` immer aktiv — auch im Store-Release.

**Ist:** Interceptor wird nur hinzugefügt wenn `BuildConfig.DEBUG == true`:
```kotlin
.apply {
    if (BuildConfig.DEBUG) {
        addInterceptor(HttpLoggingInterceptor().apply { level = Level.BASIC })
    }
}
```

Im Release-APK werden keine HTTP-Metadaten (URLs, Auth-Header-Strukturen) in Logcat geschrieben.

---

### ✅ S5 – ProGuard-Paketname korrigiert

**War:** `-keep class com.allerpaw.app.data.remote.dto.**` (falsches Paket)

**Ist:** `-keep class com.allernutri.app.data.remote.dto.**`

Ohne diese Regel würden DTO-Klassen im R8-Release-Build umbenannt → Retrofit-Parsing-Crashes.

---

## Offene Sicherheitsprobleme

### 🔴 S1 – Auth-Token unverschlüsselt im DataStore

**Betroffene Datei:** `data/repository/SessionRepository.kt`

**Problem:** Das Google `idToken` (JWT) wird via `saveSession()` als Klartext in `PreferencesDataStore` gespeichert:
```kotlin
prefs[KEY_AUTH_TOKEN] = token  // Klartext-JWT im DataStore
```

**Risiko:** Auf gerooteten Geräten oder per ADB-Backup lesbar. Angreifer mit physischem Zugriff kann den Token extrahieren.

**Lösung (Phase D):**
```kotlin
// Abhängigkeit hinzufügen:
// implementation("androidx.security:security-crypto:1.1.0-alpha06")

val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()

val encryptedPrefs = EncryptedSharedPreferences.create(
    context,
    "allernutri_secure_prefs",
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)
```

Alternativ: `androidx.security:security-crypto-ktx` mit DataStore-Integration.

**Betroffene Keys:**
- `KEY_AUTH_TOKEN` (Google JWT)
- `KEY_USER_ID`
- `KEY_USER_EMAIL`

---

### 🟠 S4 – API-Keys unverschlüsselt im DataStore

**Betroffene Datei:** `data/repository/SettingsRepository.kt`

**Problem:** USDA-API-Key und Edamam-Credentials liegen unverschlüsselt im DataStore:
```kotlin
private val KEY_USDA_KEY   = stringPreferencesKey("usda_api_key")
private val KEY_EDAMAM_ID  = stringPreferencesKey("edamam_app_id")
private val KEY_EDAMAM_KEY = stringPreferencesKey("edamam_app_key")
```

**Risiko:** Geringeres Risiko als S1 (Keys vom Nutzer selbst eingegeben, kein Auth-Token), aber auf gerooteten Geräten auslesbar.

**Lösung (Phase D):** Gleicher Ansatz wie S1 — `EncryptedSharedPreferences` für alle sensitiven Keys. Alternativ: Android Keystore direkt für die Verschlüsselung der Werte.

---

### 🟡 S6 – allowBackup ohne Ausschlussregeln

**Betroffene Datei:** `app/src/main/AndroidManifest.xml`

**Problem:**
```xml
<application android:allowBackup="true" ...>
```

Ohne `android:fullBackupContent`-Regelwerk werden alle App-Daten (inkl. DataStore mit Token und API-Keys) in ADB-Backups und Google Drive Backups eingeschlossen.

**Lösung:**

1. `res/xml/backup_rules.xml` erstellen:
```xml
<?xml version="1.0" encoding="utf-8"?>
<full-backup-content>
    <!-- DataStore-Dateien mit sensiblen Daten ausschließen -->
    <exclude domain="sharedpref" path="allernutri_prefs.preferences_pb" />
    <!-- DB-Backup ist OK (kein Token drin) -->
    <include domain="database" path="allernutri.db" />
</full-backup-content>
```

2. In `AndroidManifest.xml`:
```xml
<application
    android:allowBackup="true"
    android:fullBackupContent="@xml/backup_rules"
    android:dataExtractionRules="@xml/backup_rules"
    ...>
```

---

### 🟡 S7 – play-services-auth ungenutzte Dependency

**Betroffene Datei:** `app/build.gradle.kts`

**Problem:**
```kotlin
implementation(libs.play.services.auth)  // com.google.android.gms:play-services-auth:21.3.0
```

Kein einziger `import com.google.android.gms.auth` im Code gefunden. Die Dependency ist ein Überrest aus einer früheren Auth-Implementierung vor dem Wechsel auf Credential Manager.

**Risiko:** Erhöht APK-Größe (~500KB), erweitert Attack Surface, unnötiger Build-Input.

**Lösung:** Zeile aus `build.gradle.kts` entfernen und aus `libs.versions.toml` (`play-services-auth`-Eintrag) löschen.

**Vorsicht:** Vor dem Entfernen sicherstellen, dass kein transitiver Bedarf durch andere Libs besteht (`./gradlew dependencies | grep play-services-auth`).

---

## Sicherheits-Roadmap

### Phase D (nächste Session, ~3–4h)

```
D1 · EncryptedDataStore / EncryptedSharedPreferences einführen
     Betroffene Keys: auth_token, user_id, user_email,
                      usda_api_key, edamam_app_id, edamam_app_key
     → verify: adb backup → prefs-Datei nicht lesbar

D2 · backup_rules.xml erstellen
     DataStore vom Backup ausschließen, DB-Backup erlaubt lassen
     → verify: adb backup dann adb restore — DB wiederhergestellt,
               Token NICHT wiederhergestellt

D3 · play-services-auth entfernen
     Nach ./gradlew dependencies-Check
     → verify: Gradle sync + assembleDebug erfolgreich
```

---

## Hinweise für neue Entwickler

### local.properties Setup (Pflicht)

```properties
# local.properties — NIEMALS ins Git committen!
# Diese Datei ist in .gitignore eingetragen.

# Google OAuth Web-Client-ID
# Quelle: Google Cloud Console → APIs & Dienste → Anmeldedaten
#         → OAuth 2.0-Client-IDs → Web-Client → Client-ID kopieren
google.web.client.id=DEINE_CLIENT_ID.apps.googleusercontent.com
```

Ohne diese Datei baut die App trotzdem — der Fallback-Wert `YOUR_WEB_CLIENT_ID.apps.googleusercontent.com` greift, Google Sign-In schlägt dann zur Laufzeit fehl. Der Demo-Login (ohne Google) ist davon unberührt.

### .gitignore prüfen

Folgende Dateien dürfen niemals committed werden:
- `local.properties` ✅ bereits in .gitignore
- `*.jks`, `*.keystore` ✅ bereits durch Standard-Android-.gitignore abgedeckt
- `google-services.json` — noch nicht relevant, da kein Firebase verwendet wird

---

## Externe Security-Referenzen

- [Android Security Best Practices](https://developer.android.com/topic/security/best-practices)
- [EncryptedSharedPreferences Doku](https://developer.android.com/reference/androidx/security/crypto/EncryptedSharedPreferences)
- [Android Keystore System](https://developer.android.com/training/articles/keystore)
- [Backup & Restore Security](https://developer.android.com/guide/topics/data/autobackup)
