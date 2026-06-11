package Model;

public class ReporteVentaDTO {
    private int idProducto;
    private String nombreProducto;
    private int totalUnidadesVendidas;
    private double totalIngresosProducto;

    // Constructor vacío
    public ReporteVentaDTO() {}

    // Constructor lleno
    public ReporteVentaDTO(int idProducto, String nombreProducto, int totalUnidadesVendidas, double totalIngresosProducto) {
        this.idProducto = idProducto;
        this.nombreProducto = nombreProducto;
        this.totalUnidadesVendidas = totalUnidadesVendidas;
        this.totalIngresosProducto = totalIngresosProducto;
    }

    // Getters y Setters
    public int getIdProducto() { return idProducto; }
    public void setIdProducto(int idProducto) { this.idProducto = idProducto; }

    public String getNombreProducto() { return nombreProducto; }
    public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }

    public int getTotalUnidadesVendidas() { return totalUnidadesVendidas; }
    public void setTotalUnidadesVendidas(int totalUnidadesVendidas) { this.totalUnidadesVendidas = totalUnidadesVendidas; }

    public double getTotalIngresosProducto() { return totalIngresosProducto; }
    public void setTotalIngresosProducto(double totalIngresosProducto) { this.totalIngresosProducto = totalIngresosProducto; }
}