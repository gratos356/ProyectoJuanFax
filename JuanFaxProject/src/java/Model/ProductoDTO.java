package Model;

public class ProductoDTO {
    private int idProducto;
    private int idNegocio;
    private String nombre;
    private double precio;
    private int stock;
    private String urlImagen;
    private String estado; // 🌟 NUEVO ATRIBUTO

    public ProductoDTO() {}

    // Constructor completo actualizado
    public ProductoDTO(int idProducto, int idNegocio, String nombre, double precio, int stock, String urlImagen, String estado) {
        this.idProducto = idProducto;
        this.idNegocio = idNegocio;
        this.nombre = nombre;
        this.precio = precio;
        this.stock = stock;
        this.urlImagen = urlImagen;
        this.estado = estado;
    }

    public int getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(int idProducto) {
        this.idProducto = idProducto;
    }

    public int getIdNegocio() {
        return idNegocio;
    }

    public void setIdNegocio(int idNegocio) {
        this.idNegocio = idNegocio;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public double getPrecio() {
        return precio;
    }

    public void setPrecio(double precio) {
        this.precio = precio;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public String getUrlImagen() {
        return urlImagen;
    }

    public void setUrlImagen(String urlImagen) {
        this.urlImagen = urlImagen;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    
}