let dashboardStats = null;
let serviceAdvisors = [];
let currentPage = {
    due: 1,
    inService: 1,
    completed: 1
};
const itemsPerPage = {
    due: 5,
    inService: 5,
    completed: 3
};

$(document).ready(function() {
    $('#mobile-menu-toggle').click(function() {
        $('#sidebar').toggleClass('active');
        $(this).find('i').toggleClass('fa-bars fa-times');
    });

    $('.sidebar-menu-link').click(function() {
        if ($(window).width() < 992) {
            $('#sidebar').removeClass('active');
            $('#mobile-menu-toggle').find('i').removeClass('fa-times').addClass('fa-bars');
        }
    });

    $(window).resize(function() {
        if ($(window).width() >= 992) {
            $('#sidebar').removeClass('active');
            $('#mobile-menu-toggle').find('i').removeClass('fa-times').addClass('fa-bars');
        }
    });
});

document.addEventListener('DOMContentLoaded', function() {
    initializeApp();
    loadDashboardData();
    loadServiceAdvisors();
    setupEventListeners();
});

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
    const tokenFromStorage = localStorage.getItem("jwt-token") || sessionStorage.getItem("jwt-token");

    if (tokenFromStorage) {
        const currentUrl = new URL(window.location.href);
        const urlToken = currentUrl.searchParams.get('token');

        if (!urlToken) {
            document.querySelectorAll('.sidebar-menu-link').forEach(link => {
                const href = link.getAttribute('href');
                if (href && !href.includes('token=')) {
                    const separator = href.includes('?') ? '&' : '?';
                    link.setAttribute('href', href + separator + 'token=' + encodeURIComponent(tokenFromStorage));
                }
            });

            if (window.location.href.indexOf('token=') === -1) {
                const separator = window.location.href.indexOf('?') === -1 ? '?' : '&';
                const newUrl = window.location.href + separator + 'token=' + encodeURIComponent(tokenFromStorage);
                window.history.replaceState({}, document.title, newUrl);
            }
        }
    } else {
        window.location.href = '/admin/login?error=session_expired';
    }
}


function setupEventListeners() {
    setupPagination('dueTable', 'dueTablePagination', 'due');
    setupPagination('serviceTable', 'serviceTablePagination', 'inService');
    setupPagination('completedServicesGrid', 'completedServicesPagination', 'completed');
    setupSearch();
    setupModalListeners();
}

function setupPagination(tableId, paginationId, type) {
    const tableElement = document.getElementById(tableId);
    const paginationElement = document.getElementById(paginationId);

    if (!tableElement || !paginationElement) return;

    paginationElement.querySelectorAll('[data-page]').forEach(button => {
        button.addEventListener('click', function(e) {
            e.preventDefault();
            const page = parseInt(this.getAttribute('data-page'));
            changePage(page, type);
        });
    });

    const prevBtn = document.getElementById(`${type}PrevBtn`);
    if (prevBtn) {
        prevBtn.addEventListener('click', function(e) {
            e.preventDefault();
            if (currentPage[type] > 1) {
                changePage(currentPage[type] - 1, type);
            }
        });
    }

    const nextBtn = document.getElementById(`${type}NextBtn`);
    if (nextBtn) {
        nextBtn.addEventListener('click', function(e) {
            e.preventDefault();

            let totalItems = 0;
            if (type === 'due' && dashboardStats && dashboardStats.vehiclesDueList) {
                totalItems = dashboardStats.vehiclesDueList.length;
            } else if (type === 'inService' && dashboardStats && dashboardStats.vehiclesInServiceList) {
                totalItems = dashboardStats.vehiclesInServiceList.length;
            } else if (type === 'completed' && dashboardStats && dashboardStats.completedServicesList) {
                totalItems = dashboardStats.completedServicesList.length;
            }

            const totalPages = Math.ceil(totalItems / itemsPerPage[type]);

            if (currentPage[type] < totalPages) {
                changePage(currentPage[type] + 1, type);
            }
        });
    }
}

function changePage(page, type) {
    currentPage[type] = page;

    if (type === 'due') {
        updateDueTablePage();
    } else if (type === 'inService') {
        updateServiceTablePage();
    } else if (type === 'completed') {
        updateCompletedServicesPage();
    }

    updatePaginationUI(type);
}

function updateDueTablePage() {
    const tableRows = document.querySelectorAll('#dueTable tbody tr');
    const startIndex = (currentPage.due - 1) * itemsPerPage.due;
    const endIndex = startIndex + itemsPerPage.due;

    tableRows.forEach((row, index) => {
        row.classList.remove('active-page');
    });

    for (let i = startIndex; i < endIndex && i < tableRows.length; i++) {
        tableRows[i].classList.add('active-page');
    }
}

