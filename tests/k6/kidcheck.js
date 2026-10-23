import http from 'k6/http';
import { check } from 'k6';

const BASE = __ENV.BASE_URL || 'http://localhost:8080';
const PASSWORD = __ENV.KIDCHECK_PASSWORD || 'KidCheck2026!';
const DEVICE = '11111111-1111-1111-1111-111111111111';

export const options = {
	scenarios: {
		panel: {
			executor: 'constant-arrival-rate',
			exec: 'panel',
			rate: Number(__ENV.PANEL_RPS || 20),
			timeUnit: '1s',
			duration: __ENV.PANEL_DURATION || '30s',
			preAllocatedVUs: 10,
			maxVUs: 40
		},
		sync: {
			executor: 'constant-arrival-rate',
			exec: 'sync',
			startTime: __ENV.SYNC_START || '35s',
			rate: Number(__ENV.SYNC_RPS || 50),
			timeUnit: '1s',
			duration: __ENV.SYNC_DURATION || '30s',
			preAllocatedVUs: 20,
			maxVUs: 100
		}
	},
	thresholds: {
		'http_req_failed': ['rate<0.01'],
		'http_req_duration{scenario:panel}': ['p(95)<200'],
		'http_req_duration{scenario:sync}': ['p(95)<300']
	}
};

function login(usuario) {
	const respuesta = http.post(`${BASE}/api/v1/auth/login`, JSON.stringify({
		usuario,
		password: PASSWORD
	}), { headers: { 'Content-Type': 'application/json' } });
	check(respuesta, { 'login 200': (r) => r.status === 200 });
	return respuesta.json('accessToken');
}

export function setup() {
	return {
		admin: login('admin1'),
		vigilante: login('vigilante1')
	};
}

export function panel(datos) {
	const respuesta = http.get(`${BASE}/api/v1/panel/dia`, {
		headers: { Authorization: `Bearer ${datos.admin}` }
	});
	check(respuesta, {
		'panel 200': (r) => r.status === 200,
		'panel tiene fecha': (r) => Boolean(r.json('fecha'))
	});
}

export function sync(datos) {
	const clientId = `${Date.now().toString(36)}${__VU.toString(36)}${__ITER.toString(36)}${Math.random()
		.toString(36)
		.slice(2, 8)}`.slice(0, 36);
	const respuesta = http.post(`${BASE}/api/v1/sync/ingresos`, JSON.stringify({
		deviceUuid: DEVICE,
		ingresos: [{
			clientId,
			idEstudiante: 3,
			fechaHoraUtc: '2026-12-31T13:00:00Z',
			canal: 'qr'
		}]
	}), {
		headers: {
			'Content-Type': 'application/json',
			Authorization: `Bearer ${datos.vigilante}`
		}
	});
	check(respuesta, {
		'sync 200': (r) => r.status === 200,
		'sync sin error': (r) => {
			const cuerpo = r.json();
			return Boolean(cuerpo?.resultados?.[0]?.status) && cuerpo.resultados[0].status !== 'ERROR';
		}
	});
}
