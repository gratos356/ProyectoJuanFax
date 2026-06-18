// ====================================================================
// 🕹️ CONTROLADOR CENTRAL DE INTERFAZ (SPA) - JUANFAX ADMIN
// ====================================================================

let plantillaOriginalNegocios = "";

document.addEventListener('DOMContentLoaded', () => {
    const contenedorPrincipal = document.getElementById("informacionNegocio");
    const btnNegocios = document.getElementById("btnNavNegocios");
    const btnUsuarios = document.getElementById("btnNavUsuarios");

    // 1️⃣ Memorizar la estructura esquelética inicial del HTML
    if (contenedorPrincipal) {
        plantillaOriginalNegocios = contenedorPrincipal.innerHTML;
    }

    // 2️⃣ Carga inicial por defecto (Dashboard y Negocios)
    cargarDatosDashboard();
    cargarGestionDeNegocios();

    // 3️⃣ Manejo de navegación limpia mediante Event Listeners fijos
    if (btnNegocios) {
        btnNegocios.addEventListener("click", () => navegarAdmin('negocios'));
    }
    if (btnUsuarios) {
        btnUsuarios.addEventListener("click", () => navegarAdmin('usuarios'));
    }

    // 4️⃣ ⚡ DELEGACIÓN CENTRAL DE EVENTOS
    if (contenedorPrincipal) {
        // --- Escuchador de Clicks ---
        contenedorPrincipal.addEventListener('click', (evento) => {
            const target = evento.target;

            // Solicitudes del Dashboard (Aprobar / Rechazar)
            if (target.matches('[data-action="solicitud-gestionar"]')) {
                const id = target.getAttribute('data-id');
                const estado = target.getAttribute('data-estado'); // Envía 'APROBAR' o 'RECHAZAR'
                gestionarNegocio(id, estado);
            }

            // Enlace Ver Motivo de Bloqueo en Tabla Negocios
            if (target.matches('[data-action="negocio-ver-motivo"]')) {
                evento.preventDefault();
                const id = target.getAttribute('data-id');
                verMotivoBloqueo(id);
            }

            // Enlace Gestionar en Tabla Negocios
            if (target.matches('[data-action="negocio-gestionar"]')) {
                evento.preventDefault();
                const id = target.getAttribute('data-id');
                const nombre = target.getAttribute('data-nombre');
                abrirMenuGestion(id, nombre);
            }
        });

        // --- Escuchador de Cambios (Inputs y Selects) ---
        contenedorPrincipal.addEventListener('change', (evento) => {
            const target = evento.target;

            if (target.id === 'csvFile') {
                procesarArchivoMasivo();
            }

            if (target.classList.contains('select-cambio-estado')) {
                const idUsuario = target.getAttribute('data-id');
                const nuevoEstado = target.value;
                cambiarEstadoUsuario(idUsuario, nuevoEstado);
            }
        });
    }
});

/**
 * Conmutador dinámico de secciones del Administrador
 */
function navegarAdmin(seccion) {
    console.log(`🚀 Conmutando panel hacia: ${seccion.toUpperCase()}`);
    
    document.getElementById("btnNavNegocios").classList.remove("botonesFocus");
    document.getElementById("btnNavUsuarios").classList.remove("botonesFocus");
    
    const contenedorPrincipal = document.getElementById("informacionNegocio");
    if (!contenedorPrincipal) return;

    if (seccion === 'negocios') {
        document.getElementById("btnNavNegocios").classList.add("botonesFocus");
        contenedorPrincipal.innerHTML = plantillaOriginalNegocios;
        cargarDatosDashboard();
        cargarGestionDeNegocios();
    } 
    else if (seccion === 'usuarios') {
        document.getElementById("btnNavUsuarios").classList.add("botonesFocus");
        cargarGestionUsuarios();
    }
}