function updateServiceTablePage() {
    const tableRows = document.querySelectorAll('#serviceTable tbody tr');
    const startIndex = (currentPage.inService - 1) * itemsPerPage.inService;
    const endIndex = startIndex + itemsPerPage.inService;

    tableRows.forEach((row, index) => {
        row.classList.remove('active-page');
    });

    for (let i = startIndex; i < endIndex && i < tableRows.length; i++) {
        tableRows[i].classList.add('active-page');
    }
}

function updateCompletedServicesPage() {
    const cards = document.querySelectorAll('#completedServicesGrid .service-card');
    const startIndex = (currentPage.completed - 1) * itemsPerPage.completed;
    const endIndex = startIndex + itemsPerPage.completed;

    cards.forEach((card, index) => {
        card.classList.remove('active-page');
    });

    for (let i = startIndex; i < endIndex && i < cards.length; i++) {
        cards[i].classList.add('active-page');
    }
}

function updatePaginationUI(type) {
    let totalItems = 0;
    if (type === 'due' && dashboardStats && dashboardStats.vehiclesDueList) {
        totalItems = dashboardStats.vehiclesDueList.length;
    } else if (type === 'inService' && dashboardStats && dashboardStats.vehiclesInServiceList) {
        totalItems = dashboardStats.vehiclesInServiceList.length;
    } else if (type === 'completed' && dashboardStats && dashboardStats.completedServicesList) {
        totalItems = dashboardStats.completedServicesList.length;
    }

    const totalPages = Math.ceil(totalItems / itemsPerPage[type]);
    const paginationElement = document.getElementById(`${type}TablePagination`) ||
        document.getElementById(`${type}ServicesPagination`);

    if (!paginationElement) return;

    paginationElement.querySelectorAll('[data-page]').forEach(button => {
        button.classList.toggle('active', parseInt(button.getAttribute('data-page')) === currentPage[type]);
    });

    const prevBtn = document.getElementById(`${type}PrevBtn`);
    const nextBtn = document.getElementById(`${type}NextBtn`);

    if (prevBtn) {
        prevBtn.classList.toggle('disabled', currentPage[type] === 1);
    }

    if (nextBtn) {
        nextBtn.classList.toggle('disabled', currentPage[type] === totalPages || totalPages === 0);
    }
}

function setupSearch() {
    const dueTableSearch = document.querySelector('#dueTable .search-input');
    if (dueTableSearch) {
        dueTableSearch.addEventListener('keyup', function() {
            filterTable('dueTable', this.value);
        });
    }

    const serviceTableSearch = document.querySelector('#serviceTable .search-input');
    if (serviceTableSearch) {
        serviceTableSearch.addEventListener('keyup', function() {
            filterTable('serviceTable', this.value);
        });
    }

    const completedServicesSearch = document.getElementById('completedServiceSearch');
    if (completedServicesSearch) {
        completedServicesSearch.addEventListener('keyup', function() {
            filterCompletedServices(this.value);
        });
    }
}

function filterTable(tableId, searchTerm) {
    const table = document.getElementById(tableId);
    if (!table) return;

    const rows = table.querySelectorAll('tbody tr:not([id])');

    searchTerm = searchTerm.toLowerCase();

    if (!searchTerm) {
        if (tableId === 'dueTable') {
            updateDueTablePage();
        } else if (tableId === 'serviceTable') {
            updateServiceTablePage();
        }
        return;
    }

    rows.forEach(row => {
        row.classList.remove('active-page');
    });

    rows.forEach(row => {
        const text = row.textContent.toLowerCase();
        if (text.includes(searchTerm)) {
            row.classList.add('active-page');
        }
    });
}

function filterCompletedServices(searchTerm) {
    const cards = document.querySelectorAll('#completedServicesGrid .service-card');

    searchTerm = searchTerm.toLowerCase();

    if (!searchTerm) {
        updateCompletedServicesPage();
        return;
    }

    cards.forEach(card => {
        card.classList.remove('active-page');
    });

    cards.forEach(card => {
        const text = card.textContent.toLowerCase();
        if (text.includes(searchTerm)) {
            card.classList.add('active-page');
        }
    });
}

function setupModalListeners() {
    const confirmAssignBtn = document.getElementById('confirmAssignBtn');
    if (confirmAssignBtn) {
        confirmAssignBtn.addEventListener('click', assignServiceAdvisor);
    }

    document.addEventListener('click', function(e) {
        if (e.target.closest('.advisor-card')) {
            const card = e.target.closest('.advisor-card');
            document.querySelectorAll('.advisor-card').forEach(c => {
                c.classList.remove('selected');
            });
            card.classList.add('selected');
        }
    });
}

function getToken() {
    return localStorage.getItem('jwt-token') || sessionStorage.getItem('jwt-token');
}

