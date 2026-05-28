document.addEventListener('DOMContentLoaded', () => {
    cargarDatosDashboard();
    cargarGestionDeNegocios();
});

// --- SECCIÓN 1: DASHBOARD (ESTADÍSTICAS Y SOLICITUDES) ---
function cargarDatosDashboard() {
    fetch('../LoginServlet?accion=cargarDashboard')
        .then(response => response.json())
        .then(data => {
            console.log("Datos del Dashboard recibidos:", data);
            
            // Cargas de estadísticas existentes
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

            // Cargas de solicitudes pendientes existentes
            const containerSolicitudes = document.getElementById('Solicitudes');
            containerSolicitudes.innerHTML = '<h2 class="solicitudes-titulo"><i class="bx bx-bell"></i> SOLICITUDES</h2>'; 
            if (data.solicitudes.length > 0) {
                let listaHTML = '<div class="lista-solicitudes-wrapper">';
                data.solicitudes.forEach(negocio => {
                    listaHTML += `
                        <div class="item-solicitud">
                            <span class="solicitud-nombre txt-blanco">${negocio.nombre}</span>
                            <div class="solicitud-botones">
                                <button class="btn-dash btn-dash-aprobar" onclick="gestionarNegocio(${negocio.id}, 'APROBAR')">Aprobar</button>
                                <button class="btn-dash btn-dash-rechazar" onclick="gestionarNegocio(${negocio.id}, 'RECHAZAR')">Rechazar</button>
                            </div>
                        </div>
                    `;
                });
                listaHTML += '</div>';
                containerSolicitudes.insertAdjacentHTML('beforeend', listaHTML);
            } else {
                containerSolicitudes.innerHTML += '<p class="txt-gris no-datos-msg">No hay solicitudes pendientes 🙌</p>';
            }

            // Carga de la sección de Importación Masiva
            const containerImportacion = document.getElementById('Importacion_Masiva_De_Datos') || divsStats[1].nextElementSibling; 
            if (containerImportacion) {
                containerImportacion.innerHTML = `
                    <h2 class="solicitudes-titulo"><i class='bx bx-cloud-upload'></i> IMPORTACIÓN MASIVA</h2>
                    <div class="import-container">
                        <p class="txt-gris import-desc">Carga múltiples establecimientos a Juanfax mediante un archivo CSV o Excel.</p>
                        <label class="drop-zone" for="csvFile">
                            <i class='bx bxs-file-doc drop-icon'></i>
                            <span class="drop-text">Arrastra tu archivo o <span class="txt-dorado">examina</span></span>
                            <input type="file" id="csvFile" accept=".csv, .xlsx" hidden onchange="procesarArchivoMasivo()">
                        </label>
                    </div>
                `;
            }

            // ========================================================================
            // 🚀 COMPLETADO: RENDERIZADO 100% DINÁMICO DE ALERTAS DEL SISTEMA
            // ========================================================================
            const containerAlertas = document.getElementById('Alertas');
            if (containerAlertas) {
                let htmlAlertas = `
                    <h2 class="solicitudes-titulo"><i class='bx bx-error-alt'></i> ALERTAS DEL SISTEMA</h2>
                    <div class="lista-alertas-wrapper">
                `;

                // Validamos que existan alertas en la respuesta del Servlet
                if (data.alertas && data.alertas.length > 0) {
                    data.alertas.forEach(alerta => {
                        
                        // Mapeamos dinámicamente las clases CSS y los íconos Boxicons según el tipo de alerta
                        let itemClass = 'alerta-info';
                        let iconoClass = 'bx bx-info-circle';

                        if (alerta.tipo === 'warning') {
                            itemClass = 'alerta-warning';
                            iconoClass = 'bx bx-time';
                        } else if (alerta.tipo === 'error') {
                            itemClass = 'alerta-danger'; // Mapea a tus estilos CSS de peligro/error
                            iconoClass = 'bx bx-user-x';
                        } else if (alerta.tipo === 'success') {
                            itemClass = 'alerta-success';
                            iconoClass = 'bx bx-check-circle';
                        }

                        // Inyectamos la estructura respetando tus estilos visuales originales
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
                    // Mensaje seguro por si la base de datos no tiene alertas registradas aún
                    htmlAlertas += '<p class="txt-gris no-datos-msg" style="text-align: center; padding: 15px;">No hay alertas recientes 🙌</p>';
                }

                htmlAlertas += '</div>'; // Cerramos la envoltura de la lista
                containerAlertas.innerHTML = htmlAlertas;
            }
        })
        .catch(error => console.error('Error cargando el dashboard:', error));
}

function procesarArchivoMasivo() {
    const input = document.getElementById('csvFile');
    if (input.files.length === 0) return;

    const archivo = input.files[0];
    
    // Validación básica de extensión por seguridad
    const extension = archivo.name.split('.').pop().toLowerCase();
    if (extension !== 'csv' && extension !== 'xlsx') {
        alert('Por favor, selecciona un archivo válido (.csv o .xlsx)');
        input.value = ''; // Reseteamos el input
        return;
    }

    // Mostramos un mensaje de carga sutil en la interfaz
    const dropZoneText = document.querySelector('.drop-text');
    const copiaTextoOriginal = dropZoneText.innerHTML;
    dropZoneText.innerHTML = `<i class='bx bx-loader-alt bx-spin'></i> Subiendo e importando...`;

    // Empaquetamos el archivo usando FormData (Multipart)
    const formData = new FormData();
    formData.append('accion', 'importarNegociosMasivo');
    formData.append('archivoNegocios', archivo);

    // Enviamos la petición POST al Servlet
    fetch('../LoginServlet', {
        method: 'POST',
        body: formData // No añadimos Content-Type header, el navegador lo pone automáticamente como multipart/form-data
    })
    .then(response => {
        if (!response.ok) throw new Error('Error en el servidor al procesar el lote.');
        return response.json();
    })
    .then(data => {
        if (data.status === 'success') {
            alert(`¡Éxito! Se han importado ${data.insertados} nuevos negocios correctamente.`);
            // Recargamos el dashboard y la tabla para ver los nuevos datos inmediatamente
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
        // Restauramos el diseño original del input
        input.value = '';
        dropZoneText.innerHTML = copiaTextoOriginal;
    });
}

// --- SECCIÓN 2: GESTIÓN DE NEGOCIOS REGISTRADOS ---
function cargarGestionDeNegocios() {
    const containerGestion = document.getElementById('GestionDeNegocios');

    fetch('../LoginServlet?accion=listarTodosLosNegociosAdmin')
        .then(response => {
            if (!response.ok) {
                throw new Error("Error en la respuesta del servidor");
            }
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
                        accionHtml = `<a href="#" class="link-accion link-rojo" onclick="verMotivoBloqueo(${negocio.idNegocio})">Ver motivo</a>`;
                    } else {
                        accionHtml = `<a href="#" class="link-accion link-dorado" onclick="abrirMenuGestion(${negocio.idNegocio}, '${negocio.nombre.replace(/'/g, "\\'")}')">Gestionar</a>`;
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

// --- SECCIÓN 3: ACCIONES Y PROCESAMIENTO ---
function abrirMenuGestion(id, nombre) {
    const opcion = prompt(`Gestionar negocio: "${nombre}"\n\nEscribe el número de la acción:\n1. Aprobar / Activar\n2. Bloquear Negocio\n3. Cancelar`);
    if (opcion === "1") {
        gestionarNegocio(id, 'Activo');
    } else if (opcion === "2") {
        const motivo = prompt("Introduce el motivo del bloqueo:");
        if (motivo) {
            gestionarNegocio(id, 'Bloqueado'); 
        }
    }
}

function verMotivoBloqueo(id) {
    alert("Este negocio fue bloqueado por el administrador debido al vencimiento de términos o reportes.");
}

function gestionarNegocio(id, estado) {
    const params = new URLSearchParams();
    params.append('accion', 'actualizarEstado');
    params.append('idNegocio', id);
    params.append('estado', estado);

    fetch('../LoginServlet', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: params
    })
    .then(response => response.json())
    .then(data => {
        if (data.status === 'success') {
            alert('¡Acción realizada con éxito!');
            cargarDatosDashboard(); 
            cargarGestionDeNegocios();
        } else {
            alert('Error: ' + data.message);
        }
    })
    .catch(error => {
        console.error('Error al actualizar el estado:', error);
        alert('Ocurrió un error inesperado. Intenta de nuevo.');
    });
}