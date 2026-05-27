package Controller;

import Dao.NegocioDao;
import Model.NegocioDTO;
import java.io.PrintWriter;
import java.util.List;

import Config.conection;
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

@WebServlet(name = "LoginServlet", urlPatterns = {"/LoginServlet"})
public class LoginServlet extends HttpServlet {

    /**
     * Procesa las peticiones GET (Consultas AJAX de Juanfax)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 🌟 REGLA DE ORO: Forzamos UTF-8 al inicio para que no rompa tildes como en "Café Central Girón"
        request.setCharacterEncoding("UTF-8");
        String accion = request.getParameter("accion");

        System.out.println(" ACCION DETECTADA EN GET: [" + accion + "]");
        
        // 2. INTERCEPTAR LA PETICIÓN ASÍNCRONA PRIMERO
        if ("metricasVendedor".equals(accion)) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            // 1. Recuperamos la sesión actual de forma segura
            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("idUsuario") == null) {
                response.getWriter().print("{\"error\": \"Sesión no válida o caducada.\"}");
                return;
            }

            // 2. Capturamos el id_usuario del vendedor logueado
            int idVendedor = (int) session.getAttribute("idUsuario");
            
            //Capturar el ID del negocio enviado por el JS
            String idNegocioStr = request.getParameter("idNegocio");
            int idNegocio = 0;
            if (idNegocioStr != null && !idNegocioStr.isEmpty()) {
                idNegocio = Integer.parseInt(idNegocioStr);
            }
            
            // 3. Instanciamos tu DAO (Fíjate en la mayúscula de Dao)
            Dao.NegocioDao negocioDao = new Dao.NegocioDao();
            
            // Consultamos las métricas unificadas desde MySQL
            java.util.Map<String, Object> datosReales = negocioDao.obtenerMetricasVendedor(idVendedor ,idNegocio);

            // 4. Extraemos de forma segura los valores numéricos y la lista
            int vistas = (int) datosReales.getOrDefault("vistasTotales", 0);
            int clicks = (int) datosReales.getOrDefault("clicksEnlaces", 0);
            int resenas = (int) datosReales.getOrDefault("totalResenas", 0);
            double puntuacion = (double) datosReales.getOrDefault("puntuacion", 0.0);
            
            java.util.List<java.util.Map<String, Object>> listaComentarios = 
                (java.util.List<java.util.Map<String, Object>>) datosReales.get("comentariosRecientes");
            
            java.util.Map<String, Integer> dist = 
                (java.util.Map<String, Integer>) datosReales.get("distribucionEstrellas");

            // 5. CONSTRUCCIÓN DEL JSON DINÁMICO
            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"vistasTotales\": ").append(vistas).append(",");
            json.append("\"clicksEnlaces\": ").append(clicks).append(",");
            json.append("\"totalResenas\": ").append(resenas).append(",");
            json.append("\"puntuacion\": ").append(puntuacion).append(",");
            
            // Mantenemos las barras del gráfico temporales con los días de la semana
            json.append("\"visitasSemana\": [");
            json.append("  {\"nombreDia\":\"Lun\", \"porcentaje\": 45},");
            json.append("  {\"nombreDia\":\"Mar\", \"porcentaje\": 60},");
            json.append("  {\"nombreDia\":\"Mié\", \"porcentaje\": 80},");
            json.append("  {\"nombreDia\":\"Jue\", \"porcentaje\": 50},");
            json.append("  {\"nombreDia\":\"Vie\", \"porcentaje\": 95},");
            json.append("  {\"nombreDia\":\"Sáb\", \"porcentaje\": 70},");
            json.append("  {\"nombreDia\":\"Dom\", \"porcentaje\": 35}");
            json.append("],");

            // Mapeamos los comentarios reales traídos por el JOIN de usuarios y calificaciones_sanciones
            json.append("\"comentariosRecientes\": [");
            if (listaComentarios != null) {
                for (int i = 0; i < listaComentarios.size(); i++) {
                    java.util.Map<String, Object> c = listaComentarios.get(i);
                    
                    // Validamos que el texto del comentario no venga nulo para evitar romper el JSON
                    String texto = c.get("textoComentario") != null ? c.get("textoComentario").toString() : "";
                    // Escapamos comillas dobles internas por seguridad
                    texto = texto.replace("\"", "\\\"");

                    json.append("{");
                    json.append("\"nombreUsuario\":\"").append(c.get("nombreUsuario")).append("\",");
                    json.append("\"calificacion\":").append(c.get("calificacion")).append(",");
                    json.append("\"textoComentario\":\"").append(texto).append("\"");
                    json.append("}");
                    
                    if (i < listaComentarios.size() - 1) {
                        json.append(",");
                    }
                }
            }
            json.append("],");

            // Mapeamos la distribución porcentual calculada por el DAO
            int p5 = dist != null ? dist.getOrDefault("cinco", 0) : 0;
            int p4 = dist != null ? dist.getOrDefault("cuatro", 0) : 0;
            int p3 = dist != null ? dist.getOrDefault("tres", 0) : 0;

            json.append("\"distribucionEstrellas\": {");
            json.append("  \"cinco\": ").append(p5).append(",");
            json.append("  \"cuatro\": ").append(p4).append(",");
            json.append("  \"tres\": ").append(p3);
            json.append("}");
            json.append("}"); // Fin del JSON principal

            // 6. ENVIAMOS LA RESPUESTA LIMPIA AL FRONTEND
            try (java.io.PrintWriter out = response.getWriter()) {
                out.print(json.toString());
                out.flush();
            }
            return; // ⭐ CRUCIAL: Detiene la ejecución aquí para que no mande un HTML abajo.
        }
        // ====================================================================
        // NUEVA ACCIÓN: LISTAR NEGOCIOS ASIGNADOS AL VENDEDOR LOGUEADO
        // ====================================================================
        else if ("listarNegociosPorVendedor".equals(accion)) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("idUsuario") == null) {
                response.getWriter().print("[]");
                return;
            }

            int idVendedor = (int) session.getAttribute("idUsuario");

            // Aquí puedes instanciar tu NegocioDao para traer la lista filtrada por el ID del usuario
            Dao.NegocioDao negocioDao = new Dao.NegocioDao();
            // Nota: Debes tener o crear un método en tu DAO que haga un: 
            // "SELECT id_negocio, nombre_establecimiento, url_imagen FROM negocios WHERE id_usuario_vendedor = ?"
            List<Model.NegocioDTO> misNegocios = negocioDao.obtenerNegociosPorVendedor(idVendedor); 

            StringBuilder json = new StringBuilder();
            json.append("[");
            for (int i = 0; i < misNegocios.size(); i++) {
                Model.NegocioDTO n = misNegocios.get(i);
                json.append("{");
                json.append("\"idNegocio\":").append(n.getIdNegocio()).append(",");
                json.append("\"nombreEstablecimiento\":\"").append(escapeJson(n.getNombreEstablecimiento())).append("\",");
                json.append("\"urlImagen\":\"").append(escapeJson(n.getUrl_imagen())).append("\"");
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
        // ====================================================================
        // ACCIÓN A: EXPANDING CARDS - FILTRAR NEGOCIOS POR CATEGORÍA
        // ====================================================================
        else if ("negociosPorCategoria".equals(accion)) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            String categoria = request.getParameter("categoria");
            NegocioDao negocioDao = new NegocioDao();
            List<NegocioDTO> lista = negocioDao.obtenerNegociosPorCategoria(categoria);

            StringBuilder json = new StringBuilder();
            json.append("[");
            for (int i = 0; i < lista.size(); i++) {
                NegocioDTO n = lista.get(i);
                json.append("{");
                json.append("\"idNegocio\":").append(n.getIdNegocio()).append(","); // <--- AÑADE ESTA LÍNEA
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
        
        // ====================================================================
        // ACCIÓN B: DETALLE DE NEGOCIO ÚNICO
        // ====================================================================
       else if ("detalleNegocioUnico".equals(accion)) {
            // 1. CAMBIO: Obtenemos el "id" que envía tu JavaScript
            String idStr = request.getParameter("id"); 
            System.out.println("-> Ejecutando detalleNegocioUnico para ID: " + idStr);

            try {
                int idNegocio = Integer.parseInt(idStr); // Convertimos el string a número

                NegocioDao negocioDAO = new NegocioDao(); 
                // 2. CAMBIO: Llamamos al método por ID (el que agregamos antes al DAO)
                NegocioDTO negocio = negocioDAO.obtenerNegocioPorId(idNegocio);

                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");

                if (negocio != null) {
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
                // Manejo de error si el ID enviado no es un número válido
                System.err.println("Error: ID recibido no es un número válido: " + idStr);
                response.getWriter().print("{\"error\": \"ID inválido\"}");
            }
            return;
        }
        
        // ====================================================================
        // ACCIÓN C: CARRUSEL DESTACADOS
        // ====================================================================
        else if ("carrusel".equals(accion)) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            try (PrintWriter out = response.getWriter()) {
                NegocioDao negocioDao = new NegocioDao();
                List<NegocioDTO> lista = negocioDao.obtenerDestinosDestacados();

                StringBuilder json = new StringBuilder();
                json.append("[");
                for (int i = 0; i < lista.size(); i++) {
                    NegocioDTO n = lista.get(i);
                    json.append("{");
                    json.append("\"idNegocio\":").append(n.getIdNegocio()).append(","); // <--- AÑADE ESTA LÍNEA
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
        
        // ====================================================================
        // ACCIÓN D: LISTAR COMENTARIOS DESDE LA BD
        // ====================================================================
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
                    
                    // Formateamos la fecha de forma amigable (dd/MM/yyyy)
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy");
                    String fechaFormateada = c.getFechaPublicacion() != null ? sdf.format(c.getFechaPublicacion()) : "";

                    json.append("{");
                    json.append("\"nombreUsuario\":\"").append(escapeJson(c.getNombreUsuario())).append("\",");
                    json.append("\"textoComentario\":\"").append(escapeJson(c.getTextoComentario())).append("\",");
                    json.append("\"fecha\":\"").append(fechaFormateada).append("\"");
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
        else {
            System.out.println("⚠️ Alerta: Se detectó una petición GET sin acción AJAX válida. Redirigiendo...");
            response.sendRedirect("index.html"); 
        }
    }
        
    /**
     * Procesa las peticiones POST (Formulario de Login y Guardado)
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        String accion = request.getParameter("accion");

        // ====================================================================
        // ACCIÓN E: GUARDAR COMENTARIOS
        // ====================================================================
        if ("guardarComentario".equals(accion)) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("idUsuario") == null) {
                response.getWriter().print("{\"status\":\"error\", \"message\":\"Debes iniciar sesión para comentar.\"}");
                return;
            }

            int idUsuario = (int) session.getAttribute("idUsuario");
            int idNegocio = Integer.parseInt(request.getParameter("idNegocio"));
            String texto = request.getParameter("textoComentario"); 
            
            // 🌟 1. CAPTURAR EL VALOR DE LAS ESTRELLAS DESDE EL JS
            String valorPuntuacionStr = request.getParameter("valorPuntuacion");
            int calificacion = 0;
            
            if (valorPuntuacionStr != null && !valorPuntuacionStr.isEmpty()) {
                calificacion = Integer.parseInt(valorPuntuacionStr.trim());
            }

            // 2. POBLAR EL OBJETO DTO COMPLETAMENTE
            Model.ComentarioDTO nuevoComentario = new Model.ComentarioDTO();
            nuevoComentario.setIdNegocio(idNegocio);
            nuevoComentario.setIdUsuario(idUsuario);
            nuevoComentario.setTextoComentario(texto);
            
            // 🌟 3. ASIGNAR LA CALIFICACIÓN AL DTO ANTES DE GUARDAR
            nuevoComentario.setCalificacion(calificacion); 

            Dao.ComentarioDao comentarioDao = new Dao.ComentarioDao();
            boolean exito = comentarioDao.insertarComentario(nuevoComentario);

            if (exito) {
                response.getWriter().print("{\"status\":\"success\", \"message\":\"Comentario publicado\"}");
            } else {
                response.getWriter().print("{\"status\":\"error\", \"message\":\"No se pudo guardar el comentario\"}");
            }
            return; 
        }
        // ====================================================================
        // 🌟 NUEVA ACCIÓN: REGISTRAR UN NUEVO USUARIO (CON ROL Y TÉRMINOS)
        // ====================================================================
        else if ("registrarUsuario".equals(accion)) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            String nombreCompleto = request.getParameter("nombreCompleto");
            String correoElectronico = request.getParameter("correoElectronico");
            String contrasena = request.getParameter("contrasena");
            String idRolStr = request.getParameter("idRol");
            String aceptaTerminosStr = request.getParameter("aceptaTerminos");

            // Validar que no lleguen parámetros vacíos
            if (nombreCompleto == null || correoElectronico == null || contrasena == null ||
                nombreCompleto.isEmpty() || correoElectronico.isEmpty() || contrasena.isEmpty()) {
                response.getWriter().print("{\"status\":\"error\", \"message\":\"Datos obligatorios incompletos.\"}");
                return;
            }

            int idRol = (idRolStr != null) ? Integer.parseInt(idRolStr) : 3; 
            int aceptaTerminos = (aceptaTerminosStr != null) ? Integer.parseInt(aceptaTerminosStr) : 1;

            // Consultas adaptadas exactamente al diseño de tu base de datos
            String sqlInsert = "INSERT INTO usuarios (nombre_completo, correo_electronico, contrasena, id_rol, acepta_terminos, fecha_aceptacion_terminos, estado) VALUES (?, ?, ?, ?, ?, NOW(), 'ACTIVO')";
            String sqlCheck = "SELECT id_usuario FROM usuarios WHERE correo_electronico = ?";

            try (Connection con = conection.getConnection()) {
                
                // 1. Evitar la creación de correos duplicados
                try (PreparedStatement psCheck = con.prepareStatement(sqlCheck)) {
                    psCheck.setString(1, correoElectronico);
                    try (ResultSet rsCheck = psCheck.executeQuery()) {
                        if (rsCheck.next()) {
                            response.getWriter().print("{\"status\":\"error\", \"message\":\"El correo electrónico ya se encuentra registrado.\"}");
                            return;
                        }
                    }
                }

                // 2. Insertar el nuevo registro con estado 'ACTIVO' por defecto
                try (PreparedStatement psInsert = con.prepareStatement(sqlInsert)) {
                    psInsert.setString(1, nombreCompleto);
                    psInsert.setString(2, correoElectronico);
                    psInsert.setString(3, contrasena);
                    psInsert.setInt(4, idRol);          // Guarda el 2 (Vendedor) o 3 (Cliente)
                    psInsert.setInt(5, aceptaTerminos); // Guarda 1 (Aceptó)

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
            return; // Corta el flujo para que no intente ejecutar el código de Login inferior
        }
        // ====================================================================
        // AUTENTICACIÓN ORIGINAL DE JUANFAX (MANTENIDA INTACTA)
        // ====================================================================
        String correo = request.getParameter("correo_electronico");
        String contrasena = request.getParameter("txtPass");
        
        System.out.println("Intentando login con: " + correo + " y pass: " + contrasena);
        
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

                    if ("BLOQUEADO".equals(estado)) {
                        response.sendRedirect("index.html?error=UsuarioBloqueado");
                        return;
                    }

                    HttpSession session = request.getSession(true);
                    session.setAttribute("idUsuario", rs.getInt("id_usuario"));
                    session.setAttribute("nombre", rs.getString("nombre_completo"));
                    String rol = rs.getString("nombre_rol").toUpperCase();
                    session.setAttribute("rol", rol);

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
                    response.sendRedirect("index.html?error=InvalidCredentials");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error en LoginServlet: " + e.getMessage());
            response.sendRedirect("index.html?error=ServerError");
        }
    }

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