function showSpinner() {
    let spinnerOverlay = document.getElementById('spinnerOverlay');
    if (!spinnerOverlay) {
        spinnerOverlay = document.createElement('div');
        spinnerOverlay.id = 'spinnerOverlay';
        spinnerOverlay.className = 'position-fixed top-0 start-0 w-100 h-100 d-flex justify-content-center align-items-center bg-dark bg-opacity-25';
        spinnerOverlay.style.zIndex = '9999';
        spinnerOverlay.innerHTML = `
            <div class="spinner-border text-wine" role="status">
                <span class="visually-hidden">Loading...</span>
            </div>
        `;
        document.body.appendChild(spinnerOverlay);
    } else {
        spinnerOverlay.classList.add('show');
    }
}

function hideSpinner() {
    const spinnerOverlay = document.getElementById('spinnerOverlay');
    if (spinnerOverlay) {
        spinnerOverlay.classList.remove('show');
        setTimeout(() => {
            if (spinnerOverlay.parentNode) {
                spinnerOverlay.parentNode.removeChild(spinnerOverlay);
            }
        }, 300);
    }
}

function loadDashboardData() {
    showSpinner();

    fetch('/admin/dashboard/api/data', {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + getToken()
        }
    })
        .then(response => {
            if (response.status === 401) {
                window.location.href = '/admin/login?error=session_expired';
                throw new Error('Session expired');
            }
            return response.json();
        })
        .then(data => {
            hideSpinner();

            dashboardStats = data;

            updateDashboardStats();
            renderDueTable();
            renderServiceTable();
            renderCompletedServices();
        })
        .catch(error => {
            hideSpinner();
            showToast('Failed to load dashboard data. Please try again.', 'error');
        });
}

function loadServiceAdvisors() {
    fetch('/admin/service-advisors/api/advisors', {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + getToken()
        }
    })
        .then(response => {
            if (response.status === 401) {
                window.location.href = '/admin/login?error=session_expired';
                throw new Error('Session expired');
            }
            return response.json();
        })
        .then(data => {
            serviceAdvisors = data;
        })
        .catch(error => {
        });
}

function updateDashboardStats() {
    if (!dashboardStats) return;

    const vehiclesDueElement = document.querySelector('.stat-card:nth-child(1) .stat-value');
    const inProgressElement = document.querySelector('.stat-card:nth-child(2) .stat-value');
    const completedElement = document.querySelector('.stat-card:nth-child(3) .stat-value');
    const revenueElement = document.querySelector('.stat-card:nth-child(4) .stat-value');

    if (vehiclesDueElement) {
        vehiclesDueElement.textContent = dashboardStats.vehiclesDue || 0;
    }

    if (inProgressElement) {
        inProgressElement.textContent = dashboardStats.vehiclesInProgress || 0;
    }

    if (completedElement) {
        completedElement.textContent = dashboardStats.vehiclesCompleted || 0;
    }

    if (revenueElement) {
        revenueElement.textContent = '₹' + (dashboardStats.totalRevenue || 0);
    }
}

function renderDueTable() {
    const tableBody = document.querySelector('#dueTable tbody');
    if (!tableBody || !dashboardStats || !dashboardStats.vehiclesDueList) return;

    const existingRows = tableBody.querySelectorAll('tr:not([id])');
    existingRows.forEach(row => row.remove());

    const loadingRow = document.getElementById('loading-row');
    if (loadingRow) {
        loadingRow.style.display = 'none';
    }

    if (dashboardStats.vehiclesDueList.length === 0) {
        const emptyRow = document.getElementById('empty-row');
        if (emptyRow) {
            emptyRow.style.display = 'table-row';
        } else {
            const row = document.createElement('tr');
            row.id = 'empty-row';
            row.innerHTML = `
                <td colspan="5" class="text-center py-4">
                    <div class="no-data-message">
                        <i class="fas fa-car fa-3x mb-3 text-muted"></i>
                        <h5>No vehicles due for service</h5>
                        <p class="text-muted">All vehicles are currently serviced or no pending service requests.</p>
                    </div>
                </td>
            `;
            tableBody.appendChild(row);
        }
        return;
    }

    dashboardStats.vehiclesDueList.forEach((vehicle, index) => {
        const isActivePage = index < itemsPerPage.due;

        const row = document.createElement('tr');
        row.className = isActivePage ? 'active-page' : '';
        row.innerHTML = `
            <td>
                <div class="vehicle-info">
                    <div class="vehicle-icon">
                        <i class="fas fa-${vehicle.category === 'Bike' ? 'motorcycle' : 'car-side'}"></i>
                    </div>
                    <div class="vehicle-details">
                        <h5>${vehicle.vehicleName}</h5>
                        <p>Reg: ${vehicle.registrationNumber}</p>
                    </div>
                </div>
            </td>
            <td>
                <div class="person-info">
                    <div class="person-details">
                        <h5>${vehicle.customerName}</h5>
                        <p>${vehicle.customerEmail || ''}</p>
                    </div>
                    <div class="membership-badge membership-${(vehicle.membershipStatus || 'Standard').toLowerCase()}">
                        <i class="fas fa-${(vehicle.membershipStatus === 'Premium') ? 'crown' : 'user'}"></i>
                        ${vehicle.membershipStatus || 'Standard'}
                    </div>
                </div>
            </td>
            <td>
                <span class="status-badge status-pending">
                    <i class="fas fa-clock"></i>
                    <span>${vehicle.status}</span>
                </span>
            </td>
            <td>${formatDate(vehicle.dueDate)}</td>
            <td class="table-actions-cell">
                <button class="btn-assign"
                        onclick="showAssignAdvisorModal(${vehicle.requestId})">
                    <i class="fas fa-user-plus"></i>
                    Assign
                </button>
            </td>
        `;

        tableBody.appendChild(row);
    });

    updatePaginationUI('due');
}

