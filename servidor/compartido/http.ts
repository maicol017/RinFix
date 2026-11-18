/** Error con un mensaje listo para mostrarse en la app, y el código HTTP que le corresponde. */
export class ErrorRinFix extends Error {
  constructor(
    readonly estado: number,
    readonly codigo: string,
    mensaje: string,
  ) {
    super(mensaje);
  }
}

/** Respuesta JSON que nunca se guarda en caché: cada análisis es de una persona. */
export function json(cuerpo: unknown, estado = 200): Response {
  return Response.json(cuerpo, { status: estado, headers: { "Cache-Control": "no-store" } });
}

/** Convierte cualquier error en una respuesta con `{ error: { codigo, mensaje } }`. */
export function errorComoRespuesta(error: unknown): Response {
  if (error instanceof ErrorRinFix) {
    return json({ error: { codigo: error.codigo, mensaje: error.message } }, error.estado);
  }
  console.error("Error inesperado:", error);
  return json(
    { error: { codigo: "interno", mensaje: "Algo falló en el servidor. Inténtalo de nuevo en un momento." } },
    500,
  );
}

/** Lee el cuerpo como JSON, con un límite de tamaño. */
export async function leerJson(request: Request, maximoBytes: number): Promise<unknown> {
  const largo = Number(request.headers.get("content-length") ?? "0");
  if (largo > maximoBytes) {
    throw new ErrorRinFix(413, "muy_grande", "La solicitud pesa demasiado. Envía fotos más livianas.");
  }
  const texto = await request.text();
  if (texto.length > maximoBytes) {
    throw new ErrorRinFix(413, "muy_grande", "La solicitud pesa demasiado. Envía fotos más livianas.");
  }
  try {
    return JSON.parse(texto);
  } catch {
    throw new ErrorRinFix(400, "json_invalido", "La solicitud llegó dañada. Inténtalo de nuevo.");
  }
}

// ---------- Lectura segura de datos que llegan de afuera ----------

export function esObjeto(valor: unknown): valor is Record<string, unknown> {
  return typeof valor === "object" && valor !== null && !Array.isArray(valor);
}

export function texto(valor: unknown, maximo: number, porDefecto = ""): string {
  return typeof valor === "string" ? valor.trim().slice(0, maximo) : porDefecto;
}

export function numero(valor: unknown, minimo: number, maximo: number, porDefecto: number): number {
  return typeof valor === "number" && Number.isFinite(valor) ? Math.min(maximo, Math.max(minimo, valor)) : porDefecto;
}

export function booleano(valor: unknown, porDefecto = false): boolean {
  return typeof valor === "boolean" ? valor : porDefecto;
}

export function lista(valor: unknown, maximo: number): unknown[] {
  return Array.isArray(valor) ? valor.slice(0, maximo) : [];
}

/** El valor de la lista que coincide sin importar mayúsculas, o `null`. */
export function unoDe<T extends string>(valor: unknown, opciones: readonly T[]): T | null {
  if (typeof valor !== "string") return null;
  const buscado = valor.trim().toLowerCase();
  return opciones.find((opcion) => opcion.toLowerCase() === buscado) ?? null;
}
