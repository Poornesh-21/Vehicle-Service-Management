document.addEventListener('DOMContentLoaded', function() {
    initializeApp();
    setupEventListeners();
    loadVehiclesUnderServiceFromAPI();
});

let vehiclesUnderService = [];
let currentServicePage = 1;
const itemsPerPage = 5;
let currentServiceId = null;

function getJwtToken() {
    return localStorage.getItem("jwt-token") || sessionStorage.getItem("jwt-token");
}

function initializeApp() {
    setupMobileMenu();
    setupLogout();
    setupAuthentication();
    setupDateDisplay();
}

function setupMobileMenu() {
    const mobileMenuToggle = document.getElementById('mobileMenuToggle');
    if (mobileMenuToggle) {
        mobileMenuToggle.addEventListener('click', function() {
            document.getElementById('sidebar').classList.toggle('active');
        });
    }
}

function setupLogout() {
    const logoutBtn = document.querySelector('.logout-btn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', function() {
            if (confirm('Are you sure you want to logout?')) {
                localStorage.removeItem("jwt-token");
                sessionStorage.removeItem("jwt-token");
                localStorage.removeItem("user-role");
                localStorage.removeItem("user-name");
                sessionStorage.removeItem("user-role");
                sessionStorage.removeItem("user-name");

                window.location.href = '/admin/logout';
            }
        });
    }
}

function setupDateDisplay() {
    const dateElement = document.getElementById('current-date');
    if (dateElement) {
        const options = { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' };
        const today = new Date();
        dateElement.textContent = today.toLocaleDateString('en-US', options);
    }
}

function setupAuthentication() {
    const token = getJwtToken();
    if (!token) {
        window.location.href = '/admin/login?error=session_expired';
        return;
    }
}

function setupEventListeners() {
    const searchInput = document.getElementById('searchInput');
    if (searchInput) {
        searchInput.addEventListener('keyup', function() {
            filterVehiclesUnderService(this.value);
        });
    }

    const serviceTableSearchInput = document.querySelector('.search-input-sm');
    if (serviceTableSearchInput) {
        serviceTableSearchInput.addEventListener('keyup', function() {
            filterTableRows(this.value, 'vehiclesUnderServiceTable');
        });
    }

    document.querySelectorAll('#paginationService .page-link').forEach(link => {
        link.addEventListener('click', function(e) {
            e.preventDefault();
            if (!this.parentElement.classList.contains('disabled')) {
                const page = this.getAttribute('data-page');
                if (page) {
                    currentServicePage = parseInt(page);
                    updateServicePagination();
                    showVehiclesUnderServicePage(currentServicePage);
                }
            }
        });
    });

    document.getElementById('prevBtnService').addEventListener('click', function(e) {
        e.preventDefault();
        if (currentServicePage > 1) {
            currentServicePage--;
            updateServicePagination();
            showVehiclesUnderServicePage(currentServicePage);
        }
    });

    document.getElementById('nextBtnService').addEventListener('click', function(e) {
        e.preventDefault();
        const totalPages = Math.ceil(vehiclesUnderService.length / itemsPerPage);
        if (currentServicePage < totalPages) {
            currentServicePage++;
            updateServicePagination();
            showVehiclesUnderServicePage(currentServicePage);
        }
    });

    document.getElementById('applyFilterBtn').addEventListener('click', function() {
        applyFilters();
    });

    document.getElementById('updateStatusBtn').addEventListener('click', function() {
        const serviceId = document.getElementById('viewServiceId').textContent.replace('REQ-', '');
        updateServiceStatus(serviceId);
    });

    document.getElementById('confirmUpdateStatusBtn').addEventListener('click', function() {
        submitStatusUpdate();
    });
}

function loadVehiclesUnderServiceFromAPI() {
    document.getElementById('loading-row-service').style.display = '';
    document.getElementById('empty-row-service').style.display = 'none';

    const token = getJwtToken();
    if (!token) {
        return;
    }

    fetch('/admin/api/vehicle-tracking/under-service', {
        method: 'GET',
        headers: {
            'Authorization': 'Bearer ' + token,
            'Content-Type': 'application/json'
        }
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('API call failed: ' + response.status);
            }
            return response.json();
        })
        .then(data => {
            vehiclesUnderService = data;

            document.getElementById('loading-row-service').style.display = 'none';

            if (vehiclesUnderService.length > 0) {
                updateServicePagination();
                showVehiclesUnderServicePage(currentServicePage);
            } else {
                document.getElementById('empty-row-service').style.display = '';
                document.getElementById('empty-row-service').innerHTML = `
                <td colspan="10" class="text-center py-5">
                    <div class="my-5">
                        <i class="fas fa-car-alt fa-4x text-muted mb-4" style="opacity: 0.3;"></i>
                        <h4 class="text-wine">No Vehicles Currently Under Service</h4>
                        <p class="text-muted mt-3">
                            There are no vehicles currently undergoing service.
                            <br>Vehicles will appear here when they are received for servicing.
                        </p>
                        <button class="btn-premium primary mt-3" onclick="refreshVehicleData()">
                            <i class="fas fa-sync-alt"></i>
                            Refresh Data
                        </button>
                    </div>
                </td>
                `;
            }
        })
        .catch(error => {
            document.getElementById('loading-row-service').style.display = 'none';
            document.getElementById('empty-row-service').style.display = '';
            document.getElementById('empty-row-service').innerHTML = `
                <td colspan="10" class="text-center py-5">
                    <div class="my-5">
                        <i class="fas fa-exclamation-triangle fa-4x text-danger mb-4"></i>
                        <h4 class="text-danger">Error Loading Vehicles</h4>
                        <p class="text-muted mt-3">
                            We encountered a problem while loading vehicles under service.
                            <br>Error: ${error.message}
                        </p>
                        <button class="btn-premium primary mt-3" onclick="refreshVehicleData()">
                            <i class="fas fa-sync-alt"></i>
                            Try Again
                        </button>
                    </div>
                </td>
            `;
        });
}

