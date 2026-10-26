# Entscheidungsvorlage – LazyTasks v0.1.0

**Vorbereitet am 26.10.2026 – Diskussionsgrundlage, noch keine freigegebene Implementierung.**

> Der Computer darf beschäftigt sein. Sein Benutzer muss es nicht.

## Verifizierter Ausgangsstand

- **GitHub Actions `Build and tests`: grün**, abgeschlossen am 19.10.2026. Java-17-Build, drei Testklassen und die Prüfung der erzeugten JAR liefen erfolgreich. [Build-Lauf](https://github.com/CopySpotter/LazyTasks-SaferVersion/actions/runs/39284579686)
- **Import der Baseline: grün**, abgeschlossen am 19.10.2026. [Import-Lauf](https://github.com/CopySpotter/LazyTasks-SaferVersion/actions/runs/39284279944)
- [Pre-Release v0.0.0](https://github.com/CopySpotter/LazyTasks-SaferVersion/releases/tag/v0.0.0) bleibt als Referenz unverändert.
- **Nicht geprüft:** Start und Fensterverhalten auf einem echten Linux-Mint/Cinnamon/X11-Desktop, Bibliothekslizenzen im Detail und Sicherheitsprüfung der optionalen erweiterten Diagnose.

## Vorschlag für den ersten Prototyp

**„Die vierköpfige Band“ in leiser Ausführung:** Vier schmale, ruhige Zeilen **CPU, RAM, Datenträger-I/O und Netzwerk**, jeweils mit einem knappen Wert. Die große Übersicht bleibt auf Wunsch abrufbar. Keine Animationen, keine Warnungen als Standard. **Keine erfundene Zuordnung von Datenträger- oder Netzwerkverkehr zu einzelnen Prozessen**; die aktuelle OSHI-Messung liefert hier zunächst Systemwerte.

**Mini-Widget als Standardstart.** In `BusyTasksDashboard.main` startet derzeit standardmäßig die große Übersicht; das klassische Widget ist nur über `--classic` erreichbar. Das soll sich für v0.1.0 umkehren. Das klassische Widget ist eine Ausgangsbasis, nicht automatisch schon das endgültige Layout.

**Always-on-top bewusst implementieren und prüfen.** Im bestehenden `BusyTasks.java` wird `setAlwaysOnTop(true)` für mehrere Nebenfenster aufgerufen, aber nicht ausdrücklich für das zentrale `Hintergrundfenster`. Für v0.1.0 muss die Hauptanzeige unter Cinnamon/X11 zuverlässig sichtbar bleiben; Verhalten bei Vollbild ist gesondert zu entscheiden.

**Bedienungsvorschlag:** Linksklick öffnet die Details, Rechtsklick öffnet ein kleines Menü (Anheften, Pausieren, Einstellungen, Beenden). Freies Verschieben; letzte Position speichern. Alle Funktionen ohne Root.

**Gelassenheit:** 2 Sekunden Aktualisierungsintervall als Ausgangswert, alternativ 5 Sekunden; keine akustischen Signale, keine automatische Prozessbeendigung, keine Pflicht zur Beobachtung.

## Vier Entscheidungen für die gemeinsame Besprechung

| Thema | Vorschlag | Alternative / Frage |
| --- | --- | --- |
| Anzeige | Vier ruhige Zeilen („Band“) | Eine Zeile („Stethoskop“) oder nur Auffälligkeiten („Roadie“)? |
| Vollbild | Bei Vollbild-Präsentationen nicht aufdringlich sein | Immer oben, auch über Videos? |
| Interaktion | Linksklick Details, Rechtsklick Menü | Doppelklick für Details? |
| Tempo | 2 Sekunden, optional 5 | Standardmäßig 5 Sekunden für mehr Ruhe? |

## Konkrete Umsetzung in kleinen Schritten

1. **UI-Kern:** Eigene kompakte Swing-`JFrame`-Ansicht; explizites Always-on-top; Standardstart im Mini-Modus; Details optional.
2. **Messwerte:** `MonitorService` weiterverwenden; keine erhöhten Rechte; Daten bei Pause wirklich nicht weiter abfragen.
3. **Bedienung und Einstellungen:** Verschieben, Position speichern, gut auffindbare Schließen-/Pause-Funktion, Rechtsklick-Menü.
4. **Tests:** Rechenlogik und Einstellungen automatisiert; grafisches Verhalten manuell unter Mint/Cinnamon/X11 (Fensterwechsel, Mehrbildschirm, Vollbild, Neustart).
5. **Sicherheit/Lizenzen:** Bestehende Legacy-Befehlswege und `pkexec` gesondert prüfen; Drittbibliotheken der mitgelieferten `vendor/BusyTasks-u2.9.jar` und deren Lizenzhinweise dokumentieren.
6. **Pre-Release:** Erst nach erfolgreichem Mint-Test `v0.1.0` veröffentlichen.

## Abnahmekriterien

- Mini-Widget startet ohne `sudo` und ist auf einem normalen Mint-Desktop sichtbar, frei positionierbar und zuverlässig im Vordergrund.
- CPU und RAM zeigen plausible Werte; Datenträger- und Netzwerkwerte sind als **Systemwerte** klar bezeichnet.
- Ein Klick öffnet die Detailansicht; Pause, Optionen und Beenden sind unkompliziert erreichbar.
- Keine Passwörter werden abgefragt, gespeichert oder über eine Shell weitergereicht, solange der Benutzer nicht ausdrücklich eine gesonderte Diagnosefunktion aktiviert.
- Java-17-Build und vorhandene Tests bleiben grün; ein dokumentierter manueller Cinnamon/X11-Test liegt vor.

**Nächster gemeinsamer Schritt:** Vier Entscheidungen oben treffen, danach einen kleinen Implementierungs-Branch für v0.1.0 anlegen.
