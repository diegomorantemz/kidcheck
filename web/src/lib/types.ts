export type Rol = 'ADMINISTRADOR' | 'AUXILIAR' | 'PSICOLOGO' | 'VIGILANTE';

export interface Vinculo {
	idApoderado: number;
	nombresApellidos: string;
	telefonoWhatsapp?: string | null;
	email?: string | null;
	parentesco: string;
	esPrincipal: boolean;
	autorizadoRecoger: boolean;
}

export interface Estudiante {
	id: number;
	codigoMatricula: string;
	nombres: string;
	apellidos: string;
	nombreCompleto: string;
	fechaNacimiento?: string | null;
	estado: string;
	seccionId?: number | null;
	seccion?: string | null;
	idCredencial?: number | null;
	credencialActiva: boolean;
	apoderados: Vinculo[];
}

export interface Seccion {
	id: number;
	nombre: string;
	grado: string;
	letra: string;
	periodo: string;
	horaLimiteIngreso: string;
	toleranciaMinutos: number;
}

export interface Apoderado {
	id: number;
	nombresApellidos: string;
	telefonoWhatsapp?: string | null;
	email?: string | null;
	estado: string;
}

export interface PageResponse<T> {
	contenido: T[];
	pagina: number;
	tamano: number;
	total: number;
	totalPaginas: number;
}

export interface CredencialEmitida {
	idCredencial: number;
	idEstudiante: number;
	token: string;
	qrPngBase64: string;
}

export interface Sesion {
	usuario: { id: number; nombre: string; rol: Rol };
	colegio: { id: number; nombre: string };
	expiresIn: number;
}

export interface Me {
	usuario: string;
	autoridades: string[];
	id: number;
	nombre: string;
	rol: Rol;
	colegio: number;
}

export type EstadoAsistencia = 'PRESENTE' | 'TARDE' | 'AUSENTE' | 'JUSTIFICADO' | 'PENDIENTE';

export interface PanelResumen {
	total: number;
	presentes: number;
	tarde: number;
	ausentes: number;
	justificados: number;
	pendientes: number;
	alertas: number;
}

export interface PanelSeccion {
	id: number;
	nombre: string;
	total: number;
	presentes: number;
	tarde: number;
	pendientes: number;
}

export interface PanelEstudiante {
	idEstudiante: number;
	nombre: string;
	seccion: string;
	estado: EstadoAsistencia;
	hora?: string | null;
	tarde: boolean;
	apoderado?: string | null;
	alerta?: { id: number; tipo: string; estado: string } | null;
}

export interface PanelDia {
	fecha: string;
	actualizadoEn: string;
	resumen: PanelResumen;
	secciones: PanelSeccion[];
	estudiantes: PanelEstudiante[];
}

export interface Asistencia {
	idAsistencia: number;
	idEstudiante: number;
	nombre: string;
	seccion: string;
	estado: EstadoAsistencia;
	hora?: string | null;
	minutosTarde: number;
	origen: string;
	alertaId?: number | null;
	justificacionEstado?: string | null;
}

export interface Justificacion {
	id: number;
	idAlerta: number;
	idEstudiante: number;
	estudiante: string;
	seccion?: string | null;
	fecha?: string | null;
	motivo: string;
	detalle?: string | null;
	estado: string;
	apoderado?: string | null;
	fechaEnvio: string;
	fechaRevision?: string | null;
}

export interface Alerta {
	idAlerta: number;
	tipo: string;
	estado: string;
	umbral?: number | null;
	fechaGeneracion: string;
	idEstudiante: number;
	estudiante: string;
	seccion?: string | null;
	fechaAsistencia?: string | null;
	hora?: string | null;
	justificacion?: {
		id: number;
		motivo: string;
		detalle?: string | null;
		estado: string;
		fechaEnvio: string;
	} | null;
}

export interface JustificacionPublica {
	estudiante: string;
	seccion?: string | null;
	fecha?: string | null;
	horaLimite?: string | null;
	expiraEn: string;
	motivos: string[];
	yaEnviada: boolean;
}
