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
