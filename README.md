# Real-Time Price Tracker

## Project Overview
A Jetpack Compose Android app that tracks real-time stock prices. It simulates price movements for 25 well-known symbols, streams updates over a WebSocket echo server, and renders a responsive, animated UI.

- Key features:
  - Live connection to a WebSocket echo server (Postman Echo)
  - 25 predefined stock symbols with periodic price updates
  - MVI architecture with unidirectional data flow
  - Animated UI (price flash, change indicators, smooth top bar toggling)
  - Connection state indicator (connected/connecting/disconnected)
  - Error handling with Snackbar
  - Unit tests, repository tests, and Compose UI tests
- Technologies:
  - Kotlin, Coroutines, StateFlow/SharedFlow
  - Jetpack Compose (Material3)
  - AndroidX Lifecycle
  - OkHttp (WebSocket)
  - kotlinx.serialization (JSON)
  - MockK, Turbine, JUnit, Compose UI Test

## Architecture
The app follows the MVI (Model-View-Intent) pattern:

- Model: immutable UI state (`UiState`) in `StockPriceViewModel` and domain models (`StockSymbol`, `PriceUpdate`, `ConnectionState`).
- View: Compose UI (`MainScreen`, `TopBar`, `StockItemRow`) renders the `UiState`.
- Intent: UI emits events (`UiEvent`) to the ViewModel (`onEvent`) to mutate the state or trigger side-effects.

Data flow:
1. `StockPriceViewModel` initializes with 25 symbols and random prices.
2. When updates are toggled on, the ViewModel:
   - Starts a 2-second ticker that applies ±0.5%–±3% deltas and sends each `PriceUpdate` JSON to the WebSocket.
   - Subscribes to the WebSocket echo. Incoming JSON is parsed to `PriceUpdate` and applied to the list, which remains sorted by price (desc).
3. `WebSocketRepository` manages the socket lifecycle, exposes connection `StateFlow` and messages `SharedFlow` with reconnection and logging.
4. UI collects `uiState` with lifecycle awareness and reacts to state changes.

Package structure:
- `domain/` — `StockSymbol`, `ConnectionState`, `PriceUpdate`, `StockSymbols`
- `data/` — `WebSocketRepository`
- `presentation/` — `StockPriceViewModel`
- `presentation/ui/` — `MainScreen`, `TopBar`, `StockItemRow`

## Setup Instructions
Prerequisites:
- Android Studio (Hedgehog+ recommended) with AGP supporting Compose Compiler 1.5.14
- Android SDK: compile/target 34, minSdk 26
- JDK 17

Clone repository:
```
git clone <your_repo_url>
```

Open project in Android Studio → let Gradle sync complete. If not automatic: File → Sync Project with Gradle Files.

Build & Run:
- Select a device/emulator with internet access.
- Run the `app` configuration.

Notes:
- INTERNET permission is declared in the manifest.

## Features Implemented
Core:
- Live WebSocket connection and reconnection
- Start/Stop price updates
- Real-time list updates with sorting
- Connection status indicator in TopBar
- Snackbar error handling
- Animated price change flash and change indicators

Bonus:
- `StockItemRow` smooth background flash
- Animated Start/Stop button colors
- Test coverage across ViewModel, repository, and UI

Screenshots / Demo:
- Add images or GIFs here (e.g., `docs/screenshot_1.png`).

## Technical Details
WebSocket:
- Endpoint: `wss://ws.postman-echo.com/raw`
- `WebSocketRepository` uses OkHttp’s `WebSocketListener`.
- Emits `ConnectionState` via `StateFlow` and messages via `SharedFlow(replay=0, extraBufferCapacity=64)`.

State management:
- `StockPriceViewModel` exposes `uiState: StateFlow<UiState>`.
- UI events: `StartPriceUpdates`, `StopPriceUpdates`, `TogglePriceUpdates`, `ClearError`.

Compose UI:
- `MainScreen` uses `Scaffold`, `TopBar`, `LazyColumn` with dividers and padding.
- `TopBar` shows title, connection status emoji, and Start/Stop button.
- `StockItemRow` shows symbol, price (USD), up/down indicators, and animated flash.

Coroutines:
- `viewModelScope` drives periodic updates and flow collections.
- Repository uses `SupervisorJob + Dispatchers.IO` and a `Mutex` for thread-safety.
- Exponential backoff for reconnects.

## Testing
Run unit tests:
```
./gradlew test
```

Run instrumented UI tests (emulator/device required):
```
./gradlew connectedAndroidTest
```

Includes:
- ViewModel unit tests (`src/test/.../StockPriceViewModelTest.kt`)
- Repository tests with mocked OkHttp (`src/test/.../WebSocketRepositoryTest.kt`)
- Compose UI tests for `TopBar`, `StockItemRow`, and `MainScreen` (`src/androidTest/...`)

## Assumptions and Trade-offs
- Price updates are mock-generated in ViewModel; the echo server just reflects messages back.
- Postman Echo is not a market data feed; it’s for demonstration/testing.
- 25 symbols are efficient on modern devices; animations are lightweight and use derived state to reduce recomposition.
- Simplifications: No DI framework; simple ViewModel factory creates `WebSocketRepository`.

## Future Enhancements
- Replace echo server with a real market data source
- Add paging, search, and favorites
- Persist user settings and last-known prices
- Add historical charts and spark lines
- Theming options and tablet/landscape layouts
- Migrate to DI (Hilt/Koin) and multi-module structure

## License and Contact
- License: MIT (or your preferred license)
- Contact: Fahad Iqbal - fahad.iqbal88@gmail.com
- GitHub: https://github.com/FahadIqbal
- LinkedIn: https://www.linkedin.com/in/fahad-iqbal-07496a28/
- Project Repository: https://github.com/FahadIqbal/RealTimePriceTracker.git

