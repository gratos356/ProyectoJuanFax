package Model;

public class NegocioDTO {
    private int idNegocio;
    private String nombreEstablecimiento;
    private String descripcion;
    private String nombreCategoria;
    private String url_imagen;
    private String direccionTexto;
    private String barrio;
    private double calificacionPromedio;
    private double latitud;
    private double longitud;

    public NegocioDTO() {}

    public NegocioDTO(int idNegocio, String nombreEstablecimiento, String descripcion, String nombreCategoria, 
                      String urlImagen, String direccionTexto, String barrio, double calificacionPromedio) {
        this.idNegocio = idNegocio;
        this.nombreEstablecimiento = nombreEstablecimiento;
        this.descripcion = descripcion;
        this.nombreCategoria = nombreCategoria;
        this.url_imagen = url_imagen;
        this.direccionTexto = direccionTexto;
        this.barrio = barrio;
        this.calificacionPromedio = calificacionPromedio;
    }
    
    public NegocioDTO(String nombreEstablecimiento, String urlImagen) {
        this.nombreEstablecimiento = nombreEstablecimiento;
        this.url_imagen = url_imagen;
    }
    // Getters

    public int getIdNegocio() {
        return idNegocio;
    }

    public void setIdNegocio(int idNegocio) {
        this.idNegocio = idNegocio;
    }

    public String getNombreEstablecimiento() {
        return nombreEstablecimiento;
    }

    public void setNombreEstablecimiento(String nombreEstablecimiento) {
        this.nombreEstablecimiento = nombreEstablecimiento;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getNombreCategoria() {
        return nombreCategoria;
    }

    public void setNombreCategoria(String nombreCategoria) {
        this.nombreCategoria = nombreCategoria;
    }

    public String getUrl_imagen() {
        return url_imagen;
    }

    public void setUrl_imagen(String url_imagen) {
        this.url_imagen = url_imagen;
    }

    public String getDireccionTexto() {
        return direccionTexto;
    }

    public void setDireccionTexto(String direccionTexto) {
        this.direccionTexto = direccionTexto;
    }

    public String getBarrio() {
        return barrio;
    }

    public void setBarrio(String barrio) {
        this.barrio = barrio;
    }

    public double getCalificacionPromedio() {
        return calificacionPromedio;
    }

    public void setCalificacionPromedio(double calificacionPromedio) {
        this.calificacionPromedio = calificacionPromedio;
    }

    public double getLatitud() {
        return latitud;
    }

    public void setLatitud(double latitud) {
        this.latitud = latitud;
    }

    public double getLongitud() {
        return longitud;
    }

    public void setLongitud(double longitud) {
        this.longitud = longitud;
    }
    
    
    
    
}