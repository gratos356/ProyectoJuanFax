let idNegocioActual = null;

// 1. Ejecutamos la carga apenas se abra la página
document.addEventListener("DOMContentLoaded", () => {
    cargarDetallesNegocio();
    configurarFormularioComentario();
});

function cargarDetallesNegocio() {
    const params = new URLSearchParams(window.location.search);
    const nombreNegocio = params.get("nombre");

    if (!nombreNegocio) return;

    fetch(`../LoginServlet?accion=detalleNegocioUnico&nombre=${encodeURIComponent(nombreNegocio)}`)
    .then(response => response.json())
    .then(negocio => {
        if (!negocio || negocio.error) return;
        
        // PINTAR DATOS ORIGINALES
        const txtNombre = document.getElementById("nombreNegocio");
        const txtDescripcion = document.getElementById("descripcionNegocio");
        const contenedorImagen = document.getElementById("imagenNegocio");
        
        if (txtNombre) txtNombre.innerHTML = `<h1>${negocio.nombreEstablecimiento}</h1>`;
        if (txtDescripcion) txtDescripcion.innerHTML = `<p>${negocio.descripcion}</p>`;
        idNegocioActual = parseInt(negocio.idNegocio);
        
        if (contenedorImagen) {
            let fotoFinal = negocio.urlImagen.startsWith("http") ? negocio.urlImagen : `../imagenes/${negocio.urlImagen}`;
            contenedorImagen.innerHTML = `<div class="contenedorImagen" style="background-image: url('${fotoFinal}');" alt="${negocio.nombreEstablecimiento}"></div>`;
        }
        
        // 🌟 INDICADOR AUTOMÁTICO DE VISTA: El negocio cargó con éxito, registramos la vista en la BD
        if (idNegocioActual) {
            registrarMetricaSilenciosa(idNegocioActual, "registrarVista");
            cargarComentarios(idNegocioActual);
        }

        // INYECTAR EL MAPA AL FINAL (Solo si los datos ya se pintaron con éxito)
        if (negocio.latitud && negocio.longitud) {
            inicializarMapaGoogle(parseFloat(negocio.latitud), parseFloat(negocio.longitud), negocio.nombreEstablecimiento);
        }
    })
    .catch(error => console.error("Error en Juanfax JS:", error));
}

// 2. FUNCIÓN PARA TRAER LOS COMENTARIOS DE LA BD
function cargarComentarios(idNegocio) {
    fetch(`../LoginServlet?accion=listarComentarios&idNegocio=${idNegocio}`)
        .then(response => response.json())
        .then(comentarios => {
            const contenedorLista = document.getElementById("listaComentarios");
            if (!contenedorLista) return;

            // Limpiamos el contenedor (borra el comentario de prueba del HTML)
            contenedorLista.innerHTML = "";

            if (comentarios.length === 0) {
                contenedorLista.innerHTML = `<p style="color: #94a3b8; font-style: italic;">Sé el primero en dejar una reseña para este establecimiento.</p>`;
                return;
            }

            // Iteramos y construimos las tarjetas dinámicamente con los datos inyectados
            comentarios.forEach(c => {
                const card = document.createElement("div");
                card.className = "comentarioCard"; // Usa tus estilos CSS
                card.innerHTML = `
                    <p class="usuarioNombre"><strong>${c.nombreUsuario}</strong> <span class="fecha">${c.fecha}</span></p>
                    <p class="comentarioTexto">${c.textoComentario}</p>
                `;
                contenedorLista.appendChild(card);
            });
        })
        .catch(error => console.error("Error al listar comentarios:", error));
}

