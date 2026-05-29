let idNegocioActual = null;

// 1. Ejecutamos la carga apenas se abra la página
document.addEventListener("DOMContentLoaded", () => {
    // 1. Extraer el ID de la URL
    const params = new URLSearchParams(window.location.search);
    const idEnUrl = params.get("id"); // O el nombre de tu parámetro
    
    // 2. Asignar a la variable global
    if (idEnUrl) {
        idNegocioActual = idEnUrl;
    }

    // 3. Ahora sí, ejecutar las funciones
    cargarDetallesNegocio();
    configurarFormularioComentario();
});

function cargarDetallesNegocio() {
    const params = new URLSearchParams(window.location.search);
    const idNegocio = params.get("id");

    console.log("-> ID recibido en la página de detalle:", idNegocio); // <--- ESTO ES VITAL

    if (!idNegocio) {
        console.error("No se encontró ID en la URL");
        return;
    }
    if (!idNegocio) return;

    fetch(`../LoginServlet?accion=detalleNegocioUnico&id=${idNegocio}`)
    .then(response => response.json())
    .then(negocio => {
        console.log("Datos recibidos del servidor:", negocio);
        if (!negocio || negocio.error) return;
        
        // --- AQUÍ ESTABA EL ERROR: Necesitas asignar el ID del negocio recibido ---
        // Asegúrate de que 'negocio.idNegocio' sea el nombre correcto que viene de tu Java
        idNegocioActual = negocio.idNegocio; 
        
        // PINTAR DATOS ORIGINALES
        const txtNombre = document.getElementById("nombreNegocio");
        const txtDescripcion = document.getElementById("descripcionNegocio");
        const contenedorImagen = document.getElementById("imagenNegocio");
        
        // Validar campos para evitar errores si vienen nulos
        if (txtNombre) txtNombre.innerText = negocio.nombreEstablecimiento || "Sin nombre";
        if (txtDescripcion) txtDescripcion.innerText = negocio.descripcion || "Sin descripción";
        
        let rutaImagen = negocio.url_imagen || negocio.urlImagen;
        let fotoFinal = '../imagenes/default-negocio.jpg';

        if (rutaImagen) {
            fotoFinal = rutaImagen.startsWith("http") 
                ? rutaImagen 
                : `../verImagen?nombre=${rutaImagen}`;
        }

        if (contenedorImagen) {
            contenedorImagen.innerHTML = `<div class="contenedorImagen" style="background-image: url('${fotoFinal}');" alt="${negocio.nombreEstablecimiento}"></div>`;
        }
        
        // AHORA SÍ: Como idNegocioActual ya tiene valor, esto funcionará:
        if (idNegocioActual) {
            registrarMetricaSilenciosa(idNegocioActual, "registrarVista");
            cargarComentarios(idNegocioActual);
        }

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
                // 🌟 LOGICA VISUAL PARA LAS ESTRELLAS
                const estrellasMaximas = 5;
                let estrellasHTML = "";
                for (let i = 1; i <= estrellasMaximas; i++) {
                    if (i <= c.calificacion) {
                        estrellasHTML += '<span style="color: #ffca28; font-size: 1.1rem; margin-right: 2px;">★</span>'; // Estrella llena
                    } else {
                        estrellasHTML += '<span style="color: #cbd5e1; font-size: 1.1rem; margin-right: 2px;">☆</span>'; // Estrella vacía
                    }
                }

                const card = document.createElement("div");
                card.className = "comentarioCard"; // Usa tus estilos CSS
                card.innerHTML = `
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px;">
                        <p class="usuarioNombre" style="margin: 0;"><strong>${c.nombreUsuario}</strong> <span class="fecha" style="font-size: 0.85rem; color: #94a3b8; margin-left: 8px;">${c.fecha}</span></p>
                        <div class="estrellasContenedor">${estrellasHTML}</div>
                    </div>
                    <p class="comentarioTexto" style="margin: 0; color: #f8fafc;">${c.textoComentario}</p>
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
        
        // Capturamos cuál radio button está seleccionado (:checked)
        const estrellaSeleccionada = document.querySelector('input[name="puntuacion"]:checked');
        
        // Validación: Si no ha seleccionado ninguna estrella, detenemos el envío
        if (!estrellaSeleccionada) {
            alert("Por favor, selecciona una calificación en estrellas antes de publicar tu comentario.");
            return;
        }

        const texto = cajaTexto.value.trim();
        if (texto === "") {
            alert("El comentario no puede estar vacío.");
            return;
        }

        // Preparación de los parámetros para el Servlet
        const datos = new URLSearchParams();
        datos.append("accion", "guardarComentario");
        datos.append("idNegocio", idNegocioActual);
        datos.append("textoComentario", texto);
        
        // 🌟 CLAVE: Asegúrate de enviarlo con el nombre exacto que lee el Servlet: "valorPuntuacion"
        datos.append("valorPuntuacion", estrellaSeleccionada.value); 

        fetch("../LoginServlet", {
            method: "POST",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded"
            },
            body: datos.toString()
        })
        .then(response => response.json())
        .then(resultado => {
            if (resultado.status === "success") {
                cajaTexto.value = ""; // Limpiar texto
                estrellaSeleccionada.checked = false; // Desmarcar estrellas para el próximo uso
                
                cargarComentarios(idNegocioActual); // Recargar lista visual
                alert("¡Comentario y puntuación publicados con éxito!");
            } else {
                alert("Error al guardar: " + resultado.message);
            }
        })
        .catch(error => console.error("Error en la petición:", error));
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