function refreshVehicleData() {
    document.getElementById('loading-row-service').style.display = '';
    document.getElementById('empty-row-service').style.display = 'none';
    loadVehiclesUnderServiceFromAPI();
    showToastNotification('Refreshing Data', 'Fetching the latest vehicle service information...');
}

function filterVehiclesUnderService(searchText) {
    if (!searchText || searchText.trim() === '') {
        showVehiclesUnderServicePage(currentServicePage);
        updateServicePagination();
        return;
    }

    searchText = searchText.toLowerCase().trim();

    const filteredVehicles = vehiclesUnderService.filter(vehicle => {
        return (
            (vehicle.vehicleName && vehicle.vehicleName.toLowerCase().includes(searchText)) ||
            (vehicle.registrationNumber && vehicle.registrationNumber.toLowerCase().includes(searchText)) ||
            (vehicle.customerName && vehicle.customerName.toLowerCase().includes(searchText)) ||
            (vehicle.serviceType && vehicle.serviceType.toLowerCase().includes(searchText)) ||
            (vehicle.status && vehicle.status.toLowerCase().includes(searchText)) ||
            (vehicle.serviceAdvisorName && vehicle.serviceAdvisorName.toLowerCase().includes(searchText))
        );
    });

    displayVehiclesUnderService(filteredVehicles);
    updateServicePagination(filteredVehicles.length);
}

function filterTableRows(searchText, tableId) {
    const table = document.getElementById(tableId);
    if (!table) return;

    const rows = table.querySelectorAll('tbody tr');
    if (!rows.length) return;

    searchText = searchText.toLowerCase().trim();

    rows.forEach(row => {
        if (row.id.includes('loading-row') || row.id.includes('empty-row')) {
            return;
        }

        const text = row.textContent.toLowerCase();
        if (text.includes(searchText) || !searchText) {
            row.style.display = '';
        } else {
            row.style.display = 'none';
        }
    });
}

function showVehiclesUnderServicePage(page) {
    const startIndex = (page - 1) * itemsPerPage;
    const endIndex = Math.min(startIndex + itemsPerPage, vehiclesUnderService.length);
    const vehiclesToShow = vehiclesUnderService.slice(startIndex, endIndex);

    displayVehiclesUnderService(vehiclesToShow);
}

