/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package modelo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.swing.JOptionPane;

public class conection {
    private static final String URL = "jdbc:mysql://127.0.0.1:3306/juanfax";
    private static final String USER = "root";
    private static final String PASSWORD = "Admin123";

    public static Connection conectar() {
        System.out.println("Intentando conectar...");
        Connection conn = null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(URL, USER, PASSWORD);
            // Esto abrirá una ventana real en tu pantalla
            javax.swing.JOptionPane.showMessageDialog(null, "¡Conexión exitosa a Juanfax!");
        } catch (ClassNotFoundException | SQLException e) {
            javax.swing.JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
        }
        return conn;
    }
    public void mostrarUsuarios(){
        String sql = "SELECT * FROM usuarios";
        try(Connection conn = conectar();
                java.sql.Statement stmt = conn.createStatement();
                java.sql.ResultSet rs = stmt.executeQuery(sql)) {
                
            JOptionPane.showMessageDialog(null, "listado de Usuarios");
            while (rs.next()){
                int id = rs.getInt("id_usuario"); 
                String nombre = rs.getString("nombre_completo");
                String rol = rs.getString("rol");
                String estado = rs.getString("estado");

                System.out.println("ID: " + id + " | Nombre: " + nombre + " | Rol: " + rol + " | Estado: " + estado);
            }
        }catch (SQLException e) {
            System.out.println("Error al imprimir: " + e.getMessage());
        }
    }
    public static void main(String[] args) {
        conection objetoConexion = new conection();
        
        objetoConexion.mostrarUsuarios();
    }
}