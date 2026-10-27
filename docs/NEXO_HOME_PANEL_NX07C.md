# ESCATE NEXO — NX07C: panel visual por espacio

## Por qué NX07B parecia igual

NX07B administraba los accesos mediante cuadros de diálogo, pero el escritorio original de Lawnchair/Launcher3 mantenía sus iconos. No era un fallo del selector; era una limitación del alcance.

## Diseño de NX07C (experimental)

En el inicio de ESCATE NEXO se monta un panel reversible con:
- Cabecera que indica el espacio seleccionado.
- Cuatro botones visibles: **Trabajo, Estudio, Creación y Música**.
- Hasta ocho iconos de aplicaciones instaladas y elegidas por el usuario para ese espacio.
- Botón **Elegir aplicaciones**, enlazado al editor existente NX07B.
- Acceso al resto de aplicaciones del espacio si hay más de ocho.
- Actualización inmediata de iconos al cambiar el espacio, sin desplazamiento horizontal.
- Persistencia utilizando las preferencias locales de NX07B (no se migran ni borran).

### Límites y seguridad

- El **panel cambia sus iconos**; NO intercambia todavía las páginas completas de Launcher3.
- Los iconos permanentes del dock, los widgets y otros accesos originales **siguen ahí**, sin alterarse.
- El cajón de apps, pantalla Overview y demás estados continúan bajo el motor Launcher3. El panel se oculta al salir del estado HOME normal.
- La primera visualización usa un bloque flotante en la parte superior. Hay que verificar en el emulador que no interfiera con widgets ni gestos.
- Los iconos se abren mediante actividades Android exportadas ya accesibles; no se ejecuta shell ni se solicitan permisos adicionales.
- No se modifican base de datos, permisos, estados HOME predeterminados, firmas, ni se desinstala el launcher anterior.
- Se mantiene el código y atribución upstream.

## GATE NX07C — Criterios de aceptación

1. Verificación GitHub First del SHA remoto, checkout limpio y submódulos verificados.
2. Gradle `assembleLawnWithQuickstepGithubDebug` **BUILD SUCCESSFUL**.
3. `aapt dump badging`: package `pe.escate.nexo.debug` y label `ESCATE NEXO (Dev)`.
4. Instalar solo en un emulador Pixel 7 de prueba, conservando sus datos (`adb install -r`).
5. Desde escritorio, comprobar cabecera y los cuatro botones.
6. Asignar *Gallery* y *Maps* a Creación, y *Calculadora* a Trabajo.
7. Pulsar Creación y Trabajo: **el panel debe mostrar apps diferentes**, sin abrir un selector previo.
8. Pulsar un icono del panel: debe abrir la app correspondiente.
9. Comprobar que el dock original sigue operativo y que el panel desaparece al entrar al cajón.
10. Cerrar y volver a abrir el launcher: el contenido seleccionado permanece.

**Estado actual:** código publicado, compilación y ejecución pendientes de las pruebas del usuario. No declarar PASS hasta registrar evidencias.

## Siguientes gates

- NX07D: diseño compacto y configurable, accesibilidad y compatibilidad con widgets.
- NX08: motor de comandos en la interfaz de NEXO.
- Separación real de páginas completas por espacio: investigación separada por mayor riesgo a la base de datos y widgets de Launcher3.