function displayVehiclesUnderService(vehicles) {
    const tableBody = document.getElementById('vehiclesUnderServiceTableBody');

    const loadingRow = document.getElementById('loading-row-service');
    const emptyRow = document.getElementById('empty-row-service');

    tableBody.innerHTML = '';
    tableBody.appendChild(loadingRow);
    tableBody.appendChild(emptyRow);

    loadingRow.style.display = 'none';

    // Show all vehicles instead of filtering for assigned vehicles only
    if (vehicles.length === 0) {
        emptyRow.style.display = '';
        emptyRow.innerHTML = `
            <td colspan="10" class="text-center py-4">
                <div class="my-5">
                    <i class="fas fa-car-alt fa-3x text-muted mb-3"></i>
                    <h5>No vehicles under service</h5>
                    <p class="text-muted">There are no vehicles currently under service</p>
                </div>
            </td>
        `;
        return;
    }

    emptyRow.style.display = 'none';

    vehicles.forEach(vehicle => {
        const row = document.createElement('tr');
        row.setAttribute('data-id', vehicle.requestId);
        row.classList.add('active-page');

        const startDate = vehicle.startDate ? new Date(vehicle.startDate).toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric'
        }) : 'Not set';

        const estimatedCompletion = vehicle.estimatedCompletionDate ? new Date(vehicle.estimatedCompletionDate).toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric'
        }) : 'Not set';

        // Set default values if properties are missing
        const vehicleName = vehicle.vehicleName || (vehicle.vehicleBrand && vehicle.vehicleModel ?
            `${vehicle.vehicleBrand} ${vehicle.vehicleModel}` : 'Unknown Vehicle');
        const registrationNumber = vehicle.registrationNumber || 'Not specified';
        const customerName = vehicle.customerName || 'Unknown Customer';
        const membershipStatus = vehicle.membershipStatus || 'Standard';
        const serviceType = vehicle.serviceType || 'General Service';
        const serviceAdvisorName = vehicle.serviceAdvisorName || 'Not Assigned';
        const category = vehicle.category || 'Car';

        row.innerHTML = `
        <td>REQ-${vehicle.requestId}</td>
        <td>
            <div class="vehicle-info">
                <div class="vehicle-icon">
                    <i class="fas fa-${category.toLowerCase() === 'bike' ? 'motorcycle' : 'car'}"></i>
                </div>
                <div class="vehicle-details">
                    <h5>${vehicleName}</h5>
                    <p>${registrationNumber}</p>
                </div>
            </div>
        </td>
        <td>
            <div class="person-info">
                <h5>${customerName}</h5>
            </div>
        </td>
        <td>
            ${membershipStatus === 'Premium' ?
            '<span class="membership-badge membership-premium"><i class="fas fa-crown"></i> Premium</span>' :
            '<span class="membership-badge membership-standard"><i class="fas fa-user"></i> Standard</span>'}
        </td>
        <td>${serviceType}</td>
        <td>${serviceAdvisorName}</td>
        <td>${startDate}</td>
        <td>${estimatedCompletion}</td>
        <td>
            <div class="table-actions-cell">
                <button class="btn-table-action" id="viewServiceBtn" title="View Details">
                    <i class="fas fa-eye"></i>
                </button>
                ${serviceAdvisorName === 'Not Assigned' ?
            '<button class="btn-table-action assign-btn" title="Assign Service Advisor"><i class="fas fa-user-plus"></i></button>' : ''}
            </div>
        </td>
    `;

        tableBody.appendChild(row);
    });

    // Add click handler for the assign button if needed
    document.querySelectorAll('.assign-btn').forEach(btn => {
        btn.addEventListener('click', function(e) {
            e.stopPropagation();
            const row = this.closest('tr');
            const serviceId = row.getAttribute('data-id');
            // You can call a function to open an assignment modal here
            // For example: openAssignmentModal(serviceId);
        });
    });
}

function updateServicePagination(filteredCount) {
    const totalItems = filteredCount !== undefined ? filteredCount : vehiclesUnderService.length;
    const totalPages = Math.ceil(totalItems / itemsPerPage);

    const pagination = document.getElementById('paginationService');
    const pageLinks = pagination.querySelectorAll('.page-link[data-page]');

    pageLinks.forEach((link, index) => {
        const pageNumber = index + 1;
        const pageItem = link.parentElement;

        if (pageNumber <= totalPages) {
            pageItem.style.display = '';
            pageItem.classList.toggle('active', pageNumber === currentServicePage);
        } else {
            pageItem.style.display = 'none';
        }
    });

    const prevBtn = document.getElementById('prevBtnService');
    const nextBtn = document.getElementById('nextBtnService');

    prevBtn.parentElement.classList.toggle('disabled', currentServicePage === 1);
    nextBtn.parentElement.classList.toggle('disabled', currentServicePage === totalPages || totalPages === 0);
}