function renderServiceTable() {
    const tableBody = document.querySelector('#serviceTable tbody');
    if (!tableBody || !dashboardStats || !dashboardStats.vehiclesInServiceList) return;

    const existingRows = tableBody.querySelectorAll('tr:not([id])');
    existingRows.forEach(row => row.remove());

    if (dashboardStats.vehiclesInServiceList.length === 0) {
        const emptyRow = tableBody.querySelector('.empty-row');
        if (emptyRow) {
            emptyRow.style.display = 'table-row';
        } else {
            const row = document.createElement('tr');
            row.className = 'empty-row';
            row.innerHTML = `
                <td colspan="6" class="text-center py-4">
                    <div class="no-data-message">
                        <i class="fas fa-wrench fa-3x mb-3 text-muted"></i>
                        <h5>No vehicles currently in service</h5>
                        <p class="text-muted">There are no vehicles currently being serviced.</p>
                    </div>
                </td>
            `;
            tableBody.appendChild(row);
        }
        return;
    }

    dashboardStats.vehiclesInServiceList.forEach((vehicle, index) => {
        const isActivePage = index < itemsPerPage.inService;

        const row = document.createElement('tr');
        row.className = isActivePage ? 'active-page' : '';
        row.innerHTML = `
            <td>
                <div class="vehicle-info">
                    <div class="vehicle-icon">
                        <i class="fas fa-${vehicle.category === 'Bike' ? 'motorcycle' : 'car-side'}"></i>
                    </div>
                    <div class="vehicle-details">
                        <h5>${vehicle.vehicleName}</h5>
                        <p>Reg: ${vehicle.registrationNumber}</p>
                    </div>
                </div>
            </td>
            <td>
                <div class="person-info">
                    <div class="person-details">
                        <h5>${vehicle.serviceAdvisorName}</h5>
                        <p>ID: ${vehicle.serviceAdvisorId}</p>
                    </div>
                </div>
            </td>
            <td>
                <span class="status-badge status-${vehicle.status.toLowerCase() === 'diagnosis' ? 'progress' : 'progress'}">
                    <i class="fas fa-${vehicle.status.toLowerCase() === 'diagnosis' ? 'stethoscope' : 'wrench'}"></i>
                    <span>${vehicle.status}</span>
                </span>
            </td>
            <td>${formatDate(vehicle.startDate)}</td>
            <td>${formatDate(vehicle.estimatedCompletionDate)}</td>
            <td class="table-actions-cell">
                <button class="btn-premium sm primary"
                        onclick="showServiceRequestDetails(${vehicle.requestId})">
                    <i class="fas fa-eye"></i>
                    Details
                </button>
            </td>
        `;

        tableBody.appendChild(row);
    });

    updatePaginationUI('inService');
}

