# ESCATE NEXO — NX09E: dibujos M/T sobre zonas vacías del escritorio

## Objetivo
Dibujar **M** para la acción configurada en Gesture Studio (predeterminado Música) y **T** para Trabajo desde el fondo vacío del escritorio, **sin abrir Gesture Studio**. La función puede desactivarse inmediatamente desde el panel superior de NEXO.

## Política de seguridad
- OFF por defecto: `nexo_desktop_gestures_v1/enabled=false`. Toggle visible en NexoHomePanel.
- Se observan MotionEvents **después** de `super.dispatchTouchEvent`; no se interceptan ni consumen. Se preservan las rutas de los toques originales sobre iconos, widgets, dock y cajón. ATENCIÓN: como Lawnchair recibe los eventos originales, el scroll entre páginas puede producirse durante una letra; eso es una limitación pendiente de QA.
- Se exige `LauncherState.NORMAL`, un único dedo/puntero, inicio y trayectoria dentro del `workspace` visible y lejos de bordes del sistema (incluida la barra superior) y del hotseat.
- Se descartan trazos que toquen iconos, carpetas, widgets o el panel NEXO; también trazos cortos, demasiado rápidos, largos y multi-touch.
- Usa exactamente el clasificador M/T y las asignaciones del `NexoGestureStudio` de NX09D. El interruptor de *Modo ensayo* de Studio NO controla el escritorio: el toggle opt-in del escritorio es independiente.
- No añade permisos, servicios de accesibilidad, overlays de sistema ni configuración del launcher predeterminado.
- La función es local al escritorio NEXO; no captura movimientos en otras aplicaciones.

## Archivos
- `lawnchair/src/app/lawnchair/nexo/NexoDesktopGesture.kt`: observa y filtra los eventos, persistencia OFF/ON.
- `lawnchair/src/app/lawnchair/LawnchairLauncher.kt`: engancha el observador en `dispatchTouchEvent` de forma pasiva, excluye el panel NEXO.
- `lawnchair/src/app/lawnchair/nexo/NexoGestureStudio.kt`: reutiliza el reconocedor y `perform()` para acciones configuradas.
- `lawnchair/src/app/lawnchair/nexo/NexoHomePanel.kt`: interruptor claro ON/OFF.
- `lawnchair/res/values/strings.xml`: traducciones.
- `scripts/NEXO_NX09E_GATE.ps1`: GitHub First y compilación/instalación **solo en un emulador**.

## Matriz QA
1. Corroborar GitHub First, SHA exacto, compilar, inspeccionar APK, instalar en un único `emulator-XXXX`; no tocar Redmi.
2. Sin pulsar toggle: verificar que los trazos M/T no cambian de espacio.
3. Activar `Dibujar en escritorio: ACTIVADO`.
4. En Trabajo, dibujar una M **de un solo trazo** en el centro de una zona vacía, por debajo del panel NEXO y por encima del dock, a más de 40dp de los laterales. Confirmar `NEXO_DESKTOP down/up/gesture=draw_m` y Music en el panel.
5. En Música, dibujar una T de **un solo trazo continuo** (izquierda-derecha, regresar al centro, bajar). Confirmar `gesture=draw_t` y Trabajo.
6. Repetir desde otra página si existe, verificando que no genere falsos positivos en gestos de swipe simples.
7. Tocar y deslizar iconos con mapeos NX08C, abrir el cajón vertical, usar dock y long-press; estos gestos deberían seguir siendo entregados a Launcher3 porque el observador no los consume.
8. Abrir Gesture Studio, simular M/T y usar el lienzo grande como antes.
9. Desactivar el toggle; dibujar de nuevo M/T y verificar que no cambian espacios.
10. Reiniciar el launcher, verificar persistencia del toggle sin alterar las configuraciones de espacio ni mapas de iconos.

## Diagnóstico
```powershell
& 'C:\Sdk\platform-tools\adb.exe' -s emulator-5554 logcat -d -v time -s 'NEXO_DESKTOP:D' 'NEXO_STUDIO:D' 'AndroidRuntime:E' '*:S'
```

## Gates / estado
- Código y script de QA publicados en GitHub, sin compilar aquí.
- No declarar BUILD PASS, GESTURE PASS, REGRESSION PASS o Redmi PASS hasta que haya registros del usuario.
- POSIBLE LIMITACIÓN: el detector deliberadamente pasivo no evita que un intento de M/T active también el deslizamiento entre páginas. No prometer que no habrá conflictos: documentar cualquier conflicto y considerarlo bloqueante si impide la funcionalidad.
- Una caída heredada `NoSuchFieldError ic_qsb_search` en All Apps se analiza por separado si se reproduce en NX09E.
