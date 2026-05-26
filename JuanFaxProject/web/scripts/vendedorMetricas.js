document.addEventListener("DOMContentLoaded", () => {
    console.log("-> Script de Métricas Dinámicas del Vendedor cargado.");
    // Ejecutamos la carga inicial de datos
    cargarMetricasVendedor();
});

async function cargarMetricasVendedor() {
    try {
        // Hacemos la petición asíncrona enviando la acción correspondiente
        // Nota: El Servlet identificará al usuario mediante la sesión activa (HttpSession)
        const response = await fetch("../LoginServlet?accion=metricasVendedor");
        
        if (!response.ok) {
            throw new Error("Error al obtener las métricas desde el servidor");
        }

        const datos = await response.json();
        console.log("Datos de métricas recibidos:", datos);

        if (datos.error) {
            console.error("Error del sistema:", datos.error);
            return;
        }

        // 📊 1. PINTAR LAS 4 TARJETAS SUPERIORES
        // Buscamos los contenedores internos dentro del ID 'estadisticas'
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
            contenedorGrafico.innerHTML = ""; // Limpiamos las barras estáticas
            
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
            contenedorComentarios.innerHTML = ""; // Limpiamos los estáticos
            
            if (datos.comentariosRecientes.length === 0) {
                contenedorComentarios.innerHTML = "<p style='color: #8a99a8; font-size: 12px;'>Aún no registras reseñas.</p>";
            } else {
                datos.comentariosRecientes.forEach(comentario => {
                    // Generamos las estrellas doradas en base a la calificación
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
            // Buscamos las barras de progreso existentes para actualizarlas
            const barrasProgreso = contenedorCalificaciones.querySelectorAll(".progreso-estrellas");
            
            // Mapeamos los datos de 5, 4 y 3 estrellas
            actualizarBarraEstrellas(barrasProgreso[0], datos.distribucionEstrellas.cinco);
            actualizarBarraEstrellas(barrasProgreso[1], datos.distribucionEstrellas.cuatro);
            actualizarBarraEstrellas(barrasProgreso[2], datos.distribucionEstrellas.tres);
        }

    } catch (error) {
        console.error("Error en el fetch de métricas:", error);
    }
}

// Función auxiliar para actualizar los progress bars de las estrellas de forma limpia
function actualizarBarraEstrellas(elementoBarra, porcentaje) {
    if (!elementoBarra) return;
    const barraLlenado = elementoBarra.querySelector(".llenado");
    if (barraLlenado) {
        barraLlenado.style.width = `${porcentaje}%`;
    }
}

// Función para prevenir inyecciones XSS al renderizar textos de usuarios en el DOM
function escapeHTML(str) {
    return str.replace(/[&<>'"]/g, 
        tag => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' }[tag] || tag)
    );
}