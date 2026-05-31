
window.idNegocioActual = null;


// ====================================================================
// 🏁 EVENTO DE INICIALIZACIÓN (Al cargar el DOM de la vista)
// ====================================================================
document.addEventListener("DOMContentLoaded", () => {
    // 1. Buscamos de forma inmediata qué negocios pertenecen al vendedor logueado
    obtenerNegocioInicial();
    
    // 2. Vinculamos los eventos a los botones del menú lateral o pestañas
    configurarListenersMenu();
});



function obtenerNegocioInicial() {
    // 1️⃣ PASO CLAVE: Intentar recuperar el ID con el que el usuario viene desde 'misNegocios.html'
    const idDesdeStorage = localStorage.getItem('idNegocioGestionar');
    
    if (idDesdeStorage) {
        window.idNegocioActual = parseInt(idDesdeStorage);
        console.log("📌 Inicializado con éxito desde LocalStorage: ID -> " + window.idNegocioActual);
    }

    // 2️⃣ Hacer el fetch para cargar la lista del selector (si aplica), pero protegiendo tu elección
    fetch('../LoginServlet?accion=listarNegociosPorVendedor')
    .then(res => res.json())
    .then(negocios => {
        
        // 🛑 REGLA DE ORO: Si ya tenemos el ID del LocalStorage (el 71), NO dejamos que el fetch lo pise con el 6
        if (window.idNegocioActual) {
            console.log("⏳ El fetch de negocios terminó, pero se respeta el ID " + window.idNegocioActual + " para no sobrescribirlo.");
            
            // Si tienes un elemento <select> en el HTML, lo sincronizamos visualmente
            const selector = document.getElementById("selectNegocios"); // Cambia por el ID real de tu select
            if (selector) {
                selector.value = window.idNegocioActual;
            }
            return; // Frenamos la ejecución aquí para blindar el 71
        }

        // 3️⃣ Plan de contingencia: Si por alguna razón el LocalStorage estaba vacío, ahí sí usamos el primero
        if (negocios && negocios.length > 0) {
            const negocioValido = negocios.find(n => n.estado !== 'BLOQUEADO');
            if (negocioValido) {
                window.idNegocioActual = negocioValido.idNegocio;
                console.log("✅ Inicialización por descarte (sin LocalStorage): ID -> " + window.idNegocioActual);
            }
        }
    })
    .catch(err => console.error("Error al listar negocios del vendedor:", err));
}

function cargarVistaSuscripcion() {
    const contenedorPrincipal = document.getElementById("informacionNegocio");
    
    if (!contenedorPrincipal) return;

    // Validación de seguridad para que la aplicación no intente enviar datos nulos
    if (!window.idNegocioActual) {
        console.error("🚨 Error de control: No hay un ID de negocio asignado en la variable global.");
        contenedorPrincipal.innerHTML = `<p class="error">Por favor, espera a que cargue el sistema o selecciona un establecimiento.</p>`;
        return;
    }
    
    const idNegocio = window.idNegocioActual;
    
    // Mostramos un estado de carga visual en la interfaz limpia
    contenedorPrincipal.innerHTML = `<p class="cargando">Cargando información del plan asignado...</p>`;
    
    // 🌟 ENVIÓ DINÁMICO: Mandamos obligatoriamente el parámetro '&idNegocio=' a Tomcat
    fetch(`../LoginServlet?accion=obtenerDatosSuscripcion&idNegocio=${idNegocio}`)
    .then(res => {
        if (!res.ok) throw new Error("Error en la conexión o respuesta del Servlet.");
        return res.json();
    })
    .then(data => {
        // En caso de que el DAO devuelva un estado controlado de error
        if (data.success === false) {
            contenedorPrincipal.innerHTML = `<p class="error">${data.mensaje}</p>`;
            return;
        }

        // 3. Renderizar la tarjeta de suscripción utilizando los campos inyectados de MySQL
        contenedorPrincipal.innerHTML = `
            <div class="panel-suscripcion">
                <div class="header-suscripcion">
                    <h3>Gestión de Suscripción</h3>
                </div>
                <div class="detalles-plan">
                    <p><strong>Plan Actual:</strong> ${data.tipoPlan || 'No definido'}</p>
                    <p><strong>Estado:</strong> <span class="badge-${data.estado ? data.estado.toLowerCase() : 'inactivo'}">${data.estado || 'INACTIVO'}</span></p>
                    <p><strong>Vence el:</strong> ${data.fechaFin || 'N/A'}</p>
                    <p><strong>Días Restantes:</strong> ${data.diasRestantes ?? 0} días</p>
                </div>
                <div class="acciones-suscripcion">
                    <button class="btn-renovar" onclick="procesarPago(${idNegocio})">Renovar Plan</button>
                </div>
            </div>
        `;
    })
    .catch(error => {
        console.error("❌ Error crítico consumiendo obtenerDatosSuscripcion:", error);
        contenedorPrincipal.innerHTML = `<p class="error">No pudimos procesar la carga de tu suscripción. Intenta de nuevo más tarde.</p>`;
    });
}