function renderCompletedServices() {
    const container = document.getElementById('completedServicesGrid');
    if (!container || !dashboardStats || !dashboardStats.completedServicesList) return;

    container.innerHTML = '';

    if (dashboardStats.completedServicesList.length === 0) {
        container.innerHTML = `
            <div class="text-center py-5">
                <i class="fas fa-check-circle fa-3x text-muted mb-3"></i>
                <h5>No completed services found</h5>
                <p class="text-muted">There are no completed service requests available.</p>
            </div>
        `;
        return;
    }

    dashboardStats.completedServicesList.forEach((service, index) => {
        const isActivePage = index < itemsPerPage.completed;

        const card = document.createElement('div');
        card.className = `service-card${isActivePage ? ' active-page' : ''}`;
        card.dataset.page = Math.ceil((index + 1) / itemsPerPage.completed);

        card.innerHTML = `
            <div class="service-card-header">
                <h4 class="service-card-title">
                    <i class="fas fa-car-side"></i>
                    <span>${service.vehicleName}</span>
                </h4>
                <div class="service-status">
                    <div class="status-indicator completed"></div>
                    <div class="status-text completed">Completed</div>
                </div>
            </div>
            <div class="service-card-body">
                <div class="service-meta">
                    <div class="service-meta-item">
                        <div class="service-meta-label">Registration</div>
                        <div class="service-meta-value">${service.registrationNumber}</div>
                    </div>
                    <div class="service-meta-item">
                        <div class="service-meta-label">Completed Date</div>
                        <div class="service-meta-value">${formatDate(service.completedDate)}</div>
                    </div>
                    <div class="service-meta-item">
                        <div class="service-meta-label">Customer</div>
                        <div class="service-meta-value">${service.customerName}</div>
                    </div>
                    <div class="service-meta-item">
                        <div class="service-meta-label">Service Advisor</div>
                        <div class="service-meta-value">${service.serviceAdvisorName}</div>
                    </div>
                </div>
                <div class="price">Total Cost: ₹${service.totalCost.toLocaleString('en-IN', { maximumFractionDigits: 2 })}</div>
            </div>
            <div class="service-card-footer">
                <button class="btn-premium sm secondary view-service-details-btn"
                        data-service-id="${service.serviceId}"
                        onclick="showServiceRequestDetails(${service.serviceId})">
                    <i class="fas fa-eye"></i>
                    View Details
                </button>
                <button class="btn-premium sm primary generate-invoice-btn"
                        data-service-id="${service.serviceId}"
                        data-has-invoice="${service.hasInvoice}"
                        ${service.hasInvoice ? 'onclick="downloadInvoice(' + service.serviceId + ')"' : 'onclick="generateInvoice(' + service.serviceId + ')"'}>
                    <i class="fas fa-file-invoice"></i>
                    ${service.hasInvoice ? 'Download Invoice' : 'Generate Invoice'}
                </button>
            </div>
        `;

        container.appendChild(card);
    });

    updatePaginationUI('completed');
}


function showAssignAdvisorModal(requestId) {
    const request = dashboardStats.vehiclesDueList.find(req => req.requestId === requestId);

    if (!request) {
        showToast('Error: Request not found', 'error');
        return;
    }

    let modal = document.getElementById('assignAdvisorModal');

    if (!modal) {
        const modalHtml = `
        <div class="modal fade premium-modal" id="assignAdvisorModal" tabindex="-1" aria-labelledby="assignAdvisorModalLabel" aria-hidden="true">
            <div class="modal-dialog modal-lg">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title" id="assignAdvisorModalLabel">
                            <i class="fas fa-user-plus"></i>
                            Assign Service Advisor
                        </h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <form>
                        <div class="modal-body">
                            <div class="mb-4 p-3 bg-light rounded-3">
                                <div class="d-flex align-items-center gap-3">
                                    <div class="vehicle-icon">
                                        <i class="fas fa-car"></i>
                                    </div>
                                    <div>
                                        <h4 class="mb-1" id="assignVehicleName"></h4>
                                        <div class="d-flex flex-wrap gap-3">
                                            <p class="mb-0"><strong>Registration:</strong> <span id="assignVehicleReg"></span></p>
                                            <p class="mb-0"><strong>Customer:</strong> <span id="assignCustomerName"></span></p>
                                            <p class="mb-0"><strong>Request ID:</strong> <span id="assignRequestId"></span></p>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <input type="hidden" id="selectedRequestId" value="">

                            <div class="modal-section">
                                <div class="modal-section-title">
                                    <i class="fas fa-user-tie"></i> Available Service Advisors
                                </div>

                                <div id="advisorsList">
                                    <div class="text-center py-3">
                                        <div class="spinner-border text-wine" role="status">
                                            <span class="visually-hidden">Loading advisors...</span>
                                        </div>
                                        <p class="mt-2">Loading service advisors...</p>
                                    </div>
                                </div>
                            </div>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn-premium secondary" data-bs-dismiss="modal">Cancel</button>
                            <button type="button" class="btn-premium primary" id="confirmAssignBtn">
                                <i class="fas fa-check"></i>
                                Confirm Assignment
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>`;

        document.body.insertAdjacentHTML('beforeend', modalHtml);
        modal = document.getElementById('assignAdvisorModal');

        document.getElementById('confirmAssignBtn').addEventListener('click', assignServiceAdvisor);
    }

    document.getElementById('assignVehicleName').textContent = request.vehicleName;
    document.getElementById('assignVehicleReg').textContent = request.registrationNumber;
    document.getElementById('assignCustomerName').textContent = request.customerName;
    document.getElementById('assignRequestId').textContent = `REQ-${request.requestId}`;
    document.getElementById('selectedRequestId').value = request.requestId;

    populateServiceAdvisorsList();

    const bsModal = new bootstrap.Modal(modal);
    bsModal.show();
}

