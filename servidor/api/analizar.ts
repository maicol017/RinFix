import type Anthropic from "@anthropic-ai/sdk";
import { ModeloAnalisis, pedirJson } from "../compartido/claude.js";
import {
  ErrorRinFix,
  booleano,
  errorComoRespuesta,
  esObjeto,
  json,
  leerJson,
  lista,
  numero,
  texto,
  unoDe,
} from "../compartido/http.js";
import { limitarUso, verificarClaveApp } from "../compartido/seguridad.js";

/**
 * POST /api/analizar — Claude mira de 1 a 3 fotos del mismo rin y devuelve los daños visibles.
 *
 * Entra: { fotos: [base64 JPEG/PNG/WebP], material, pulgadas, vehiculo }
 * Sale:  { esRin, calidadFoto, danos: [...], recomendaciones, requiereInspeccion, alerta, resumen, modelo }
 *
 * Los precios NO salen de aquí: la app los calcula con el catálogo del taller.
 */

const MaximoFotos = 3;
/** Por foto, en caracteres de base64 (unos 1,1 MB de imagen). La app las manda de ~300 KB. */
const MaximoBase64 = 1_500_000;
const MaximoCuerpo = 4_300_000;

const Tipos = ["Rayon", "Raspadura", "Deformacion", "Fisura", "Corrosion", "Pintura", "Otro"] as const;
const Gravedades = ["Leve", "Moderada", "Alta"] as const;
const Materiales = ["Aleacion", "Acero", "Desconocido"] as const;
const Calidades = ["buena", "borrosa", "oscura", "lejana", "tapada"] as const;

type TipoImagen = "image/jpeg" | "image/png" | "image/webp";

interface Solicitud {
  fotos: { datos: string; tipo: TipoImagen }[];
  material: (typeof Materiales)[number];
  pulgadas: number | null;
  vehiculo: string | null;
}

export async function POST(request: Request): Promise<Response> {
  try {
    verificarClaveApp(request);
    limitarUso(request, "analizar", 12, 10 * 60_000);
    const solicitud = validar(await leerJson(request, MaximoCuerpo));
    const crudo = await pedirJson({
      modelo: ModeloAnalisis,
      sistema: Instrucciones,
      mensajes: [{ role: "user", content: contenido(solicitud) }],
      esquema: Esquema,
      maxTokens: 6_000,
      esfuerzo: "medium",
    });
    return json(normalizar(crudo, solicitud.fotos.length));
  } catch (error) {
    return errorComoRespuesta(error);
  }
}

// ---------- Entrada ----------

function validar(cuerpo: unknown): Solicitud {
  if (!esObjeto(cuerpo)) throw new ErrorRinFix(400, "solicitud_invalida", "La solicitud llegó incompleta.");
  const fotos = lista(cuerpo.fotos, MaximoFotos + 1);
  if (fotos.length === 0) throw new ErrorRinFix(400, "sin_fotos", "Envía al menos una foto del rin.");
  if (fotos.length > MaximoFotos) throw new ErrorRinFix(400, "muchas_fotos", `Envía máximo ${MaximoFotos} fotos.`);

  return {
    fotos: fotos.map((foto, indice) => {
      if (typeof foto !== "string" || foto.length < 100) {
        throw new ErrorRinFix(400, "foto_invalida", `La foto ${indice + 1} llegó vacía.`);
      }
      if (foto.length > MaximoBase64) {
        throw new ErrorRinFix(413, "foto_pesada", `La foto ${indice + 1} pesa demasiado.`);
      }
      const tipo = tipoDeImagen(foto);
      if (!tipo) throw new ErrorRinFix(400, "formato_invalido", `La foto ${indice + 1} no es JPG, PNG ni WebP.`);
      return { datos: foto, tipo };
    }),
    material: unoDe(cuerpo.material, Materiales) ?? "Desconocido",
    pulgadas: typeof cuerpo.pulgadas === "number" && cuerpo.pulgadas >= 12 && cuerpo.pulgadas <= 24
      ? Math.round(cuerpo.pulgadas)
      : null,
    vehiculo: texto(cuerpo.vehiculo, 60) || null,
  };
}

