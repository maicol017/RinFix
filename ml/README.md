# ml/ — Modelos de visión de RinFix

Aquí vive el entrenamiento de los modelos YOLO que la app usa en el teléfono.

## Contenido

- `rinfix_yolo_colab.ipynb`: cuaderno para Google Colab. Descarga datasets públicos de Roboflow Universe, los une con las clases de RinFix, entrena dos modelos (rin y daños), los mide y los exporta a LiteRT (`.tflite`).
- `models/`: aquí se copian los modelos descargados de Drive. No se sube a GitHub.

## Cómo usarlo

1. Abre [Google Colab](https://colab.research.google.com) → `Archivo → Subir cuaderno` → elige `rinfix_yolo_colab.ipynb`.
2. Cambia el entorno a GPU T4.
3. Guarda tu clave de Roboflow en los Secretos de Colab con el nombre `ROBOFLOW_API_KEY`.
4. `Entorno de ejecución → Ejecutar todo`.
5. Descarga de Google Drive la carpeta `RinFix/ml/modelos/<fecha>` y cópiala en `ml/models/`.

## Por qué dos modelos

Los datasets públicos están etiquetados a medias: los de rines no marcan daños y los de daños no marcan el rin. Por eso un modelo encuentra el rin y otro busca daños dentro del recorte.

## Licencias

- Datos: datasets de Roboflow Universe con licencia CC BY 4.0. El cuaderno genera `CREDITOS.md` con las fuentes usadas.
- Detector: Ultralytics YOLOv8, licencia AGPL-3.0. Si el modelo va dentro del APK, el código del proyecto debe publicarse bajo AGPL-3.0 o se necesita la licencia Enterprise de Ultralytics.
