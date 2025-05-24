const dashboardState = {
    stats: null,
    currentPage: {
        due: 1,
        inService: 1,
        completed: 1
    },
    itemsPerPage: {
        due: 5,
        inService: 5,
        completed: 3
    },
    isLoading: false
};

document.addEventListener('DOMContentLoaded', () => {
    initializeApp();
});

function initializeApp() {
    setupMobileMenu();
    setupLogout();
    setupEventListeners();
    setupDateDisplay();
    setupUserName();
    loadDashboardData();
}

function setupMobileMenu() {
    const mobileMenuToggle = document.getElementById('mobileMenuToggle');
    const sidebar = document.getElementById('sidebar');

    if (mobileMenuToggle && sidebar) {
        mobileMenuToggle.addEventListener('click', () => {
            sidebar.classList.toggle('active');
            const icon = mobileMenuToggle.querySelector('i');
            if (icon) {
                icon.classList.toggle('fa-bars');
                icon.classList.toggle('fa-times');
            }
        });

        window.addEventListener('resize', () => {
            if (window.innerWidth >= 992 && sidebar.classList.contains('active')) {
                sidebar.classList.remove('active');
                const icon = mobileMenuToggle.querySelector('i');
                if (icon) {
                    icon.classList.remove('fa-times');
                    icon.classList.add('fa-bars');
                }
            }
        });
    }
}

function setupLogout() {
    const logoutBtn = document.getElementById('logoutBtn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', () => {
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

function setupUserName() {
    const userNameElement = document.getElementById('userName');
    if (userNameElement) {
        const userName = localStorage.getItem("user-name") ||
            sessionStorage.getItem("user-name") ||
            "Administrator";
        userNameElement.textContent = userName;
    }
}

function setupEventListeners() {
    setupPagination('dueTable', 'dueTablePagination', 'due');
    setupPagination('serviceTable', 'serviceTablePagination', 'inService');
    setupPagination('completedServicesGrid', 'completedServicesPagination', 'completed');
    setupSearch();
    setupViewAllButtons();

    const retryButton = document.getElementById('apiErrorRetry');
    if (retryButton) {
        retryButton.addEventListener('click', () => {
            hideApiError();
            loadDashboardData();
        });
    }
}

function setupViewAllButtons() {
    const buttons = {
        'viewAllDueBtn': '/admin/service-requests?filter=due',
        'viewAllInServiceBtn': '/admin/under-service',
        'viewAllCompletedBtn': '/admin/completed-services'
    };

    Object.entries(buttons).forEach(([id, url]) => {
        const button = document.getElementById(id);
        if (button) {
            button.href = url;
        }
    });
}

function setupPagination(tableId, paginationId, type) {
    const paginationElement = document.getElementById(paginationId);
    if (!paginationElement) return;

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
            if (dashboardState.currentPage[type] > 1) {
                changePage(dashboardState.currentPage[type] - 1, type);
            }
        });
    }

    const nextBtn = document.getElementById(`${type}NextBtn`);
    if (nextBtn) {
        nextBtn.addEventListener('click', function(e) {
            e.preventDefault();
            const totalItems = getTotalItemsForType(type);
            const totalPages = Math.ceil(totalItems / dashboardState.itemsPerPage[type]);

            if (dashboardState.currentPage[type] < totalPages) {
                changePage(dashboardState.currentPage[type] + 1, type);
            }
        });
    }
}

function setupSearch() {
    const searchMappings = {
        'dueTableSearch': { tableId: 'dueTable', type: 'table' },
        'serviceTableSearch': { tableId: 'serviceTable', type: 'table' },
        'completedServiceSearch': { tableId: 'completedServicesGrid', type: 'grid' }
    };

    Object.entries(searchMappings).forEach(([searchId, config]) => {
        const searchInput = document.getElementById(searchId);
        if (searchInput) {
            searchInput.addEventListener('keyup', function() {
                const searchTerm = this.value.trim();
                if (config.type === 'table') {
                    filterTable(config.tableId, searchTerm);
                } else {
                    filterCompletedServices(searchTerm);
                }
            });
        }
    });
}

