package cl.duoc.gymflow.catalog.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Contexto de cada petición (EP5), para los logs y los eventos de auditoría:
 * <ul>
 *   <li>{@code traceId}: el de {@code X-Trace-Id} si viene uno válido, o uno nuevo. Se devuelve en la respuesta.</li>
 *   <li>Quién: {@code X-User-Id} ({@code oid}) y {@code X-User-Name} (URL-encoded en UTF-8), que envía el BFF.</li>
 *   <li>Desde dónde: {@code X-Client-Ip}, que el BFF toma de {@code X-Forwarded-For}.</li>
 * </ul>
 * Se guarda en el MDC porque catalog no recibe la identidad en los controladores; así los servicios no cambian de firma.
 * Este microservicio no valida JWT: no está expuesto a internet y solo lo llama el BFF (que ya validó el token).
 */
@Component
public class FiltroContexto extends OncePerRequestFilter {

    public static final String CABECERA_TRAZA = "X-Trace-Id";
    public static final String CABECERA_USUARIO_ID = "X-User-Id";
    public static final String CABECERA_USUARIO_NOMBRE = "X-User-Name";
    public static final String CABECERA_IP = "X-Client-Ip";

    public static final String MDC_TRAZA = "traceId";
    public static final String MDC_USUARIO_ID = "userId";
    public static final String MDC_USUARIO_NOMBRE = "userName";
    public static final String MDC_IP = "clientIp";

    private static final Pattern TRAZA_VALIDA = Pattern.compile("[A-Za-z0-9._-]{1,64}");
    private static final Pattern IP_VALIDA = Pattern.compile("[0-9A-Fa-f:.]{2,45}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String traza = request.getHeader(CABECERA_TRAZA);
        String traceId = traza != null && TRAZA_VALIDA.matcher(traza).matches() ? traza : UUID.randomUUID().toString();
        MDC.put(MDC_TRAZA, traceId);
        poner(MDC_USUARIO_ID, recortar(request.getHeader(CABECERA_USUARIO_ID), 64));
        poner(MDC_USUARIO_NOMBRE, recortar(decodificar(request.getHeader(CABECERA_USUARIO_NOMBRE)), 150));
        String ip = request.getHeader(CABECERA_IP);
        if (ip != null && IP_VALIDA.matcher(ip.trim()).matches()) {
            MDC.put(MDC_IP, ip.trim());
        }
        response.setHeader(CABECERA_TRAZA, traceId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_TRAZA);
            MDC.remove(MDC_USUARIO_ID);
            MDC.remove(MDC_USUARIO_NOMBRE);
            MDC.remove(MDC_IP);
        }
    }

    private static void poner(String clave, String valor) {
        if (valor != null && !valor.isBlank()) {
            MDC.put(clave, valor.trim());
        }
    }

    private static String decodificar(String valor) {
        if (valor == null) {
            return null;
        }
        try {
            return URLDecoder.decode(valor, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException malCodificado) {
            return valor;
        }
    }

    private static String recortar(String valor, int maximo) {
        return valor == null || valor.length() <= maximo ? valor : valor.substring(0, maximo);
    }
}
