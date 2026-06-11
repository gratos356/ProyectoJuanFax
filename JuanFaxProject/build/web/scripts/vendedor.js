
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
        // 🌟 CORRECCIÓN 1: Agregamos la estructura de la tabla abajo del panel
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

            <div class="historial-pagos-contenedor" style="margin-top: 25px;">
                <h3>Historial de Transacciones</h3>
                <table id="tablaHistorialPagos" class="tabla-dinamica">
                    <thead>
                        <tr>
                            <th>Fecha de Pago</th>
                            <th>ID Transacción</th>
                            <th>Monto</th>
                            <th>Estado</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr><td colspan="4" class="cargando">Buscando transacciones...</td></tr>
                    </tbody>
                </table>
            </div>
        `;

        // 🌟 CORRECCIÓN 2: Como la tabla ya se dibujó en el DOM, mandamos a llamarla
        if (typeof cargarHistorialPagos === "function") {
            cargarHistorialPagos(idNegocio);
        } else {
            console.error("🚨 Error: La función cargarHistorialPagos() no está accesible en este ámbito.");
        }
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
function confirmarPlan(idNegocio, tipoPlanSeleccionado, montoPlan) {
    // 1. Simular una confirmación o pasarela de pago
    const confirmar = confirm(`¿Deseas proceder con el pago del Plan ${tipoPlanSeleccionado} por valor de $${montoPlan.toLocaleString('es-CO')} COP?`);
    
    if (!confirmar) return; // Si el usuario cancela, frena la ejecución

    // 2. Generamos un código de transacción único para la auditoría de caja de Juanfax
    const idTransaccionSimulado = "TRX-" + Math.floor(Math.random() * 10000000);

    console.log(`🚀 Enviando pago transaccional... Plan: ${tipoPlanSeleccionado}, Monto: ${montoPlan}, Token: ${idTransaccionSimulado}`);

    // 3. Armamos los parámetros estructurados en formato URL-Encoded
    const params = new URLSearchParams();
    params.append("accion", "renovarSuscripcion");
    params.append("idNegocio", idNegocio);
    params.append("tipoPlan", tipoPlanSeleccionado); 
    params.append("monto", montoPlan);
    params.append("idTransaccion", idTransaccionSimulado);

    // 4. Realizamos la petición HTTP POST hacia el Servidor
    fetch("../LoginServlet", {
        method: "POST",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: params.toString()
    })
    .then(response => {
        if (!response.ok) {
            throw new Error("Error en la respuesta del servidor");
        }
        return response.json();
    })
    .then(data => {
        if (data.success) {
            alert("✅ " + data.mensaje);
            
            // 🔄 5. Retornar al panel principal y recargar componentes actualizados de inmediato
            if (typeof cargarVistaSuscripcion === "function") {
                cargarVistaSuscripcion(); 
            } else {
                location.reload(); // Fallback por si la vista requiere recarga completa
            }
        } else {
            alert("❌ Hubo un inconveniente: " + data.mensaje);
        }
    })
    .catch(error => {
        console.error("🚨 Error en la petición asíncrona de pago:", error);
        alert("No se pudo conectar con el servidor de cobros. Intenta de nuevo.");
    });
}


function configurarListenersMenu() {
    const informacionNegocio = document.getElementById("informacionNegocio"); 
    const vistaReporteVentas = document.getElementById("vistaReporteVentas"); 

    // 🌟 TRUCO MAESTRO: Guardamos en una variable global de ventana el HTML original de las métricas
    // Solo lo guardamos la primera vez (cuando aún existe en el HTML) antes de que la suscripción lo borre.
    if (informacionNegocio && !window.htmlMetricasOriginal) {
        window.htmlMetricasOriginal = informacionNegocio.innerHTML;
        console.log("💾 Estructura HTML de Métricas respaldada con éxito en caché.");
    }

    /**
     * 🌟 FUNCIÓN CENTRAL DE ENRUTAMIENTO (SPA)
     */
    function conmutarSecciones(seccionActiva) {
        if (seccionActiva === "metricas") {
            if (informacionNegocio) informacionNegocio.style.display = ""; 
            if (vistaReporteVentas) vistaReporteVentas.classList.add("vistaReporteVentasOculta");
        } 
        else if (seccionActiva === "reportes") {
            if (informacionNegocio) informacionNegocio.style.display = "none";
            if (vistaReporteVentas) vistaReporteVentas.classList.remove("vistaReporteVentasOculta");
        } 
        else if (seccionActiva === "suscripcion") {
            if (informacionNegocio) informacionNegocio.style.display = ""; 
            if (vistaReporteVentas) vistaReporteVentas.classList.add("vistaReporteVentasOculta");
        }
    }

    // 2. LISTENERS DE LA BARRA LATERAL (SIDEBAR)

    // 🏠 Botón: Métricas
    const btnVerMetricas = document.getElementById("btnVerMetricas");
    if (btnVerMetricas) {
        btnVerMetricas.addEventListener("click", () => {
            console.log("🏠 Volviendo al Dashboard principal y restaurando maquetación de métricas...");
            
            // 1. Cambiamos los estados visuales de las secciones
            conmutarSecciones("metricas");
            
            // 2. 🌟 RESTAURACIÓN: Reinyectamos las cajas, estadísticas y el simulador gráfico original
            if (informacionNegocio && window.htmlMetricasOriginal) {
                informacionNegocio.innerHTML = window.htmlMetricasOriginal;
            }
            
            // 3. Si tienes un Fetch en 'vendedorMetricas.js' para traer datos reales de la BD, ejecútalo aquí:
            if (typeof cargarMetricasVendedor === "function") {
                cargarMetricasVendedor(window.idNegocioActual);
            } else if (typeof inicializarMetricas === "function") {
                inicializarMetricas();
            }
        });
    }

    // 📊 Botón: Historial Ventas
    const btnRepoteVentas = document.getElementById("btnVerReporteVentas");
    if (btnRepoteVentas) {
        btnRepoteVentas.addEventListener("click", () => {
            if (window.idNegocioActual) {
                console.log("📊 Abriendo el historial de ventas...");
                conmutarSecciones("reportes");
                cargarResumenVentasVendedor(window.idNegocioActual); 
            } else {
                alert("⚠️ No se ha detectado ningún establecimiento seleccionado.");
            }
        });
    }

    // 💳 Botón: Suscripción
    const btnSuscripcion = document.getElementById("btnSuscripcion");
    if (btnSuscripcion) {
        btnSuscripcion.addEventListener("click", () => {
            console.log("💳 Abriendo gestión de suscripción...");
            conmutarSecciones("suscripcion");
            if (typeof cargarVistaSuscripcion === "function") {
                cargarVistaSuscripcion();
            }
        });
    }

    // 📦 Botón: Productos
    const btnProductos = document.getElementById("btnProductos");
    if (btnProductos) {
        btnProductos.addEventListener("click", () => {
            if (!window.idNegocioActual) {
                alert("No se ha detectado ningún establecimiento seleccionado para gestionar.");
                return;
            }
            window.location.href = `gestionProductos.html?idNegocio=${window.idNegocioActual}`;
        });
    }

    // Clic en el Título Principal JUANFAX
    const tituloHeader = document.getElementById("titulo");
    if (tituloHeader) {
        tituloHeader.style.cursor = "pointer";
        tituloHeader.addEventListener("click", () => {
            window.location.href = "misNegocios.html";
        });
    }
}

// ============================================================================
// LOGICA DE INTEGRACIÓN DE MI PERFIL (VENDEDOR)
// ============================================================================

// Escucha del avatar de perfil existente en mainVendedor.html
const profileAvatar = document.getElementById("profile");
if (profileAvatar) {
    profileAvatar.style.cursor = "pointer"; // Hacemos que se note que es cliqueable
    profileAvatar.addEventListener("click", abrirModalPerfilVendedor);
}

// Configurar los botones internos del modal clonado para vendedor
// Usamos selectores condicionales para evitar colisiones
const cerrarMdl = document.getElementById("btnCerrarModal");
if (cerrarMdl) cerrarMdl.addEventListener("click", () => document.getElementById("modalPerfil").style.display = "none");

const logoutBtn = document.getElementById("btnLogOut");
if (logoutBtn) logoutBtn.addEventListener("click", () => window.location.href = "../LoginServlet?accion=cerrarSesion");

const formPerf = document.getElementById("formMiPerfil");
if (formPerf) formPerf.addEventListener("submit", ejecutarActualizacionPerfilVendedor);

const bajaBtn = document.getElementById("btnBorradoLogico");
if (bajaBtn) bajaBtn.addEventListener("click", ejecutarBorradoLogicoCuentaVendedor);

async function abrirModalPerfilVendedor() {
    try {
        const res = await fetch("../LoginServlet?accion=verPerfil");
        const user = await res.json();
        
        if (user.error) {
            alert(user.error);
            window.location.href = "../index.html";
            return;
        }

        document.getElementById("perfilNombre").value = user.nombre;
        document.getElementById("perfilCorreo").value = user.correo;
        document.getElementById("perfilRol").innerText = user.rol;
        document.getElementById("perfilEstado").innerText = user.estado;

        document.getElementById("modalPerfil").style.display = "flex";
    } catch (err) {
        console.error("Error:", err);
        alert("Ocurrió un inconveniente al cargar tu perfil.");
    }
}

async function ejecutarActualizacionPerfilVendedor(e) {
    e.preventDefault();
    const nombre = document.getElementById("perfilNombre").value.trim();
    const correo = document.getElementById("perfilCorreo").value.trim();

    const params = new URLSearchParams();
    params.append("accion", "actualizarPerfil");
    params.append("nombre", nombre);
    params.append("correo", correo);

    try {
        const res = await fetch("../LoginServlet", {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: params
        });
        const data = await res.json();
        
        alert(data.mensaje);
        if (data.success) {
            document.getElementById("modalPerfil").style.display = "none";
            // Cambiar de forma dinámica el texto del avatar de iniciales si cambia de nombre
            if(document.querySelector("#profile p")) {
                const iniciales = nombre.split(" ").map(n => n[0]).join("").substring(0,2).toUpperCase();
                document.querySelector("#profile p").innerText = iniciales;
            }
        }
    } catch (err) {
        alert("Error al procesar la actualización.");
    }
}

function ejecutarBorradoLogicoCuentaVendedor() {
    const seguro = confirm("⚠️ ¿Confirmas la desactivación completa de tu cuenta de vendedor? Tus negocios y catálogos quedarán ocultos temporalmente.");
    if (!seguro) return;

    const params = new URLSearchParams();
    params.append("accion", "eliminarMiCuenta");

    fetch("../LoginServlet", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: params
    })
    .then(res => res.json())
    .then(data => {
        alert(data.mensaje);
        if (data.success) {
            window.location.href = "../index.html";
        }
    })
    .catch(() => alert("No se pudo tramitar la baja del perfil."));
}

// 🌟 FUNCIÓN MUDADA A VENDEDOR.JS PARA EVITAR ERRORES DE ÁMBITO
function cargarHistorialPagos(idNegocio) {
    fetch(`../LoginServlet?accion=historialPagos&idNegocio=${idNegocio}`)
    .then(response => response.json())
    .then(data => {
        const tablaBody = document.querySelector("#tablaHistorialPagos tbody");
        if (!tablaBody) return; // Validación de seguridad por si cambias de pestaña rápido
        
        tablaBody.innerHTML = ""; 

        if (data.length === 0) {
            tablaBody.innerHTML = "<tr><td colspan='4' style='text-align:center;'>No hay transacciones registradas.</td></tr>";
            return;
        }

        // Recorremos los pagos enviados por el Servlet e inyectamos las filas
        data.forEach(pago => {
            const fila = `
                <tr>
                    <td>${pago.fecha}</td>
                    <td><code>${pago.transaccion}</code></td>
                    <td>$${pago.monto.toLocaleString()} COP</td>
                    <td><span class="badge">${pago.estado}</span></td>
                </tr>
            `;
            tablaBody.insertAdjacentHTML("beforeend", fila);
        });
    })
    .catch(error => {
        console.error("❌ Error cargando el historial de pagos:", error);
        const tablaBody = document.querySelector("#tablaHistorialPagos tbody");
        if (tablaBody) {
            tablaBody.innerHTML = "<tr><td colspan='4' class='error'>No se pudo cargar el historial.</td></tr>";
        }
    });
}


function cargarResumenVentasVendedor(idNegocio) {
    const contenedorTabla = document.getElementById("tablaResumenVentas");
    const contenedorGranTotal = document.getElementById("granTotalVentas");
    
    if (!contenedorTabla || !idNegocio) return;

    fetch(`../PedidoServlet?accion=resumenVentasNegocio&idNegocio=${idNegocio}`)
    .then(response => {
        if (!response.ok) throw new Error("Fallo en la comunicación con el servidor");
        return response.json();
    })
    .then(ventas => {
        if (ventas.length === 0) {
            contenedorTabla.innerHTML = `
                <tr>
                    <td colspan="4" class="fila-vacia-mensaje">
                        📦 Este establecimiento no registra ventas procesadas todavía.
                    </td>
                </tr>`;
            if (contenedorGranTotal) contenedorGranTotal.innerText = "$0 COP";
            return;
        }

        let granTotalAcumulado = 0;

        contenedorTabla.innerHTML = ventas.map(v => {
            granTotalAcumulado += v.totalIngresosProducto;
            
            return `
                <tr>
                    <td class="col-id">#${v.idProducto}</td>
                    <td class="col-producto">${v.nombreProducto}</td>
                    <td style="text-align: center;">
                        <span class="badge-unidades">${v.totalUnidadesVendidas}</span>
                    </td>
                    <td class="col-ingresos">
                        $${v.totalIngresosProducto.toLocaleString('es-CO', { minimumFractionDigits: 0 })} COP
                    </td>
                </tr>
            `;
        }).join('');

        if (contenedorGranTotal) {
            contenedorGranTotal.innerText = `$${granTotalAcumulado.toLocaleString('es-CO', { minimumFractionDigits: 0 })} COP`;
        }
    })
    .catch(error => {
        console.error("❌ Error cargando reporte de ventas:", error);
        contenedorTabla.innerHTML = `
            <tr>
                <td colspan="4" class="fila-error-mensaje">
                    No se pudo sincronizar el reporte de ventas con el servidor.
                </td>
            </tr>`;
    });
}