function loadDashboardData() {
    if (dashboardState.isLoading) return;

    dashboardState.isLoading = true;
    showSpinner();
    hideApiError();

    const token = getToken();
    if (!token) {
        console.error('No authentication token found when loading data');
        window.location.href = '/admin/login?error=session_expired';
        return;
    }

    const url = `${window.location.origin}/admin/dashboard/api/data`;

    fetch(url, {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + token
        },
        credentials: 'same-origin'
    })
        .then(response => {
            if (response.status === 401 || response.status === 403) {
                window.location.href = '/admin/login?error=session_expired';
                throw new Error('Session expired');
            }

            if (!response.ok) {
                return response.text().then(text => {
                    try {
                        const errorData = JSON.parse(text);
                        throw new Error(errorData.message || errorData.error || `Server error: ${response.status}`);
                    } catch (e) {
                        throw new Error(`Server error: ${response.status} - ${text || response.statusText}`);
                    }
                });
            }

            return response.json();
        })
        .then(data => {
            dashboardState.isLoading = false;
            hideSpinner();

            if (!data) {
                throw new Error('Empty response received from server');
            }

            dashboardState.stats = data;
            updateDashboardUI();
        })
        .catch(error => {
            dashboardState.isLoading = false;
            hideSpinner();
            console.error('Error loading dashboard data:', error);
            showApiError(`Failed to load dashboard data: ${error.message}`);
        });
}

function updateDashboardUI() {
    if (!dashboardState.stats) {
        console.error('No dashboard stats available to update UI');
        return;
    }

    updateDashboardStats();
    renderDueTable();
    renderServiceTable();
    renderCompletedServices();
    updateAllPagination();
}

function updateDashboardStats() {
    if (!dashboardState.stats) return;

    const elements = {
        vehiclesDue: document.getElementById('vehiclesDueCount'),
        vehiclesInProgress: document.getElementById('vehiclesInProgressCount'),
        vehiclesCompleted: document.getElementById('vehiclesCompletedCount'),
        totalRevenue: document.getElementById('totalRevenueAmount')
    };

    if (elements.vehiclesDue) {
        elements.vehiclesDue.textContent = dashboardState.stats.vehiclesDue || 0;
    }

    if (elements.vehiclesInProgress) {
        elements.vehiclesInProgress.textContent = dashboardState.stats.vehiclesInProgress || 0;
    }

    if (elements.vehiclesCompleted) {
        elements.vehiclesCompleted.textContent = dashboardState.stats.vehiclesCompleted || 0;
    }

    if (elements.totalRevenue) {
        elements.totalRevenue.textContent = '₹' + formatCurrency(dashboardState.stats.totalRevenue || 0);
    }
}

function updateAllPagination() {
    updatePaginationUI('due');
    updatePaginationUI('inService');
    updatePaginationUI('completed');
}

