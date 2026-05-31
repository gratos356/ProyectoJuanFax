document.addEventListener("DOMContentLoaded", () => {
    const idNegocio = localStorage.getItem("idNegocioEditar");
    if (!idNegocio) {
        alert("No se seleccionó ningún negocio para editar.");
        window.location.href = "misNegocios.html";
        return;
    }

    // 1. Cargar la información actual del negocio en el formulario
    fetch(`../LoginServlet?accion=obtenerNegocioPorId&id=${idNegocio}`)
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                document.getElementById("idNegocio").value = data.id;
                document.getElementById("nit").value = data.nit;
                document.getElementById("nombre").value = data.nombre;
                document.getElementById("descripcion").value = data.descripcion;
                document.getElementById("idCategoria").value = data.idCategoria;
            } else {
                alert("Error: " + data.message);
            }
        });
});

// 2. Enviar los datos actualizados mediante POST
document.getElementById("formEditarNegocio").addEventListener("submit", (e) => {
    e.preventDefault();

    const formData = new FormData(document.getElementById("formEditarNegocio"));
    // Convertimos el FormData a parámetros legibles de URL para que el Servlet los procese directo
    const params = new URLSearchParams(formData).toString();

    fetch("../LoginServlet?accion=actualizarNegocio", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: params
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            alert("🙌 " + data.message);
            window.location.href = "misNegocios.html"; // Regresa al panel de tarjetas
        } else {
            alert("❌ Falló la actualización: " + data.message);
        }
    })
    .catch(err => console.error("Error al actualizar:", err));
});