// ====================================================================
// --- SECCIÓN 1: DASHBOARD (ESTADÍSTICAS Y SOLICITUDES) ---
// ====================================================================
function cargarDatosDashboard() {
    fetch('../LoginServlet?accion=cargarDashboard')
        .then(response => response.json())
        .then(data => {
            console.log("Datos del Dashboard recibidos:", data);
            
            const divsStats = document.querySelectorAll('.estads');
            if (divsStats.length >= 2) {
                divsStats[0].innerHTML = `
                    <span class="num-estadistica txt-dorado">${data.stats.pendientes}</span>
                    <p class="lbl-estadistica">Pendientes</p>
                `;
                divsStats[1].innerHTML = `
                    <span class="num-estadistica txt-blanco">${data.stats.aprobados}</span>
                    <p class="lbl-estadistica">Aprobados</p>
                `;
            }

            const containerSolicitudes = document.getElementById('Solicitudes');
            if (containerSolicitudes) {
                containerSolicitudes.innerHTML = '<h2 class="solicitudes-titulo"><i class="bx bx-bell"></i> SOLICITUDES</h2>'; 
                if (data.solicitudes.length > 0) {
                    let listaHTML = '<div class="lista-solicitudes-wrapper">';
                    data.solicitudes.forEach(negocio => {
                        listaHTML += `
                            <div class="item-solicitud">
                                <span class="solicitud-nombre txt-blanco">${negocio.nombre}</span>
                                <div class="solicitud-botones">
                                    <button class="btn-dash btn-dash-aprobar" data-action="solicitud-gestionar" data-id="${negocio.id}" data-estado="APROBAR">Aprobar</button>
                                    <button class="btn-dash btn-dash-rechazar" data-action="solicitud-gestionar" data-id="${negocio.id}" data-estado="RECHAZAR">Rechazar</button>
                                </div>
                            </div>
                        `;
                    });
                    listaHTML += '</div>';
                    containerSolicitudes.insertAdjacentHTML('beforeend', listaHTML);
                } else {
                    containerSolicitudes.innerHTML += '<p class="txt-gris no-datos-msg">No hay solicitudes pendientes 🙌</p>';
                }
            }

            const containerImportacion = document.getElementById('Importacion_Masiva_De_Datos') || (divsStats.length > 1 ? divsStats[1].nextElementSibling : null); 
            if (containerImportacion) {
                containerImportacion.innerHTML = `
                    <h2 class="solicitudes-titulo"><i class='bx bx-cloud-upload'></i> IMPORTACIÓN MASIVA</h2>
                    <div class="import-container">
                        <p class="txt-gris import-desc">Carga múltiples establecimientos a Juanfax mediante un archivo CSV o Excel.</p>
                        <label class="drop-zone" for="csvFile">
                            <i class='bx bxs-file-doc drop-icon'></i>
                            <span class="drop-text">Arrastra tu archivo o <span class="txt-dorado">examina</span></span>
                            <input type="file" id="csvFile" accept=".csv, .xlsx" hidden>
                        </label>
                    </div>
                `;
            }

            const containerAlertas = document.getElementById('Alertas');
            if (containerAlertas) {
                let htmlAlertas = `
                    <h2 class="solicitudes-titulo"><i class='bx bx-error-alt'></i> ALERTAS DEL SISTEMA</h2>
                    <div class="lista-alertas-wrapper">
                `;

                if (data.alertas && data.alertas.length > 0) {
                    data.alertas.forEach(alerta => {
                        let itemClass = 'alerta-info';
                        let iconoClass = 'bx bx-info-circle';

                        if (alerta.tipo === 'warning') {
                            itemClass = 'alerta-warning';
                            iconoClass = 'bx bx-time';
                        } else if (alerta.tipo === 'error') {
                            itemClass = 'alerta-danger'; 
                            iconoClass = 'bx bx-user-x';
                        } else if (alerta.tipo === 'success') {
                            itemClass = 'alerta-success';
                            iconoClass = 'bx bx-check-circle';
                        }

                        htmlAlertas += `
                            <div class="item-alerta ${itemClass}">
                                <div class="alerta-icono"><i class='${iconoClass}'></i></div>
                                <div class="alerta-info">
                                    <span class="alerta-msg txt-blanco">${alerta.mensaje}</span>
                                    <span class="alerta-tiempo">${alerta.tiempoRelativo}</span>
                                </div>
                            </div>
                        `;
                    });
                } else {
                    htmlAlertas += '<p class="txt-gris no-datos-msg" style="text-align: center; padding: 15px;">No hay alertas recientes 🙌</p>';
                }

                htmlAlertas += '</div>';
                containerAlertas.innerHTML = htmlAlertas;
            }
        })
        .catch(error => console.error('Error cargando el dashboard:', error));
}

