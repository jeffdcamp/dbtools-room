# DBTools Room

Kotlin Multiplatform library of extensions and tools for Room KMP (`androidx.room3`) and SQLite databases. Published to Maven Central as `org.dbtools:dbtools-room`.

## Build & Checks

Run from this directory (the one containing `gradlew`):

```bash
./gradlew build                                   # Assemble all targets and run checks
./gradlew detekt                                  # Static analysis (fails on any finding)
./gradlew allTests                                # All unit tests (JVM, Android host, native) with aggregated report
./gradlew checkKotlinAbi                          # Fail if the public API differs from the committed dump in dbtools-room/api/
./gradlew updateKotlinAbi                         # Regenerate dbtools-room/api/ after an intentional API change
./gradlew jvmTest                                 # JVM tests only (includes the real-SQLite tests)
./gradlew koverHtmlReport                         # Code coverage report
./gradlew dependencyUpdates -Drevision=release    # Check for dependency updates
./gradlew publishToMavenLocal                     # Install locally for testing in an app
```

Run a single test class: `./gradlew jvmTest --tests "org.dbtools.room.ext.SqlSchemaExtTest"`

- ABI validation (Kotlin Gradle plugin `abiValidation()`) runs as part of `check`. Commit `dbtools-room/api/` changes with the code that causes them. A removed or changed signature (including adding a parameter, even with a default value) breaks already-compiled callers, so it needs a major version bump or a hidden `@Deprecated` overload that keeps the old signature.
- The detekt config is downloaded at build time into `dbtools-room/build/config/detektConfig.yml`; `allRules = true`, so every finding fails the build.
- iOS/macOS test tasks are skipped on Linux; Apple klibs still cross-compile (`kotlin.native.enableKlibsCrossCompilation=true`), but frameworks only link on a Mac.
- CI is GitHub Actions (`.github/workflows`): `dev.yml` runs `build` + `check` on `master`; `release.yml` also publishes to Maven Central from the `release` branch.

## Project Structure

Single module `:dbtools-room`, package `org.dbtools.room`.

Targets: Android (minSdk 26, JVM 17), JVM (21), `linuxX64`, `iosArm64`, `iosSimulatorArm64`, `macosArm64`. All library code is in `commonMain`.

```
converter/   → Room TypeConverters (KotlinDateTimeTextConverter)
data/        → Result types (AttachedDatabaseInfo, DatabaseTableInfo, DatabaseColumnInfo, DatabaseTableType)
database/    → DatabaseProvider (single lazily-created database) and RoomDatabaseRepository (keyed set of same-schema databases)
ext/         → Extension functions (the bulk of the library)
log/         → DbToolsRoomCrashLogException
DatabaseViewQuery.kt → view name + query pair for the create/drop/recreate view helpers
```

### Extension functions (`ext/`)

Most database operations exist on three receivers, kept in parallel with the same names and parameters:

- `SQLiteConnectionExt.kt` — `SQLiteConnection` (synchronous)
- `TransactorExt.kt` — Room `Transactor` (suspend, via `usePrepared`)
- `RoomDatabaseExt.kt` — `RoomDatabase` (suspend; delegates to the `Transactor` form through `useReaderConnection` / `useWriterConnection`)

When adding or renaming one of these, update all three. SQL text and row readers shared between the forms live in `SqlSchemaExt.kt` as `internal` helpers, so the forms differ only in how they get a connection.

Other extension files:

- `SQLiteStatementExt.kt` — column lookup, nullable getters, `bindArgs()` / `getValue()` / `getValues()`
- `SQLiteDriverExt.kt` — operations that open a database file by name (identity hash fix-up, validation)
- `MergeDatabaseExt.kt` — merge tables from another database via `ATTACH`
- `OkioFilesystemExt.kt` / `KotlinFilesystemExt.kt` — the same database file helpers (delete/rename including `-journal`, `-shm`, `-wal`, `.lck` sidecars; parse and run `.sql` files) for Okio and kotlinx-io; keep them in sync

### SQL safety

- SQLite cannot bind identifiers. Quote table/column/schema names with `String.quoteSqlIdentifier()` rather than concatenating them raw.
- Bind values (including table names passed to `pragma_table_info()`) instead of building them into the SQL string.
- Functions that take a `databaseName` work against an attached database; `""` means `main`.

## Key Libraries

- **Room KMP** (`androidx.room3`) — runtime only; the library does not define any databases
- **androidx.sqlite** — `SQLiteConnection`, `SQLiteStatement`, `SQLiteDriver`
- **Okio** and **kotlinx-io** — filesystem helpers
- **kotlinx-coroutines**, **kotlinx-datetime**, **kotlinx-serialization**, **atomicfu** (locks)
- **Kermit** — logging
- Versions are in `gradle/libs.versions.toml`; the library version is `VERSION_NAME` in `dbtools-room/gradle.properties`

## Testing

- `commonTest` — filesystem extension tests using Okio's `FakeFileSystem`
- `jvmTest` — tests that run real SQL against an in-memory database (`BundledSQLiteDriver().open(":memory:")`, `sqlite-bundled` test dependency). Only the `SQLiteConnection` forms are tested; the `Transactor` and `RoomDatabase` forms share the same SQL and readers.

Frameworks: `kotlin.test`, AssertK, kotlinx-coroutines-test.

## Code Conventions

- When writing tests, always make the test function names human readable (use `` `backtick style` `` function names, e.g. `` fun `row count of an empty table is zero`() ``).
- Use `runCatching { }` instead of `try { } catch { }`.
- 4-space indentation, standard Kotlin naming; public API gets KDoc.
- `commonMain` must compile for all targets: no `java.*` / `android.*` APIs.
- Record user-facing changes in `CHANGELOG.md` under `[Unreleased]` ([Keep a Changelog](https://keepachangelog.com/en/1.0.0/) format), and update `README.md` features when adding a new capability.
- Run `detekt`, `allTests`, and `checkKotlinAbi` before committing (or just `check`).
- Commit messages: short, with multiple changes separated by ` / ` (e.g. `10.2.1 / Room 3.0.3 / Gradle 9.8.0`).