function procesarPago(idNegocio) {
    const contenedorPrincipal = document.getElementById("informacionNegocio");
    if (!contenedorPrincipal) return;

    console.log("🎯 Abriendo opciones de renovación (Mensual/Anual) para el negocio ID: " + idNegocio);

    contenedorPrincipal.innerHTML = `
        <div class="contenedor-planes">
            <div class="planes-header">
                <h2>Renueva la suscripción de tu negocio</h2>
                <p>Selecciona el periodo que mejor se adapte a tus necesidades de facturación.</p>
            </div>
            
            <div class="grid-planes-dual">
                <div class="tarjeta-plan">
                    <div class="plan-badge">Control total</div>
                    <h3>Plan Mensual</h3>
                    <div class="plan-precio">$45.000 <span>/ mes</span></div>
                    <ul class="plan-caracteristicas">
                        <li>✅ Acceso completo a Juanfax</li>
                        <li>✅ Facturación electrónica ilimitada</li>
                        <li>✅ Reportes y métricas del mes</li>
                        <li>✅ Soporte técnico incluido</li>
                        <li>⏳ Renovación cada 30 días</li>
                    </ul>
                    <button class="btn-seleccionar-plan" onclick="confirmarPlan(${idNegocio}, 'MENSUAL', 45000)">
                        Contratar Mes
                    </button>
                </div>

                <div class="tarjeta-plan plan-destacado">
                    <div class="plan-badge destacado">¡Ahorra 2 meses!</div>
                    <h3>Plan Anual</h3>
                    <div class="plan-precio">$450.000 <span>/ año</span></div>
                    <ul class="plan-caracteristicas">
                        <li>✅ Acceso completo a Juanfax por 365 días</li>
                        <li>✅ Facturación electrónica ilimitada</li>
                        <li>✅ Reportes e históricos anuales</li>
                        <li>✅ Soporte técnico prioritario</li>
                        <li>🔥 Descuento especial aplicado ($90.000 de ahorro)</li>
                    </ul>
                    <button class="btn-seleccionar-plan btn-pro" onclick="confirmarPlan(${idNegocio}, 'ANUAL', 450000)">
                        Contratar Año
                    </button>
                </div>
            </div>

            <div class="planes-footer">
                <button class="btn-regresar" onclick="cargarVistaSuscripcion()">⬅ Volver a Suscripción</button>
            </div>
        </div>
    `;
}

/**
 * Procesa la confirmación y envía el tipo de plan ('MENSUAL' o 'ANUAL') al Backend
 */
function confirmarPlan(idNegocio, tipoPlan, precio) {
    console.log(`🛒 Solicitud de renovación: Tipo -> ${tipoPlan} | Valor -> $${precio} | Negocio ID -> ${idNegocio}`);
    
    const formatoPrecio = precio.toLocaleString('es-CO');
    const confirmar = confirm(`¿Confirmas la renovación del Plan ${tipoPlan} por un valor de $${formatoPrecio} COP?`);
    
    if (confirmar) {
        fetch(`../LoginServlet?accion=renovarSuscripcion&idNegocio=${idNegocio}&tipoPlan=${tipoPlan}`, {
            method: 'POST'
        })
        .then(res => {
            if (!res.ok) throw new Error("Error en la respuesta del servidor al procesar la renovación.");
            return res.json();
        })
        .then(respuesta => {
            if (respuesta.success) {
                alert("✨ ¡Suscripción renovada con éxito!");
                cargarVistaSuscripcion(); // Refresca el panel de control con las nuevas fechas
            } else {
                alert("⚠️ No se pudo procesar: " + respuesta.mensaje);
            }
        })
        .catch(error => {
            console.error("❌ Error en la petición de renovación:", error);
            alert("Hubo un problema de conexión con el servidor.");
        });
    }
}


function configurarListenersMenu() {
    const btnSuscripcion = document.getElementById("btnSuscripcion");
    
    if (btnSuscripcion) {
        btnSuscripcion.addEventListener("click", cargarVistaSuscripcion);
    }
    

}