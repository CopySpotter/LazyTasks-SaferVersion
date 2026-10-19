# Versionsgeschichte

## BusyTasks Safe 1.1 (19.10.2026, inoffiziell)

- Neues, großes Dashboard in Java Swing als Standardoberfläche (1100 × 780 Pixel).
- Deutsch beschriftete und dauerhaft sichtbare CPU-, RAM-, Datenträger- und Netzwerk-Karten.
- Live-Verlaufsgrafiken; aktualisierbare, sortierbare und filterbare Prozesstabelle.
- Details per Doppelklick; Pause/Fortsetzen, manuelles Aktualisieren, auswählbare Intervalle.
- Fenstergröße und Aktualisierungsintervall werden über SafeConfig ohne Passwörter gesichert.
- Sammlung der Daten im Hintergrund mittels SwingWorker, kein sudo/pkexec für das Dashboard.
- Ohne Root verfügbare aggregierte Disk-/Netzwerkzähler; kein Anspruch auf per-Prozess-I/O/NET.
- Klassisches, bereits vergrößerbares Widget bleibt erhalten (`--classic` oder Menü Datei).
- Einfache `start.sh`, Offline-Build mit Testgruppen und Java-17-Kompatibilität.
- Test unter virtuellem X11: Fenster öffnet sich, Messungen und Prozessliste angezeigt.

## BusyTasks Safe 1.0

- Aus der Original-JAR entstand eine Java-17-fähige Safe-Version.
- Passwortspeicherung, `sudo -S` und das Laden der früheren Java-Objektkonfiguration entfernt.
- Geschützte Konfiguration unter `~/.config/busytasks-safe/`.
- Optionale privilegierte Auswertung nur nach Freigabe über PolicyKit im klassischen Widget.