function procesarArchivoMasivo() {
    const input = document.getElementById('csvFile');
    if (!input || input.files.length === 0) return;

    const archivo = input.files[0];
    const extension = archivo.name.split('.').pop().toLowerCase();
    
    if (extension !== 'csv' && extension !== 'xlsx') {
        alert('Por favor, selecciona un archivo válido (.csv o .xlsx)');
        input.value = ''; 
        return;
    }

    const dropZoneText = document.querySelector('.drop-text');
    const copiaTextoOriginal = dropZoneText ? dropZoneText.innerHTML : "";
    if (dropZoneText) {
        dropZoneText.innerHTML = `<i class='bx bx-loader-alt bx-spin'></i> Subiendo e importando...`;
    }

    const formData = new FormData();
    formData.append('accion', 'importarNegociosMasivo');
    formData.append('archivoNegocios', archivo);

    fetch('../LoginServlet', {
        method: 'POST',
        body: formData 
    })
    .then(response => {
        if (!response.ok) throw new Error('Error en el servidor al procesar el lote.');
        return response.json();
    })
    .then(data => {
        if (data.status === 'success') {
            alert(`¡Éxito! Se han importado ${data.insertados} nuevos negocios correctamente.`);
            cargarDatosDashboard();
            cargarGestionDeNegocios();
        } else {
            alert('Error en la importación: ' + data.message);
        }
    })
    .catch(error => {
        console.error('Error en la importación masiva:', error);
        alert('Ocurrió un fallo de red o el archivo tiene un formato inválido.');
    })
    .finally(() => {
        if (input) input.value = '';
        if (dropZoneText) dropZoneText.innerHTML = copiaTextoOriginal;
    });
}