// 3. FUNCIÓN PARA ENVIAR UN NUEVO COMENTARIO (POST)
function configurarFormularioComentario() {
    const formulario = document.getElementById("formComentario");
    if (!formulario) return;

    formulario.addEventListener("submit", (e) => {
        e.preventDefault(); 

        const cajaTexto = document.getElementById("txtComentario");
        // 🌟 CAPTURAMOS LA ESTRELLA SELECCIONADA
        const estrellaSeleccionada = document.querySelector('input[name="puntuacion"]:checked');
        
        if (!cajaTexto || !estrellaSeleccionada) {
            alert("Por favor, selecciona una puntuación en estrellas.");
            return;
        }
        
        const texto = cajaTexto.value.trim();
        const valorEstrellas = estrellaSeleccionada.value; // Guardará "5", "4" o "3"

        if (!idNegocioActual) {
            alert("Error: No se pudo identificar el negocio.");
            return;
        }

        if (texto === "") {
            alert("El comentario no puede estar vacío.");
            return;
        }

        // Construimos los parámetros para el Servlet
        const datos = new URLSearchParams();
        datos.append("accion", "guardarComentario");
        datos.append("idNegocio", idNegocioActual);
        datos.append("textoComentario", texto); 
        datos.append("valorPuntuacion", valorEstrellas); // 👈 ENVIAMOS LAS ESTRELLAS AL BACKEND

        fetch("../LoginServlet", {
            method: "POST",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded"
            },
            body: datos.toString()
        })
        .then(response => response.json())
        .then(resultado => {
            console.log("Respuesta real del servidor:", resultado);

            if (resultado.status === "success") {
                cajaTexto.value = ""; 
                // Limpiamos la selección de estrellas para el próximo comentario
                estrellaSeleccionada.checked = false; 
                cargarComentarios(idNegocioActual); // Refrescamos la lista
            } else {
                const mensajeError = resultado.message || resultado.error || "Error interno.";
                alert("No se pudo publicar: " + mensajeError);
            }
        })
        .catch(error => {
            console.error("Error en la petición FETCH de Juanfax:", error);
            alert("Error crítico de conexión.");
        });
    });
}

const botonVolver = document.querySelector("#back");
function volverAtras() {
    if (document.referrer && window.history.length > 1) {
        window.history.back();
    } else {
        window.location.href = "mainUser.html"; 
    }
}
botonVolver.addEventListener("click", volverAtras);

// 4. FUNCIÓN AISLADA PARA EL MAPA
function inicializarMapaGoogle(lat, lng, nombre) {
    const contenedorMapa = document.getElementById("showMap");
    if (!contenedorMapa) return;

    const ubicacion = { lat: lat, lng: lng };

    const map = new google.maps.Map(contenedorMapa, {
        zoom: 17,
        center: ubicacion,
        styles: [
            { elementType: "geometry", stylers: [{ color: "#0f172a" }] },
            { elementType: "labels.text.fill", stylers: [{ color: "#94a3b8" }] },
            { featureType: "water", stylers: [{ color: "#020617" }] }
        ]
    });

    new google.maps.Marker({
        position: ubicacion,
        map: map,
        title: nombre
    });

    // 🌟 REGISTRO POR CLIC EN EL MAPA: Escuchamos la interacción con el contenedor del mapa
    contenedorMapa.addEventListener("click", () => {
        // Usamos una bandera en dataset para evitar que clics repetidos saturen la base de datos
        if (!contenedorMapa.dataset.clicRegistrado && idNegocioActual) {
            registrarMetricaSilenciosa(idNegocioActual, "registrarClic");
            contenedorMapa.dataset.clicRegistrado = "true";
        }
    });
}

// 🌟 FUNCIÓN AUXILIAR: ENVÍA LAS INTERACCIONES EN SEGUNDO PLANO AL METRICASSERVLET
async function registrarMetricaSilenciosa(idNegocio, accionMetrica) {
    try {
        const url = `../MetricasServlet?accion=${accionMetrica}&idNegocio=${idNegocio}`;
        const response = await fetch(url, { method: "POST" });
        if (response.ok) {
            console.log(`✅ Métrica '${accionMetrica}' guardada silenciosamente para el negocio #${idNegocio}`);
        }
    } catch (error) {
        console.error("❌ Error de red reportando métrica:", error);
    }
}

// Declaramos la función initMap vacía por si Google la llama por defecto en la URL
function initMap() {}