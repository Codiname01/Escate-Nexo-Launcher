# ESCATE NEXO — NX07A: selector experimental de espacios

Base: Lawnchair 16 / Launcher3; rama previa `feature/nexo-v0.1-home-intro`.

## Implementado en esta rama

- Nueva opción **NEXO Spaces** en el menú de pulsación prolongada del escritorio.
- Diálogo con cuatro modos: **Trabajo, Estudio, Creación, Música**.
- Guarda el último modo seleccionado en preferencias privadas de la app (`nexo_spaces_v1/active_space`).
- Al volver a abrir el selector, mantiene marcada la última selección.
- Mantiene funcionales el cajón de aplicaciones, iconos, gestos, widgets y demás funciones de Lawnchair.

## No implementado todavía

- Los iconos del escritorio **no cambian** automáticamente al seleccionar un espacio.
- Cada espacio todavía **no** tiene un conjunto de aplicaciones o widgets independiente.
- No se fuerza aún un escritorio de una sola página.
- No se agregan permisos nuevos, servicios ni ejecución de comandos.

## Prueba manual en el emulador (NX07A)

1. Comprobar que el APK generado pertenece a `pe.escate.nexo.debug`.
2. Abrir ESCATE NEXO como pantalla principal del emulador.
3. Mantener pulsado un área vacía del escritorio.
4. Elegir **NEXO Spaces**.
5. Elegir **Música**, cerrar el diálogo, abrirlo nuevamente y confirmar que Música está marcada.
6. Cambiar a **Trabajo**, cerrar y abrir nuevamente NEXO; confirmar que sigue marcada.
7. Abrir y ejecutar una aplicación desde el cajón para comprobar que el motor base no perdió funcionalidad.
8. Recuperar Pixel Launcher desde Ajustes de Android si se desea.

**Criterio de aceptación:** el selector se abre sin cierres, persiste el modo elegido y no rompe ninguna función base. La aparición del botón por sí sola no significa que existan aún escritorios separados.

**Siguiente etapa NX07B:** modelo de paneles del escritorio por espacio, mapeo explícito de accesos e integración con la UI, conservando un único espacio de pantalla física.

## Evidencia

No atribuir estado PASS a esta rama hasta contar con compilación local verificada, SHA-256 del APK y una prueba visual en el emulador. Conservar licencias y atribución de Lawnchair.