function populateServiceAdvisorsList() {
    const advisorsList = document.getElementById('advisorsList');
    if (!advisorsList) return;

    if (!serviceAdvisors || serviceAdvisors.length === 0) {
        advisorsList.innerHTML = `
        <div class="alert alert-info">
            <i class="fas fa-info-circle me-2"></i>
            No service advisors available. Please add service advisors first.
        </div>
        `;
        return;
    }

    let html = '';

    serviceAdvisors.forEach(advisor => {
        const workloadPercentage = advisor.workloadPercentage ||
            (advisor.activeServices ? Math.min(Math.round((advisor.activeServices / 10) * 100), 100) : 0);

        let workloadClass = 'bg-success';
        if (workloadPercentage > 75) {
            workloadClass = 'bg-danger';
        } else if (workloadPercentage > 50) {
            workloadClass = 'bg-warning';
        }

        const name = advisor.firstName && advisor.lastName ?
            `${advisor.firstName} ${advisor.lastName}` :
            advisor.name || 'Unnamed Advisor';

        const initials = (advisor.firstName ? advisor.firstName.charAt(0) : '') +
            (advisor.lastName ? advisor.lastName.charAt(0) : '');

        html += `
        <div class="advisor-card" data-advisor-id="${advisor.advisorId || advisor.id}">
            <div class="advisor-avatar">${initials || 'SA'}</div>
            <div class="advisor-info">
                <div class="advisor-name">${name}</div>
                <div class="advisor-meta">
                    <div class="advisor-stat">
                        <i class="fas fa-clipboard-list"></i>
                        ${advisor.activeServices || 0} active requests
                    </div>
                    <div class="advisor-stat">
                        <i class="fas fa-id-badge"></i>
                        ${advisor.formattedId || `SA-${String(advisor.advisorId || 0).padStart(3, '0')}`}
                    </div>
                </div>
                <div class="advisor-services">
                    <i class="fas fa-tools"></i>
                    ${advisor.specialization || 'General Service'}
                </div>
                <div class="advisor-workload">
                    <div class="progress">
                        <div class="progress-bar ${workloadClass}" role="progressbar"
                            style="width: ${workloadPercentage}%"
                            aria-valuenow="${workloadPercentage}" aria-valuemin="0" aria-valuemax="100">
                        </div>
                    </div>
                    <div class="workload-text">
                        Workload: ${workloadPercentage}%
                    </div>
                </div>
            </div>
        </div>
        `;
    });

    advisorsList.innerHTML = html;

    document.querySelectorAll('.advisor-card').forEach(card => {
        card.addEventListener('click', function() {
            document.querySelectorAll('.advisor-card').forEach(c => {
                c.classList.remove('selected');
            });
            this.classList.add('selected');
        });
    });
}

function assignServiceAdvisor() {
    const requestId = document.getElementById('selectedRequestId').value;
    const selectedAdvisor = document.querySelector('.advisor-card.selected');

    if (!selectedAdvisor) {
        showToast('Please select a service advisor', 'error');
        return;
    }

    const advisorId = selectedAdvisor.getAttribute('data-advisor-id');

    showSpinner();

    fetch(`/admin/dashboard/api/assign/${requestId}?advisorId=${advisorId}`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + getToken()
        }
    })
        .then(response => {
            if (response.status === 401) {
                window.location.href = '/admin/login?error=session_expired';
                throw new Error('Session expired');
            }

            if (!response.ok) {
                throw new Error('Failed to assign service advisor');
            }
            return response.json();
        })
        .then(data => {
            hideSpinner();

            const modal = bootstrap.Modal.getInstance(document.getElementById('assignAdvisorModal'));
            if (modal) {
                modal.hide();
            }

            showToast('Service advisor assigned successfully!', 'success');

            setTimeout(() => {
                window.location.reload();
            }, 2000);
        })
        .catch(error => {
            hideSpinner();
            showToast('Failed to assign service advisor. Please try again.', 'error');
        });
}

