# ESCATE NEXO — NX08A1: reparación de gestos de iconos

## Síntoma
El usuario indicó que NX08A «no funciona todavía», sin salida de compilación o confirmación de versión instalada. Se detectó en el código un riesgo de cancelación de toques por el contenedor de Launcher3 y que no existía forma visible de configurar el gesto si la pulsación larga fallaba.

## Cambio (publicado; compilación pendiente)

- En los iconos propios de NEXO se solicita mantener el flujo táctil desde ACTION_DOWN y se libera en ACTION_UP/CANCEL, evitando que el padre intercepte MOVE durante el gesto.
- Se incorpora botón **Gestos** en cabecera del panel. Ofrece los iconos configurados en el espacio activo y permite asignar una segunda aplicación sin depender de la pulsación larga.
- El gesto original de NX08A permanece: tocar icono = abrir su app; pulsación larga = configurar; deslizar hacia arriba sobre icono = abrir la segunda app.
- La asignación se guarda con la misma clave que NX08A para conservar preferencias anteriores.
- No se modifica la navegación del sistema, el HOME predeterminado, el dock, la base de datos de Launcher3 ni permisos.
- Se mantiene el cajón de aplicaciones.

## Criterio de prueba en Pixel 7

1. GitHub First: rama y SHA remoto iguales al checkout limpio.
2. Kotlin / Gradle BUILD SUCCESSFUL; APK auditada con `aapt`.
3. APK instalada en exactamente un emulador, sin tocar dispositivos físicos. Confirmar versión `versionName=16.Dev.(<sha7>)` en Android y que coincide con el APK.
4. En Música deben verse apps ya elegidas (ej. Chrome y Cámara).
5. Tocar **Gestos** en la cabecera, elegir Chrome y asignar Cámara.
6. Debe aparecer `↑ Chrome`; al deslizar hacia arriba **sobre el icono** debe abrir Cámara.
7. Tocar Chrome normalmente debe abrir Chrome.
8. Mantener pulsado el icono Chrome: debe abrir el selector de segunda app.
9. Cambiar a Creación no altera los gestos de Música; al volver, persisten.
10. Si la aplicación no inicia, recoger `adb logcat -b crash`; si el gesto no se dispara, capturar vídeo / `adb logcat` y verificar la versión.

## Limitaciones

- No se agregan aún pellizcos, rotaciones ni gestos de dibujo de letras.
- No se añade un motor global de gestos al teléfono: los gestos adicionales se limitan al panel NEXO.
- La opción Gestos resuelve la **configuración** aun si el emulador interpreta mal la pulsación larga; requiere prueba de deslizamiento real.
- No afirmar PASS sin comprobar compilación y el gesto en Android.

## Rollback
Mantener en GitHub la rama previa `feature/nexo-v0.1-home-panel`. `adb install -r` de la APK previa verificada puede fallar si el versionCode/firma/rollback no permiten downgrade: no desinstalar ni borrar datos sin respaldo. Desde Ajustes se puede recuperar el HOME original.
