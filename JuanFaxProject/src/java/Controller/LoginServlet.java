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

@WebServlet("/LoginServlet")
public class LoginServlet extends HttpServlet {

    // Método GET: Ideal para que el JS consulte datos de manera limpia
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String accion = request.getParameter("accion");

        // SI EL JAVASCRIPT SOLICITA EL CARRUSEL
        if ("carrusel".equals(accion)) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            try (PrintWriter out = response.getWriter()) {
                NegocioDao negocioDao = new NegocioDao();
                // Llamamos a tu método del DAO que saca los 3 destinos con mayor promedio
                List<NegocioDTO> lista = negocioDao.obtenerDestinosDestacados();

                // 🔍 LÍNEA DE DIAGNÓSTICO 1: Ver cuántos datos trae la lista en Java
                System.out.println("====== DIAGNÓSTICO JUANFAX ======");
                System.out.println("Cantidad de negocios traídos de la BD: " + lista.size());
                
                // Construimos el JSON manualmente paso a paso
                StringBuilder json = new StringBuilder();
                json.append("[");
                for (int i = 0; i < lista.size(); i++) {
                    NegocioDTO n = lista.get(i);
                    json.append("{");
                    json.append("\"nombreEstablecimiento\":\"").append(n.getNombreEstablecimiento()).append("\",");
                    json.append("\"urlImagen\":\"").append(n.getUrl_imagen()).append("\"");
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
            return; // Detiene el flujo para que no intente ejecutar lógica de login
        }

        // Si entran al GET por otra razón, puedes redirigir al login index
        response.sendRedirect("index.html"); 
    }
        
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            
            throws ServletException, IOException {

        // Capturar los parámetros enviados por el formulario usando los atributos 'name' u obtenidos por Fetch
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
            ps.setString(2, contrasena); // Nota: En producción aplica Hash (BCrypt) aquí.

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

                    // Redirección en el Servidor según tu árbol de vistas
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
                    // Credenciales incorrectas
                    response.sendRedirect("index.html?error=InvalidCredentials");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error en LoginServlet: " + e.getMessage());
            response.sendRedirect("index.html?error=ServerError");
        }
    }
}