/** Reconoce el formato por los primeros bytes, sin confiar en lo que diga la app. */
function tipoDeImagen(base64: string): TipoImagen | null {
  const inicio = Buffer.from(base64.slice(0, 24), "base64");
  if (inicio[0] === 0xff && inicio[1] === 0xd8 && inicio[2] === 0xff) return "image/jpeg";
  if (inicio[0] === 0x89 && inicio[1] === 0x50 && inicio[2] === 0x4e && inicio[3] === 0x47) return "image/png";
  if (inicio.toString("ascii", 0, 4) === "RIFF" && inicio.toString("ascii", 8, 12) === "WEBP") return "image/webp";
  return null;
}

function contenido(solicitud: Solicitud): Anthropic.ContentBlockParam[] {
  const partes: Anthropic.ContentBlockParam[] = [];
  solicitud.fotos.forEach((foto, indice) => {
    partes.push({ type: "text", text: `Foto ${indice}:` });
    partes.push({ type: "image", source: { type: "base64", media_type: foto.tipo, data: foto.datos } });
  });
  const datos = [
    `Material del rin: ${solicitud.material === "Desconocido" ? "no se sabe" : solicitud.material === "Aleacion" ? "aleación" : "acero"}.`,
    `Tamaño: ${solicitud.pulgadas ? `${solicitud.pulgadas} pulgadas` : "no se sabe"}.`,
    `Vehículo: ${solicitud.vehiculo ?? "no se sabe"}.`,
  ].join(" ");
  partes.push({
    type: "text",
    text: `${datos}\nRevisa las fotos y responde con el formato pedido.`,
  });
  return partes;
}

// ---------- Instrucciones y forma de la respuesta ----------

const Instrucciones = `Eres el sistema de diagnóstico visual de RinFix, una app de un taller de rines en Bucaramanga, Colombia.
Recibes de 1 a 3 fotos del mismo rin de carro, tomadas con el celular del cliente. Tu trabajo es identificar los daños VISIBLES en el rin.

Tipos de daño (usa exactamente estos nombres):
- Rayon: rayones en la pintura o en la cara del rin.
- Raspadura: raspado contra el andén en el borde o labio exterior.
- Deformacion: borde doblado, golpe, abolladura u ovalado.
- Fisura: grieta o posible fisura en el metal.
- Corrosion: óxido, sulfatación o pintura levantada por corrosión.
- Pintura: pintura desgastada, descascarada o decolorada en general.
- Otro: un daño del rin que no encaje en los anteriores.

Reglas:
- Reporta solo lo que se ve en las fotos. No inventes daños. Si algo no se ve con claridad, márcalo con confirmado=false y una confianza baja.
- La llanta (el caucho) no es el rin: ignora sus daños salvo que afecten el borde del rin.
- Suciedad, polvo o reflejos no son daños.
- Una fisura o una deformación siempre requieren inspección presencial: requiereInspeccion=true y una alerta que diga que no use el rin en carretera hasta que el taller lo revise.
- Nunca digas que el rin es seguro. No des precios ni tiempos: eso lo calcula el taller.
- Si las fotos no muestran un rin de carro, responde esRin=false y danos vacío.
- calidadFoto: "buena" si se puede evaluar; si no, el problema principal (borrosa, oscura, lejana o tapada).
- zona: el recuadro aproximado del daño en la foto indicada, con x, y, ancho y alto entre 0 y 1 (x e y son la esquina de arriba a la izquierda). Si no puedes ubicarlo, ubicada=false.
- descripcion: frase corta (máximo 60 caracteres), por ejemplo «Rayón en el borde exterior».
- explicacion: una o dos frases sencillas para alguien que no sabe de rines.
- recomendaciones: máximo 3, prácticas y cortas.
- resumen: una frase con lo principal.
- Escribe en español de Colombia, tuteando, sin tecnicismos.`;

