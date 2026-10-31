# ESCATE NEXO — NX09C / Gesture Studio sin dibujar

**Motivo:** Al tratar de dibujar M/T sobre el emulador integrado en Android Studio, el usuario activa el panel de ajustes y notificaciones de Android en vez del gesto de NEXO. En la captura, además, Android Studio tenía seleccionado el proyecto independiente `My Application`, que no es nuestro launcher.

**Base:** NX09B `027403e0025cd201c78cf821e7760eb39ec7f146`. La NX08C del Redmi (`aa781b27ced097aaff9f7d55428a48b345e76569`) debe permanecer intacta.

## Cambio

- Debajo del lienzo Gesture Studio hay botones **Simular M** y **Simular T**.
- Cada botón construye un trazo sintético dentro del área del lienzo, lo muestra y llama al **mismo reconocedor geométrico** que las entradas del dedo/mouse. Después usa la lógica de ejecución/ensayo existente.
- Modo ensayo activado por defecto: los botones detectan pero **no ejecutan** acción. Modo ejecución: siguen la acción configurada en las filas (Música/Trabajo por defecto).
- `NEXO_STUDIO` ahora registra `synthetic expected=... detected=...`. Esto prueba algoritmo y acciones; **NO** valida la recepción real de eventos táctiles, por lo que no debe declararse el gesto a mano PASS todavía.
- La ruta táctil mantiene su lienzo; captura además la posición final de ACTION_UP para trazos reales.
- No añade permisos, no desactiva la barra de notificaciones, no bloquea los gestos del sistema, no modifica gestos NX08C de iconos, dock, ni del cajón.

## Protocolo de QA

1. GitHub First: comprobar SHA remoto exacto, checkout limpio en carpeta independiente.
2. Ejecutar `scripts/NEXO_NX09C_GATE.ps1` para compilar e instalar solamente en exactamente un emulador.
3. Verificar versión instalada en `package: name='pe.escate.nexo.debug'` con sufijo del SHA de NX09C.
4. En Pixel 7, iniciar paquete `pe.escate.nexo.debug/app.lawnchair.LawnchairLauncher` por `adb shell am start`. **No** pulsar Run del proyecto `My Application`.
5. En pantalla principal NEXO, tocar **Gesture Studio** bajo la cabecera. Desplazar el contenido del diálogo si los botones quedan más abajo.
6. Con **Modo ensayo**, pulsar **Simular M**: aparece `Ensayo: Detectado: Dibujar M`; comprobar en logcat `synthetic expected=draw_m detected=draw_m`. Repetir con T. El espacio no debe cambiar.
7. Cambiar a **Modo ejecución** y tocar **Simular M**: debe activar espacio Música; luego reabrir Studio y pulsar **Simular T**: Trabajo.
8. Prueba de regresión: swipe-up sobre Chrome del dock/escritorio/cajón, toque ordinario y panel NEXO.
9. Si Android abre su panel de notificaciones al intentar dibujar, **no** insistir con el mouse de arriba hacia abajo. El gesto de sistema tiene precedencia; reabrir la app y usar los botones o, en una prueba posterior, dibujar solo dentro del lienzo y sin comenzar en el borde superior.
10. Capturar registros si falla: `adb -s emulator-5554 logcat -d -v time -s NEXO_STUDIO:D NEXO_GESTURE:D AndroidRuntime:E *:S`.
11. Solo desplegar al Redmi tras QA y solicitud explícita del usuario.

## Gates

- GitHub: código fuente publicado.
- Validación matemática local: simulaciones estándar M/T producen la clase esperada con el mismo esquema de resample, normalización y error de plantilla.
- Compilación Gradle: **pendiente**.
- Instalación y pruebas reales en Pixel 7: **pendientes**.

**Futuro:** estudiar la activación opcional sobre zonas vacías del escritorio con exclusión estricta de bordes, iconos, dock, widgets y áreas reservadas por Android. No interferir con la barra de notificaciones.
