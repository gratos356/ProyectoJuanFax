// 1. Ejecutamos la carga apenas se abra la página (Como lo tenías originalmente)
document.addEventListener("DOMContentLoaded", () => {
    cargarDetallesNegocio();
});

function cargarDetallesNegocio() {
    const params = new URLSearchParams(window.location.search);
    const nombreNegocio = params.get("nombre");

    if (!nombreNegocio) return;

    // Tu fetch original que sí funcionaba
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
            
            if (contenedorImagen) {
                let fotoFinal = negocio.urlImagen.startsWith("http") ? negocio.urlImagen : `../imagenes/${negocio.urlImagen}`;
                contenedorImagen.innerHTML = `<div class="contenedorImagen" style="background-image: url('${fotoFinal}');" alt="${negocio.nombreEstablecimiento}"></div>`;
            }

            // 🌟 INYECTAR EL MAPA AL FINAL (Solo si los datos ya se pintaron con éxito)
            if (negocio.latitud && negocio.longitud) {
                inicializarMapaGoogle(parseFloat(negocio.latitud), parseFloat(negocio.longitud), negocio.nombreEstablecimiento);
            }
        })
        .catch(error => console.error("Error en Juanfax JS:", error));
}

// 2. Función aislada para el mapa (Evita que si Google falla, se caiga el resto de la página)
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
}

// Declaramos la función initMap vacía por si Google la llama por defecto en la URL
function initMap() {}