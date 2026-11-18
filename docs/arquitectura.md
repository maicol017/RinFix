# Arquitectura de RinFix

## Carpetas de la app

Todo el código de la app está en `app/src/main/java/com/rinfix/app/`.

| Carpeta | Qué contiene |
| --- | --- |
| `apertura/` | La animación del rin al abrir la app |
| `diseno/` | Colores, letras, formas, sombras y el tema general |
| `componentes/` | Piezas que se reutilizan: botones, tarjetas, etiquetas, barra inferior |
| `navegacion/` | Las rutas entre pantallas, la barra inferior de cada rol y la puerta que decide qué se ve al abrir la app (cuenta, desbloqueo, activar huella o la app) |
| `cliente/` | Pantallas del cliente: Inicio, Analizar, Resultado, Agendar, Órdenes, Seguimiento, Pago, Asistente, Garaje |
| `taller/` | Pantallas del trabajador: Panel, Trabajos, Detalle del trabajo, Registrar avance, Agenda |
| `cuenta/` | Inicio de sesión, registro, desbloqueo con huella y perfil |
| `datos/` | Modelos y servicios. El análisis y el asistente usan Claude a través del servidor (`AnalisisConClaude`, `AsistenteConClaude`, `Servidor.kt`) cuando el APK tiene su dirección; lo demás usa servicios de ejemplo. `Servicios.kt` es el único lugar que cambia al conectar Supabase |
| `seguridad/` | Desbloqueo con huella: la llave del teléfono (Android Keystore), el candado guardado y la ventana de huella del sistema. RinFix nunca recibe datos biométricos |
| `ia/` | Trabajo con las fotos en el teléfono: la revisión de luz en vivo y la preparación antes de enviarlas (enderezar y aligerar). Después, la detección del rin con el modelo YOLO |

Cada pantalla tiene su archivo, por ejemplo `cliente/Inicio.kt`. Cuando una pantalla necesite datos o reglas, tendrá además un archivo de lógica con el mismo nombre, por ejemplo `cliente/InicioLogica.kt`.

`MainActivity.kt`, `AndroidManifest.xml` y los archivos `build.gradle.kts` conservan su nombre porque Android los exige así.

## Otras carpetas del repositorio

| Carpeta | Qué contiene |
| --- | --- |
| `servidor/` | Funciones de Vercel que hablan con Claude: análisis de fotos y asistente. La llave de Claude solo vive ahí |
| `ml/` | Entrenamiento de los modelos YOLO en Google Colab |
| `docs/` | Documentación del proyecto y las imágenes del README |
