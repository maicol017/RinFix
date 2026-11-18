import { ModeloAnalisis, ModeloAsistente } from "../compartido/claude.js";
import { json } from "../compartido/http.js";

/**
 * GET /api/estado — para comprobar desde el navegador o la app que el servidor responde.
 * Nunca muestra las llaves: solo dice si están puestas.
 */
export function GET(): Response {
  return json({
    ok: true,
    servicio: "RinFix",
    configurado: Boolean(process.env.ANTHROPIC_API_KEY && process.env.RINFIX_CLAVE_APP),
    modelos: { analisis: ModeloAnalisis, asistente: ModeloAsistente },
  });
}
