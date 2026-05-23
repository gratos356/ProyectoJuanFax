package Controller;

import Dao.NegocioDao;
import Model.NegocioDTO;
import java.io.PrintWriter;
import java.util.List;

import Config.Conection;
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

        request.setCharacterEncoding("UTF-8");
        String accion = request.getParameter("accion");

        // Imprime en la consola de NetBeans para diagnóstico básico
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
            return; // Corta el flujo de inmediato
        } 
        // ====================================================================
        // ACCIÓN B: OBTENER UN ÚNICO NEGOCIO CON SUS COORDENADAS
        // ====================================================================
        else if ("detalleNegocioUnico".equals(accion)) {
            String nombreNegocio = request.getParameter("nombre");
            System.out.println("-> Ejecutando detalleNegocioUnico para: " + nombreNegocio);

            NegocioDao negocioDAO = new NegocioDao(); 
            NegocioDTO negocio = negocioDAO.obtenerNegocioPorNombre(nombreNegocio);

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            if (negocio != null) {
                String json = "{"
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
            return; // Corta el flujo de inmediato
        }
        // ====================================================================
        // ACCIÓN C: CARRUSEL HERO TRADICIONAL
        // ====================================================================
        else if ("carrusel".equals(accion)) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            try (PrintWriter out = response.getWriter()) {
                NegocioDao negocioDao = new NegocioDao();
                List<NegocioDTO> lista = negocioDao.obtenerDestinosDestacados();

                System.out.println("====== DIAGNÓSTICO JUANFAX ======");
                System.out.println("Cantidad de negocios traídos de la BD: " + lista.size());

                StringBuilder json = new StringBuilder();
                json.append("[");
                for (int i = 0; i < lista.size(); i++) {
                    NegocioDTO n = lista.get(i);
                    json.append("{");
                    json.append("\"nombreEstablecimiento\":\"").append(escapeJson(n.getNombreEstablecimiento())).append("\",");
                    json.append("\"urlImagen\":\"").append(escapeJson(n.getUrl_imagen())).append("\"");
                    json.append("}");
                    if (i < lista.size() - 1) {
                        json.append(",");
                    }
                }
                json.append("]");

                out.print(json.toString());
                out.flush();
            } catch (Exception e) {
                e.printStackTrace();
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
            return; // Corta el flujo de inmediato
        }
        // ====================================================================
        // PROTECCIÓN DE SEGURIDAD GENERAL
        // ====================================================================
        else {
            System.out.println("⚠️ Alerta: Se detectó una petición GET sin acción AJAX válida. Redirigiendo...");
            response.sendRedirect("index.html"); 
        }
    }
        
    /**
     * Procesa las peticiones POST (Formulario de Login)
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        String correo = request.getParameter("correo_electronico");
        String contrasena = request.getParameter("txtPass");
        
        System.out.println("Intentando login con: " + correo + " y pass: " + contrasena);
        
        String sql = "SELECT u.id_usuario, u.nombre_completo, u.estado, r.nombre_rol " +
                     "FROM usuarios u " +
                     "INNER JOIN roles r ON u.id_rol = r.id_rol " +
                     "WHERE u.correo_electronico = ? AND u.contrasena = ?";

        try (Connection con = Conection.getConnection();
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

                    // Crear la sesión del usuario
                    HttpSession session = request.getSession(true);
                    session.setAttribute("idUsuario", rs.getInt("id_usuario"));
                    session.setAttribute("nombre", rs.getString("nombre_completo"));
                    String rol = rs.getString("nombre_rol").toUpperCase();
                    session.setAttribute("rol", rol);

                    // Redirección por Roles
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

    /**
     * Función auxiliar para sanitizar el JSON manual
     */
    private String escapeJson(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\b", "\\b")
                    .replace("\f", "\\f")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }
}