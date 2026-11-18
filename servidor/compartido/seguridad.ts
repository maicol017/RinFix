import { timingSafeEqual } from "node:crypto";
import { ErrorRinFix } from "./http.js";

/**
 * Comprueba la clave que la app manda en la cabecera `x-rinfix-clave`.
 *
 * Es una barrera básica: la clave va dentro del APK y alguien decidido podría sacarla.
 * Por eso también hay un límite de uso y un tope de gasto en la consola de Anthropic.
 * Cuando entre Supabase, el servidor verificará además la sesión de cada persona.
 */
export function verificarClaveApp(request: Request): void {
  const esperada = process.env.RINFIX_CLAVE_APP;
  if (!esperada) {
    throw new ErrorRinFix(503, "sin_configurar", "El servidor todavía no está configurado.");
  }
  const recibida = Buffer.from(request.headers.get("x-rinfix-clave") ?? "");
  const correcta = Buffer.from(esperada);
  if (recibida.length !== correcta.length || !timingSafeEqual(recibida, correcta)) {
    throw new ErrorRinFix(401, "no_autorizado", "Esta versión de la app no está autorizada. Actualízala.");
  }
}

const visitas = new Map<string, number[]>();

/**
 * Límite de solicitudes por dirección IP, guardado en la memoria de la función.
 * Frena abusos obvios (alguien repitiendo la misma llamada), aunque no es un límite global:
 * Vercel puede tener varias copias de la función al tiempo.
 */
export function limitarUso(request: Request, nombre: string, maximo: number, ventanaMs: number): void {
  const ip = request.headers.get("x-forwarded-for")?.split(",")[0]?.trim() || request.headers.get("x-real-ip") || "desconocida";
  const clave = `${nombre}:${ip}`;
  const ahora = Date.now();
  const recientes = (visitas.get(clave) ?? []).filter((momento) => ahora - momento < ventanaMs);
  if (recientes.length >= maximo) {
    throw new ErrorRinFix(
      429,
      "muchas_solicitudes",
      "Hiciste muchas solicitudes seguidas. Espera unos minutos e inténtalo de nuevo.",
    );
  }
  recientes.push(ahora);
  visitas.set(clave, recientes);
  if (visitas.size > 5_000) visitas.clear();
}