function renderDueTable() {
    const tableBody = document.getElementById('dueTableBody');
    if (!tableBody) return;

    const loadingRow = document.getElementById('dueTableLoading');
    if (loadingRow) {
        loadingRow.remove();
    }

    const dueList = dashboardState.stats?.vehiclesDueList || [];

    if (dueList.length === 0) {
        tableBody.innerHTML = getEmptyStateHTML('No vehicles due for service', 'car',
            'All vehicles are currently serviced or no pending service requests.');
        return;
    }

    tableBody.innerHTML = '';

    dueList.forEach((vehicle, index) => {
        const isActivePage = index < dashboardState.itemsPerPage.due;

        const row = document.createElement('tr');
        row.className = `data-row${isActivePage ? ' active-page' : ''}`;
        row.dataset.page = Math.ceil((index + 1) / dashboardState.itemsPerPage.due);

        row.innerHTML = `
      <td>
        <div class="vehicle-info">
          <div class="vehicle-icon">
            <i class="fas fa-${vehicle.category === 'Bike' ? 'motorcycle' : 'car-side'}"></i>
          </div>
          <div class="vehicle-details">
            <h5>${escapeHTML(vehicle.vehicleName || 'Unknown Vehicle')}</h5>
            <p>Reg: ${escapeHTML(vehicle.registrationNumber || 'N/A')}</p>
          </div>
        </div>
      </td>
      <td>
        <div class="person-info">
          <div class="person-details">
            <h5>${escapeHTML(vehicle.customerName || 'Unknown Customer')}</h5>
            <p>${escapeHTML(vehicle.customerEmail || '')}</p>
          </div>
          <div class="membership-badge membership-${(vehicle.membershipStatus || 'Standard').toLowerCase()}">
            <i class="fas fa-${(vehicle.membershipStatus === 'Premium') ? 'crown' : 'user'}"></i>
            ${escapeHTML(vehicle.membershipStatus || 'Standard')}
          </div>
        </div>
      </td>
      <td>
        <span class="status-badge status-pending">
          <i class="fas fa-clock"></i>
          <span>${escapeHTML(vehicle.status || 'Pending')}</span>
        </span>
      </td>
      <td>${formatDate(vehicle.dueDate)}</td>
      <td class="table-actions-cell">
        <a href="/admin/service-requests/${vehicle.requestId}" 
           class="btn-premium sm primary">
          <i class="fas fa-eye"></i>
          View Details
        </a>
      </td>
    `;

        tableBody.appendChild(row);
    });
}

function renderServiceTable() {
    const tableBody = document.getElementById('serviceTableBody');
    if (!tableBody) return;

    const loadingRow = document.getElementById('serviceTableLoading');
    if (loadingRow) {
        loadingRow.remove();
    }

    const inServiceList = dashboardState.stats?.vehiclesInServiceList || [];

    if (inServiceList.length === 0) {
        tableBody.innerHTML = getEmptyStateHTML('No vehicles currently in service', 'wrench',
            'There are no vehicles currently being serviced.');
        return;
    }

    tableBody.innerHTML = '';

    inServiceList.forEach((vehicle, index) => {
        const isActivePage = index < dashboardState.itemsPerPage.inService;

        const row = document.createElement('tr');
        row.className = `data-row${isActivePage ? ' active-page' : ''}`;
        row.dataset.page = Math.ceil((index + 1) / dashboardState.itemsPerPage.inService);

        row.innerHTML = `
      <td>
        <div class="vehicle-info">
          <div class="vehicle-icon">
            <i class="fas fa-${vehicle.category === 'Bike' ? 'motorcycle' : 'car-side'}"></i>
          </div>
          <div class="vehicle-details">
            <h5>${escapeHTML(vehicle.vehicleName || 'Unknown Vehicle')}</h5>
            <p>Reg: ${escapeHTML(vehicle.registrationNumber || 'N/A')}</p>
          </div>
        </div>
      </td>
      <td>
        <div class="person-info">
          <div class="person-details">
            <h5>${escapeHTML(vehicle.serviceAdvisorName || 'Not Assigned')}</h5>
            <p>${vehicle.serviceAdvisorId ? 'ID: ' + vehicle.serviceAdvisorId : ''}</p>
          </div>
        </div>
      </td>
      <td>
        <span class="status-badge status-${getStatusClass(vehicle.status)}">
          <i class="fas fa-${getStatusIcon(vehicle.status)}"></i>
          <span>${escapeHTML(vehicle.status || 'In Progress')}</span>
        </span>
      </td>
      <td>${formatDate(vehicle.startDate)}</td>
      <td>${formatDate(vehicle.estimatedCompletionDate)}</td>
      <td class="table-actions-cell">
        <a href="/admin/under-service/${vehicle.requestId}" 
           class="btn-premium sm primary">
          <i class="fas fa-eye"></i>
          View Details
        </a>
      </td>
    `;

        tableBody.appendChild(row);
    });
}

