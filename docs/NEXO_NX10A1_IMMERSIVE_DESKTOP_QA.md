# ESCATE NEXO NX10A1 — Escritorios extendidos, cabecera compacta y carpetas visuales

## Punto de partida comprobado
NX10A `b001519b45c2a7d191e83b92b75d090adc81858a` compiló e instaló en Pixel 7. Capturas de la persona muestran Trabajo con Chrome, Cámara, carpeta "juan", un diálogo de carpeta con aplicaciones, dock y búsqueda nativos visibles. La persistencia después de reiniciar, y el aislamiento de los otros tres espacios, todavía requieren QA.

## Alcance de NX10A1
1. **Cabecera compacta** en modo Escritorios propios: título NEXO · <espacio>, cuatro chips de espacios, Opciones y acceso permanente a **Volver al escritorio original**. Gesture Studio y configuración de gestos continúan en Opciones. En modo original se conserva el panel anterior.
2. **Escritorio expandido**: el contenedor visual NEXO usa toda la región disponible entre el panel compacto y el área protegida del dock/búsqueda. La cuadrícula permanece de cuatro columnas y permite desplazamiento vertical dentro de esa región si se agregan muchos iconos. Esta fase **no** permite posicionamiento libre en celdas arbitrarias; el orden se cambia por pulsación larga.
3. **Vista previa de carpetas**: muestra hasta cuatro iconos reales de aplicaciones, sin cambiar sus componentes o etiquetas guardadas.
4. **Carpetas visuales**: tocar una carpeta con contenido abre una cuadrícula con iconos y etiquetas de sus aplicaciones. La opción Editar permite añadir/quitar componentes sin modificar el formato de datos. Una carpeta vacía lleva al editor.
5. Conserva `nexo_workspace_layouts_v1`, `nexo_independent_workspaces_v1`, `nexo_spaces_v1` y las preferencias previas. No se migra ni se sobrescribe `Launcher3 Favorites`. Ningún método cambia el HOME predeterminado.

## Fuera de alcance
- Widgets propios (NX10B): no se simulan ni se asignan appWidgetIds.
- Arrastrar y soltar iconos entre celdas, páginas nativas múltiples, gestos de M/T sobre fondo cuando el modo propio está activo.
- Dock independiente; el dock compartido de Launcher3 se conserva.

## Gate de emulador
`scripts/NEXO_NX10A1_GATE.ps1` exige rama y SHA remoto exacto, checkout limpio, verifica presencia de implementación y compila `assembleLawnWithQuickstepGithubDebug`. Examina package/versión, instala y comprueba versión **solo si hay exactamente un emulador autorizado**. No toca teléfonos físicos ni usa `pm clear`, `adb uninstall` o `git reset`.

## QA manual obligatorio
1. Confirmar instalados SHA/versión de NX10A1 en Pixel 7, sin modificar Redmi NX08C.
2. Activar Escritorios propios, comprobar que aparece la barra compacta y que **Volver al escritorio original** funciona con un toque.
3. Confirmar que la zona visual rellena el espacio vertical entre el panel y el dock; no tapa búsqueda ni barra de navegación.
4. Comprobar que Trabajo todavía conserva **Chrome**, **Cámara** y la carpeta **juan**, sin borrar ni cambiar sus datos al instalar la actualización.
5. Abrir juan y observar miniaturas, abrir una aplicación desde su cuadrícula; volver y editar la lista.
6. Añadir 10–16 aplicaciones (si el emulador dispone de ellas) y comprobar desplazamiento y orden mediante menú largo.
7. Cambiar a Música / Estudio / Creación, verificar que cada uno mantiene sus propias aplicaciones, y volver a Trabajo sin contaminación.
8. Reiniciar launcher, comprobar persistencia de datos y de la activación del modo propio.
9. Desactivar Escritorios propios; el Launcher3 original, widgets nativos, dock y búsqueda deben reaparecer con el mismo contenido.
10. Abrir App Drawer, usar gesto NX08C sobre iconos nativos y comprobar que ninguna vista queda invisible o bloqueada.
11. Revisar logcat `NEXO_WORKSPACE`, `AndroidRuntime` en caso de crash.

## Observaciones de riesgo
- El diseño visual todavía es una capa NEXO sobre Launcher3, no un reemplazo de la DB nativa, ni un escritorio con widget-host por espacio.
- La cuadrícula anclada arriba ocupa el contenedor completo pero las posiciones aún son secuenciales; mejorar la colocación libre será una fase posterior.
- El alert visual usa `ScrollView`; carpetas con listas muy grandes deben inspeccionarse por recortes.
- El crash histórico `NoSuchFieldError: ic_qsb_search` del cajón se trata como un incidente separado si vuelve a ocurrir.
- No declarar Build/Runtime PASS antes de recibir evidencia del emulador.

## Estado
Publicación GitHub First y revisión estática; falta compilar y validar Android en Pixel 7.
