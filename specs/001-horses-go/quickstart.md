# Validación de Horses Go

Ejecutar los comandos desde la raíz del repositorio. El [plan](plan.md)
documenta la arquitectura y las versiones de las dependencias.

## Preparación

1. Desde la raíz del repositorio, verificar JDK 17 y Gradle 9.3.1.
2. Preparar Android SDK 36 y un dispositivo o emulador de 64 bits con
   Android API 23 o superior.
3. En una Mac con Apple Silicon, preparar Xcode y un simulador iOS ARM64.
   El mínimo de la aplicación y de su framework compartido es iOS 15.

```bash
java -version
./gradlew --version
xcodebuild -version
xcrun simctl list devices available
adb devices -l
```

## Compilación y pruebas

```bash
./gradlew :shared:allTests :androidApp:assembleDebug
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
./gradlew :androidApp:installDebug
```

Para iOS, abrir `iosApp/iosApp.xcodeproj` en Xcode y ejecutar el esquema
`iosApp` en un simulador ARM64. El paso de compilación del host invoca
`:shared:embedAndSignAppleFrameworkForXcode`. Para Android, abrir Horses Go
desde el emulador o dispositivo.

## Escenarios de aceptación

| Escenario | Acción | Resultado esperado |
| --- | --- | --- |
| Niveles | En cada plataforma, elegir cada nivel y pulsar **Cargar mapa**. | Se abre el tablero correcto con posiciones de FR-002 y turno negro. Se puede completar una partida por nivel. |
| Legalidad y victoria | Realizar saltos legales y tocar destinos ocupados, destruidos y fuera de alcance. Ejercitar una victoria de cada participante. | Los rechazos conservan el estado. Cada salto válido destruye el origen. Se anuncia al autor del último salto y se bloquean más jugadas. |
| Profundidad de MiniMax | Ejecutar casos controlados en `commonTest` con ramas abiertas y terminales justo en el límite. | La primera jugada candidata cuenta para la profundidad. Se aplican los límites 4, 6 y 8, la evaluación terminal y las reglas comunes. |
| Reapertura | Tras un salto confirmado, anotar posiciones, casillas destruidas y turno. Terminar el proceso conservando los datos y reabrir. Repetir con una partida terminada. | Se recupera la última copia, incluido el resultado. Si la copia indica turno blanco, se calcula y aplica una sola jugada. |
| Resultado tardío | En `commonTest`, controlar cuándo termina MiniMax, cargar otro mapa y liberar la respuesta anterior. | El tablero, turno y copia guardada corresponden al mapa nuevo. |
| Recuperación ante fallos | En pruebas con almacén controlado, devolver datos inválidos o rechazar una escritura. | La copia inválida abre el selector con aviso. La escritura rechazada conserva el estado y deja aplicar el cambio una sola vez al reintentar. |
| Tiempo de respuesta | Medir 30 turnos blancos con jugadas disponibles por nivel en un dispositivo de referencia por plataforma. Medir desde que se muestra el turno blanco hasta que aparece la jugada. | Al menos 29 de 30 respuestas por nivel y dispositivo aparecen en dos segundos o menos. |
| Primer uso | Observar a cinco personas que juegan por primera vez. Medir desde el selector hasta el primer salto legal. | Al menos cuatro cargan un mapa y hacen un salto legal en menos de 60 segundos sin ayuda. |

Registrar versiones, dispositivos y resultados con la revisión de
implementación. El [contrato de interacción](contracts/gameplay.md)
describe los estados que deben observarse.

## Verificar el tablero nativo

1. En Android y en iOS, carga los mapas 4×4, 5×5, 6×6 y 8×8. Comprueba las
   coordenadas y los dos caballos. Las aperturas son aleatorias.
2. Toca uno de los destinos verdes. Tras la respuesta de la máquina, comprueba
   el contador de jugadas y las casillas destruidas. En 5×5, abre el árbol
   MiniMax y vuelve al tablero.
3. Cierra y reabre la app. Comprueba que se mantienen las dos posiciones y
   las casillas destruidas. Para comparar el aspecto entre plataformas,
   abre la misma partida guardada en ambos dispositivos.

## Resultados de implementación

Validación local del 2026-11-19 con JDK 17, Gradle 9.3.1 y Xcode 27.0
(27A266a). El APK debug se instaló en un Pixel 8a emulado con Android 16.
El proyecto iOS compiló para simulador y para dispositivo ARM64 sin firma.
La ejecución de iOS se probó en un iPhone 16 Pro simulado con iOS 18.6.

En ambos entornos se completaron partidas de los tres niveles, se
confirmaron victorias de ambos participantes en principiante y se
recuperaron desde MMKV el tablero de una partida en curso y el de una
partida terminada después de cerrar el proceso. También se verificaron
el rechazo de un destino no válido y el reemplazo de mapa. Las pruebas
comunes verifican el descarte de una respuesta tardía tras el reemplazo.

Las 18 pruebas compartidas pasaron en Android host y en iOS Simulator ARM64.

### SC-003 · Respuesta de la máquina

La medición automatizada comienza antes de tocar la casilla y termina
cuando la respuesta blanca aparece en pantalla. Incluye la entrada
de prueba y el renderizado.

| Nivel | Pixel 8a emulado ≤ 2 s | Máximo | iPhone 16 Pro simulado ≤ 2 s | Máximo |
| --- | ---: | ---: | ---: | ---: |
| Principiante | 30/30 | 0,578 s | 30/30 | 0,560 s |
| Intermedio | 30/30 | 0,530 s | 30/30 | 0,596 s |
| Avanzado | 30/30 | 0,944 s | 30/30 | 0,631 s |

### SC-004 · Primer uso

Pendiente de observar a cinco personas que jueguen por primera vez.
