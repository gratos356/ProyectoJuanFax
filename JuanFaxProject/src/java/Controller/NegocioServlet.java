package Controller;

import Dao.NegocioDao;
import Model.NegocioDTO;
import java.io.File;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;

@WebServlet(name = "NegocioServlet", urlPatterns = {"/NegocioServlet"})
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024 * 2,
    maxFileSize = 1024 * 1024 * 10,
    maxRequestSize = 1024 * 1024 * 50
)
public class NegocioServlet extends HttpServlet {

   @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 1. Validar sesión
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("idUsuario") == null) {
            response.sendRedirect("../index.html?error=SinSesion");
            return;
        }
        int idVendedor = (int) session.getAttribute("idUsuario");

        try {
            // 2. Capturar parámetros básicos
            String nombreNegocio = request.getParameter("nombre");
            String nit = request.getParameter("nit");
            int idCategoria = Integer.parseInt(request.getParameter("categoria"));
            String descripcion = request.getParameter("descripcion");
            
            // 🌟 NUEVO: Capturar el plan seleccionado ("Mensual" o "Anual")
            String tipoPlan = request.getParameter("tipoPlan"); 

            // 3. Capturar NUEVA ubicación
            double latitud = Double.parseDouble(request.getParameter("latitud"));
            double longitud = Double.parseDouble(request.getParameter("longitud"));

            // 4. Processar foto (Con la validación que ya teníamos)
            Part part = request.getPart("foto");
            String nombreFotoFinal = "default-negocio.jpg";

            if (part != null && part.getSubmittedFileName() != null && !part.getSubmittedFileName().isEmpty()) {
                String nombreArchivoOriginal = part.getSubmittedFileName();
                nombreFotoFinal = System.currentTimeMillis() + "_" + nombreArchivoOriginal.replaceAll("\\s+", "_");
                String pathDestino = "D:/doc/descktop/ProyectoSena-Juan_David_Ramirez_Saavedra/imagenesJuanFax/";

                File folder = new File(pathDestino);
                if (!folder.exists()) {
                    folder.mkdirs(); // Esto creará la carpeta automáticamente si aún no existe
                }
                part.write(pathDestino + nombreFotoFinal);
            }

            // 5. Preparar DTO
            Model.NegocioDTO nuevoNegocio = new Model.NegocioDTO();
            nuevoNegocio.setNombreEstablecimiento(nombreNegocio);
            nuevoNegocio.setUrl_imagen(nombreFotoFinal);

            // 6. Enviar al DAO (Agregamos 'tipoPlan' al final)
            Dao.NegocioDao negocioDao = new Dao.NegocioDao();
            boolean exito = negocioDao.registrarNegocio(nuevoNegocio, idVendedor, idCategoria, nit, descripcion, latitud, longitud, tipoPlan);

            // 7. Redirección
            if (exito) {
                response.sendRedirect("vistas/misNegocios.html?registro=ok");
            } else {
                response.sendRedirect("vistas/misNegocios.html?error=SqlError");
            }

        } catch (Exception e) {
            System.err.println("🚨 ERROR EN SERVLET: " + e.getMessage());
            e.printStackTrace();
            response.sendRedirect("vistas/misNegocios.html?error=Excepcion");
        }
    }
}