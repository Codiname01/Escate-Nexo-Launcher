# ESCATE NEXO — NX07B: apps por espacio

Branch: `feature/nexo-v0.1-space-apps`  
Base: NX07A (`06bf343c61655aed9bf287a078f8ac115f0c1161`)

## Alcance implementado (pendiente de compilar y probar)

**NEXO Spaces** en el menú de pulsación larga del escritorio abre Trabajo, Estudio, Creación y Música. Al elegir uno se abre un panel propio.

- **Elegir aplicaciones:** abre una lista de aplicaciones instaladas y permite asignar una o varias al espacio actual.
- **Abrir aplicación:** se abre tocando su nombre en el panel de ese espacio.
- **Cambiar espacio:** regresa a los cuatro espacios sin navegación horizontal.
- **Persistencia:** los identificadores Android `ComponentName` quedan guardados por espacio en preferencias privadas.
- **Compartición:** una misma aplicación puede figurar en varios espacios.
- **Sin bloqueo:** todas las aplicaciones siguen disponibles en el cajón original de Launcher3.

### Ejemplo

- Trabajo: Archivos, Calculadora, Correo.
- Estudio: navegador, notas, lectura.
- Creación: Cámara, Galería, audio.
- Música: reproductor y partituras.

La selección inicial no se rellena con nombres de apps inventadas: el usuario elige entre las que están realmente instaladas en su dispositivo.

## Límites de esta etapa

- La selección aparece en un **panel de diálogo accesible desde el escritorio**, no reordena todavía los iconos visibles de Launcher3 ni instala widgets por modo.
- El escritorio principal conserva sus iconos originales. NX07C tratará la integración visual en la propia pantalla de inicio.
- El editor lista actividades `ACTION_MAIN/CATEGORY_LAUNCHER` del usuario principal; los perfiles de trabajo gestionados, múltiples usuarios y accesos profundos quedan fuera del alcance inicial.
- No se modifican base de datos de Launcher3, configuraciones de seguridad, permisos, widgets ni la selección de HOME predeterminado.

## Criterios de aceptación NX07B

1. Gradle `assembleLawnWithQuickstepGithubDebug` termina correctamente.
2. APK tiene package `pe.escate.nexo.debug` y etiqueta `ESCATE NEXO (Dev)`.
3. El emulador abre el launcher y el menú **NEXO Spaces**.
4. Elegir Trabajo -> **Elegir aplicaciones** -> seleccionar dos aplicaciones instaladas -> **Guardar**.
5. Confirmar que las dos aplicaciones se muestran en el panel de Trabajo y se abren.
6. Elegir Música -> seleccionar una aplicación distinta -> Guardar.
7. Regresar a Trabajo: debe conservar sus dos aplicaciones, sin mostrar la exclusiva de Música.
8. Cerrar y volver a abrir NEXO: las listas persisten.
9. El cajón normal sigue mostrando las aplicaciones instaladas.

**Estado:** cambios publicados en GitHub; hasta verificar compilación y UI, NX07B no es PASS.

## Próximo gate

NX07C: interfaz visual integrada en pantalla principal con acceso a los espacios, sin desplazamiento lateral. No hacerlo como migración irreversible de los elementos de Launcher3.
