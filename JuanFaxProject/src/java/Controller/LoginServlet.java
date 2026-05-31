package Controller;

import Dao.NegocioDao;
import Model.NegocioDTO;
import java.io.PrintWriter;
import java.util.List;

import Config.conection;
import Dao.AlertaDao;
import Dao.SuscripcionDao;
import Model.SuscripcionDTO;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * CONTROLADOR PRINCIPAL DE AUTENTICACIÓN Y ENRUTAMIENTO DE DATOS (MAPPING /LoginServlet)
 * Aquí centralizo todo el flujo de datos de Juanfax. Este Servlet hereda de HttpServlet
 * para poder capturar los verbos HTTP (GET y POST) que envía el cliente.
 */
@WebServlet(name = "LoginServlet", urlPatterns = {"/LoginServlet"})
@jakarta.servlet.annotation.MultipartConfig(
    fileSizeThreshold = 1024 * 1024 * 2, // 2MB
    maxFileSize = 1024 * 1024 * 10,      // 10MB
    maxRequestSize = 1024 * 1024 * 50    // 50MB
)
public class LoginServlet extends HttpServlet {

    /**
     * ========================================================================
     * MÉTODO GET: CONTROLADOR DE LECTURA Y ENVIÓ DE DATOS ASÍNCRONOS (FETCH / AJAX)
     * ========================================================================
     * Lo utilizo exclusivamente cuando el Front-end me solicita datos para pintar las vistas
     * sin recargar la página. Todas las respuestas de este método se estructuran en formato JSON.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // CONFIGURACIÓN DE ENTRADA: Aseguro que los datos de la petición vengan en UTF-8 
        // para que no se rompan caracteres especiales, eñes o tildes.
        request.setCharacterEncoding("UTF-8");
        
        // CAPTURA DEL PARÁMETRO DE ACCIÓN: Sirve como enrutador interno para saber qué vista está pidiendo datos.
        String accion = request.getParameter("accion");

        // MONITOR DE CONSOLA: Para saber exactamente en tiempo de ejecución qué está intentando mapear el JavaScript.
        System.out.println(" 🔍 ACCION DETECTADA EN GET: [" + accion + "]");
        
        // --------------------------------------------------------------------
        // ACCIÓN 1: MÉTRICAS DEL PANEL DEL VENDEDOR
        // --------------------------------------------------------------------
        if ("metricasVendedor".equals(accion)) {
            // Seteo las cabeceras de respuesta para avisarle al navegador que lo que devuelvo es un objeto JSON en UTF-8
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            // SEGURIDAD: Valido que el vendedor tenga una sesión activa en el servidor antes de exponer datos
            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("idUsuario") == null) {
                response.getWriter().print("{\"error\": \"Sesión no válida o caducada.\"}");
                return; // Corto la ejecución aquí si no está logueado
            }

            // Recupero el ID del vendedor que se guardó en la sesión durante el login
            int idVendedor = (int) session.getAttribute("idUsuario");
            
            // FILTRO DE NEGOCIO: Si el front-end me envía un ID específico de un establecimiento, lo capturo
            String idNegocioStr = request.getParameter("idNegocio");
            int idNegocio = 0;
            if (idNegocioStr != null && !idNegocioStr.isEmpty()) {
                idNegocio = Integer.parseInt(idNegocioStr);
            }
            
            // CONEXIÓN AL DAO: Instancio el objeto de acceso a datos para traer las métricas reales
            Dao.NegocioDao negocioDao = new Dao.NegocioDao();
            java.util.Map<String, Object> datosReales = negocioDao.obtenerMetricasVendedor(idVendedor ,idNegocio);

            // EXTRACCIÓN SEGURA: Mapeo las variables numéricas usando getOrDefault por si la BD devuelve valores nulos
            int vistas = (int) datosReales.getOrDefault("vistasTotales", 0);
            int clicks = (int) datosReales.getOrDefault("clicksEnlaces", 0);
            int resenas = (int) datosReales.getOrDefault("totalResenas", 0);
            double puntuacion = (double) datosReales.getOrDefault("puntuacion", 0.0);
            
            // Casteo las estructuras complejas (listas de comentarios y mapas de distribución de estrellas)
            java.util.List<java.util.Map<String, Object>> listaComentarios = 
                (java.util.List<java.util.Map<String, Object>>) datosReales.get("comentariosRecientes");
            
            java.util.Map<String, Integer> dist = 
                (java.util.Map<String, Integer>) datosReales.get("distribucionEstrellas");

            // CONSTRUCCIÓN MANUAL DEL JSON: Uso StringBuilder para ensamblar la estructura de forma óptima
            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"vistasTotales\": ").append(vistas).append(",");
            json.append("\"clicksEnlaces\": ").append(clicks).append(",");
            json.append("\"totalResenas\": ").append(resenas).append(",");
            json.append("\"puntuacion\": ").append(puntuacion).append(",");
            
            // SIMULACIÓN DE GRÁFICO: Cargo datos duros temporales para rellenar las barras del gráfico semanal en la UI
            json.append("\"visitasSemana\": [");
            json.append("  {\"nombreDia\":\"Lun\", \"porcentaje\": 45},");
            json.append("  {\"nombreDia\":\"Mar\", \"porcentaje\": 60},");
            json.append("  {\"nombreDia\":\"Mié\", \"porcentaje\": 80},");
            json.append("  {\"nombreDia\":\"Jue\", \"porcentaje\": 50},");
            json.append("  {\"nombreDia\":\"Vie\", \"porcentaje\": 95},");
            json.append("  {\"nombreDia\":\"Sáb\", \"porcentaje\": 70},");
            json.append("  {\"nombreDia\":\"Dom\", \"porcentaje\": 35}");
            json.append("],");

            // COMENTARIOS REALES: Recorro los registros obtenidos del JOIN de SQL para meterlos al array de comentarios del JSON
            json.append("\"comentariosRecientes\": [");
            if (listaComentarios != null) {
                for (int i = 0; i < listaComentarios.size(); i++) {
                    java.util.Map<String, Object> c = listaComentarios.get(i);
                    String texto = c.get("textoComentario") != null ? c.get("textoComentario").toString() : "";
                    texto = texto.replace("\"", "\\\""); // Evito que comillas internas del texto rompan el string de JS

                    json.append("{");
                    json.append("\"nombreUsuario\":\"").append(c.get("nombreUsuario")).append("\",");
                    json.append("\"calificacion\":").append(c.get("calificacion")).append(",");
                    json.append("\"textoComentario\":\"").append(texto).append("\"");
                    json.append("}");
                    
                    // Si no es el último elemento de la lista, añado una coma para mantener la sintaxis JSON válida
                    if (i < listaComentarios.size() - 1) {
                        json.append(",");
                    }
                }
            }
            json.append("],");

            // DISTRIBUCIÓN DE ESTRELLAS: Extraigo los conteos de calificaciones para pintar el desglose de barras
            int p5 = dist != null ? dist.getOrDefault("cinco", 0) : 0;
            int p4 = dist != null ? dist.getOrDefault("cuatro", 0) : 0;
            int p3 = dist != null ? dist.getOrDefault("tres", 0) : 0;

            json.append("\"distribucionEstrellas\": {");
            json.append("  \"cinco\": ").append(p5).append(",");
            json.append("  \"cuatro\": ").append(p4).append(",");
            json.append("  \"tres\": ").append(p3);
            json.append("}");
            json.append("}");

            // ENVÍO DE RESPUESTA: Envío el JSON completado directamente al flujo de salida hacia el Frontend
            try (java.io.PrintWriter out = response.getWriter()) {
                out.print(json.toString());
                out.flush();
            }
            return; // Termino el procesamiento de la acción
        }
        // --------------------------------------------------------------------
        // ACCIÓN 2: LISTAR LOS NEGOCIOS DEL VENDEDOR ACTUAL
        // --------------------------------------------------------------------
        else if ("listarNegociosPorVendedor".equals(accion)) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            // SEGURIDAD: Verifico la validez de la sesión del usuario actual
            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("idUsuario") == null) {
                response.getWriter().print("[]"); // Si no está logueado devuelvo un array vacío para no generar errores en JS
                return;
            }

            int idVendedor = (int) session.getAttribute("idUsuario");
            Dao.NegocioDao negocioDao = new Dao.NegocioDao();
            // Ejecuto la consulta para traerme solo los negocios asociados a su ID
            List<Model.NegocioDTO> misNegocios = negocioDao.obtenerNegociosPorVendedor(idVendedor); 

            // Formateo la lista de DTOs a una estructura limpia de arreglo JSON
            StringBuilder json = new StringBuilder();
            json.append("[");
            for (int i = 0; i < misNegocios.size(); i++) {
                Model.NegocioDTO n = misNegocios.get(i);
                json.append("{");
                json.append("\"idNegocio\":").append(n.getIdNegocio()).append(",");
                json.append("\"nombreEstablecimiento\":\"").append(escapeJson(n.getNombreEstablecimiento())).append("\",");
                json.append("\"urlImagen\":\"").append(escapeJson(n.getUrl_imagen())).append("\","); // 🌟 Se agregó la coma aquí
                json.append("\"estado\":\"").append(escapeJson(n.getEstado())).append("\"");       // 🌟 NUEVO: Propiedad agregada al JSON
                json.append("}");
                if (i < misNegocios.size() - 1) json.append(",");
            }
            json.append("]");

            try (PrintWriter out = response.getWriter()) {
                out.print(json.toString());
                out.flush();
            }
            return;
        }
        // --------------------------------------------------------------------
        // ACCIÓN 3: FILTRAR NEGOCIOS POR CATEGORÍA (VISTA TURISTA)
        // --------------------------------------------------------------------
        else if ("negociosPorCategoria".equals(accion)) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            // Capturo la categoría enviada desde las tarjetas dinámicas (ej: "Gastronomía")
            String categoria = request.getParameter("categoria");
            NegocioDao negocioDao = new NegocioDao();
            List<NegocioDTO> lista = negocioDao.obtenerNegociosPorCategoria(categoria);

            // Construyo el JSON con los negocios filtrados para renderizar las Expanding Cards
            StringBuilder json = new StringBuilder();
            json.append("[");
            for (int i = 0; i < lista.size(); i++) {
                NegocioDTO n = lista.get(i);
                json.append("{");
                json.append("\"idNegocio\":").append(n.getIdNegocio()).append(","); 
                json.append("\"nombreEstablecimiento\":\"").append(escapeJson(n.getNombreEstablecimiento())).append("\",");
                json.append("\"urlImagen\":\"").append(escapeJson(n.getUrl_imagen())).append("\"");
                json.append("}");
                if (i < lista.size() - 1) json.append(",");
            }
            json.append("]");

            try (PrintWriter out = response.getWriter()) {
                out.print(json.toString());
                out.flush();
            }
            return;
        } 
        // --------------------------------------------------------------------
        // ACCIÓN 4: EXTRAER EL DETALLE DE UN NEGOCIO ESPECÍFICO
        // --------------------------------------------------------------------
       else if ("detalleNegocioUnico".equals(accion)) {
            String idStr = request.getParameter("id"); 
            System.out.println("-> Ejecutando detalleNegocioUnico para ID: " + idStr);

            try {
                int idNegocio = Integer.parseInt(idStr);
                NegocioDao negocioDAO = new NegocioDao(); 
                NegocioDTO negocio = negocioDAO.obtenerNegocioPorId(idNegocio);

                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");

                // Si el objeto de transferencia de datos no es nulo, mapeo todos sus atributos a JSON
                if (negocio != null) {
                    // Nota: Aquí se inyectan Latitud y Longitud esenciales para pintar el marcador en el mapa de Leaflet/Google Maps
                    String json = "{"
                            + "\"idNegocio\":" + negocio.getIdNegocio() + ","
                            + "\"nombreEstablecimiento\":\"" + escapeJson(negocio.getNombreEstablecimiento()) + "\","
                            + "\"descripcion\":\"" + escapeJson(negocio.getDescripcion()) + "\","
                            + "\"urlImagen\":\"" + escapeJson(negocio.getUrl_imagen()) + "\","
                            + "\"latitud\":" + negocio.getLatitud() + ","
                            + "\"longitud\":" + negocio.getLongitud()
                            + "}";

                    try (PrintWriter out = response.getWriter()) {
                        out.print(json);
                        out.flush();
                    }
                } else {
                    try (PrintWriter out = response.getWriter()) {
                        out.print("{\"error\": \"El negocio no fue encontrado en Juanfax\"}");
                        out.flush();
                    }
                }
            } catch (NumberFormatException e) {
                // Manejo preventivo si por error del Front-end se inyecta un ID que no sea un número válido
                System.err.println("Error: ID recibido no es un número válido: " + idStr);
                response.getWriter().print("{\"error\": \"ID inválido\"}");
            }
            return;
        }
       else if ("obtenerNegocioPorId".equals(accion)) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            try {
                int id = Integer.parseInt(request.getParameter("id"));
                Dao.NegocioDao dao = new Dao.NegocioDao();
                Model.NegocioDTO n = dao.obtenerNegocioPorId(id);

                if (n != null) {
                    // Reemplazamos los nulos por textos vacíos para que no rompan el JSON
                    String nit = (n.getNit() != null) ? n.getNit() : "";
                    String nombre = (n.getNombreEstablecimiento() != null) ? n.getNombreEstablecimiento().replace("\"", "\\\"") : "";
                    String descripcion = (n.getDescripcion() != null) ? n.getDescripcion().replace("\"", "\\\"") : "";

                    String json = String.format(
                        "{\"success\": true, \"id\": %d, \"nit\": \"%s\", \"nombre\": \"%s\", \"descripcion\": \"%s\", \"idCategoria\": %d}",
                        n.getIdNegocio(), nit, nombre, descripcion, n.getIdCategoria()
                    );
                    response.getWriter().write(json);
                } else {
                    response.getWriter().write("{\"success\": false, \"message\": \"No se encontró el establecimiento en el sistema.\"}");
                }
            } catch (Exception e) {
                // Si algo falla, respondemos en formato JSON y evitamos que Tomcat mande el HTML de error
                System.err.println("Error crítico en Servlet obtenerNegocioPorId: " + e.getMessage());
                response.getWriter().write("{\"success\": false, \"message\": \"Error interno: " + e.getMessage() + "\"}");
            }
            return;
        }
       if ("obtenerDatosSuscripcion".equals(accion)) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            PrintWriter out = response.getWriter();

            // Capturamos el idNegocio enviado desde el fetch
            int idNegocio = Integer.parseInt(request.getParameter("idNegocio"));

            SuscripcionDao dao = new SuscripcionDao();
            String jsonRespuesta = dao.obtenerDatosSuscripcionJSON(idNegocio);

            out.print(jsonRespuesta);
            out.flush();
            return;
        }

        if ("renovarSuscripcion".equals(accion)) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            PrintWriter out = response.getWriter();

            int idNegocio = Integer.parseInt(request.getParameter("idNegocio"));

            SuscripcionDao dao = new SuscripcionDao();
            boolean ejecutado = dao.renovarSuscripcion(idNegocio);

            if (ejecutado) {
                out.print("{\"success\": true, \"mensaje\": \"¡Plan activado con éxito!\"}");
            } else {
                out.print("{\"success\": false, \"mensaje\": \"No se pudo actualizar la suscripción.\"}");
            }
            out.flush();
            return;
        }
        else if ("listarUsuarios".equals(accion)) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            // CONTROL DE ACCESO INTERNO: Verificamos que sea ADMINISTRADOR
            HttpSession session = request.getSession(false);
            if (session == null || !"ADMINISTRADOR".equals(session.getAttribute("role") != null ? session.getAttribute("role") : session.getAttribute("rol"))) {
                response.getWriter().print("[]"); 
                return;
            }

            // Instancias tu DAO de usuarios (Ajusta los nombres según tus clases reales de usuario)
            Dao.UsuarioDao usuarioDao = new Dao.UsuarioDao();
            List<Model.UsuarioDTO> listaUsuarios = usuarioDao.obtenerTodosLosUsuarios(); 

            // Transformamos la lista a formato JSON de manera manual
            StringBuilder json = new StringBuilder();
            json.append("[");
            for (int i = 0; i < listaUsuarios.size(); i++) {
                Model.UsuarioDTO u = listaUsuarios.get(i);

                json.append("{");
                json.append("\"idUsuario\":").append(u.getIdUsuario()).append(",");
                json.append("\"nombre\":\"").append(escapeJson(u.getNombreCompleto())).append("\",");
                json.append("\"correo\":\"").append(escapeJson(u.getCorreoElectronico())).append("\",");
                json.append("\"rol\":\"").append(escapeJson(u.getNombreRol())).append("\","); // Si manejas el String del rol
                json.append("\"estado\":\"").append(escapeJson(u.getEstado())).append("\"");
                json.append("}");

                if (i < listaUsuarios.size() - 1) {
                    json.append(",");
                }
            }
            json.append("]");

            try (PrintWriter out = response.getWriter()) {
                out.print(json.toString());
                out.flush();
            }
            return; 
        }
        // --------------------------------------------------------------------
        // ACCIÓN 5: OBTENER LOS DESTINOS RECOMENDADOS DEL CARRUSEL principal
        // --------------------------------------------------------------------
        else if ("carrusel".equals(accion)) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            try (PrintWriter out = response.getWriter()) {
                NegocioDao negocioDao = new NegocioDao();
                // Consulto los negocios que cumplen con el criterio de destacados en la app
                List<NegocioDTO> lista = negocioDao.obtenerDestinosDestacados();

                StringBuilder json = new StringBuilder();
                json.append("[");
                for (int i = 0; i < lista.size(); i++) {
                    NegocioDTO n = lista.get(i);
                    json.append("{");
                    json.append("\"idNegocio\":").append(n.getIdNegocio()).append(","); 
                    json.append("\"nombreEstablecimiento\":\"").append(escapeJson(n.getNombreEstablecimiento())).append("\",");
                    json.append("\"urlImagen\":\"").append(escapeJson(n.getUrl_imagen())).append("\"");
                    json.append("}");
                    if (i < lista.size() - 1) json.append(",");
                }
                json.append("]");

                out.print(json.toString());
                out.flush();
            } catch (Exception e) {
                e.printStackTrace();
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
            return;
        }
        // --------------------------------------------------------------------
        // ACCIÓN 6: EXTRAER LA LISTA DE OPINIONES DE LOS USUARIOS
        // --------------------------------------------------------------------
        else if ("listarComentarios".equals(accion)) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            String idNegocioStr = request.getParameter("idNegocio");
            
            if (idNegocioStr != null && !idNegocioStr.isEmpty()) {
                int idNegocio = Integer.parseInt(idNegocioStr);
                Dao.ComentarioDao comentarioDao = new Dao.ComentarioDao();
                List<Model.ComentarioDTO> lista = comentarioDao.obtenerComentariosPorNegocio(idNegocio);

                StringBuilder json = new StringBuilder();
                json.append("[");
                for (int i = 0; i < lista.size(); i++) {
                    Model.ComentarioDTO c = lista.get(i);
                    
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy");
                    String fechaFormateada = c.getFechaPublicacion() != null ? sdf.format(c.getFechaPublicacion()) : "";

                    json.append("{");
                    json.append("\"nombreUsuario\":\"").append(escapeJson(c.getNombreUsuario())).append("\",");
                    json.append("\"textoComentario\":\"").append(escapeJson(c.getTextoComentario())).append("\",");
                    json.append("\"fecha\":\"").append(fechaFormateada).append("\",");
                    json.append("\"calificacion\":").append(c.getCalificacion()); // 🌟 Entrega el número de estrellas al JS
                    json.append("}");
                    if (i < lista.size() - 1) json.append(",");
                }
                json.append("]");

                try (PrintWriter out = response.getWriter()) {
                    out.print(json.toString());
                    out.flush();
                }
            } else {
                try (PrintWriter out = response.getWriter()) {
                    out.print("[]");
                    out.flush();
                }
            }
            return; 
        }
        // --------------------------------------------------------------------
        // ACCIÓN 7: CARGAR ESTADÍSTICAS DEL CONTADOR GENERAL DEL ADMINISTRADOR
        // --------------------------------------------------------------------
        else if ("cargarDashboard".equals(accion)) {
            NegocioDao dao = new NegocioDao();
            
            // Ejecuto conteos de control interno del estado de las solicitudes
            int pendientes = dao.contarNegociosPorEstado("PENDIENTE");
            int aprobados = dao.contarNegociosPorEstado("APROBADO");
            List<NegocioDTO> listaPendientes = dao.obtenerNegociosPorEstado("PENDIENTE");

            // Estructuro la respuesta que alimentará el dashboard de admin
            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"stats\": {");
            json.append("\"pendientes\":").append(pendientes).append(",");
            json.append("\"aprobados\":").append(aprobados);
            json.append("},");

            // Incluyo la lista de nombres de las solicitudes que están esperando moderación
            json.append("\"solicitudes\": [");
            for (int i = 0; i < listaPendientes.size(); i++) {
                NegocioDTO n = listaPendientes.get(i);
                json.append("{");
                json.append("\"id\":").append(n.getIdNegocio()).append(",");
                json.append("\"nombre\":\"").append(escapeJson(n.getNombreEstablecimiento())).append("\"");
                json.append("}");
                if (i < listaPendientes.size() - 1) json.append(",");
            }
            json.append("],"); // Cambiamos de llave a coma para continuar el objeto

            // ================================================================
            // 🚀 NUEVO: CONSULTA DE ALERTAS REALES DESDE LA BASE DE DATOS
            // ================================================================
            json.append("\"alertas\": [");
            String sqlAlertas = "SELECT tipo, mensaje, "
                    + "CASE "
                    + "  WHEN TIMESTAMPDIFF(MINUTE, fecha_creacion, NOW()) < 60 THEN CONCAT('Hace ', TIMESTAMPDIFF(MINUTE, fecha_creacion, NOW()), ' min') "
                    + "  WHEN TIMESTAMPDIFF(HOUR, fecha_creacion, NOW()) < 24 THEN CONCAT('Hace ', TIMESTAMPDIFF(HOUR, fecha_creacion, NOW()), ' horas') "
                    + "  ELSE DATE_FORMAT(fecha_creacion, '%d/%m/%Y') "
                    + "END as tiempo_relativo "
                    + "FROM alertas_sistema ORDER BY fecha_creacion DESC LIMIT 5";

            try (Connection con = Config.conection.getConnection();
                 PreparedStatement psAlertas = con.prepareStatement(sqlAlertas);
                 ResultSet rsAlertas = psAlertas.executeQuery()) {
                
                boolean primeraAlerta = true;
                while (rsAlertas.next()) {
                    if (!primeraAlerta) json.append(",");
                    
                    String tipo = rsAlertas.getString("tipo");
                    String mensaje = rsAlertas.getString("mensaje");
                    String tiempoRelativo = rsAlertas.getString("tiempo_relativo");

                    json.append("{");
                    json.append("\"tipo\":\"").append(escapeJson(tipo)).append("\",");
                    json.append("\"mensaje\":\"").append(escapeJson(mensaje)).append("\",");
                    json.append("\"tiempoRelativo\":\"").append(escapeJson(tiempoRelativo)).append("\"");
                    json.append("}");
                    primeraAlerta = false;
                }
            } catch (SQLException e) {
                System.err.println("❌ Error trayendo alertas del sistema: " + e.getMessage());
                // Si falla la tabla, dejamos el array vacío para no tumbar todo el JSON del dashboard
            }
            json.append("]");
            json.append("}");

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(json.toString());
            return;
        }
        // --------------------------------------------------------------------
        // 🛠️ ACCIÓN 8 (CORREGIDA): RECUPERAR TODOS LOS NEGOCIOS PARA EL PANEL ADMIN
        // --------------------------------------------------------------------
        // CRUCIAL: Se movió del doPost al doGet porque el Front-end hace un fetch tradicional (GET).
        // Al estar en POST antes, causaba un error de parseo porque el script recibía el HTML de index.html por descarte.
        else if ("listarTodosLosNegociosAdmin".equals(accion)) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            // CONTROL DE ACCESO INTERNO: Verifico rigurosamente que el usuario de la sesión sea ADMINISTRADOR
            HttpSession session = request.getSession(false);
            if (session == null || !"ADMINISTRADOR".equals(session.getAttribute("role") != null ? session.getAttribute("role") : session.getAttribute("rol"))) {
                response.getWriter().print("[]"); // Si no tiene el rol, le niego el retorno de la data sensible devolviendo array vacío
                return;
            }

            NegocioDao negocioDao = new NegocioDao();
            List<NegocioDTO> listaNegocios = negocioDao.obtenerTodosLosNegociosAdmin();

            // Transformo toda la tabla de negocios del sistema a formato JSON
            StringBuilder json = new StringBuilder();
            json.append("[");
            for (int i = 0; i < listaNegocios.size(); i++) {
                NegocioDTO n = listaNegocios.get(i);
                
                // CONTROL DE NULOS: Asigno valores por defecto si hay celdas vacías en la BD para blindar la estabilidad del JSON
                String categoria = n.getNombreCategoria() != null ? n.getNombreCategoria() : "Sin categoría";
                String suscripcion = n.getSuscripcion() != null ? n.getSuscripcion() : "Mensual";
                String estado = n.getEstado() != null ? n.getEstado() : "PENDIENTE";

                json.append("{");
                json.append("\"idNegocio\":").append(n.getIdNegocio()).append(",");
                json.append("\"nombre\":\"").append(escapeJson(n.getNombreEstablecimiento())).append("\",");
                json.append("\"categoria\":\"").append(escapeJson(categoria)).append("\",");
                json.append("\"suscripcion\":\"").append(escapeJson(suscripcion)).append("\",");
                json.append("\"vistas\":").append(n.getVistas()).append(",");
                json.append("\"calificacion\":").append(n.getCalificacion()).append(",");
                json.append("\"estado\":\"").append(escapeJson(estado)).append("\"");
                json.append("}");

                if (i < listaNegocios.size() - 1) {
                    json.append(",");
                }
            }
            json.append("]");

            try (PrintWriter out = response.getWriter()) {
                out.print(json.toString());
                out.flush();
            }
            return; 
        }
        // FLUJO ALTERNATIVO DE SEGURIDAD: Si invocan el servlet por GET sin parámetros válidos, redirige al inicio
        else {
            System.out.println("⚠️ Alerta: Se detectó una petición GET sin acción AJAX válida. Redirigiendo...");
            response.sendRedirect("index.html"); 
        }
    }

        
    /**
     * ========================================================================
     * MÉTODO POST: ENCARGADO DE LA CREACIÓN, MODIFICACIÓN Y PROCESOS DE ESCRITURA
     * ========================================================================
     * Es el encargado de procesar inserciones pesadas en la base de datos, 
     * el envío de formularios de registro y el sistema clásico de validación de credenciales.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        String accion = request.getParameter("accion");

        // --------------------------------------------------------------------
        // ACCIÓN POST A: REGISTRAR UN NUEVO COMENTARIO CON CALIFICACIÓN
        // --------------------------------------------------------------------
        if ("guardarComentario".equals(accion)) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            // CONTROL DE USUARIO: Nadie puede comentar anónimamente en Juanfax
            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("idUsuario") == null) {
                response.getWriter().print("{\"status\":\"error\", \"message\":\"Debes iniciar sesión para comentar.\"}");
                return;
            }

            // Capturo los parámetros provenientes del formulario modal de opiniones
            int idUsuario = (int) session.getAttribute("idUsuario");
            int idNegocio = Integer.parseInt(request.getParameter("idNegocio"));
            String texto = request.getParameter("textoComentario"); 
            
            String valorPuntuacionStr = request.getParameter("valorPuntuacion");
            int calificacion = 0;
            
            // Valido que las estrellas seleccionadas se hayan capturado correctamente
            if (valorPuntuacionStr != null && !valorPuntuacionStr.isEmpty()) {
                calificacion = Integer.parseInt(valorPuntuacionStr.trim());
            }

            // ENCAPSULAMIENTO: Empaqueto la información en un objeto DTO limpio
            Model.ComentarioDTO nuevoComentario = new Model.ComentarioDTO();
            nuevoComentario.setIdNegocio(idNegocio);
            nuevoComentario.setIdUsuario(idUsuario);
            nuevoComentario.setTextoComentario(texto);
            nuevoComentario.setCalificacion(calificacion); 

            // Interecto con la base de datos mediante el DAO de comentarios
            Dao.ComentarioDao comentarioDao = new Dao.ComentarioDao();
            boolean exito = comentarioDao.insertarComentario(nuevoComentario);

            // Respondo el estatus al fetch emisor para actualizar la interfaz del Front-end en caliente
            if (exito) {
                response.getWriter().print("{\"status\":\"success\", \"message\":\"Comentario publicado\"}");
            } else {
                response.getWriter().print("{\"status\":\"error\", \"message\":\"No se pudo guardar el comentario\"}");
            }
            return; 
        }
        // --------------------------------------------------------------------
        // ACCIÓN POST B: PROCESAMIENTO DEL REGISTRO DE NUEVAS CUENTAS (SIGN-UP)
        // --------------------------------------------------------------------
        else if ("registrarUsuario".equals(accion)) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            // Recojo los strings del formulario de registro
            String nombreCompleto = request.getParameter("nombreCompleto");
            String correoElectronico = request.getParameter("correoElectronico");
            String contrasena = request.getParameter("contrasena");
            String idRolStr = request.getParameter("idRol");
            String aceptaTerminosStr = request.getParameter("aceptaTerminos");

            // VALIDACIÓN DE NULOS EN SERVIDOR: Segunda capa de seguridad por si vulneran el HTML5 del Frontend
            if (nombreCompleto == null || correoElectronico == null || contrasena == null ||
                nombreCompleto.isEmpty() || correoElectronico.isEmpty() || contrasena.isEmpty()) {
                response.getWriter().print("{\"status\":\"error\", \"message\":\"Datos obligatorios incompletos.\"}");
                return;
            }

            // REGLA DE ASIGNACIÓN: Si no viene rol especificado, le asigno automáticamente el ID 3 (Turista / Usuario Estándar)
            int idRol = (idRolStr != null) ? Integer.parseInt(idRolStr) : 3; 
            int aceptaTerminos = (aceptaTerminosStr != null) ? Integer.parseInt(aceptaTerminosStr) : 1;

            // SENTENCIAS SQL: Consultas parametrizadas preparadas para mitigar ataques de inyección SQL
            String sqlInsert = "INSERT INTO usuarios (nombre_completo, correo_electronico, contrasena, id_rol, acepta_terminos, fecha_aceptacion_terminos, estado) VALUES (?, ?, ?, ?, ?, NOW(), 'ACTIVO')";
            String sqlCheck = "SELECT id_usuario FROM usuarios WHERE correo_electronico = ?";

            // Gestión automática de recursos con try-with-resources para cerrar conexiones con el pool
            try (Connection con = conection.getConnection()) {
                
                // REGLA DE NEGOCIO: Verificación estricta de cuentas duplicadas por correo electrónico
                try (PreparedStatement psCheck = con.prepareStatement(sqlCheck)) {
                    psCheck.setString(1, correoElectronico);
                    try (ResultSet rsCheck = psCheck.executeQuery()) {
                        if (rsCheck.next()) {
                            response.getWriter().print("{\"status\":\"error\", \"message\":\"El correo electrónico ya se encuentra registrado.\"}");
                            return;
                        }
                    }
                }

                // INSERCIÓN DE DATOS: Execución de la inserción física en la base de datos relacional
                try (PreparedStatement psInsert = con.prepareStatement(sqlInsert)) {
                    psInsert.setString(1, nombreCompleto);
                    psInsert.setString(2, correoElectronico);
                    psInsert.setString(3, contrasena);
                    psInsert.setInt(4, idRol);          
                    psInsert.setInt(5, aceptaTerminos); 

                    int filasAfectadas = psInsert.executeUpdate();

                    if (filasAfectadas > 0) {
                        response.getWriter().print("{\"status\":\"success\", \"message\":\"¡Te has registrado con éxito en Juanfax!\"}");
                    } else {
                        response.getWriter().print("{\"status\":\"error\", \"message\":\"No se pudo insertar el registro en el sistema.\"}");
                    }
                }

            } catch (SQLException e) {
                System.err.println("Error SQL en Registro Juanfax: " + e.getMessage());
                response.getWriter().print("{\"status\":\"error\", \"message\":\"Error interno en el servidor al intentar registrar.\"}");
            }
            return; 
        }
        // --------------------------------------------------------------------
        // ACCIÓN POST C: MODERACIÓN Y CAMBIO DE ESTADO DE SOLICITUDES POR EL ADMIN
        // --------------------------------------------------------------------
        else if ("actualizarEstado".equals(accion)) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            String idNegocioStr = request.getParameter("idNegocio");
            String estadoRecibido = request.getParameter("estado"); // Captura lo que viene del frontend ("APROBAR" o "RECHAZAR")

            if (idNegocioStr != null && estadoRecibido != null) {
                int idNegocio = Integer.parseInt(idNegocioStr);
                
                // 🌟 FILTRO CORRECTOR PARA EL ENUM DE LA BASE DE DATOS
                String nuevoEstado = "PENDIENTE"; // Valor por defecto seguro
                String estadoUpper = estadoRecibido.toUpperCase().trim();

                if ("APROBAR".equals(estadoUpper)) {
                    nuevoEstado = "APROBADO";
                } else if ("RECHAZAR".equals(estadoUpper)) {
                    nuevoEstado = "RECHAZADO";
                } else {
                    nuevoEstado = estadoUpper; // Por si en algún momento ya viene como APROBADO/RECHAZADO
                }

                // Impresión de control para que verifiques el cambio en la consola de NetBeans
                System.out.println("Executing UPDATE - ID: " + idNegocio + " - Estado Frontend: " + estadoRecibido + " -> Convertido a: " + nuevoEstado);

                NegocioDao dao = new NegocioDao();
                // Pasamos 'nuevoEstado' que ya contiene el valor corregido apto para el ENUM
                boolean exito = dao.actualizarEstadoNegocio(idNegocio, nuevoEstado);

                if (exito) {
                    System.out.println("✅ Estado del negocio ID " + idNegocio + " cambiado a " + nuevoEstado);
                    //  NUEVO: REGISTRAR ALERTA AUTOMÁTICA EN LA BASE DE DATOS
                    AlertaDao alertaDao = new AlertaDao();
                    String tipoAlerta = "success".equals(nuevoEstado.toLowerCase()) || "aprobado".equals(nuevoEstado.toLowerCase()) ? "success" : "error";
                    String mensajeAlerta = "El establecimiento con ID: " + idNegocio + " ha sido cambiado al estado: " + nuevoEstado;
    
                    
                    Integer idAdminLogueado = (Integer) request.getSession().getAttribute("idUsuario"); 

                    alertaDao.registrarAlerta(tipoAlerta, mensajeAlerta, idAdminLogueado, idNegocio);
                    //  FIN REGISTRO ALERTA
                    response.getWriter().print("{\"status\":\"success\", \"message\":\"Estado actualizado correctamente\"}");
                } else {
                    response.getWriter().print("{\"status\":\"error\", \"message\":\"No se pudo actualizar el estado\"}");
                }
            }
            return; 
        }
        else if ("cambiarEstadoUsuario".equals(accion)) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            try {
                int idUsuario = Integer.parseInt(request.getParameter("idUsuario"));
                String nuevoEstado = request.getParameter("nuevoEstado");

                Dao.UsuarioDao usuarioDao = new Dao.UsuarioDao();
                boolean actualizado = usuarioDao.cambiarEstadoUsuario(idUsuario, nuevoEstado);

                if (actualizado) {
                    response.getWriter().print("{\"success\": true, \"mensaje\": \"Estado actualizado correctamente\"}");
                } else {
                    response.getWriter().print("{\"success\": false, \"mensaje\": \"No se pudo actualizar el estado en la base de datos\"}");
                }
            } catch (Exception e) {
                response.getWriter().print("{\"success\": false, \"mensaje\": \"Error en el servidor: " + e.getMessage() + "\"}");
            }
            return;
        }
        else if ("importarNegociosMasivo".equals(accion)) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            // SEGURIDAD: Control de rol de Administrador
            HttpSession session = request.getSession(false);
            if (session == null || !"ADMINISTRADOR".equals(session.getAttribute("role") != null ? session.getAttribute("role") : session.getAttribute("rol"))) {
                response.getWriter().print("{\"status\":\"error\", \"message\":\"Acceso denegado.\"}");
                return;
            }

            jakarta.servlet.http.Part filePart = request.getPart("archivoNegocios");
            if (filePart == null || filePart.getSize() == 0) {
                response.getWriter().print("{\"status\":\"error\", \"message\":\"No se seleccionó ningún archivo.\"}");
                return;
            }

            int registrosInsertados = 0;

            // QUERIES AJUSTADAS:
            // 1. Insertamos en 'negocios'
            String sqlNegocio = "INSERT INTO negocios (id_vendedor, id_categoria, nit, nombre_establecimiento, descripcion, estado_revision) VALUES (?, ?, ?, ?, ?, 'APROBADO')";
            
            // 2. Insertamos en 'puntos_ubicacion'
            String sqlUbicacion = "INSERT INTO puntos_ubicacion (id_negocio, id_destino, latitud, longitud, ciudad) VALUES (?, ?, ?, ?, 'Girón')";

            // 3. AGREGADO: Insertamos en 'imagenes' para que la pantalla de detalles no falle
            String sqlImagen = "INSERT INTO imagenes (id_negocio, url_imagen, descripcion, es_portada) VALUES (?, 'default_portada.jpg', 'Imagen de importación masiva', TRUE)";

            try (java.io.InputStream fileContent = filePart.getInputStream();
                 java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(fileContent, java.nio.charset.StandardCharsets.UTF_8));
                 Connection con = Config.conection.getConnection()) {

                String linea;
                boolean esPrimeraLinea = true;

                con.setAutoCommit(false);

                // Añadimos psImagen a los Statements preparados
                try (PreparedStatement psNegocio = con.prepareStatement(sqlNegocio, PreparedStatement.RETURN_GENERATED_KEYS);
                     PreparedStatement psUbicacion = con.prepareStatement(sqlUbicacion);
                     PreparedStatement psImagen = con.prepareStatement(sqlImagen)) {
                    
                    while ((linea = reader.readLine()) != null) {
                        if (esPrimeraLinea) {
                            esPrimeraLinea = false;
                            continue; // Omitimos la cabecera
                        }

                        String[] columnas = linea.split(",");
                        
                        // Valida tus 8 columnas originales del archivo
                        if (columnas.length >= 8) {
                            int idVendedor   = Integer.parseInt(columnas[0].trim());
                            int idCategoria  = Integer.parseInt(columnas[1].trim());
                            String nit       = columnas[2].trim();
                            String nombre    = columnas[3].trim();
                            String desc      = columnas[4].trim();
                            double latitud   = Double.parseDouble(columnas[5].trim());
                            double longitud  = Double.parseDouble(columnas[6].trim());
                            int idDestino    = Integer.parseInt(columnas[7].trim()); 

                            // PASO A: Insertar el negocio
                            psNegocio.setInt(1, idVendedor);
                            psNegocio.setInt(2, idCategoria);
                            psNegocio.setString(3, nit);
                            psNegocio.setString(4, nombre);
                            psNegocio.setString(5, desc);
                            
                            int filasNegocio = psNegocio.executeUpdate();
                            
                            if (filasNegocio > 0) {
                                try (ResultSet generatedKeys = psNegocio.getGeneratedKeys()) {
                                    if (generatedKeys.next()) {
                                        int idNegocioGenerado = generatedKeys.getInt(1);
                                        
                                        // PASO B: Insertar la ubicación
                                        psUbicacion.setInt(1, idNegocioGenerado);
                                        psUbicacion.setInt(2, idDestino); 
                                        psUbicacion.setDouble(3, latitud);
                                        psUbicacion.setDouble(4, longitud);
                                        psUbicacion.executeUpdate();
                                        
                                        // PASO C: AGREGADO AUTOMÁTICO - Insertar la imagen de portada vinculada
                                        psImagen.setInt(1, idNegocioGenerado);
                                        psImagen.executeUpdate();

                                        registrosInsertados++;
                                    }
                                }
                            }
                        }
                    }

                    con.commit();
                    
                    try (PreparedStatement psAlerta = con.prepareStatement("INSERT INTO alertas_sistema (tipo, mensaje) VALUES ('info', ?)")) {
                        psAlerta.setString(1, "Importación masiva exitosa: " + registrosInsertados + " establecimientos mapeados geográficamente con imágenes base.");
                        psAlerta.executeUpdate();
                    } catch (SQLException eAlerta) {
                        System.err.println("⚠️ Alerta no guardada (pero los negocios sí): " + eAlerta.getMessage());
                    }

                    response.getWriter().print("{\"status\":\"success\", \"insertados\":" + registrosInsertados + "}");
                    
                } catch (Exception e) {
                    con.rollback(); 
                    throw e;
                }

            } catch (Exception e) {
                System.err.println("❌ Error en la importación masiva de Juanfax: " + e.getMessage());
                response.getWriter().print("{\"status\":\"error\", \"message\":\"Error al procesar el archivo: " + e.getMessage() + "\"}");
            }
            return;
        }
        else if ("eliminarNegocio".equals(accion)) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            // SEGURIDAD: Validamos que quien borre tenga una sesión activa
            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("idUsuario") == null) {
                response.getWriter().write("{\"success\": false, \"message\": \"Sesión inválida o expirada. Inicia sesión nuevamente.\"}");
                return;
            }

            try {
                // Capturamos el ID enviado por el parámetro &id= del fetch
                int idNegocio = Integer.parseInt(request.getParameter("id"));

                Dao.NegocioDao negocioDao = new Dao.NegocioDao();
                // Ejecutamos la eliminación en cascada de la DB
                boolean eliminado = negocioDao.eliminarNegocio(idNegocio);

                // Respondemos textualmente el JSON estructurado que tu JavaScript espera procesar
                if (eliminado) {
                    response.getWriter().write("{\"success\": true, \"message\": \"Establecimiento eliminado correctamente de Juanfax.\"}");
                } else {
                    response.getWriter().write("{\"success\": false, \"message\": \"El negocio no pudo ser encontrado o eliminado.\"}");
                }

            } catch (NumberFormatException e) {
                response.getWriter().write("{\"success\": false, \"message\": \"El identificador del negocio no es válido.\"}");
            } catch (Exception e) {
                response.getWriter().write("{\"success\": false, \"message\": \"Error crítico en el servidor: " + e.getMessage() + "\"}");
            }
            return; // Importante para que no siga ejecutando código de abajo
        }
        else if ("actualizarNegocio".equals(accion)) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            try {
                // 1. Capturamos los datos básicos que vienen desde el JavaScript (Formulario/Fetch)
                int id = Integer.parseInt(request.getParameter("id"));
                String nit = request.getParameter("nit");
                String nombre = request.getParameter("nombre");
                String descripcion = request.getParameter("descripcion");
                int idCategoria = Integer.parseInt(request.getParameter("idCategoria"));

                // 2. Instanciamos y empaquetamos el objeto DTO que espera tu DAO
                Model.NegocioDTO negocioModificado = new Model.NegocioDTO();
                negocioModificado.setIdNegocio(id);
                negocioModificado.setNit(nit);
                negocioModificado.setNombreEstablecimiento(nombre);
                negocioModificado.setDescripcion(descripcion);
                negocioModificado.setIdCategoria(idCategoria);

                // 3. Instanciamos el DAO y ejecutamos tu método pasando el objeto
                Dao.NegocioDao dao = new Dao.NegocioDao();
                boolean exito = dao.actualizarNegocio(negocioModificado); // 🌟 Aquí se conecta con tu método

                // 4. Respondemos al frontend según el resultado de la BD
                if (exito) {
                    response.getWriter().write("{\"success\": true, \"message\": \"Establecimiento actualizado correctamente.\"}");
                } else {
                    response.getWriter().write("{\"success\": false, \"message\": \"No se pudieron salvar los cambios en la base de datos.\"}");
                }
            } catch (Exception e) {
                System.err.println("❌ Error en LoginServlet al actualizar establecimiento: " + e.getMessage());
                response.getWriter().write("{\"success\": false, \"message\": \"Error interno en el servidor: " + e.getMessage() + "\"}");
            }
            return; // 💥 Crucial para frenar el hilo y que no intente hacer el Login tradicional abajo
        }
        

        else if ("renovarSuscripcion".equals(accion)) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            java.io.PrintWriter out = response.getWriter();

            try {
                String idNegocioParam = request.getParameter("idNegocio");
                if (idNegocioParam == null || idNegocioParam.trim().isEmpty()) {
                    out.print("{\"success\": false, \"mensaje\": \"El ID del negocio no fue proporcionado.\"}");
                    return;
                }

                int idNegocio = Integer.parseInt(idNegocioParam);
                String tipoPlan = request.getParameter("tipoPlan"); // Captura dinámicamente el plan (MENSUAL o ANUAL)

                Dao.SuscripcionDao dao = new Dao.SuscripcionDao();
                boolean actualizado;

                // Si mandas un plan específico desde el JS, usamos el nuevo método robusto con ENUMs
                if (tipoPlan != null && !tipoPlan.trim().isEmpty()) {
                    actualizado = dao.actualizarPlan(idNegocio, tipoPlan);
                } else {
                    // Fallback por si mantienes algún botón de renovación simple de un mes
                    actualizado = dao.renovarSuscripcion(idNegocio);
                }

                if (actualizado) {
                    out.print("{\"success\": true, \"mensaje\": \"¡Suscripción de Juanfax procesada con éxito!\"}");
                } else {
                    out.print("{\"success\": false, \"mensaje\": \"No se pudo actualizar la suscripción. Inténtalo de nuevo.\"}");
                }

            } catch (NumberFormatException e) {
                out.print("{\"success\": false, \"mensaje\": \"Error: El ID del negocio debe ser un número válido.\"}");
            } catch (Exception e) {
                out.print("{\"success\": false, \"mensaje\": \"Error en el servidor: " + e.getMessage() + "\"}");
            } finally {
                out.flush();
            }
            return; // 💥 ¡SOLUCIÓN CLAVE! Evita que el flujo siga derecho hacia la consulta del Login
        }

        // ====================================================================
        // FLUJO DE CONTROL PREDETERMINADO: LOGICA DEL LOGIN TRADICIONAL (FORM SUBMIT)
        // ====================================================================
        // Este bloque se ejecuta por descarte cuando la petición POST viene directa desde el formulario del index.html
        String correo = request.getParameter("correo_electronico");
        String contrasena = request.getParameter("txtPass");
        
        System.out.println("Intentando login con: " + correo);
        
        // QUERY DE ACCESO: Unifico la verificación uniendo las tablas usuarios y roles mediante un INNER JOIN
        String sql = "SELECT u.id_usuario, u.nombre_completo, u.estado, r.nombre_rol " +
                     "FROM usuarios u " +
                     "INNER JOIN roles r ON u.id_rol = r.id_rol " +
                     "WHERE u.correo_electronico = ? AND u.contrasena = ?";

        try (Connection con = conection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, correo);
            ps.setString(2, contrasena);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String estado = rs.getString("estado");

                    // REGLA DE SEGURIDAD: Denegación de entrada inmediata si la cuenta fue penalizada
                    if ("BLOQUEADO".equals(estado)) {
                        response.sendRedirect("index.html?error=UsuarioBloqueado");
                        return;
                    }

                    // CONTROL DE SESIONES COMPARTIDAS: Inicializo la sesión HTTP en true para guardar variables globales en servidor
                    HttpSession session = request.getSession(true);
                    session.setAttribute("idUsuario", rs.getInt("id_usuario"));
                    session.setAttribute("nombre", rs.getString("nombre_completo"));
                    String rol = rs.getString("nombre_rol").toUpperCase();
                    session.setAttribute("rol", rol);

                    // REDIRECCIÓN DINÁMICA: Dependiendo de la cadena del rol mapeada, se redirige a su respectivo módulo de trabajo
                    switch (rol) {
                        case "ADMINISTRADOR":
                            response.sendRedirect("vistas/mainAdministrador.html");
                            break;
                        case "VENDEDOR":
                            response.sendRedirect("vistas/misNegocios.html");
                            break;
                        case "TURISTA":
                            response.sendRedirect("vistas/mainUser.html");
                            break;
                    }
                } else {
                    // Si el ResultSet vino vacío, redirige al index inyectando bandera de error por credenciales incorrectas
                    response.sendRedirect("index.html?error=InvalidCredentials");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error en LoginServlet: " + e.getMessage());
            response.sendRedirect("index.html?error=ServerError");
        }
    }

    /**
     * ========================================================================
     * MÉTODO AUXILIAR DE SEGURIDAD Y SOPORTE DE DATOS
     * ========================================================================
     * Reemplaza de forma explícita las barras invertidas, comillas o saltos de renglón
     * dentro de las cadenas obtenidas de la BD para que las estructuras JSON que armo
     * a mano usando StringBuilder no causen un desplome de sintaxis al ser procesadas en JS.
     */
    private String escapeJson(String valor) {
        if (valor == null) return "";
        return valor.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\b", "\\b")
                    .replace("\f", "\\f")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }
}