function renderCompletedServices() {
    const container = document.getElementById('completedServicesGrid');
    if (!container) return;

    const loadingElement = document.getElementById('completedServicesLoading');
    if (loadingElement) {
        loadingElement.remove();
    }

    const completedList = dashboardState.stats?.completedServicesList || [];

    if (completedList.length === 0) {
        container.innerHTML = `
      <div class="text-center py-5 w-100">
        <i class="fas fa-check-circle fa-3x text-muted mb-3"></i>
        <h5>No completed services found</h5>
        <p class="text-muted">There are no completed service requests available.</p>
      </div>
    `;
        return;
    }

    container.innerHTML = '';

    completedList.forEach((service, index) => {
        const isActivePage = index < dashboardState.itemsPerPage.completed;

        const card = document.createElement('div');
        card.className = `service-card${isActivePage ? ' active-page' : ''}`;
        card.dataset.page = Math.ceil((index + 1) / dashboardState.itemsPerPage.completed);

        card.innerHTML = `
      <div class="service-card-header">
        <h4 class="service-card-title">
          <i class="fas fa-${service.category === 'Bike' ? 'motorcycle' : 'car-side'}"></i>
          <span>${escapeHTML(service.vehicleName || 'Unknown Vehicle')}</span>
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
            <div class="service-meta-value">${escapeHTML(service.registrationNumber || 'N/A')}</div>
          </div>
          <div class="service-meta-item">
            <div class="service-meta-label">Completed Date</div>
            <div class="service-meta-value">${formatDate(service.completedDate)}</div>
          </div>
          <div class="service-meta-item">
            <div class="service-meta-label">Customer</div>
            <div class="service-meta-value">${escapeHTML(service.customerName || 'Unknown Customer')}</div>
          </div>
          <div class="service-meta-item">
            <div class="service-meta-label">Service Advisor</div>
            <div class="service-meta-value">${escapeHTML(service.serviceAdvisorName || 'Not Assigned')}</div>
          </div>
        </div>
        <div class="price">Total Cost: ₹${formatCurrency(service.totalCost || 0)}</div>
      </div>
      <div class="service-card-footer">
        <a href="/admin/completed-services/${service.serviceId || service.requestId}" 
           class="btn-premium sm secondary">
          <i class="fas fa-eye"></i>
          View Details
        </a>
        <a href="/admin/completed-services/${service.serviceId || service.requestId}/invoice" 
           class="btn-premium sm primary">
          <i class="fas fa-file-invoice"></i>
          ${service.hasInvoice ? 'View Invoice' : 'Generate Invoice'}
        </a>
      </div>
    `;

        container.appendChild(card);
    });
}

function getStatusClass(status) {
    if (!status) return 'progress';

    switch (status.toLowerCase()) {
        case 'received':
            return 'pending';
        case 'completed':
            return 'completed';
        default:
            return 'progress';
    }
}

function getStatusIcon(status) {
    if (!status) return 'spinner';

    switch (status.toLowerCase()) {
        case 'received':
            return 'clock';
        case 'diagnosis':
            return 'stethoscope';
        case 'repair':
            return 'wrench';
        case 'completed':
            return 'check-circle';
        default:
            return 'spinner';
    }
}

function getTotalItemsForType(type) {
    if (!dashboardState.stats) return 0;

    switch (type) {
        case 'due':
            return dashboardState.stats.vehiclesDueList?.length || 0;
        case 'inService':
            return dashboardState.stats.vehiclesInServiceList?.length || 0;
        case 'completed':
            return dashboardState.stats.completedServicesList?.length || 0;
        default:
            return 0;
    }
}

function changePage(page, type) {
    dashboardState.currentPage[type] = page;

    switch (type) {
        case 'due':
            updateDueTablePage();
            break;
        case 'inService':
            updateServiceTablePage();
            break;
        case 'completed':
            updateCompletedServicesPage();
            break;
    }

    updatePaginationUI(type);
}

function updateDueTablePage() {
    const tableRows = document.querySelectorAll('#dueTable tbody tr.data-row');
    const startIndex = (dashboardState.currentPage.due - 1) * dashboardState.itemsPerPage.due;
    const endIndex = startIndex + dashboardState.itemsPerPage.due;

    tableRows.forEach(row => {
        row.classList.remove('active-page');
    });

    for (let i = startIndex; i < endIndex && i < tableRows.length; i++) {
        tableRows[i].classList.add('active-page');
    }
}

