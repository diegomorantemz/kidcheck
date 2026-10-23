package pe.edu.kidcheck.seccion;

import java.time.LocalTime;

public record SeccionDto(
        Long id,
        String nombre,
        String grado,
        String letra,
        String periodo,
        LocalTime horaLimiteIngreso,
        int toleranciaMinutos
) {
    public static SeccionDto de(Seccion seccion) {
        return new SeccionDto(
                seccion.getId(),
                seccion.nombre(),
                seccion.getGrado(),
                seccion.getLetra(),
                seccion.getPeriodo().getNombre(),
                seccion.getHoraLimiteIngreso(),
                seccion.getToleranciaMinutos());
    }
}
