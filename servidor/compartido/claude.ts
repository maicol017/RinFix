import Anthropic from "@anthropic-ai/sdk";
import { ErrorRinFix } from "./http.js";

/** Modelos por defecto. Se pueden cambiar en Vercel sin tocar código. */
export const ModeloAnalisis = process.env.MODELO_ANALISIS || "claude-sonnet-5-5";
export const ModeloAsistente = process.env.MODELO_ASISTENTE || "claude-haiku-5-5";

let cliente: Anthropic | null = null;

/** Cliente de Claude. La llave vive solo en las variables de entorno de Vercel. */
function claude(): Anthropic {
  const llave = process.env.ANTHROPIC_API_KEY;
  if (!llave) {
    throw new ErrorRinFix(503, "sin_configurar", "El servidor todavía no tiene la llave de Claude.");
  }
  cliente ??= new Anthropic({ apiKey: llave, maxRetries: 2, timeout: 50_000 });
  return cliente;
}

interface PedidoJson {
  modelo: string;
  sistema: string;
  mensajes: Anthropic.MessageParam[];
  esquema: Record<string, unknown>;
  maxTokens: number;
  esfuerzo: "low" | "medium" | "high";
}

/**
 * Pide a Claude una respuesta con la forma exacta del esquema (salida estructurada)
 * y la devuelve ya convertida desde JSON.
 */
export async function pedirJson(pedido: PedidoJson): Promise<unknown> {
  let respuesta: Anthropic.Message;
  try {
    respuesta = await claude().messages.create({
      model: pedido.modelo,
      max_tokens: pedido.maxTokens,
      system: pedido.sistema,
      messages: pedido.mensajes,
      output_config: {
        effort: pedido.esfuerzo,
        format: { type: "json_schema", schema: pedido.esquema },
      },
    });
  } catch (error) {
    throw traducirError(error);
  }

  if (respuesta.stop_reason === "refusal") {
    throw new ErrorRinFix(422, "rechazada", "No pudimos procesar esa solicitud. Intenta con otra foto o con otra pregunta.");
  }
  if (respuesta.stop_reason === "max_tokens") {
    throw new ErrorRinFix(502, "incompleta", "La respuesta quedó incompleta. Inténtalo de nuevo.");
  }
  const bloque = respuesta.content.find((parte) => parte.type === "text");
  if (!bloque || bloque.type !== "text") {
    throw new ErrorRinFix(502, "sin_respuesta", "No llegó una respuesta. Inténtalo de nuevo.");
  }
  try {
    return JSON.parse(bloque.text);
  } catch {
    throw new ErrorRinFix(502, "respuesta_invalida", "La respuesta llegó con un formato inesperado. Inténtalo de nuevo.");
  }
}

/** Errores de la API de Claude convertidos en mensajes para la persona. Los detalles van al registro. */
function traducirError(error: unknown): ErrorRinFix {
  if (error instanceof Anthropic.RateLimitError) {
    return new ErrorRinFix(429, "claude_ocupado", "El análisis está muy solicitado ahora. Espera un momento e inténtalo de nuevo.");
  }
  if (error instanceof Anthropic.AuthenticationError || error instanceof Anthropic.PermissionDeniedError) {
    console.error("La llave de Claude no sirve:", error.message);
    return new ErrorRinFix(503, "llave_invalida", "El servidor no pudo conectarse con el análisis. Avísale al equipo de RinFix.");
  }
  if (error instanceof Anthropic.APIConnectionTimeoutError) {
    return new ErrorRinFix(504, "tardo_mucho", "El análisis tardó demasiado. Inténtalo de nuevo.");
  }
  if (error instanceof Anthropic.APIConnectionError) {
    return new ErrorRinFix(502, "sin_conexion_claude", "El servidor no pudo conectarse con el análisis. Inténtalo de nuevo.");
  }
  if (error instanceof Anthropic.InternalServerError) {
    return new ErrorRinFix(503, "claude_no_disponible", "El análisis no está disponible en este momento. Inténtalo en unos minutos.");
  }
  if (error instanceof Anthropic.BadRequestError) {
    console.error("Claude rechazó la solicitud:", error.message);
    return new ErrorRinFix(502, "solicitud_rechazada", "No pudimos analizar esa solicitud. Intenta con otra foto.");
  }
  console.error("Error llamando a Claude:", error);
  return new ErrorRinFix(500, "interno", "Algo falló en el servidor. Inténtalo de nuevo en un momento.");
}
