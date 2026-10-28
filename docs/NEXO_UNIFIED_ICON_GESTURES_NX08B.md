# ESCATE NEXO — NX08B: gesto de icono unificado

Fecha: 2026-10-27. Base: NX08A1 (`96f37bf2c2a6b4dbf2cb61a0bd26972ef864857f`).

## Qué implementa esta rama

Se reutiliza **la misma asignación** de NEXO (preferencias `nexo_icon_gestures_v1`, clave `espacio|ComponentName`) en:

- Iconos del panel NEXO: pulsación larga para configurar, toque normal para abrir la app base, deslizamiento arriba para ejecutar la secundaria.
- Iconos normales del escritorio y del dock construidos con `BubbleTextView` compatible: **deslizar arriba** ejecuta la secundaria de NEXO asociada a la aplicación **en el espacio activo**.
- Editor visible **Gestos**: ahora lista las aplicaciones lanzables instaladas, incluso si el panel de NEXO del espacio activo está vacío. Permite configurar Chrome del dock o cualquier otra app instalada.
- El panel NEXO mantiene el mismo aspecto y guarda las preferencias previas.
- Al cambiar de espacio NEXO, el resolvedor consulta las preferencias de ese espacio sin reiniciar el launcher.
- Se limita a aplicaciones del perfil principal; accesos del perfil administrado conservan el manejo nativo.
- **Prioridad:** si el icono tiene un gesto SWIPE_UP explícito de Lawnchair, prevalece ese gesto; NEXO actúa solo como fallback.
- No hay nuevos permisos, overlay de sistema, accesibilidad ni modificaciones a la base de datos del escritorio.

## Límites

1. No se reemplaza el motor nativo de toque de Launcher3. En iconos normales, Lawnchair detecta un *fling* vertical; requiere gesto rápido y desplazamiento apreciable (la implementación base usa umbral de 100 px y de velocidad).
2. Los iconos internos del panel NEXO tienen detección propia de deslizamiento; su umbral es distinto. Comprobar ambos en el emulador.
3. Los gestos de fondo de escritorio, pinza, rotación, dibujo de letras y carpetas especiales quedan para posteriores gates.
4. Iconos de widgets, carpetas, apps en perfiles gestionados y accesos directos complejos no están cubiertos por la asignación NEXO de esta versión.
5. El dock se contempla a través de `BubbleTextView` en modo workspace; confirmar su variante y el comportamiento real con el emulador. No declarar PASS para el dock sin esa prueba.

## Prueba NX08B en Pixel 7

1. Verificar checkout limpio, SHA remoto, compilación Gradle y paquete `pe.escate.nexo.debug`.
2. Instalar la APK **solo en un emulador**; confirmar la versión instalada y el proceso abierto.
3. Seleccionar Trabajo en el panel (puede estar vacío).
4. Pulsar **Gestos** -> Chrome -> Cámara.
5. En el **Chrome del dock o escritorio ORIGINAL** (fuera del panel), deslizar hacia arriba rápidamente al menos 100 px. Debe abrir Cámara.
6. Tocar Chrome normalmente: debe abrir Chrome.
7. Añadir Chrome al panel superior y verificar que ese mismo deslizamiento abre Cámara.
8. Cambiar a Música, configurar Chrome -> Galería, cambiar entre Trabajo y Música. En cada uno debe abrir la app correspondiente.
9. Comprobar que un gesto nativo de Lawnchair sobre Chrome tiene prioridad si ya existe.
10. Reiniciar launcher y verificar persistencia. Confirmar cajón vertical, widgets y dock sin regresiones.

## Rollback

La rama anterior `feature/nexo-v0.1-icon-gestures-hotfix` permanece disponible. No desinstalar APK ni borrar datos sin respaldo. Si falla, enviar log Kotlin/Gradle y no instalar en Redmi todavía.

**Estado actual:** código publicado, compilación y pruebas de interacción pendientes. No declarar NX08B PASS hasta la evidencia.
