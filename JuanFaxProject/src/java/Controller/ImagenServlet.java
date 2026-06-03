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
    
    // Método que captura las peticiones GET enviadas mediante etiquetas como <img src="verImagen?nombre=archivo.jpg">
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Recuperamos el parámetro 'nombre' proveniente de la URL de la solicitud HTTP
        String nombreImagen = request.getParameter("nombre");
        
        // Creamos un objeto File que apunta a la ruta física absoluta de almacenamiento en el sistema de archivos local
        File file = new File("D:/doc/descktop/ProyectoSena-Juan_David_Ramirez_Saavedra/imagenesJuanFax/" + nombreImagen);
        
        // Seteamos la cabecera Content-Type de la respuesta HTTP para que el navegador sepa
        // que va a recibir un flujo binario correspondiente a una imagen JPEG y no código HTML estructurado
        response.setContentType("image/jpeg");
        
        // Bloque try-with-resources: Abre de forma automática el OutputStream de salida del cliente (navegador)
        // y el FileInputStream de entrada para leer los bytes del disco duro. Ambos se liberarán de forma segura al finalizar.
        try (OutputStream out = response.getOutputStream(); 
             FileInputStream in = new FileInputStream(file)) {
            
            // Creamos un arreglo de bytes (buffer) de 4KB (4096 bytes) para fragmentar la lectura de la imagen de forma óptima
            byte[] buffer = new byte[4096];
            int length;
            
            // Bucle de lectura activa: 'in.read(buffer)' llena el arreglo con bytes del archivo físico y devuelve la cantidad leída.
            // Continuará iterando cíclicamente en bloques de 4KB hasta llegar al final del archivo (cuando devuelva -1 o 0)
            while ((length = in.read(buffer)) > 0) {
                // Escribe en el flujo de respuesta HTTP los bytes del buffer desde la posición 0 hasta el límite de datos leídos ('length')
                out.write(buffer, 0, length);
            }
            // out.flush() se ejecuta de manera interna e implícita al completarse el flujo para forzar el envío de los bytes acumulados
            
        } catch (IOException e) {
            // Captura errores del sistema de entrada/salida (I/O), por ejemplo, si el archivo de la imagen fue borrado o no existe
            System.err.println("❌ Error al renderizar la imagen física en ImagenServlet: " + e.getMessage());
            // En caso de fallo preventivo, envía un estado de error HTTP 404 (No Encontrado) al navegador del cliente
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }
}