function showServiceRequestDetails(requestId) {
    let request;

    if (dashboardStats && dashboardStats.vehiclesDueList) {
        request = dashboardStats.vehiclesDueList.find(req => req.requestId === requestId);
    }

    if (!request && dashboardStats && dashboardStats.vehiclesInServiceList) {
        request = dashboardStats.vehiclesInServiceList.find(req => req.requestId === requestId);
    }

    if (!request && dashboardStats && dashboardStats.completedServicesList) {
        request = dashboardStats.completedServicesList.find(service => service.serviceId === requestId);
    }

    if (!request) {
        showToast('Error: Request not found', 'error');
        return;
    }

    let modal = document.getElementById('viewServiceRequestModal');
    if (!modal) {
        const modalHtml = `
        <div class="modal fade premium-modal" id="viewServiceRequestModal" tabindex="-1" aria-labelledby="viewServiceRequestModalLabel" aria-hidden="true">
            <div class="modal-dialog modal-lg">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title" id="viewServiceRequestModalLabel">
                            <i class="fas fa-clipboard-list"></i>
                            Service Request Details
                        </h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body">
                        <div class="row mb-4">
                            <div class="col-md-6">
                                <div class="card h-100">
                                    <div class="card-header">
                                        <h6 class="card-title mb-0">Request Information</h6>
                                    </div>
                                    <div class="card-body">
                                        <div class="mb-3">
                                            <label class="form-label text-muted">Request ID</label>
                                            <div id="viewRequestId" class="fw-bold"></div>
                                        </div>
                                        <div class="mb-3">
                                            <label class="form-label text-muted">Status</label>
                                            <div id="viewStatus"></div>
                                        </div>
                                        <div class="mb-3">
                                            <label class="form-label text-muted">Created Date</label>
                                            <div id="viewCreatedDate"></div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            <div class="col-md-6">
                                <div class="card h-100">
                                    <div class="card-header">
                                        <h6 class="card-title mb-0">Vehicle Information</h6>
                                    </div>
                                    <div class="card-body">
                                        <div class="mb-3">
                                            <label class="form-label text-muted">Vehicle Name</label>
                                            <div id="viewVehicleName" class="fw-bold"></div>
                                        </div>
                                        <div class="mb-3">
                                            <label class="form-label text-muted">Registration Number</label>
                                            <div id="viewRegistrationNumber"></div>
                                        </div>
                                        <div class="mb-3">
                                            <label class="form-label text-muted">Category</label>
                                            <div id="viewVehicleCategory"></div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                        <div class="row mb-4">
                            <div class="col-md-6">
                                <div class="card h-100">
                                    <div class="card-header">
                                        <h6 class="card-title mb-0">Customer Information</h6>
                                    </div>
                                    <div class="card-body">
                                        <div class="mb-3">
                                            <label class="form-label text-muted">Customer Name</label>
                                            <div id="viewCustomerName" class="fw-bold"></div>
                                        </div>
                                        <div class="mb-3">
                                            <label class="form-label text-muted">Contact</label>
                                            <div id="viewCustomerContact"></div>
                                        </div>
                                        <div class="mb-3">
                                            <label class="form-label text-muted">Membership</label>
                                            <div id="viewMembership"></div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            <div class="col-md-6">
                                <div class="card h-100">
                                    <div class="card-header">
                                        <h6 class="card-title mb-0">Service Information</h6>
                                    </div>
                                    <div class="card-body">
                                        <div class="mb-3">
                                            <label class="form-label text-muted">Service Type</label>
                                            <div id="viewServiceType" class="fw-bold"></div>
                                        </div>
                                        <div class="mb-3">
                                            <label class="form-label text-muted">Delivery Date</label>
                                            <div id="viewDeliveryDate"></div>
                                        </div>
                                        <div class="mb-3">
                                            <label class="form-label text-muted">Service Advisor</label>
                                            <div id="viewServiceAdvisor"></div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                        <div class="card mb-4">
                            <div class="card-header">
                                <h6 class="card-title mb-0">Service Description</h6>
                            </div>
                            <div class="card-body">
                                <p id="viewDescription"></p>
                            </div>
                        </div>
                        <div class="card mb-4" id="materialsSection" style="display: none;">
                            <div class="card-header">
                                <h6 class="card-title mb-0">Materials Used</h6>
                            </div>
                            <div class="card-body">
                                <div class="table-responsive">
                                    <table class="table table-striped">
                                        <thead>
                                            <tr>
                                                <th>Item</th>
                                                <th>Quantity</th>
                                                <th>Unit Price</th>
                                                <th>Total</th>
                                            </tr>
                                        </thead>
                                        <tbody id="materialsTableBody">
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn-premium secondary" data-bs-dismiss="modal">Close</button>
                    </div>
                </div>
            </div>
        </div>`;

        document.body.insertAdjacentHTML('beforeend', modalHtml);
        modal = document.getElementById('viewServiceRequestModal');
    }

    document.getElementById('viewRequestId').textContent = `REQ-${requestId}`;

    const statusElement = document.getElementById('viewStatus');
    const status = request.status || 'Received';
    statusElement.innerHTML = `
        <span class="status-badge status-${status.toLowerCase() === 'completed' ? 'completed' :
        status.toLowerCase() === 'diagnosis' || status.toLowerCase() === 'repair' ? 'progress' : 'pending'}">
            <i class="fas fa-${
        status === 'Received' ? 'clock' :
            status === 'Diagnosis' ? 'stethoscope' :
                status === 'Repair' ? 'wrench' :
                    'check-circle'
    }"></i>
            ${status}
        </span>
    `;

    document.getElementById('viewCreatedDate').textContent = formatDate(request.createdAt);

    const vehicleName = request.vehicleName ||
        (request.vehicleBrand && request.vehicleModel ?
            request.vehicleBrand + ' ' + request.vehicleModel : 'N/A');
    document.getElementById('viewVehicleName').textContent = vehicleName;

    document.getElementById('viewRegistrationNumber').textContent = request.registrationNumber || 'N/A';
    document.getElementById('viewVehicleCategory').textContent = request.category || 'Car';
    document.getElementById('viewCustomerName').textContent = request.customerName || 'N/A';

    const customerContactElement = document.getElementById('viewCustomerContact');
    if (customerContactElement) {
        customerContactElement.textContent = request.customerEmail || 'N/A';
    }

    const membershipElement = document.getElementById('viewMembership');
    if (membershipElement) {
        const membershipStatus = request.membershipStatus || 'Standard';
        membershipElement.innerHTML = `
            <span class="membership-badge membership-${membershipStatus.toLowerCase()}">
                <i class="fas fa-${membershipStatus === 'Premium' ? 'crown' : 'user'}"></i>
                ${membershipStatus}
            </span>
        `;
    }

    document.getElementById('viewServiceType').textContent = request.serviceType || 'N/A';
    document.getElementById('viewDeliveryDate').textContent = formatDate(request.deliveryDate);
    document.getElementById('viewServiceAdvisor').textContent = request.serviceAdvisorName || 'Not Assigned';
    document.getElementById('viewDescription').textContent = request.additionalDescription || 'No additional description provided.';

    const materialsSection = document.getElementById('materialsSection');
    if (materialsSection) {
        if (request.materials && request.materials.length > 0) {
            materialsSection.style.display = 'block';
            populateMaterialsTable(request.materials);
        } else {
            materialsSection.style.display = 'none';
        }
    }

    const bsModal = new bootstrap.Modal(modal);
    bsModal.show();
}