// ====================================================================
// --- SECCIÓN 2: GESTIÓN DE NEGOCIOS REGISTRADOS ---
// ====================================================================
function cargarGestionDeNegocios() {
    const containerGestion = document.getElementById('GestionDeNegocios');
    if (!containerGestion) return;

    fetch('../LoginServlet?accion=listarTodosLosNegociosAdmin')
        .then(response => {
            if (!response.ok) throw new Error("Error en la respuesta del servidor");
            return response.json();
        })
        .then(data => {
            console.log("Datos de Gestión recibidos:", data);

            let htmlEstructura = `
                <div class="tabla-header-container">
                    <h2 class="tabla-titulo"><i class='bx bxs-store-alt'></i> NEGOCIOS REGISTRADOS</h2>
                </div>
                <table class="tabla-negocios">
                    <thead>
                        <tr>
                            <th>NOMBRE</th>
                            <th>CATEGORÍA</th>
                            <th>ESTADO</th>
                            <th>SUSCRIPCIÓN</th>
                            <th>CALIFIC.</th>
                            <th>VISTAS</th>
                            <th>ACCIÓN</th>
                        </tr>
                    </thead>
                    <tbody id="tbodyNegocios">
            `;

            if (data && data.length > 0) {
                data.forEach(negocio => {
                    let badgeClass = 'badge-defecto';
                    let estadoTexto = negocio.estado ? negocio.estado : 'PENDIENTE';
                    let estadoLower = estadoTexto.toLowerCase();

                    if (estadoLower.includes('activo') || estadoLower.includes('aprobado')) badgeClass = 'badge-activo';
                    else if (estadoLower.includes('trial')) badgeClass = 'badge-trial';
                    else if (estadoLower.includes('bloqueado') || estadoLower.includes('rechazado')) badgeClass = 'badge-bloqueado';

                    let accionHtml = '';
                    if (estadoLower === 'bloqueado' || estadoLower === 'rechazado') {
                        accionHtml = `<a href="#" class="link-accion link-rojo" data-action="negocio-ver-motivo" data-id="${negocio.idNegocio}">Ver motivo</a>`;
                    } else {
                        const nombreLimpio = negocio.nombre.replace(/"/g, '&quot;');
                        accionHtml = `<a href="#" class="link-accion link-dorado" data-action="negocio-gestionar" data-id="${negocio.idNegocio}" data-nombre="${nombreLimpio}">Gestionar</a>`;
                    }

                    let califNum = typeof negocio.calificacion === 'number' ? negocio.calificacion : parseFloat(negocio.calificacion) || 0.0;
                    let vistasNum = typeof negocio.vistas === 'number' ? negocio.vistas : parseInt(negocio.vistas) || 0;

                    htmlEstructura += `
                        <tr>
                            <td class="txt-blanco">${negocio.nombre}</td>
                            <td class="txt-gris">${negocio.categoria}</td>
                            <td><span class="badge-estado ${badgeClass}">${estadoTexto}</span></td>
                            <td class="txt-gris">${negocio.suscripcion}</td>
                            <td class="txt-dorado">${califNum.toFixed(1)} ★</td>
                            <td class="txt-gris">${vistasNum.toLocaleString()}</td>
                            <td>${accionHtml}</td>
                        </tr>
                    `;
                });
            } else {
                htmlEstructura += `<tr><td colspan="7" style="text-align:center; color: #8a99ad;">No hay negocios registrados.</td></tr>`;
            }

            htmlEstructura += `</tbody></table>`;
            containerGestion.innerHTML = htmlEstructura;
        })
        .catch(error => console.error('Error cargando gestión de negocios:', error));
}

// ====================================================================
// --- SECCIÓN 3: GESTIÓN DE USUARIOS REGISTRADOS ---
// ====================================================================
function cargarGestionUsuarios() {
    const contenedorPrincipal = document.getElementById("informacionNegocio");
    if (!contenedorPrincipal) return;

    contenedorPrincipal.innerHTML = `<div style="padding:20px; color:#8a99ad;"><i class='bx bx-loader-alt bx-spin'></i> Trayendo listado de usuarios...</div>`;

    fetch('../LoginServlet?accion=listarUsuarios')
    .then(res => {
        if (!res.ok) throw new Error("Error en la petición de usuarios.");
        return res.json();
    })
    .then(usuarios => {
        let tablaHTML = `
            <div class="modulo-gestion" style="padding: 10px 0;">
                <div class="tabla-header-container" style="margin-bottom: 20px;">
                    <h2 class="tabla-titulo"><i class='bx bx-group'></i> GESTIÓN DE USUARIOS DEL SISTEMA</h2>
                </div>
                <table class="tabla-negocios">
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>NOMBRE COMPLETO</th>
                            <th>CORREO ELECTRÓNICO</th>
                            <th>ROL</th>
                            <th>ESTADO ACTUAL</th>
                            <th>ACCIONES</th>
                        </tr>
                    </thead>
                    <tbody id="tbodyUsuarios">
        `;

        if (usuarios.length === 0) {
            tablaHTML += `<tr><td colspan="6" style="text-align:center; color: #8a99ad;">No hay usuarios registrados en Juanfax.</td></tr>`;
        } else {
            usuarios.forEach(u => {
                const badgeClass = u.estado === 'ACTIVO' ? 'badge-activo' : 'badge-bloqueado';
                
                tablaHTML += `
                    <tr>
                        <td class="txt-dorado"><strong>#${u.idUsuario}</strong></td>
                        <td class="txt-blanco">${u.nombre}</td>
                        <td class="txt-gris">${u.correo}</td>
                        <td><span class="badge-estado badge-defecto" style="text-transform: uppercase;">${u.rol}</span></td>
                        <td><span class="badge-estado ${badgeClass}">${u.estado}</span></td>
                        <td>
                            <select class="select-cambio-estado" data-id="${u.idUsuario}" style="background:#1a202c; color:#fff; border:1px solid #8a99ad; padding:4px; border-radius:4px; cursor:pointer;">
                                <option value="ACTIVO" ${u.estado === 'ACTIVO' ? 'selected' : ''}>🔓 Activo</option>
                                <option value="BLOQUEADO" ${u.estado === 'BLOQUEADO' ? 'selected' : ''}>🔒 Bloqueado</option>
                            </select>
                        </td>
                    </tr>
                `;
            });
        }

        tablaHTML += `</tbody></table></div>`;
        contenedorPrincipal.innerHTML = tablaHTML;
    })
    .catch(err => {
        console.error("❌ Fallo al cargar usuarios:", err);
        contenedorPrincipal.innerHTML = `<p style="color:red; padding:20px;">Error al conectar con el servidor.</p>`;
    });
}

// ====================================================================
// --- SECCIÓN 4: ACCIONES Y PROCESAMIENTO ---
// ====================================================================
function abrirMenuGestion(id, nombre) {
    const opcion = prompt(`Gestionar negocio: "${nombre}"\n\nEscribe el número de la acción:\n1. Aprobar / Activar\n2. Bloquear Negocio\n3. Cancelar`);
    
    if (opcion === "1") {
        // 🎯 ESTANDARIZADO: 'APROBAR' para alinearse con los botones del dashboard
        gestionarNegocio(id, 'APROBAR');
    } else if (opcion === "2") {
        const motivo = prompt("Introduce el motivo del bloqueo:");
        if (motivo && motivo.trim() !== "") {
            // 🎯 ESTANDARIZADO: 'BLOQUEAR' y guardamos el motivo en la sesión/alerta si lo requieres luego
            gestionarNegocio(id, 'BLOQUEAR'); 
        } else if (motivo !== null) {
            alert("⚠️ Debes especificar un motivo para efectuar el bloqueo del establecimiento.");
        }
    }
}

function verMotivoBloqueo(id) {
    alert("Este negocio fue suspendido por el administrador debido al incumplimiento de las políticas del servicio o a la revocación de su suscripción.");
}

function gestionarNegocio(id, estado) {
    const params = new URLSearchParams();
    params.append('accion', 'actualizarEstado');
    params.append('idNegocio', id);
    params.append('estado', estado); // Viaja limpio como 'APROBAR', 'RECHAZAR' o 'BLOQUEAR'

    fetch('../LoginServlet', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: params
    })
    .then(response => response.json())
    .then(data => {
        if (data.status === 'success') {
            alert('¡Estado de establecimiento actualizado con éxito!');
            cargarDatosDashboard(); 
            cargarGestionDeNegocios();
        } else {
            alert('Error en pasarela: ' + data.message);
        }
    })
    .catch(error => {
        console.error('Error al actualizar el estado:', error);
        alert('Ocurrió un error inesperado. Intenta de nuevo.');
    });
}

