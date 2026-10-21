# Oxygen Launcher

Ein nativer Android-Launcher in Kotlin mit minimalistischem OxygenOS-inspiriertem Design.

## Features
- Als Android-Home-App auswählbar
- App-Drawer mit Live-Suche
- App-Grid mit 3–6 Spalten
- Homescreen-Seiten und Dock
- Dunkles Design, Akzentfarben und Icon-Labels
- Animierte Übergänge und Touch-Feedback
- Long-Press-Aktionen für App-Info und Deinstallation
- System-Wallpaper-Einstellungen
- Widget-Seite als Ausgangspunkt für Widgets
- Einstellungsseite mit lokal gespeicherten Optionen

## Voraussetzungen
- Android Studio (aktuelle stabile Version)
- JDK 17
- Android SDK Platform 35
- Ein Android-Gerät oder Emulator mit Android 8.0+ (API 26+)

## Bauen
1. ZIP entpacken.
2. In Android Studio **Open** auswählen und den Ordner `OxygenLauncher` öffnen.
3. Warten, bis Gradle synchronisiert ist. Falls Android Studio ein SDK installieren möchte, Platform 35 installieren.
4. Für ein Gerät: Entwickleroptionen und USB-Debugging aktivieren, Gerät verbinden und **Run** drücken.
5. APK bauen: **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
6. APK liegt üblicherweise unter `app/build/outputs/apk/debug/app-debug.apk`.

## Als Standard-Launcher setzen
Nach der Installation die Home-Taste drücken und **Oxygen Launcher** wählen. Alternativ die Android-Einstellungen für Standard-Apps > Home-App öffnen.

## Hinweise
- Die App-Liste wird mit `ACTION_MAIN` + `CATEGORY_LAUNCHER` abgefragt. `QUERY_ALL_PACKAGES` ist für den Launcher-Anwendungsfall deklariert; Verteilung über Stores kann zusätzliche Richtlinienanforderungen haben.
- Das Widget-System unterscheidet sich zwischen Android-Versionen und Geräteherstellern. Die Widget-Seite ist hier ein Basis-Panel; vollständige Drag-and-drop-Widget-Platzierung und der vollständige Widget-Picker sind ein eigener Ausbauschritt.
- Launcher-Ersatz-Apps können Systemanimationen, Gesten-Navigation und Statusleisten nicht auf jedem Herstellergerät vollständig steuern.
- Dieses Projekt verwendet bewusst native Android Views und keine zusätzlichen UI-Abhängigkeiten.