function viewServiceUnderServiceDetails(serviceId) {
    document.getElementById('spinnerOverlay').classList.add('show');
    currentServiceId = serviceId;

    const token = getJwtToken();
    if (!token) {
        return;
    }

    const vehicle = vehiclesUnderService.find(v => v.requestId === parseInt(serviceId));

    fetch(`/admin/api/vehicle-tracking/service-request/${serviceId}`, {
        method: 'GET',
        headers: {
            'Authorization': 'Bearer ' + token,
            'Content-Type': 'application/json'
        }
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('API call failed: ' + response.status);
            }
            return response.json();
        })
        .then(service => {
            const mergedService = { ...vehicle, ...service };
            document.getElementById('spinnerOverlay').classList.remove('show');

            const startDate = new Date(mergedService.startDate || mergedService.createdAt || Date.now()).toLocaleDateString('en-US', {
                year: 'numeric',
                month: 'long',
                day: 'numeric'
            });

            const estimatedCompletion = new Date(mergedService.estimatedCompletionDate || mergedService.deliveryDate || Date.now()).toLocaleDateString('en-US', {
                year: 'numeric',
                month: 'long',
                day: 'numeric'
            });

            document.getElementById('viewServiceId').textContent = 'REQ-' + serviceId;

            const status = (mergedService.status || 'Received').toLowerCase();
            let statusBadge = '';
            switch (status) {
                case 'received':
                    statusBadge = `<span class="status-badge status-received"><i class="fas fa-clock"></i> Received</span>`;
                    break;
                case 'diagnosis':
                    statusBadge = `<span class="status-badge status-diagnosis"><i class="fas fa-search"></i> Diagnosis</span>`;
                    break;
                case 'repair':
                    statusBadge = `<span class="status-badge status-repair"><i class="fas fa-wrench"></i> Repair</span>`;
                    break;
                default:
                    statusBadge = `<span class="status-badge status-received"><i class="fas fa-clock"></i> Received</span>`;
            }

            document.getElementById('viewStatus').innerHTML = statusBadge;
            document.getElementById('viewStartDate').textContent = startDate;
            document.getElementById('viewVehicleName').textContent = mergedService.vehicleName || mergedService.vehicleModel || 'Vehicle';
            document.getElementById('viewRegistrationNumber').textContent = mergedService.registrationNumber || 'Not specified';
            document.getElementById('viewVehicleCategory').textContent = mergedService.category || mergedService.vehicleType || 'Car';
            document.getElementById('viewCustomerName').textContent = mergedService.customerName || 'Customer';
            document.getElementById('viewCustomerContact').textContent = mergedService.customerPhone || mergedService.customerEmail || 'Not available';

            let membershipBadge = '';
            if ((mergedService.membershipStatus || '') === 'Premium') {
                membershipBadge = '<span class="membership-badge membership-premium"><i class="fas fa-crown"></i> Premium</span>';
            } else {
                membershipBadge = '<span class="membership-badge membership-standard"><i class="fas fa-user"></i> Standard</span>';
            }

            document.getElementById('viewMembership').innerHTML = membershipBadge;
            document.getElementById('viewServiceType').textContent = mergedService.serviceType || 'Regular Service';
            document.getElementById('viewEstimatedCompletion').textContent = estimatedCompletion;
            document.getElementById('viewServiceAdvisor').textContent = mergedService.serviceAdvisorName || 'Not Assigned';
            document.getElementById('viewServiceDescription').textContent = mergedService.additionalDescription || 'No description provided.';

            const progressBar = document.querySelector('.progress-bar');
            const progressPercentage = calculateProgressPercentage(mergedService.status);
            progressBar.style.width = progressPercentage + '%';
            progressBar.textContent = progressPercentage + '%';
            progressBar.setAttribute('aria-valuenow', progressPercentage);

            document.getElementById('viewProgressNotes').textContent = getProgressNotes(mergedService) || 'No progress notes available.';

            const modal = document.getElementById('viewServiceDetailsModal');
            $(modal).modal('show');
            setTimeout(() => {
                modal.style.zIndex = "1060";
                document.querySelector('.modal-backdrop').style.zIndex = "1050";
            }, 100);
        })
        .catch(error => {
            document.getElementById('spinnerOverlay').classList.remove('show');
            alert('Failed to load service details: ' + error.message);
        });
}

