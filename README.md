# TFI Go

A Material You redesign of TFI Live, Ireland's public transport app.

## Features

- **Live Departures** — Real-time bus, train, Luas, and coach departures with live indicators
- **Stop Search** — Search for any stop, station, or Luas stop across Ireland
- **Favourites** — Save your most-used stops for quick access
- **Service Filtering** — Filter departures by route number
- **Auto-refresh** — Departures update every 30 seconds
- **Dark Mode** — Follows your system theme
- **Material You** — Clean, modern Material Design 3 interface

## Building

```bash
./gradlew assembleDebug
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

## Data Source

Uses the same TFI (Transport for Ireland) public API as the official TFI Live app.

## License

MIT
