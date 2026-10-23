import { json } from '@sveltejs/kit';
import { env } from '$env/dynamic/private';
import type { RequestHandler } from './$types';

const BACKEND = env.API_URL ?? 'http://localhost:8080';

const ACCESS = 'kc_access';
const REFRESH = 'kc_refresh';
const USER = 'kc_user';

interface RespuestaLogin {
	accessToken: string;
	refreshToken: string;
	expiresIn: number;
	usuario: { id: number; nombre: string; rol: string };
	colegio: { id: number; nombre: string };
}

function guardarSesion(cookies: import('@sveltejs/kit').Cookies, datos: RespuestaLogin) {
	cookies.set(ACCESS, datos.accessToken, {
		path: '/',
		httpOnly: true,
		sameSite: 'lax',
		maxAge: 15 * 60
	});
	cookies.set(REFRESH, datos.refreshToken, {
		path: '/',
		httpOnly: true,
		sameSite: 'lax',
		maxAge: 8 * 60 * 60
	});
	// Visible para la interfaz (nombre y rol), sin secretos.
	cookies.set(
		USER,
		JSON.stringify({ nombre: datos.usuario.nombre, rol: datos.usuario.rol, colegio: datos.colegio.nombre }),
		{ path: '/', sameSite: 'lax', maxAge: 8 * 60 * 60 }
	);
}

function borrarSesion(cookies: import('@sveltejs/kit').Cookies) {
	cookies.delete(ACCESS, { path: '/' });
	cookies.delete(REFRESH, { path: '/' });
	cookies.delete(USER, { path: '/' });
}

/**
 * BFF: el navegador habla solo con este proxy. Los JWT viven en cookies
 * httpOnly y nunca llegan al JavaScript del panel.
 */
export const fallback: RequestHandler = async ({ params, request, cookies, url }) => {
	const ruta = params.ruta ?? '';

	if (ruta === 'v1/auth/logout') {
		borrarSesion(cookies);
		return new Response(null, { status: 204 });
	}

	const destino = `${BACKEND}/api/${ruta}${url.search}`;
	const method = request.method;
	const cuerpo = ['GET', 'HEAD'].includes(method) ? undefined : await request.text();

	const headers = new Headers();
	const tipoContenido = request.headers.get('content-type');
	if (tipoContenido) {
		headers.set('content-type', tipoContenido);
	}
	const access = cookies.get(ACCESS);
	if (access) {
		headers.set('authorization', `Bearer ${access}`);
	}

	const esLogin = ruta === 'v1/auth/login' || ruta === 'v1/auth/refresh';

	let respuesta = await fetch(destino, { method, headers, body: cuerpo });

	// Renovación transparente: si el access token expiró, se usa el refresh.
	if (respuesta.status === 401 && !esLogin && cookies.get(REFRESH)) {
		const refresco = await fetch(`${BACKEND}/api/v1/auth/refresh`, {
			method: 'POST',
			headers: { 'content-type': 'application/json' },
			body: JSON.stringify({ refreshToken: cookies.get(REFRESH) })
		});

		if (refresco.ok) {
			const datos = (await refresco.json()) as RespuestaLogin;
			guardarSesion(cookies, datos);
			headers.set('authorization', `Bearer ${datos.accessToken}`);
			respuesta = await fetch(destino, { method, headers, body: cuerpo });
		} else {
			borrarSesion(cookies);
		}
	}

	if (esLogin) {
		if (!respuesta.ok) {
			return new Response(await respuesta.text(), {
				status: respuesta.status,
				headers: { 'content-type': respuesta.headers.get('content-type') ?? 'application/json' }
			});
		}

		const datos = (await respuesta.json()) as RespuestaLogin;
		guardarSesion(cookies, datos);
		return json({ usuario: datos.usuario, colegio: datos.colegio, expiresIn: datos.expiresIn });
	}

	const salida = new Headers();
	const contentType = respuesta.headers.get('content-type');
	if (contentType) {
		salida.set('content-type', contentType);
	}
	const disposicion = respuesta.headers.get('content-disposition');
	if (disposicion) {
		salida.set('content-disposition', disposicion);
	}

	return new Response(await respuesta.text(), { status: respuesta.status, headers: salida });
};