function populateMaterialsTable(materials) {
    const tableBody = document.getElementById('materialsTableBody');
    if (!tableBody) return;

    tableBody.innerHTML = '';

    let totalCost = 0;

    materials.forEach(material => {
        const row = document.createElement('tr');
        const itemTotal = material.quantity * material.unitPrice;
        totalCost += itemTotal;

        row.innerHTML = `
            <td>${material.name}</td>
            <td>${material.quantity}</td>
            <td>₹${material.unitPrice.toFixed(2)}</td>
            <td>₹${itemTotal.toFixed(2)}</td>
        `;

        tableBody.appendChild(row);
    });

    const totalRow = document.createElement('tr');
    totalRow.className = 'fw-bold';
    totalRow.innerHTML = `
        <td colspan="3" class="text-end">Total</td>
        <td>₹${totalCost.toFixed(2)}</td>
    `;

    tableBody.appendChild(totalRow);
}

function generateInvoice(serviceId) {
    showToast('Invoice generation initiated. Please wait...', 'info');

    setTimeout(() => {
        showToast('Invoice generated successfully!', 'success');

        const button = document.querySelector(`.generate-invoice-btn[data-service-id="${serviceId}"]`);
        if (button) {
            button.innerHTML = '<i class="fas fa-file-invoice"></i> Download Invoice';
            button.setAttribute('onclick', `downloadInvoice(${serviceId})`);
            button.setAttribute('data-has-invoice', 'true');
        }
    }, 2000);
}

function downloadInvoice(serviceId) {
    showToast('Invoice download initiated. Please wait...', 'info');

    setTimeout(() => {
        showToast('Invoice downloaded successfully!', 'success');
    }, 1000);
}

function showToast(message, type = 'success') {
    let toastContainer = document.querySelector('.toast-container');
    if (!toastContainer) {
        toastContainer = document.createElement('div');
        toastContainer.className = 'toast-container position-fixed top-0 end-0 p-3';
        document.body.appendChild(toastContainer);
    }

    const toastId = 'toast-' + Date.now();
    const toastHTML = `
        <div class="toast" role="alert" aria-live="assertive" aria-atomic="true" id="${toastId}">
            <div class="toast-header">
                <strong class="me-auto">
                    <i class="fas fa-${type === 'success' ? 'check-circle text-success' :
        type === 'error' ? 'exclamation-circle text-danger' :
            'info-circle text-info'} me-2"></i>
                    ${type.charAt(0).toUpperCase() + type.slice(1)}
                </strong>
                <button type="button" class="btn-close" data-bs-dismiss="toast" aria-label="Close"></button>
            </div>
            <div class="toast-body">
                ${message}
            </div>
        </div>
    `;

    toastContainer.insertAdjacentHTML('beforeend', toastHTML);

    const toastElement = document.getElementById(toastId);
    const toast = new bootstrap.Toast(toastElement, {
        autohide: true,
        delay: 5000
    });
    toast.show();

    toastElement.addEventListener('hidden.bs.toast', function() {
        toastElement.remove();
    });
}

function formatDate(dateString) {
    if (!dateString) return 'N/A';

    const options = { year: 'numeric', month: 'long', day: 'numeric' };
    return new Date(dateString).toLocaleDateString('en-US', options);
}

function selectAdvisor(card) {
    document.querySelectorAll('.advisor-card').forEach(c => {
        c.classList.remove('selected');
    });
    card.classList.add('selected');
}

window.showAssignAdvisorModal = showAssignAdvisorModal;
window.assignServiceAdvisor = assignServiceAdvisor;
window.showServiceRequestDetails = showServiceRequestDetails;
window.generateInvoice = generateInvoice;
window.downloadInvoice = downloadInvoice;
window.selectAdvisor = selectAdvisor;