function calculateProgressPercentage(status) {
    switch ((status || '').toLowerCase()) {
        case 'received':
            return 10;
        case 'diagnosis':
            return 40;
        case 'repair':
            return 70;
        case 'completed':
            return 100;
        default:
            return 0;
    }
}

function getProgressNotes(service) {
    switch ((service.status || '').toLowerCase()) {
        case 'received':
            return 'Vehicle received and awaiting diagnosis. Service advisor will be assigned soon.';
        case 'diagnosis':
            return `Diagnosis in progress by ${service.serviceAdvisorName}. Checking for issues related to ${service.serviceType}.`;
        case 'repair':
            return `Repair work in progress. Estimated completion on ${new Date(service.estimatedCompletionDate || service.deliveryDate).toLocaleDateString()}.`;
        case 'completed':
            return 'Service completed. Vehicle ready for pickup.';
        default:
            return 'Status updates will appear here.';
    }
}

function applyFilters() {
    const vehicleType = document.getElementById('filterByVehicleType').value;
    const serviceType = document.getElementById('filterByServiceType').value;
    const status = document.getElementById('filterByStatus').value;
    const dateFrom = document.getElementById('filterDateFrom').value;
    const dateTo = document.getElementById('filterDateTo').value;
    const serviceAdvisor = document.getElementById('filterByServiceAdvisor').value;

    const token = getJwtToken();
    if (!token) {
        return;
    }

    const filterCriteria = {
        vehicleType: vehicleType || null,
        serviceType: serviceType || null,
        status: status || null,
        dateFrom: dateFrom || null,
        dateTo: dateTo || null,
        serviceAdvisor: serviceAdvisor || null
    };

    document.getElementById('loading-row-service').style.display = '';
    document.getElementById('empty-row-service').style.display = 'none';

    fetch('/admin/api/vehicle-tracking/under-service/filter', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + token
        },
        body: JSON.stringify(filterCriteria)
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('API call failed: ' + response.status);
            }
            return response.json();
        })
        .then(data => {
            vehiclesUnderService = data;

            document.getElementById('loading-row-service').style.display = 'none';

            if (vehiclesUnderService.length > 0) {
                displayVehiclesUnderService(vehiclesUnderService);
                updateServicePagination();
            } else {
                document.getElementById('empty-row-service').style.display = '';
            }

            $('#filterVehiclesModal').modal('hide');
        })
        .catch(error => {
            document.getElementById('loading-row-service').style.display = 'none';
            document.getElementById('empty-row-service').style.display = '';
            showToastNotification('Error', 'Failed to apply filters: ' + error.message);
        });
}

function updateServiceStatus(serviceId) {
    const currentStatusElement = document.getElementById('viewStatus');
    const currentStatusText = currentStatusElement.textContent.trim();
    let currentStatus = '';

    if (currentStatusText.includes('Received')) {
        currentStatus = 'Received';
    } else if (currentStatusText.includes('Diagnosis')) {
        currentStatus = 'Diagnosis';
    } else if (currentStatusText.includes('Repair')) {
        currentStatus = 'Repair';
    } else {
        currentStatus = 'Received';
    }

    currentServiceId = serviceId;

    let recommendedStatus = '';
    if (currentStatus === 'Received') {
        recommendedStatus = 'Diagnosis';
    } else if (currentStatus === 'Diagnosis') {
        recommendedStatus = 'Repair';
    } else if (currentStatus === 'Repair') {
        recommendedStatus = 'Completed';
    } else {
        recommendedStatus = 'Diagnosis';
    }

    document.getElementById('updateServiceId').textContent = 'REQ-' + serviceId;
    document.getElementById('currentStatusText').textContent = currentStatus;
    document.getElementById('newStatusText').textContent = recommendedStatus;

    const statusSelect = document.getElementById('serviceStatusSelect');
    statusSelect.value = recommendedStatus;

    for (let i = 0; i < statusSelect.options.length; i++) {
        const option = statusSelect.options[i];

        if (option.value === 'Completed' && currentStatus !== 'Repair') {
            option.disabled = true;
        }
        else if (option.value === 'Repair' && currentStatus === 'Received') {
            option.disabled = true;
        } else {
            option.disabled = false;
        }
    }

    statusSelect.onchange = function() {
        document.getElementById('newStatusText').textContent = this.value;
    };

    $('#viewServiceDetailsModal').modal('hide');

    setTimeout(function() {
        $('.modal-backdrop').remove();
        $('body').removeClass('modal-open');
        $('body').css('padding-right', '');
        $('#updateStatusModal').modal('show');
    }, 100);
}

