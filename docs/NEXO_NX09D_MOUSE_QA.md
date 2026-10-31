# NX09D: capturar y diagnosticar gestos reales dibujados con el mouse

## Evidencia previa confirmada
- NX09C instalado en emulador: `16.Dev.(a879b72)`.
- Botón `Simular M`: `synthetic expected=draw_m detected=draw_m` y `practice=false action=space:music`, `active_space=music`.
- La persona indica que dibujar manualmente con el mouse **no** activa Música/Trabajo; al arrastrar desde arriba Android puede desplegar las notificaciones. La simulación no prueba la captura física.

## NX09D, cambios incluidos
- Botón **Lienzo grande para mouse**: abre cuadro de dibujo dentro de Gesture Studio, separado del ScrollView de configuración. El trazo debe comenzar dentro del área oscura, lejos de la barra de estado.
- Confirmación visible y logcat `NEXO_STUDIO` para `touch_down`, `touch_move points=`, `touch_up`, `touch_cancelled`.
- Si se rechaza la forma, mostrar total de puntos, error contra las plantillas M/T y umbral activo para diferenciar un defecto de entrada, una trayectoria demasiado corta y el clasificador.
- Continúan intactos M/T sintéticos, modo ensayo/ejecución, asignaciones y espacios; no se interceptan eventos globales ni se reemplazan gestos del sistema.
- La T se prueba con **un único trazo continuo**: izquierda a derecha, regresar al centro y bajar. Dibujar una T con dos pulsaciones independientes aún no es compatible.

## Plan de prueba (solo emulador)
1. Confirmar commit publicado y script `scripts/NEXO_NX09D_GATE.ps1`: GitHub First, build, aapt, instalación de una única instancia de emulador, versión exacta instalada.
2. Abrir NEXO > Studio > Lienzo grande. En **ensayo**: clic izquierdo sostenido, dibujar una M dentro del marco.
3. Si sale `Clic o toque capturado`: la app recibió ACTION_DOWN. Si `Trazando: N puntos`: llegaron eventos MOVE. El mensaje de detección o rechazo, y log `stroke M=... T=... threshold=...`, dirá si el clasificador aceptó el trazo.
4. Cambiar a **modo ejecución**, desde Trabajo dibujar M, confirmar espacio Música. Desde Música, T en un trazo, confirmar Trabajo. Comparar con Simular M/T.
5. Si abre la barra de notificaciones y **no** hay `touch_down`: el gesto empezó fuera del cuadro o fue interceptado por Android; no modificar umbrales por ese motivo.
6. Diagnóstico: `C:\Sdk\platform-tools\adb.exe -s emulator-5554 logcat -d -v time -s NEXO_STUDIO:D AndroidRuntime:E '*:S'`.
7. Verificar regresión de NX08C en iconos, dock, app drawer; NX09C botones simulados y persistencia de asignaciones.

## Conservación / limitaciones
- El script del gate solo instala `pe.escate.nexo.debug` en un emulador y **nunca** en Redmi. No desinstala, limpia datos, cambia HOME ni borra el workspace.
- No hay compilación Android validada aquí; esperar BUILD PASS del usuario. Una prueba exitosa de `Simular M` no sustituye la prueba de interacción con mouse.
- Aún no añadimos dibujo sobre el fondo del escritorio ni gestos globales, por conflicto con gestos de Android.