function updateServiceTablePage() {
    const tableRows = document.querySelectorAll('#serviceTable tbody tr.data-row');
    const startIndex = (dashboardState.currentPage.inService - 1) * dashboardState.itemsPerPage.inService;
    const endIndex = startIndex + dashboardState.itemsPerPage.inService;

    tableRows.forEach(row => {
        row.classList.remove('active-page');
    });

    for (let i = startIndex; i < endIndex && i < tableRows.length; i++) {
        tableRows[i].classList.add('active-page');
    }
}

function updateCompletedServicesPage() {
    const cards = document.querySelectorAll('#completedServicesGrid .service-card');
    const startIndex = (dashboardState.currentPage.completed - 1) * dashboardState.itemsPerPage.completed;
    const endIndex = startIndex + dashboardState.itemsPerPage.completed;

    cards.forEach(card => {
        card.classList.remove('active-page');
    });

    for (let i = startIndex; i < endIndex && i < cards.length; i++) {
        cards[i].classList.add('active-page');
    }
}

function updatePaginationUI(type) {
    const totalItems = getTotalItemsForType(type);
    const totalPages = Math.ceil(totalItems / dashboardState.itemsPerPage[type]);

    const paginationElement = document.getElementById(`${type}TablePagination`) ||
        document.getElementById(`${type}ServicesPagination`);

    if (!paginationElement) return;

    paginationElement.querySelectorAll('[data-page]').forEach(button => {
        const buttonPage = parseInt(button.getAttribute('data-page'));
        button.classList.toggle('active', buttonPage === dashboardState.currentPage[type]);

        if (buttonPage > totalPages) {
            button.style.display = 'none';
        } else {
            button.style.display = '';
        }
    });

    const prevBtn = document.getElementById(`${type}PrevBtn`);
    if (prevBtn) {
        prevBtn.classList.toggle('disabled', dashboardState.currentPage[type] === 1);
    }

    const nextBtn = document.getElementById(`${type}NextBtn`);
    if (nextBtn) {
        nextBtn.classList.toggle('disabled', dashboardState.currentPage[type] === totalPages || totalPages === 0);
    }
}

