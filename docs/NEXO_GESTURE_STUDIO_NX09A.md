# ESCATE NEXO — NX09A / Gesture Studio

Base estable preservada: NX08C (`aa781b27ced097aaff9f7d55428a48b345e76569`). El usuario confirmó que NX08C permite gestos sobre los iconos normales. NX09A está **experimental** hasta compilar y validarse.

## Objetivo NX09

Gestos tipo Nova con acciones propias de NEXO, editor visual y acceso desde el escritorio. Para evitar romper iconos, dock, cajón y navegación del sistema, **NX09A reconoce gestos SOLO dentro del lienzo de Gesture Studio**. No afirmar que pellizcar/rotar/dibujar letras funciona todavía sobre el fondo completo del launcher. Esto se evaluará en un gate posterior una vez que los reconocedores estén probados.

## Funcionalidad NX09A

- Nuevo `NexoGestureStudio.kt` con diálogo y lienzo de dibujo.
- Acceso desde el botón **Gesture Studio** en el panel superior; también entrada en pulsación larga sobre fondo del escritorio (menú de Launcher3).
- Reconocimiento de 5 movimientos:
  - Dibujar M en **un trazo continuo** (recorre izquierda-abajo, izquierda-arriba, valle, pico, derecha-abajo): espacio Música por defecto.
  - Dibujar T en **un trazo continuo** (horizontal izquierda-derecha, regresa al centro y baja): espacio Trabajo por defecto.
  - Pellizcar con dos dedos: selector de espacios NEXO.
  - Expandir dos dedos: abrir la lista de aplicaciones del espacio.
  - Rotar dos dedos: cambiar al siguiente espacio.
- Cada uno es configurable desde su fila: Sin acción, Seleccionar espacio, Siguiente espacio, Mostrar aplicaciones del espacio, espacio Trabajo/Estudio/Creación/Música o cualquier app instalada y lanzable.
- Preferencias locales `nexo_gesture_studio_v1`, independientes de `nexo_icon_gestures_v1`; conserva gestos sobre iconos.
- Reconocedor de letras: plantilla geométrica por resample de 48 puntos, normalización por caja envolvente y puntuación de distancia. Heurístico experimental, no OCR ni modelo ML; requiere ensayo para falsos positivos y sensibilidad.
- Gestos dobles: escala 0.72 (pinch) / 1.38 (spread), rotación 42 grados, evitando confundir expansión con rotación.
- Registro Android `NEXO_STUDIO` con clasificación y acción; no se solicita permiso de accesibilidad, ni se intercepta navegación global.
- Botón **Studio** debajo de la cabecera (no en la misma fila) para preservar espacio del panel en móviles.

## QA recomendado / Pixel 7

1. GitHub SHA remoto exacto, working tree limpio, submódulos, Gradle `assembleLawnWithQuickstepGithubDebug` y `BUILD SUCCESSFUL`.
2. `aapt dump badging`: `pe.escate.nexo.debug`, versión `16.Dev.(<sha7>)`, nombre ESCATE NEXO.
3. Iniciar exactamente un emulador. `adb install -r`, confirmar versión instalada con dumpsys y abrir.
4. Abre **Studio** desde panel y desde pulsación larga del fondo.
5. En el lienzo dibuja M (trazo único) -> cambia a Música; vuelve al Studio, dibuja T -> cambia a Trabajo.
6. En el emulador, simula dos dedos (Ctrl + ratón, si el emulador soporta la función), pellizca -> selector; expande -> apps; rota -> siguiente espacio.
7. Toca fila Dibujar M, asígnala a Cámara; dibuja M -> abre Cámara. Vuelve a asignar Música y verifica persistencia tras reinicio.
8. Prueba que tocar Chrome abre Chrome, y que swipe-up NX08C en Chrome fuera del panel sigue lanzando la app secundaria.
9. Comprueba cajón vertical, dock, widgets, menú del fondo y popup nativo; detectar posibles solapamientos del nuevo botón.
10. Captura `adb logcat -d -s NEXO_STUDIO:D AndroidRuntime:E *:S`; si no reconoce letras, probar trazos más grandes y claros y revisar puntuación.

## Gates

- **GITHUB FIRST**: publicado en rama separada, SHA verificable.
- **COMPILACIÓN:** pendiente de evidencia del equipo Windows.
- **INSTALACIÓN:** pendiente.
- **TEST FUNCIONAL:** pendiente.

## Próximo gate: NX09B

Tras QA, considerar gestos sobre fondo vacío del escritorio (no en apps, widgets, dock ni cajón) mediante un controlador de toque que compruebe el objetivo inicial y respete los gestos nativos; mantener opción de desactivar y pruebas de navegación. No implementar a ciegas sin evidencia de NX09A.

## Rollback

NX08C sigue publicada. No borrar datos, reinstalar con `adb uninstall` ni hacer `git reset --hard` por defecto. No desplegar a dispositivo físico hasta validar regresiones.
