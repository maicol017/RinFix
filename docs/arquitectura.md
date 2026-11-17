# Arquitectura de RinFix

## Carpetas de la app

Todo el código de la app está en `app/src/main/java/com/rinfix/app/`.

| Carpeta | Qué contiene |
| --- | --- |
| `apertura/` | La animación del rin al abrir la app |
| `diseno/` | Colores, letras, formas, sombras y el tema general |
| `componentes/` | Piezas que se reutilizan: botones, tarjetas, etiquetas, barra inferior |
| `navegacion/` | Las rutas entre pantallas, la barra inferior de cada rol y la puerta que decide qué se ve al abrir la app (cuenta, desbloqueo, activar huella o la app) |
| `cliente/` | Pantallas del cliente: Inicio, Analizar, Asistente, Órdenes |
| `taller/` | Pantallas del trabajador: Panel, Agenda, Trabajos |
| `cuenta/` | Inicio de sesión, registro, desbloqueo con huella y perfil |
| `datos/` | Lo que la app necesita de cuentas y datos. Hoy lo cumplen servicios de ejemplo; `Servicios.kt` es el único lugar que cambia al conectar Supabase |
| `seguridad/` | Desbloqueo con huella: la llave del teléfono (Android Keystore), el candado guardado y la ventana de huella del sistema. RinFix nunca recibe datos biométricos |
| `ia/` (próximamente) | Detección del rin en el teléfono y llamadas al servidor de análisis |

Cada pantalla tiene su archivo, por ejemplo `cliente/Inicio.kt`. Cuando una pantalla necesite datos o reglas, tendrá además un archivo de lógica con el mismo nombre, por ejemplo `cliente/InicioLogica.kt`.

`MainActivity.kt`, `AndroidManifest.xml` y los archivos `build.gradle.kts` conservan su nombre porque Android los exige así.

## Otras carpetas del repositorio

| Carpeta | Qué contiene |
| --- | --- |
| `ml/` | Entrenamiento de los modelos YOLO en Google Colab |
| `docs/` | Documentación del proyecto |