function filterTable(tableId, searchTerm) {
    const tableBody = document.querySelector(`#${tableId} tbody`);
    if (!tableBody) return;

    const rows = tableBody.querySelectorAll('.data-row');
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

function getEmptyStateHTML(title, icon, message) {
    return `
    <tr>
      <td colspan="6" class="text-center py-4">
        <div class="no-data-message">
          <i class="fas fa-${icon} fa-3x mb-3 text-muted"></i>
          <h5>${title}</h5>
          <p class="text-muted">${message}</p>
        </div>
      </td>
    </tr>
  `;
}

function showSpinner() {
    let spinnerOverlay = document.getElementById('spinnerOverlay');
    if (!spinnerOverlay) {
        spinnerOverlay = document.createElement('div');
        spinnerOverlay.id = 'spinnerOverlay';
        spinnerOverlay.className = 'spinner-overlay';
        spinnerOverlay.innerHTML = `
      <div class="spinner-container">
        <div class="albany-spinner">
          <div class="spinner-letter">A</div>
          <div class="spinner-circle"></div>
          <div class="spinner-circle"></div>
        </div>
        <div class="spinner-text">Loading...</div>
      </div>
    `;
        document.body.appendChild(spinnerOverlay);
    } else {
        spinnerOverlay.style.display = 'flex';
    }
}

function hideSpinner() {
    const spinnerOverlay = document.getElementById('spinnerOverlay');
    if (spinnerOverlay) {
        spinnerOverlay.style.display = 'none';
        spinnerOverlay.parentNode.removeChild(spinnerOverlay);
    }

    const loadingElements = document.querySelectorAll('.loading-data, .loading-message');
    loadingElements.forEach(element => {
        if (element && element.parentNode) {
            element.parentNode.removeChild(element);
        }
    });
}

function showApiError(message) {
    let errorContainer = document.getElementById('apiErrorContainer');
    if (!errorContainer) {
        errorContainer = document.createElement('div');
        errorContainer.id = 'apiErrorContainer';
        errorContainer.className = 'alert alert-danger alert-dismissible fade show mb-4';
        errorContainer.setAttribute('role', 'alert');

        errorContainer.innerHTML = `
      <div class="d-flex align-items-center">
        <i class="fas fa-exclamation-triangle me-2"></i>
        <div class="flex-grow-1">
          <span id="apiErrorMessage"></span>
        </div>
        <div class="d-flex gap-2">
          <button id="apiErrorRetry" type="button" class="btn btn-sm btn-outline-danger">
            <i class="fas fa-sync-alt me-1"></i>Retry
          </button>
          <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
      </div>
    `;

        const mainContent = document.querySelector('.main-content');
        if (mainContent) {
            mainContent.insertBefore(errorContainer, mainContent.firstChild.nextSibling);
        } else {
            document.body.insertBefore(errorContainer, document.body.firstChild);
        }

        const retryButton = document.getElementById('apiErrorRetry');
        if (retryButton) {
            retryButton.addEventListener('click', () => {
                hideApiError();
                loadDashboardData();
            });
        }
    }

    const errorMessage = document.getElementById('apiErrorMessage');
    if (errorMessage) {
        errorMessage.textContent = message;
    }

    errorContainer.style.display = 'block';
}

function hideApiError() {
    const errorContainer = document.getElementById('apiErrorContainer');
    if (errorContainer) {
        errorContainer.style.display = 'none';
    }
}

function formatDate(dateString) {
    if (!dateString) return 'N/A';

    try {
        const options = { year: 'numeric', month: 'long', day: 'numeric' };
        return new Date(dateString).toLocaleDateString('en-US', options);
    } catch (e) {
        return dateString;
    }
}

function formatCurrency(value) {
    if (value === null || value === undefined) return '0.00';

    try {
        return Number(value).toLocaleString('en-IN', {
            maximumFractionDigits: 2,
            minimumFractionDigits: 2
        });
    } catch (e) {
        return value.toString();
    }
}

function showToast(message, type = 'success') {
    let toastContainer = document.querySelector('.toast-container');
    if (!toastContainer) return;

    const toastId = 'toast-' + Date.now();
    const iconClass = type === 'success' ? 'check-circle text-success' :
        type === 'error' ? 'exclamation-circle text-danger' :
            'info-circle text-info';

    const toastHTML = `
    <div class="toast" role="alert" aria-live="assertive" aria-atomic="true" id="${toastId}">
      <div class="toast-header">
        <strong class="me-auto">
          <i class="fas fa-${iconClass} me-2"></i>
          ${type.charAt(0).toUpperCase() + type.slice(1)}
        </strong>
        <button type="button" class="btn-close" data-bs-dismiss="toast" aria-label="Close"></button>
      </div>
      <div class="toast-body">
        ${escapeHTML(message)}
      </div>
    </div>
  `;

    toastContainer.insertAdjacentHTML('beforeend', toastHTML);

    const toastElement = document.getElementById(toastId);
    if (toastElement && typeof bootstrap !== 'undefined') {
        const toast = new bootstrap.Toast(toastElement, {
            autohide: true,
            delay: 5000
        });

        toast.show();

        toastElement.addEventListener('hidden.bs.toast', function() {
            toastElement.remove();
        });
    }
}

function getToken() {
    return localStorage.getItem('jwt-token') || sessionStorage.getItem('jwt-token');
}

function escapeHTML(str) {
    if (!str) return '';
    return str.toString()
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

function makeAuthenticatedRequest(url, options = {}) {
    const token = getToken();

    if (!token) {
        window.location.href = '/admin/login?error=session_expired';
        return Promise.reject(new Error('Authentication token not found'));
    }

    options.headers = options.headers || {};
    options.headers['Authorization'] = 'Bearer ' + token;

    return fetch(url, options)
        .then(response => {
            if (response.status === 401 || response.status === 403) {
                window.location.href = '/admin/login?error=session_expired';
                throw new Error('Session expired');
            }
            return response;
        });
}