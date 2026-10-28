# ESCATE NEXO — NX08A / Gestos de iconos

## Contexto

NX07C1 ya crea un panel visual de espacios en el escritorio. Lawnchair 16 contiene gestos globales y por icono de Launcher3, pero los iconos nuevos del **panel NEXO** son vistas propias, no `BubbleTextView`; por eso no reciben automáticamente los gestos de Lawnchair.

## Implementación NX08A (experimental; aún sin compilación validada)

En los iconos visibles del panel NEXO:
- **Toque breve**: abre la aplicación representada por ese icono (comportamiento anterior).
- **Pulsación larga**: abre `Deslizar arriba en [Aplicación]` y permite elegir otra aplicación instalada, o `Sin acción`.
- **Deslizar hacia arriba sobre ese icono**: abre la aplicación secundaria configurada, si aún está instalada.
- Las asignaciones se guardan localmente con clave espacio + componente original. Una aplicación puede tener gestos diferentes en Trabajo y Música.
- Cuando se configura el gesto, aparece una flecha `↑` delante del título del icono.
- Sin servicios de accesibilidad ni permisos nuevos. No intercepta gestos de navegación ni deslizados en toda la pantalla.

Lawnchair ya admite configuraciones de gesto de fondo e iconos tradicionales en sus propios menús; esas funciones se conservan.

## Criterios de aceptación

1. GitHub First: commit remoto exacto, checkout limpio, sin modificaciones locales ni reescrituras de historia.
2. `assembleLawnWithQuickstepGithubDebug` termina `BUILD SUCCESSFUL`.
3. `aapt` valida paquete `pe.escate.nexo.debug` y nombre `ESCATE NEXO (Dev)`.
4. En el emulador, seleccionar Música y tocar Chrome: debe abrir Chrome.
5. Mantener pulsado Chrome dentro del panel de Música, elegir Cámara, confirmar `↑ Chrome`.
6. Deslizar hacia arriba **sobre Chrome en el panel NEXO**: debe abrir Cámara, sin abrir Chrome.
7. Tocar Chrome normalmente: debe seguir abriendo Chrome.
8. Cambiar a Trabajo: la configuración de Música no debe alterar el gesto de otra app o espacio.
9. Reiniciar ESCATE NEXO: la asignación debe persistir.
10. En el selector de gesto, elegir `Sin acción`: desaparece la flecha y se recupera el comportamiento no asignado.
11. Verificar que los gestos existentes en ajustes y el cajón vertical siguen funcionando.
12. Volver a Pixel Launcher si se observa cualquier anomalía.

## Preparación del teléfono real

NO sustituir todavía el launcher principal del Redmi hasta completar la prueba NX08A en emulador. Confirmar versión Android, compatibilidad, recuperación sencilla del launcher stock y copia de seguridad. En HyperOS, los **gestos de navegación del sistema** podrían no funcionar con launchers de terceros; esto es independiente de los gestos internos de ESCATE NEXO.

## Siguiente etapa

- NX08B: acciones configurables para doble toque y dos dedos (reutilizando las opciones nativas de Lawnchair).
- NX08C: acciones `Siguiente espacio` / `Espacio Música` desde gestos globales, si realmente mejoran el uso diario.
- Lanzamiento beta en Redmi solo después de verificar Android, firma del APK y rollback.

**Estado:** código GitHub publicado, pendiente de `BUILD SUCCESSFUL`, instalación y prueba visual del usuario.
