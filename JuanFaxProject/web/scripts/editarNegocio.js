document.addEventListener("DOMContentLoaded", () => {
    const idNegocio = localStorage.getItem("idNegocioEditar");
    if (!idNegocio) {
        alert("No se seleccionó ningún negocio para editar.");
        window.location.href = "misNegocios.html";
        return;
    }

    // 1. Cargar la información actual del negocio en el formulario al iniciar
    fetch(`../LoginServlet?accion=obtenerNegocioPorId&id=${idNegocio}`)
        .then(response => {
            if (!response.ok) throw new Error("Error al obtener datos comerciales.");
            return response.json();
        })
        .then(data => {
            if (data.success) {
                document.getElementById("idNegocio").value = data.id;
                
                // Asignar el valor y bloquear el campo NIT para que no sea editable
                const nitInput = document.getElementById("nit");
                nitInput.value = data.nit;
                nitInput.readOnly = true; // 👈 Bloquea la edición pero permite el envío de datos
                
                document.getElementById("nombre").value = data.nombre;
                document.getElementById("descripcion").value = data.descripcion;
                document.getElementById("idCategoria").value = data.idCategoria;
            } else {
                alert("Error del sistema: " + data.message);
                window.location.href = "misNegocios.html";
            }
        })
        .catch(err => {
            console.error("Error de precarga de negocio:", err);
            alert("No se pudo conectar con el servidor para leer la información del local.");
        });
});

// 2. Enviar los datos actualizados mediante POST con validaciones previas
document.getElementById("formEditarNegocio").addEventListener("submit", (e) => {
    e.preventDefault();

    const nit = document.getElementById("nit").value.trim();
    const nombre = document.getElementById("nombre").value.trim();
    const descripcion = document.getElementById("descripcion").value.trim();
    const idCategoria = document.getElementById("idCategoria").value;

    // --- REGLAS DE VALIDACIÓN EN EL CLIENTE ---
    const regexNIT = /^[0-9.\-]+$/; // Permite números, puntos y guiones del formato NIT colombiano
    if (!regexNIT.test(nit)) {
        alert("⚠️ El formato del NIT no es válido. Ingresa solo números o la estructura estándar (ej. 900.123.456-7).");
        return;
    }

    if (nombre.length < 3 || nombre.length > 100) {
        alert("⚠️ El Nombre Comercial debe contar con una longitud de entre 3 y 100 caracteres.");
        return;
    }

    if (descripcion.length > 500) {
        alert("⚠️ La descripción comercial simplificada no puede exceder los 500 caracteres.");
        return;
    }

    if (!idCategoria) {
        alert("⚠️ Selecciona una categoría válida para el establecimiento.");
        return;
    }

    // --- PROCEDER CON EL ENVÍO SI TODO ESTÁ CORRECTO ---
    const formData = new FormData(document.getElementById("formEditarNegocio"));
    // Convertimos el FormData a parámetros legibles de URL para el procesamiento directo en el Servlet
    const params = new URLSearchParams(formData).toString();

    fetch("../LoginServlet?accion=actualizarNegocio", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: params
    })
    .then(response => {
        if (!response.ok) throw new Error("Fallo en la comunicación post.");
        return response.json();
    })
    .then(data => {
        if (data.success) {
            alert("🙌 " + data.message);
            window.location.href = "misNegocios.html"; // Regresa con éxito al panel de tarjetas principales
        } else {
            alert("❌ Falló la actualización: " + data.message);
        }
    })
    .catch(err => {
        console.error("Error al actualizar el establecimiento:", err);
        alert("Hubo un problema de red y no se guardaron los cambios.");
    });
});