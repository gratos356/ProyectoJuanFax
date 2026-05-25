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

        // ====================================================================
        // ACCIÓN A: EXPANDING CARDS - FILTRAR NEGOCIOS POR CATEGORÍA
        // ====================================================================
        if ("negociosPorCategoria".equals(accion)) {
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
            String nombreNegocio = request.getParameter("nombre");
            System.out.println("-> Ejecutando detalleNegocioUnico para: " + nombreNegocio);

            NegocioDao negocioDAO = new NegocioDao(); 
            NegocioDTO negocio = negocioDAO.obtenerNegocioPorNombre(nombreNegocio);

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            if (negocio != null) {
                // 🌟 CORREGIDO: Añadimos \"idNegocio\" al JSON para que el JS sepa qué establecimiento es
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
            
            // 🌟 CORREGIDO: Macheamos con "textoComentario" que es el que manda tu JS
            String texto = request.getParameter("textoComentario"); 

            Model.ComentarioDTO nuevoComentario = new Model.ComentarioDTO();
            nuevoComentario.setIdNegocio(idNegocio);
            nuevoComentario.setIdUsuario(idUsuario);
            nuevoComentario.setTextoComentario(texto);

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
        if ("registrarUsuario".equals(accion)) {
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
                            response.sendRedirect("vistas/mainVendedor.html");
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