function cambiarEstadoUsuario(idUsuario, nuevoEstado) {
    if (confirm(`¿Confirmas el cambio de estado a [${nuevoEstado}] para el usuario #${idUsuario}?`)) {
        fetch(`../LoginServlet?accion=cambiarEstadoUsuario&idUsuario=${idUsuario}&nuevoEstado=${nuevoEstado}`, {
            method: 'POST'
        })
        .then(res => {
            if (!res.ok) throw new Error("Error del servidor.");
            return res.json();
        })
        .then(respuesta => {
            if (respuesta.success) {
                alert("✨ ¡Estado de cuenta actualizado!");
                cargarGestionUsuarios(); 
            } else {
                alert("⚠️ " + respuesta.mensaje);
            }
        })
        .catch(err => {
            console.error("Error al actualizar usuario:", err);
            alert("No se pudieron guardar los cambios.");
        });
    } else {
        cargarGestionUsuarios(); 
    }
}

// ============================================================================
// CONTROL DE CIERRE DE SESIÓN PARA EL ADMINISTRADOR
// ============================================================================
document.addEventListener("DOMContentLoaded", () => {
    const profileAdmin = document.getElementById("profile");
    
    if (profileAdmin) {
        profileAdmin.style.cursor = "pointer";
        profileAdmin.addEventListener("click", () => {
            const seguro = confirm("🔒 ¿Estás seguro de que deseas cerrar la sesión de Administrador?");
            if (seguro) {
                window.location.href = "../LoginServlet?accion=cerrarSesion";
            }
        });
    }
});