const Esquema: Record<string, unknown> = {
  type: "object",
  properties: {
    esRin: { type: "boolean", description: "true si las fotos muestran un rin de carro" },
    calidadFoto: { type: "string", enum: [...Calidades] },
    danos: {
      type: "array",
      items: {
        type: "object",
        properties: {
          tipo: { type: "string", enum: [...Tipos] },
          descripcion: { type: "string" },
          gravedad: { type: "string", enum: [...Gravedades] },
          confianza: { type: "number", description: "Qué tan seguro estás, de 0 a 1" },
          confirmado: { type: "boolean", description: "false si se sospecha pero no se ve con claridad" },
          foto: { type: "integer", description: "Número de la foto donde se ve, desde 0" },
          zona: {
            type: "object",
            properties: {
              ubicada: { type: "boolean" },
              x: { type: "number" },
              y: { type: "number" },
              ancho: { type: "number" },
              alto: { type: "number" },
            },
            required: ["ubicada", "x", "y", "ancho", "alto"],
            additionalProperties: false,
          },
          explicacion: { type: "string" },
        },
        required: ["tipo", "descripcion", "gravedad", "confianza", "confirmado", "foto", "zona", "explicacion"],
        additionalProperties: false,
      },
    },
    recomendaciones: { type: "array", items: { type: "string" } },
    requiereInspeccion: { type: "boolean" },
    alerta: { type: "string", description: "Aviso de seguridad, o cadena vacía si no hace falta" },
    resumen: { type: "string" },
  },
  required: ["esRin", "calidadFoto", "danos", "recomendaciones", "requiereInspeccion", "alerta", "resumen"],
  additionalProperties: false,
};

// ---------- Salida ----------

/**
 * Revisa lo que devolvió Claude antes de mandarlo a la app: valores dentro de rango,
 * textos cortos y las reglas de seguridad aplicadas aunque el modelo las olvide.
 */
function normalizar(crudo: unknown, cantidadFotos: number) {
  const datos = esObjeto(crudo) ? crudo : {};
  const esRin = booleano(datos.esRin, false);

  const danos = esRin
    ? lista(datos.danos, 6).filter(esObjeto).map((dano) => {
        const zona = esObjeto(dano.zona) ? dano.zona : {};
        const x = numero(zona.x, 0, 1, 0);
        const y = numero(zona.y, 0, 1, 0);
        const ubicada = booleano(zona.ubicada) && numero(zona.ancho, 0, 1, 0) > 0.01 && numero(zona.alto, 0, 1, 0) > 0.01;
        return {
          tipo: unoDe(dano.tipo, Tipos) ?? "Otro",
          descripcion: texto(dano.descripcion, 80, "Daño visible en el rin"),
          gravedad: unoDe(dano.gravedad, Gravedades) ?? "Moderada",
          confianza: redondear(numero(dano.confianza, 0, 1, 0.5)),
          confirmado: booleano(dano.confirmado, false),
          foto: Math.round(numero(dano.foto, 0, Math.max(0, cantidadFotos - 1), 0)),
          caja: ubicada
            ? {
                x: redondear(x),
                y: redondear(y),
                ancho: redondear(numero(zona.ancho, 0, 1 - x, 0)),
                alto: redondear(numero(zona.alto, 0, 1 - y, 0)),
              }
            : null,
          explicacion: texto(dano.explicacion, 300),
        };
      })
    : [];

  const peligroso = danos.some(
    (dano) => dano.tipo === "Fisura" || (dano.tipo === "Deformacion" && dano.gravedad !== "Leve"),
  );
  const requiereInspeccion = peligroso || booleano(datos.requiereInspeccion, false);
  const alerta = texto(datos.alerta, 240) ||
    (peligroso ? "Puede afectar la seguridad. No uses este rin en carretera hasta que el taller lo revise." : "");

  return {
    esRin,
    calidadFoto: unoDe(datos.calidadFoto, Calidades) ?? "buena",
    danos,
    recomendaciones: lista(datos.recomendaciones, 3).map((r) => texto(r, 160)).filter(Boolean),
    requiereInspeccion,
    alerta: alerta || null,
    resumen: texto(datos.resumen, 200),
    modelo: ModeloAnalisis,
  };
}

/** Tres decimales bastan para ubicar un recuadro en la foto. */
function redondear(valor: number): number {
  return Math.round(valor * 1_000) / 1_000;
}