function submitStatusUpdate() {
    document.getElementById('spinnerOverlay').classList.add('show');
    const serviceId = currentServiceId;
    const token = getJwtToken();
    if (!token) {
        return;
    }

    const newStatus = document.getElementById('serviceStatusSelect').value;
    $('#updateStatusModal').modal('hide');

    setTimeout(function() {
        $('.modal-backdrop').remove();
        $('body').removeClass('modal-open');
        $('body').css('padding-right', '');
    }, 100);

    fetch(`/admin/api/vehicle-tracking/service-request/${serviceId}/status`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + token
        },
        body: JSON.stringify({
            status: newStatus
        })
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('API call failed: ' + response.status);
            }
            return response.json();
        })
        .then(data => {
            document.getElementById('spinnerOverlay').classList.remove('show');

            if (newStatus === 'Completed') {
                vehiclesUnderService = vehiclesUnderService.filter(v => v.requestId !== parseInt(serviceId));
                updateServicePagination();
                showVehiclesUnderServicePage(currentServicePage);

                document.getElementById('successTitle').textContent = 'Service Completed!';
                document.getElementById('successMessage').textContent =
                    `Service REQ-${serviceId} has been marked as completed and moved to Completed Services.`;
                $('#successModal').modal('show');
            } else {
                const serviceIndex = vehiclesUnderService.findIndex(v => v.requestId === parseInt(serviceId));
                if (serviceIndex !== -1) {
                    vehiclesUnderService[serviceIndex].status = newStatus;
                    showVehiclesUnderServicePage(currentServicePage);
                }

                showToastNotification('Status Updated!', `Service REQ-${serviceId} status updated to ${newStatus}.`);
            }
        })
        .catch(error => {
            document.getElementById('spinnerOverlay').classList.remove('show');
            showToastNotification('Error', 'Failed to update status: ' + error.message);
        });
}

function showToastNotification(title, message) {
    let toastContainer = document.querySelector('.toast-container');
    if (!toastContainer) {
        toastContainer = document.createElement('div');
        toastContainer.className = 'toast-container position-fixed top-0 end-0 p-3';
        document.body.appendChild(toastContainer);
    }

    const toastId = 'toast-' + Date.now();
    const toastHtml = `
        <div id="${toastId}" class="toast align-items-center text-white bg-wine border-0" role="alert" aria-live="assertive" aria-atomic="true">
            <div class="d-flex">
                <div class="toast-body">
                    <strong>${title}</strong> ${message}
                </div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
            </div>
        </div>
        `;

    toastContainer.insertAdjacentHTML('beforeend', toastHtml);

    const toastElement = document.getElementById(toastId);
    const toast = new bootstrap.Toast(toastElement, {
        animation: true,
        autohide: true,
        delay: 3000
    });

    toast.show();

    toastElement.addEventListener('hidden.bs.toast', function() {
        toastElement.remove();
    });
}

document.addEventListener('click', function(e) {
    if (e.target && e.target.id === 'viewServiceBtn' || e.target.closest('#viewServiceBtn')) {
        const row = e.target.closest('tr');
        if (row) {
            const serviceId = row.getAttribute('data-id');
            if (serviceId) {
                viewServiceUnderServiceDetails(serviceId);
            }
        }
    }

    if (e.target && e.target.tagName !== 'BUTTON' && e.target.closest('tr') && !e.target.closest('thead')) {
        const row = e.target.closest('tr');
        if (row.id !== 'loading-row-service' && row.id !== 'empty-row-service') {
            const serviceId = row.getAttribute('data-id');
            if (serviceId) {
                if (e.target.closest('.table-actions-cell') ||
                    e.target.closest('button') ||
                    (e.target.closest('i') && e.target.closest('button'))) {
                    return;
                }

                viewServiceUnderServiceDetails(serviceId);
            }
        }
    }
});