# LazyTasks – To-do und Arbeitsagenda

Stand: 26.10.2026 · Nächste Phase: Entscheidungen für v0.1.0

> **Der Computer darf beschäftigt sein. Sein Benutzer muss es nicht.**

**Projektziel:** Ein winziger, auf Wunsch ständig sichtbarer Systembeobachter mit minimaler Ablenkung. Die große Diagnoseansicht bleibt verfügbar, ist aber nicht das Hauptprodukt.

## P0 – Vor der nächsten Programmversion prüfen

- [x] **GitHub Actions geprüft (26.10.):** `Build and tests` und Import vom 19.10. erfolgreich; Java-17-Build, Tests und JAR-Prüfung grün. [Build-Lauf](https://github.com/CopySpotter/LazyTasks-SaferVersion/actions/runs/39284579686)
- [x] **Release 0 als Referenz dokumentiert:** `v0.0.0` mit ZIP, Quellcode, Lizenz und Originalquelle. [Release](https://github.com/CopySpotter/LazyTasks-SaferVersion/releases/tag/v0.0.0)
- [ ] **Build aus frischem Checkout auf Mint prüfen:** `bash build.sh test`, anschließend grafischer Start ohne `sudo`; Fehler und Systemdaten dokumentieren.
- [ ] **Sicherheitsprüfung zu Ende führen:** Keine Passwortspeicherung und kein `sudo -S`; Umgang mit privilegierten Aufrufen und Alt-Konfiguration nachvollziehen. Kein Sicherheitsversprechen ohne Test.
- [ ] **Fremdbibliotheken/Lizenzen prüfen:** Mitgelieferte `vendor/BusyTasks-u2.9.jar` auf Abhängigkeiten, Lizenzen und Updatefähigkeit untersuchen; Originalautor Bernhard Mayr weiterhin nennen.

## P1 – Gemeinsam über v0.1.0 entscheiden

[Entscheidungsvorlage v0.1.0](ENTSCHEIDUNGSVORLAGE_V0.1.0.md) – vier offene Produktentscheidungen, konkreter Umsetzungsvorschlag und Abnahmekriterien.

- [ ] **Kernprodukt festlegen:** Klassische kleine Anzeige ist der Hauptmodus; große Übersicht nur auf Wunsch.
- [ ] **Always-on-top unter Cinnamon/X11 testen:** Verhalten beim Fensterwechsel, über Vollbild, bei mehreren Bildschirmen und nach Neustart.
- [ ] **Mini-Anzeige skizzieren:** Vier Zeilen CPU, RAM, I/O, NET samt jeweils relevantestem Prozess; Lesbarkeit, Maße und Position.
- [ ] **Einfachste Bedienung definieren:** Ein Klick = Details; Rechtsklick = Optionen; klare Pause-/Schließen-Funktionen. Keine versteckten Pflichtaktionen.
- [ ] **Gelassenheit konkretisieren:** Was zeigt das Widget *nicht*? Ruhige Aktualisierung, sparsame Animationen, keine unnötigen Warnungen.
- [ ] **Einstellungen und Speicherorte:** Position, Größe, Transparenz, Ausrichtung; keine Geheimnisse in Konfigurationsdateien.

## P2 – Später, nur wenn wirklich sinnvoll

- [ ] Einstellbare Minimalansichten (1 bis 4 Messbereiche) und ein „Lazy“-Profil.
- [ ] Sehr zurückhaltende Hinweise **nur** bei relevanten Ausnahmen – abschaltbar.
- [ ] Ressourcenverbrauch von LazyTasks messen: Es darf nicht selbst zum nennenswerten Verursacher werden.
- [ ] Screenshots, kurze Mint-Installationsanleitung und Entwicklerdokumentation aktualisieren.
- [ ] Nach bestandenen Tests den nächsten Pre-Release `v0.1.0` veröffentlichen.

## Bewusst nicht geplant

- Kein zweiter vollständiger KDE-Systemmonitor.
- Keine Daueralarme, Ranglisten oder Gamification der Rechneraktivität.
- Kein grundsätzliches Erfordernis von Root-Rechten.
- Keine automatische Prozessbeendigung ohne ausdrücklichen Wunsch.

## Nächste Sitzung – Vorschlag für 30 Minuten

1. **5 Minuten:** Was nervt an der aktuellen Mini-Anzeige? Was ist schon gut?
2. **10 Minuten:** Brainstorming anhand von [BRAINSTORMING.md](BRAINSTORMING.md).
3. **10 Minuten:** Eine Minimalansicht und zwei bis drei Benutzerinteraktionen entscheiden.
4. **5 Minuten:** Prioritäten für v0.1.0 und klare Abnahmekriterien festlegen.

**Erfolgskriterium v0.1.0:** Unter Linux Mint/Cinnamon/X11 ist eine kleine, frei platzierbare Anzeige zuverlässig immer im Vordergrund; CPU/RAM sind ohne erhöhte Rechte sichtbar; das Fenster nervt nicht und lässt sich mit einem Klick erweitern und unkompliziert beenden.
