document.addEventListener("DOMContentLoaded", () => {
    console.log("-> Script de Métricas Dinámicas del Vendedor cargado.");
    
    // 🌟 1. Recuperamos el ID del negocio seleccionado de la pasarela intermedia
    const idNegocio = localStorage.getItem("idNegocioGestionar");

    // Redirigimos si intenta entrar al dashboard a las malas sin seleccionar un local
    if (!idNegocio) {
        alert("Por favor, selecciona primero un establecimiento para gestionar.");
        window.location.href = "misNegocios.html";
        return;
    }

    // Ejecutamos la carga inicial pasándole el ID correspondiente
    cargarMetricasVendedor(idNegocio);
});

async function cargarMetricasVendedor(idNegocio) {
    try {
        // 🌟 2. CLAVE: Modificamos el fetch para inyectar la variable &idNegocio
        const response = await fetch(`../LoginServlet?accion=metricasVendedor&idNegocio=${idNegocio}`);
        
        if (!response.ok) {
            throw new Error("Error al obtener las métricas desde el servidor");
        }

        const datos = await response.json();
        console.log(`Datos de métricas recibidos del negocio #${idNegocio}:`, datos);

        if (datos.error) {
            console.error("Error del sistema:", datos.error);
            return;
        }

        // 📊 1. PINTAR LAS 4 TARJETAS SUPERIORES
        const tarjetas = document.querySelectorAll("#estadisticas .estads");
        if (tarjetas.length >= 4) {
            tarjetas[0].querySelector("p").textContent = datos.vistasTotales.toLocaleString();
            tarjetas[1].querySelector("p").textContent = datos.clicksEnlaces.toLocaleString();
            tarjetas[2].querySelector("p").textContent = datos.totalResenas.toLocaleString();
            tarjetas[3].querySelector("p").textContent = datos.puntuacion.toFixed(1);
        }

        // 📉 2. PINTAR EL GRÁFICO DE BARRAS (Frecuencia de Visitas)
        const contenedorGrafico = document.querySelector(".simulador-grafico");
        if (contenedorGrafico && datos.visitasSemana) {
            contenedorGrafico.innerHTML = ""; 
            
            datos.visitasSemana.forEach(dia => {
                const barra = document.createElement("div");
                barra.className = "barra-css";
                barra.style.setProperty("--alto", `${dia.porcentaje}%`);
                
                const label = document.createElement("span");
                label.textContent = dia.nombreDia;
                
                barra.appendChild(label);
                contenedorGrafico.appendChild(barra);
            });
        }

        // 💬 3. PINTAR LAS RESEÑAS RECIENTES DINÁMICAMENTE
        const contenedorComentarios = document.querySelector(".lista-comentarios-vendedor");
        if (contenedorComentarios && datos.comentariosRecientes) {
            contenedorComentarios.innerHTML = ""; 
            
            if (datos.comentariosRecientes.length === 0) {
                contenedorComentarios.innerHTML = "<p style='color: #8a99a8; font-size: 12px;'>Aún no registras reseñas.</p>";
            } else {
                datos.comentariosRecientes.forEach(comentario => {
                    // Generamos las estrellas doradas basándonos en la calificación real de la base de datos
                    const estrellasStr = "⭐".repeat(comentario.calificacion);
                    
                    const divComentario = document.createElement("div");
                    divComentario.className = "comentario-mini";
                    divComentario.innerHTML = `
                        <div class="meta-comentario">
                            <strong>${escapeHTML(comentario.nombreUsuario)}</strong>
                            <span class="estrellitas">${estrellasStr}</span>
                        </div>
                        <p>"${escapeHTML(comentario.textoComentario)}"</p>
                    `;
                    contenedorComentarios.appendChild(divComentario);
                });
            }
        }

        // ⭐ 4. PINTAR LA DISTRIBUCIÓN DE ESTRELLAS
        const contenedorCalificaciones = document.getElementById("calificaciones");
        if (contenedorCalificaciones && datos.distribucionEstrellas) {
            const barrasProgreso = contenedorCalificaciones.querySelectorAll(".progreso-estrellas");
            
            actualizarBarraEstrellas(barrasProgreso[0], datos.distribucionEstrellas.cinco);
            actualizarBarraEstrellas(barrasProgreso[1], datos.distribucionEstrellas.cuatro);
            actualizarBarraEstrellas(barrasProgreso[2], datos.distribucionEstrellas.tres);
        }

    } catch (error) {
        console.error("Error en el fetch de métricas:", error);
    }
}

function actualizarBarraEstrellas(elementoBarra, porcentaje) {
    if (!elementoBarra) return;
    const barraLlenado = elementoBarra.querySelector(".llenado");
    if (barraLlenado) {
        barraLlenado.style.width = `${porcentaje}%`;
    }
}

function escapeHTML(str) {
    if (!str) return ""; // Evita errores si el nombre de usuario o comentario viene nulo
    return str.replace(/[&<>'"]/g, 
        tag => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' }[tag] || tag)
    );
}