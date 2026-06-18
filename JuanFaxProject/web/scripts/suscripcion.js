// 🌟 CAPTURA DINÁMICA: Lee el ID real del negocio seleccionado por el usuario
const ID_NEGOCIO_ACTUAL = localStorage.getItem("idNegocioGestionar");

// Ejecutar automáticamente cuando cargue la página
document.addEventListener("DOMContentLoaded", () => {
    if (!ID_NEGOCIO_ACTUAL) {
        console.error("🚨 Error: No se encontró 'idNegocioGestionar' en el localStorage.");
        alert("Por favor, selecciona un negocio desde el panel antes de gestionar la suscripción.");
        return;
    }
    
    // Convertimos a entero de forma segura desde la inicialización
    cargarDatosSuscripcion(parseInt(ID_NEGOCIO_ACTUAL));
});

// 1. Función para cargar los datos de suscripción al abrir la sección
function cargarDatosSuscripcion(idNegocio) {
    fetch(`../LoginServlet?accion=obtenerDatosSuscripcion&idNegocio=${idNegocio}`)
    .then(response => response.json())
    .then(data => {
        if (!data.success) {
            console.error("Error del servidor:", data.mensaje);
            return;
        }

        document.getElementById("nombrePlan").innerText = data.tipoPlan;
        
        const estadoElem = document.getElementById("estadoPlan");
        const estadoTexto = data.estado ? data.estado.toUpperCase().trim() : "";
        estadoElem.innerText = estadoTexto;
        
        // 🎨 RENDERIZADO VISUAL COMPLETO SEGÚN EL ENUM DE LA BASE DE DATOS
        if (estadoTexto === 'ACTIVO') {
            estadoElem.style.color = '#22c55e'; // Verde Esmeralda (Éxito)
        } else if (estadoTexto === 'SUSPENDIDO' || estadoTexto === 'INACTIVO' || estadoTexto === 'RECHAZADO') {
            estadoElem.style.color = '#ef4444'; // Rojo (Bloqueos o Revocaciones)
        } else {
            estadoElem.style.color = '#eab308'; // Dorado/Amarillo (Trial, Pendiente)
        }
        
        document.getElementById("fechaVencimiento").innerText = data.fechaFin;
    })
    .catch(error => console.error("Error al obtener suscripción:", error));
}

// 2. Función para renovar manualmente
function gestionarPlan() {
    if (!confirm("¿Deseas activar/renovar tu suscripción de Juanfax?")) return;

    fetch(`../LoginServlet?accion=renovarSuscripcion&idNegocio=${ID_NEGOCIO_ACTUAL}`, {
        method: 'POST'
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            alert(data.mensaje);
            // 🎯 CORRECCIÓN: Volvemos a parsear el ID para mantener consistencia numérica
            cargarDatosSuscripcion(parseInt(ID_NEGOCIO_ACTUAL)); 
        } else {
            alert("Hubo un error: " + data.mensaje);
        }
    })
    .catch(error => {
        console.error("Error en la petición de renovación:", error);
        alert("No se pudo conectar con el servidor.");
    });
}