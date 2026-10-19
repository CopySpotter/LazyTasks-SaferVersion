# LazyTasks-SaferVersion

> **Der Computer darf beschäftigt sein. Sein Benutzer muss es nicht.**

Ein inoffizieller Fork von [BusyTasks](https://www.opendesktop.org/p/1201835) von **Bernhard Mayr** (`unknown-spirit`). Unser Ziel ist ein kleiner, stets sichtbarer Systemmonitor, den man auch guten Gewissens ignorieren darf.

**Lazy** = weniger Kontrollrituale. **Rock ’n’ Roll** = Spielraum statt überflüssiger Perfektion. **Safer** = minimale Berechtigungen, keine gespeicherten Administratorpasswörter.

## Entwicklungsstand

- **Ausgangsversion:** [LazyTasks – Initial Baseline, v0.0.0 (Pre-Release)](https://github.com/CopySpotter/LazyTasks-SaferVersion/releases/tag/v0.0.0)
- **Download:** [BusyTasks_Safe_Mint_v1_1.zip](https://github.com/CopySpotter/LazyTasks-SaferVersion/releases/download/v0.0.0/BusyTasks_Safe_Mint_v1_1.zip)
- **Enthaltene Software:** BusyTasks Safe 1.1 für Linux Mint / Cinnamon / X11 (Java 17+)
- **Status:** Experimentell, keine abgeschlossene Sicherheitsprüfung; insbesondere die klassische erweiterte Prozessdiagnose ist auf Mint noch nicht vollständig getestet

Die Veröffentlichung `v0.0.0` hält die **technische Ausgangsbasis** fest. Sie ist noch **nicht** die fertig entwickelte LazyTasks-Oberfläche.

## Starten

Release-ZIP entpacken und im entpackten Ordner:

```bash
./start.sh
```

oder:

```bash
java -jar dist/BusyTasks-Safe-1.1.jar
```

**Nicht mit `sudo` starten.** Die Standardansicht benötigt keine Administratorrechte.

## Nachbau aus dem Repository

Benötigt: Java 17 JDK, `unzip`, `jar` und die als `vendor/BusyTasks-u2.9.jar` bereitgestellte Ursprungs-JAR mit Java-Laufzeitbibliotheken.

```bash
bash build.sh test
./start.sh
```

Das Buildskript arbeitet **offline**. Es ersetzt die Original-Anwendungsklassen durch die Quellen unter `src/`, übernimmt jedoch Drittbibliotheken und Ressourcen aus der Ursprungs-JAR. Die Java-Anwendungsquellen sind offen im Repo; es handelt sich **nicht** um einen vollständig aus Quellen selbst gebauten gesamten Dependency-Stack.

Der Quellcode steht unter `src/`, die Tests unter `tests/`. Die ausführbare JAR liegt im [Release-ZIP](https://github.com/CopySpotter/LazyTasks-SaferVersion/releases/tag/v0.0.0) und wird nicht zusätzlich auf `main` versioniert.

## Ursprung und Lizenz

- Original: **Bernhard Mayr**, [BusyTasks auf OpenDesktop](https://www.opendesktop.org/p/1201835)
- Original-/Fork-Lizenz: **GNU GPL v3 oder später**, vollständiger Lizenztext in [COPYING](COPYING)
- Veränderungen: Passwortspeicherung entfernt, neue Standardoberfläche und Prozessübersicht; Details siehe [CHANGELOG.md](CHANGELOG.md)
- Eingebundene Bibliotheken und deren eigene Lizenzen bedürfen weiterhin einer separaten Überprüfung.

Dies ist **kein offizielles Update** des ursprünglichen Entwicklers.

## Leitidee

Die Dokumentation [Vom BusyTasks zu LazyTasks](docs/PHILOSOPHIE.md) beschreibt den philosophischen Hintergrund und die Designprinzipien.

**Always on top bleibt der Kern.** Eine große Diagnoseansicht kann ergänzen, soll den kleinen Beobachter aber nicht ersetzen.

## Geplante Entwicklung

- [x] Ausgangsstand v0.0.0 als Pre-Release dokumentieren
- [x] Philosophie und Urheberschaft dokumentieren
- [ ] Übernahme von Quellcode, Tests und Build-Abhängigkeit in `main` verifizieren
- [ ] Minimalen Vordergrundmodus zur Hauptfunktion machen
- [ ] Sicherheit und Lizenzhinweise aller Drittbibliotheken überprüfen
- [ ] Release und Build auf Linux Mint reproduzierbar testen
