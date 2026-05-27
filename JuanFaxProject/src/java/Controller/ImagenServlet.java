package Controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/verImagen")
public class ImagenServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String nombreImagen = request.getParameter("nombre");
        // Apuntamos a tu ruta exacta
        File file = new File("D:/doc/descktop/ProyectoSena-Juan_David_Ramirez_Saavedra/imagenesJuanFax/" + nombreImagen);
        
        response.setContentType("image/jpeg");
        try (OutputStream out = response.getOutputStream(); 
             FileInputStream in = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
        }
    }
}