export class ApiError extends Error {
	status: number;
	detail?: string;
	code?: string;

	constructor(status: number, message: string, detail?: string, code?: string) {
		super(message);
		this.status = status;
		this.detail = detail;
		this.code = code;
	}
}

interface Opciones {
	method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
	body?: unknown;
}

/**
 * Todas las llamadas pasan por /api (proxy BFF de SvelteKit), que guarda
 * los tokens en cookies httpOnly. El navegador nunca ve el JWT.
 *
 * Se aplica un timeout para que la interfaz nunca quede en "Cargando…"
 * si la API se cae, se reinicia o no responde.
 */
const TIMEOUT_MS = 10_000;

export async function api<T>(ruta: string, opciones: Opciones = {}): Promise<T> {
	let respuesta: Response;
	try {
		respuesta = await fetch(`/api/v1${ruta}`, {
			method: opciones.method ?? 'GET',
			headers: opciones.body !== undefined ? { 'content-type': 'application/json' } : undefined,
			body: opciones.body !== undefined ? JSON.stringify(opciones.body) : undefined,
			signal: AbortSignal.timeout(TIMEOUT_MS)
		});
	} catch (error) {
		if (error instanceof DOMException && error.name === 'TimeoutError') {
			throw new ApiError(
				408,
				'El servidor tardó demasiado en responder. Verifica que la API esté en línea.'
			);
		}
		throw new ApiError(0, 'No se pudo conectar con el servidor. Revisa tu conexión.');
	}

	if (respuesta.status === 204) {
		return undefined as T;
	}

	const datos = await respuesta.json().catch(() => null);

	if (!respuesta.ok) {
		throw new ApiError(
			respuesta.status,
			datos?.title ?? 'No se pudo completar la operación',
			datos?.detail,
			datos?.code
		);
	}

	return datos as T;
}

export function mensajeDeError(error: unknown): string {
	if (error instanceof ApiError) {
		return error.detail ?? error.message;
	}
	if (error instanceof Error) {
		return error.message;
	}
	return 'Ocurrió un error inesperado.';
}
