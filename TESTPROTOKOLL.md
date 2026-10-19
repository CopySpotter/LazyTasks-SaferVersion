# Prüfprotokoll BusyTasks Safe 1.1 – 19.10.2026

## Ausgeführt

- Java-Kompilierung mit `--release 17`: erfolgreich.
- `SafeConfigTest`: Rechte, Speicherung, Grenzwerte, Symlink-Schutz, ohne Passwörter: bestanden.
- `DetailsFilterTest`: Prozessfilter für `/proc`/I/O/FD/`ss` (Legacy Code): bestanden.
- `MonitorServiceTest`: Zählerraten, Reset, Prozentwerte, Einheiten: bestanden.
- GUI-Smoke-Test: Java-App unter Xvfb (X11) bei 1440 × 960 und 1366 × 768 gestartet; Fenster, vier Anzeigekarten und Prozesstabelle angezeigt; keine unbehandelte Ausnahme im Standardstart.
- Neues Dashboard startet ohne `sudo`, `pkexec` oder Administrator-Passwortdialog.
- `java -jar` startet die neue Dashboard-Klasse laut Manifest.

## Nicht abschließend geprüft

- Kein Test direkt auf Linux Mint Cinnamon des Nutzers.
- Keine Gesamtsicherheitsprüfung der vom Originalpaket mitgelieferten OSHI/JNA/SLF4J-Bibliotheken.
- Keine vollständigen Tests der optionalen privilegierten Funktionen im klassischen Mini-Widget.
- Prozess-/Disk-/Netzwerkstatistiken können von Linux-Kernel, Berechtigungen und Hardware abhängen.
- Keine dauerhafte Stabilitäts- oder Lastmessung über Stunden/Tage.

## Änderungen gegenüber dem Original

Die neue, standardmäßig gestartete Dashboard-Klasse verwendet nur OSHI-Systemabfragen ohne Privileg-Eskalation. Die klassische Safe-1.0-Klasse bleibt optional erhalten. Anders als diese klassisch privilegierten Erhebungen zeigen die neuen I/O-/NET-Karten ausschließlich Gesamtverkehr.
