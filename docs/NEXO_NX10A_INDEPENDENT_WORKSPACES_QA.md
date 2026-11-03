# ESCATE NEXO NX10A — Escritorios independientes, fase 1

## Objetivo
Dar a Trabajo, Estudio, Música y Creación una distribución propia y persistente de **aplicaciones y carpetas**, visible como escritorio, en vez de una simple lista de accesos dentro del panel NEXO.

## Límites honestos
- NX10A es un **escritorio visual independiente superpuesto** en el DragLayer de Lawnchair; no modifica ni sustituye aún la base de datos `Favorites` del escritorio Launcher3. El escritorio original queda **INVISIBLE**, no eliminado, cuando se activa NX10A; vuelve a mostrarse al desactivar.
- La primera fase tiene iconos de apps independientes por espacio; crear carpetas, escoger sus aplicaciones, abrirlas, mover un tile una posición antes/después y quitar tiles. NO tiene drag-and-drop de iconos, ni páginas múltiples nativas, ni widgets independientes todavía.
- NX10B trabajará widget hosting (asignación de `appWidgetId`, consentimiento de Android, ciclo de vida y restauración), con pruebas de respaldo. No prometer widgets funcionales en NX10A.
- El dock y cajón son los originales de Launcher3; el **dock todavía es compartido**. Las aplicaciones y carpetas del área de escritorio NEXO se almacenan por espacio.
- M/T sobre fondo de NX09F se pausa mientras el modo de escritorios independientes está activo, para no interceptar los controles del nuevo escritorio. Su preferencia se conserva para recuperarla al salir.
- El modo NX10A está **desactivado por defecto**; se activa/desactiva desde el panel. No se solicita cambiar HOME ni root ni permisos especiales.

## Diseño y almacenamiento
- `nexo_independent_workspaces_v1/enabled`: booleano opt-in. No modifica la base de datos de Launcher3.
- `nexo_workspace_layouts_v1/layout_work|layout_study|layout_music|layout_create`: JSON v1 con lista de tiles (`app`, `folder`), etiqueta, ComponentName serializado, e IDs de apps en carpetas.
- El almacén rechaza estados desconocidos, esquemas incompatibles o datos corruptos **sin sobrescribir el valor existente**; escribe mediante `commit()` y solo si la entrada actual es válida.
- `NexoWorkspaceSurface` dibuja un grid 4 columnas con iconos reales de aplicaciones, abre apps por intents estándar, listas de carpetas, edición y ordenación por menú largo. No elimina el icono de la aplicación del escritorio nativo.
- `LawnchairLauncher` mantiene la instancia y alterna visibilidad del workspace original y la superficie NX10A según el estado NORMAL.
- `NexoSpaces` mantiene IDs estables de cuatro espacios; las funciones heredadas cambian espacio y la interfaz se refresca.
- `NexoHomePanel`: toggle visible para NX10A, conserva modos de espacio; oculta selector anterior de apps mientras el modo propio está activo para evitar editores contradictorios.

## QA / GATES obligatorios
1. GITHUB FIRST: commit y SHA remotos exactos para rama `feature/nexo-nx10a-independent-desktop-layouts`, sin modificaciones locales.
2. `scripts/NEXO_NX10A_GATE.ps1`: compilar variante `assembleLawnWithQuickstepGithubDebug`, inspeccionar badging (paquete y versión), instalar/abrir **solo si hay exactamente un emulador**. Nunca toca teléfonos físicos.
3. NX10A OFF por defecto: pantalla nativa original, dock, iconos y widgets siguen presentes, y NX09F responde si está activado.
4. Activar NX10A: el área de escritorio nativa se oculta, aparece área NEXO. Trabajo inicialmente vacío. Añadir Chrome y Cámara, comprobar iconos propios y que se abren.
5. Crear carpeta llamada "CAD", abrir y editar contenido eligiendo aplicaciones. Long-press: mover antes/después, quitar sin desinstalar apps.
6. Cambiar a Música: añadir solo reproductor musical. Volver a Trabajo: los otros iconos y carpeta siguen allí sin contaminar Música.
7. Estudio y Creación siguen aislados. Repetir tras reiniciar launcher para validar persistencia JSON y modo ON.
8. Desactivar NX10A: vuelve inmediatamente el workspace nativo anterior, **con sus iconos y widgets**, sin reconstruir Launcher3 DB. Activar otra vez: tiles de NX10A persisten.
9. Abrir cajón vertical, dock, menú largo, lanzar apps y comprobar sus estados normales. Abrir Studio y volver.
10. Revisar `adb -s emulator-5554 logcat -d -v time -s NEXO_WORKSPACE:D AndroidRuntime:E '*:S'` y capturas visuales.
11. **No instalar en Redmi** hasta que las pruebas de emulador y recuperación aprueben.

## Riesgos / decisiones abiertas
- El host de widgets debe diseñarse para evitar perder `appWidgetId` o causar fugas; no añadir widgets ficticios al JSON.
- Animaciones de transición Launcher3 pueden modificar visibilidad/alpha de Workspace; probar entrada/salida al cajón y Overview.
- El grid NX10A es una superficie de aplicación encima del launcher nativo y no implementa aún páginas nativas ni DnD. Es un escalón funcional, no afirmación de integración final.
- Fallo histórico independiente de búsqueda `NoSuchFieldError ic_qsb_search` requiere hotfix específico si vuelve a reproducirse.
- Para NX10C, evaluar esquema y migración transaccional de workspace nativo con rollback y copia de seguridad antes de tocar Favorites.

## Estado
Código y gate publicados; inspección estática realizada. BUILD y QA en Android pendientes.
