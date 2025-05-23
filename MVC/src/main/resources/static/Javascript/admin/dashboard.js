/**
 * Albany Dashboard JavaScript
 * Optimized to handle dashboard data loading and display
 */

// Dashboard state management
let dashboardStats = null;
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

/**
 * Initialize the application on document ready
 */
document.addEventListener('DOMContentLoaded', () => {
    initializeApp();
    loadDashboardData();
    setupEventListeners();
});

/**
 * Initialize the application with basic setup
 */
function initializeApp() {
    setupMobileMenu();
    setupLogout();
    setupAuthentication();
    setupDateDisplay();
    setupUserName();

    // Setup navigation buttons with token
    const token = getToken();
    if (token) {
        const viewAllDueBtn = document.getElementById('viewAllDueBtn');
        const viewAllInServiceBtn = document.getElementById('viewAllInServiceBtn');
        const viewAllCompletedBtn = document.getElementById('viewAllCompletedBtn');

        if (viewAllDueBtn) {
            viewAllDueBtn.href = `/admin/service-requests?token=${encodeURIComponent(token)}&filter=due`;
        }
        if (viewAllInServiceBtn) {
            viewAllInServiceBtn.href = `/admin/under-service?token=${encodeURIComponent(token)}`;
        }
        if (viewAllCompletedBtn) {
            viewAllCompletedBtn.href = `/admin/completed-services?token=${encodeURIComponent(token)}`;
        }
    }
}

/**
 * Setup mobile menu toggle functionality
 */
function setupMobileMenu() {
    const mobileMenuToggle = document.getElementById('mobileMenuToggle');
    if (mobileMenuToggle) {
        mobileMenuToggle.addEventListener('click', () => {
            document.getElementById('sidebar').classList.toggle('active');
            mobileMenuToggle.querySelector('i').classList.toggle('fa-bars');
            mobileMenuToggle.querySelector('i').classList.toggle('fa-times');
        });
    }

    // Close menu on window resize if desktop view
    window.addEventListener('resize', () => {
        if (window.innerWidth >= 992) {
            document.getElementById('sidebar').classList.remove('active');
            if (mobileMenuToggle) {
                mobileMenuToggle.querySelector('i').classList.remove('fa-times');
                mobileMenuToggle.querySelector('i').classList.add('fa-bars');
            }
        }
    });
}

/**
 * Setup logout button functionality
 */
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

/**
 * Display the username in the sidebar
 */
function setupUserName() {
    const userNameElement = document.getElementById('userName');
    if (userNameElement) {
        const userName = localStorage.getItem("user-name") || sessionStorage.getItem("user-name") || "Administrator";
        userNameElement.textContent = userName;
    }
}

/**
 * Display the current date
 */
function setupDateDisplay() {
    const dateElement = document.getElementById('current-date');
    if (dateElement) {
        const options = { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' };
        const today = new Date();
        dateElement.textContent = today.toLocaleDateString('en-US', options);
    }
}

/**
 * Setup authentication and token handling
 */
function setupAuthentication() {
    const token = getToken();

    if (token) {
        // Ensure all links have the token
        document.querySelectorAll('.sidebar-menu-link').forEach(link => {
            if (link.getAttribute('href') && !link.getAttribute('href').includes('token=')) {
                const href = link.getAttribute('href');
                const separator = href.includes('?') ? '&' : '?';
                link.setAttribute('href', href + separator + 'token=' + encodeURIComponent(token));
            }
        });

        // Add token to current URL if not already present
        if (window.location.href.indexOf('token=') === -1) {
            const separator = window.location.href.indexOf('?') === -1 ? '?' : '&';
            const newUrl = window.location.href + separator + 'token=' + encodeURIComponent(token);
            window.history.replaceState({}, document.title, newUrl);
        }
    } else {
        window.location.href = '/admin/login?error=session_expired';
    }
}

/**
 * Setup event listeners for pagination and search
 */
function setupEventListeners() {
    setupPagination('dueTable', 'dueTablePagination', 'due');
    setupPagination('serviceTable', 'serviceTablePagination', 'inService');
    setupPagination('completedServicesGrid', 'completedServicesPagination', 'completed');
    setupSearch();
}

/**
 * Setup pagination for the given table
 */
function setupPagination(tableId, paginationId, type) {
    const paginationElement = document.getElementById(paginationId);
    if (!paginationElement) return;

    // Setup page buttons
    paginationElement.querySelectorAll('[data-page]').forEach(button => {
        button.addEventListener('click', function(e) {
            e.preventDefault();
            const page = parseInt(this.getAttribute('data-page'));
            changePage(page, type);
        });
    });

    // Setup previous button
    const prevBtn = document.getElementById(`${type}PrevBtn`);
    if (prevBtn) {
        prevBtn.addEventListener('click', function(e) {
            e.preventDefault();
            if (currentPage[type] > 1) {
                changePage(currentPage[type] - 1, type);
            }
        });
    }

    // Setup next button
    const nextBtn = document.getElementById(`${type}NextBtn`);
    if (nextBtn) {
        nextBtn.addEventListener('click', function(e) {
            e.preventDefault();
            const totalItems = getTotalItemsForType(type);
            const totalPages = Math.ceil(totalItems / itemsPerPage[type]);

            if (currentPage[type] < totalPages) {
                changePage(currentPage[type] + 1, type);
            }
        });
    }
}

/**
 * Get total items count for the given type
 */
function getTotalItemsForType(type) {
    if (!dashboardStats) return 0;

    switch (type) {
        case 'due':
            return dashboardStats.vehiclesDueList?.length || 0;
        case 'inService':
            return dashboardStats.vehiclesInServiceList?.length || 0;
        case 'completed':
            return dashboardStats.completedServicesList?.length || 0;
        default:
            return 0;
    }
}

/**
 * Change the current page for pagination
 */
function changePage(page, type) {
    currentPage[type] = page;

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

/**
 * Update the due table page
 */
function updateDueTablePage() {
    const tableRows = document.querySelectorAll('#dueTable tbody tr.data-row');
    const startIndex = (currentPage.due - 1) * itemsPerPage.due;
    const endIndex = startIndex + itemsPerPage.due;

    tableRows.forEach(row => {
        row.classList.remove('active-page');
    });

    for (let i = startIndex; i < endIndex && i < tableRows.length; i++) {
        tableRows[i].classList.add('active-page');
    }
}

/**
 * Update the service table page
 */
function updateServiceTablePage() {
    const tableRows = document.querySelectorAll('#serviceTable tbody tr.data-row');
    const startIndex = (currentPage.inService - 1) * itemsPerPage.inService;
    const endIndex = startIndex + itemsPerPage.inService;

    tableRows.forEach(row => {
        row.classList.remove('active-page');
    });

    for (let i = startIndex; i < endIndex && i < tableRows.length; i++) {
        tableRows[i].classList.add('active-page');
    }
}

/**
 * Update the completed services page
 */
function updateCompletedServicesPage() {
    const cards = document.querySelectorAll('#completedServicesGrid .service-card');
    const startIndex = (currentPage.completed - 1) * itemsPerPage.completed;
    const endIndex = startIndex + itemsPerPage.completed;

    cards.forEach(card => {
        card.classList.remove('active-page');
    });

    for (let i = startIndex; i < endIndex && i < cards.length; i++) {
        cards[i].classList.add('active-page');
    }
}

/**
 * Update the pagination UI
 */
function updatePaginationUI(type) {
    const totalItems = getTotalItemsForType(type);
    const totalPages = Math.ceil(totalItems / itemsPerPage[type]);

    const paginationElement = document.getElementById(`${type}TablePagination`) ||
        document.getElementById(`${type}ServicesPagination`);

    if (!paginationElement) return;

    // Update page buttons
    paginationElement.querySelectorAll('[data-page]').forEach(button => {
        button.classList.toggle('active', parseInt(button.getAttribute('data-page')) === currentPage[type]);
    });

    // Update previous button
    const prevBtn = document.getElementById(`${type}PrevBtn`);
    if (prevBtn) {
        prevBtn.classList.toggle('disabled', currentPage[type] === 1);
    }

    // Update next button
    const nextBtn = document.getElementById(`${type}NextBtn`);
    if (nextBtn) {
        nextBtn.classList.toggle('disabled', currentPage[type] === totalPages || totalPages === 0);
    }
}

/**
 * Setup search functionality
 */
function setupSearch() {
    // Due table search
    const dueTableSearch = document.getElementById('dueTableSearch');
    if (dueTableSearch) {
        dueTableSearch.addEventListener('keyup', function() {
            filterTable('dueTable', this.value);
        });
    }

    // Service table search
    const serviceTableSearch = document.getElementById('serviceTableSearch');
    if (serviceTableSearch) {
        serviceTableSearch.addEventListener('keyup', function() {
            filterTable('serviceTable', this.value);
        });
    }

    // Completed services search
    const completedServicesSearch = document.getElementById('completedServiceSearch');
    if (completedServicesSearch) {
        completedServicesSearch.addEventListener('keyup', function() {
            filterCompletedServices(this.value);
        });
    }
}

/**
 * Filter table by search term
 */
function filterTable(tableId, searchTerm) {
    const tableBody = document.querySelector(`#${tableId} tbody`);
    if (!tableBody) return;

    const rows = tableBody.querySelectorAll('.data-row');
    searchTerm = searchTerm.toLowerCase();

    // If no search term, reset to paginated view
    if (!searchTerm) {
        if (tableId === 'dueTable') {
            updateDueTablePage();
        } else if (tableId === 'serviceTable') {
            updateServiceTablePage();
        }
        return;
    }

    // Hide all rows first
    rows.forEach(row => {
        row.classList.remove('active-page');
    });

    // Show rows matching search term
    rows.forEach(row => {
        const text = row.textContent.toLowerCase();
        if (text.includes(searchTerm)) {
            row.classList.add('active-page');
        }
    });
}

/**
 * Filter completed services by search term
 */
function filterCompletedServices(searchTerm) {
    const cards = document.querySelectorAll('#completedServicesGrid .service-card');
    searchTerm = searchTerm.toLowerCase();

    // If no search term, reset to paginated view
    if (!searchTerm) {
        updateCompletedServicesPage();
        return;
    }

    // Hide all cards first
    cards.forEach(card => {
        card.classList.remove('active-page');
    });

    // Show cards matching search term
    cards.forEach(card => {
        const text = card.textContent.toLowerCase();
        if (text.includes(searchTerm)) {
            card.classList.add('active-page');
        }
    });
}

/**
 * Get the authentication token
 */
function getToken() {
    return localStorage.getItem('jwt-token') || sessionStorage.getItem('jwt-token');
}

/**
 * Show a loading spinner
 */
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
        spinnerOverlay.style.display = 'flex';
    }
}

/**
 * Hide the loading spinner
 */
function hideSpinner() {
    const spinnerOverlay = document.getElementById('spinnerOverlay');
    if (spinnerOverlay) {
        spinnerOverlay.style.display = 'none';
    }
}

/**
 * Load dashboard data from the API
 */
function loadDashboardData() {
    showSpinner();

    // IMPORTANT: Using the revised API endpoint path
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
            if (!response.ok) {
                throw new Error(`Server returned ${response.status}: ${response.statusText}`);
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
            showApiError('Failed to load dashboard data: ' + error.message);
            console.error('Error loading dashboard data:', error);
        });
}

/**
 * Show API error message
 */
function showApiError(message) {
    const errorContainer = document.getElementById('apiErrorContainer');
    const errorMessage = document.getElementById('apiErrorMessage');

    if (errorContainer && errorMessage) {
        errorMessage.textContent = message;
        errorContainer.style.display = 'block';
    }
}

/**
 * Update dashboard statistics
 */
function updateDashboardStats() {
    if (!dashboardStats) return;

    // Update counters
    const vehiclesDueCount = document.getElementById('vehiclesDueCount');
    const vehiclesInProgressCount = document.getElementById('vehiclesInProgressCount');
    const vehiclesCompletedCount = document.getElementById('vehiclesCompletedCount');
    const totalRevenueAmount = document.getElementById('totalRevenueAmount');

    if (vehiclesDueCount) vehiclesDueCount.textContent = dashboardStats.vehiclesDue || 0;
    if (vehiclesInProgressCount) vehiclesInProgressCount.textContent = dashboardStats.vehiclesInProgress || 0;
    if (vehiclesCompletedCount) vehiclesCompletedCount.textContent = dashboardStats.vehiclesCompleted || 0;
    if (totalRevenueAmount) totalRevenueAmount.textContent = '₹' + (dashboardStats.totalRevenue || 0);
}

/**
 * Render the due vehicles table
 */
function renderDueTable() {
    const tableBody = document.getElementById('dueTableBody');
    if (!tableBody || !dashboardStats || !dashboardStats.vehiclesDueList) return;

    // Remove loading indicator
    const loadingRow = document.getElementById('dueTableLoading');
    if (loadingRow) {
        loadingRow.remove();
    }

    // If no data, show empty state
    if (dashboardStats.vehiclesDueList.length === 0) {
        tableBody.innerHTML = `
            <tr>
                <td colspan="5" class="text-center py-4">
                    <div class="no-data-message">
                        <i class="fas fa-car fa-3x mb-3 text-muted"></i>
                        <h5>No vehicles due for service</h5>
                        <p class="text-muted">All vehicles are currently serviced or no pending service requests.</p>
                    </div>
                </td>
            </tr>
        `;
        return;
    }

    // Clear existing rows
    tableBody.innerHTML = '';

    // Create rows for each vehicle
    dashboardStats.vehiclesDueList.forEach((vehicle, index) => {
        const isActivePage = index < itemsPerPage.due;
        const token = getToken();

        const row = document.createElement('tr');
        row.className = `data-row${isActivePage ? ' active-page' : ''}`;
        row.dataset.page = Math.ceil((index + 1) / itemsPerPage.due);

        row.innerHTML = `
            <td>
                <div class="vehicle-info">
                    <div class="vehicle-icon">
                        <i class="fas fa-${vehicle.category === 'Bike' ? 'motorcycle' : 'car-side'}"></i>
                    </div>
                    <div class="vehicle-details">
                        <h5>${vehicle.vehicleName || 'Unknown Vehicle'}</h5>
                        <p>Reg: ${vehicle.registrationNumber || 'N/A'}</p>
                    </div>
                </div>
            </td>
            <td>
                <div class="person-info">
                    <div class="person-details">
                        <h5>${vehicle.customerName || 'Unknown Customer'}</h5>
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
                    <span>${vehicle.status || 'Pending'}</span>
                </span>
            </td>
            <td>${formatDate(vehicle.dueDate)}</td>
            <td class="table-actions-cell">
                <a href="/admin/service-requests/${vehicle.requestId}?token=${encodeURIComponent(token)}" 
                   class="btn-premium sm primary">
                    <i class="fas fa-eye"></i>
                    View Details
                </a>
            </td>
        `;

        tableBody.appendChild(row);
    });

    updatePaginationUI('due');
}

/**
 * Render the vehicles under service table
 */
function renderServiceTable() {
    const tableBody = document.getElementById('serviceTableBody');
    if (!tableBody || !dashboardStats || !dashboardStats.vehiclesInServiceList) return;

    // Remove loading indicator
    const loadingRow = document.getElementById('serviceTableLoading');
    if (loadingRow) {
        loadingRow.remove();
    }

    // If no data, show empty state
    if (dashboardStats.vehiclesInServiceList.length === 0) {
        tableBody.innerHTML = `
            <tr>
                <td colspan="6" class="text-center py-4">
                    <div class="no-data-message">
                        <i class="fas fa-wrench fa-3x mb-3 text-muted"></i>
                        <h5>No vehicles currently in service</h5>
                        <p class="text-muted">There are no vehicles currently being serviced.</p>
                    </div>
                </td>
            </tr>
        `;
        return;
    }

    // Clear existing rows
    tableBody.innerHTML = '';

    // Create rows for each vehicle
    dashboardStats.vehiclesInServiceList.forEach((vehicle, index) => {
        const isActivePage = index < itemsPerPage.inService;
        const token = getToken();

        const row = document.createElement('tr');
        row.className = `data-row${isActivePage ? ' active-page' : ''}`;
        row.dataset.page = Math.ceil((index + 1) / itemsPerPage.inService);

        row.innerHTML = `
            <td>
                <div class="vehicle-info">
                    <div class="vehicle-icon">
                        <i class="fas fa-${vehicle.category === 'Bike' ? 'motorcycle' : 'car-side'}"></i>
                    </div>
                    <div class="vehicle-details">
                        <h5>${vehicle.vehicleName || 'Unknown Vehicle'}</h5>
                        <p>Reg: ${vehicle.registrationNumber || 'N/A'}</p>
                    </div>
                </div>
            </td>
            <td>
                <div class="person-info">
                    <div class="person-details">
                        <h5>${vehicle.serviceAdvisorName || 'Not Assigned'}</h5>
                        <p>${vehicle.serviceAdvisorId ? 'ID: ' + vehicle.serviceAdvisorId : ''}</p>
                    </div>
                </div>
            </td>
            <td>
                <span class="status-badge status-${getStatusClass(vehicle.status)}">
                    <i class="fas fa-${getStatusIcon(vehicle.status)}"></i>
                    <span>${vehicle.status || 'In Progress'}</span>
                </span>
            </td>
            <td>${formatDate(vehicle.startDate)}</td>
            <td>${formatDate(vehicle.estimatedCompletionDate)}</td>
            <td class="table-actions-cell">
                <a href="/admin/under-service/${vehicle.requestId}?token=${encodeURIComponent(token)}" 
                   class="btn-premium sm primary">
                    <i class="fas fa-eye"></i>
                    View Details
                </a>
            </td>
        `;

        tableBody.appendChild(row);
    });

    updatePaginationUI('inService');
}

/**
 * Get CSS class for status badge
 */
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

/**
 * Get icon for status badge
 */
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

/**
 * Render the completed services grid
 */
function renderCompletedServices() {
    const container = document.getElementById('completedServicesGrid');
    if (!container || !dashboardStats || !dashboardStats.completedServicesList) return;

    // Remove loading indicator
    const loadingElement = document.getElementById('completedServicesLoading');
    if (loadingElement) {
        loadingElement.remove();
    }

    // If no data, show empty state
    if (dashboardStats.completedServicesList.length === 0) {
        container.innerHTML = `
            <div class="text-center py-5 w-100">
                <i class="fas fa-check-circle fa-3x text-muted mb-3"></i>
                <h5>No completed services found</h5>
                <p class="text-muted">There are no completed service requests available.</p>
            </div>
        `;
        return;
    }

    // Clear existing cards
    container.innerHTML = '';

    // Create cards for each completed service
    dashboardStats.completedServicesList.forEach((service, index) => {
        const isActivePage = index < itemsPerPage.completed;
        const token = getToken();

        const card = document.createElement('div');
        card.className = `service-card${isActivePage ? ' active-page' : ''}`;
        card.dataset.page = Math.ceil((index + 1) / itemsPerPage.completed);

        card.innerHTML = `
            <div class="service-card-header">
                <h4 class="service-card-title">
                    <i class="fas fa-${service.category === 'Bike' ? 'motorcycle' : 'car-side'}"></i>
                    <span>${service.vehicleName || 'Unknown Vehicle'}</span>
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
                        <div class="service-meta-value">${service.registrationNumber || 'N/A'}</div>
                    </div>
                    <div class="service-meta-item">
                        <div class="service-meta-label">Completed Date</div>
                        <div class="service-meta-value">${formatDate(service.completedDate)}</div>
                    </div>
                    <div class="service-meta-item">
                        <div class="service-meta-label">Customer</div>
                        <div class="service-meta-value">${service.customerName || 'Unknown Customer'}</div>
                    </div>
                    <div class="service-meta-item">
                        <div class="service-meta-label">Service Advisor</div>
                        <div class="service-meta-value">${service.serviceAdvisorName || 'Not Assigned'}</div>
                    </div>
                </div>
                <div class="price">Total Cost: ₹${formatCurrency(service.totalCost || 0)}</div>
            </div>
            <div class="service-card-footer">
                <a href="/admin/completed-services/${service.serviceId || service.requestId}?token=${encodeURIComponent(token)}" 
                   class="btn-premium sm secondary">
                    <i class="fas fa-eye"></i>
                    View Details
                </a>
                <a href="/admin/completed-services/${service.serviceId || service.requestId}/invoice?token=${encodeURIComponent(token)}" 
                   class="btn-premium sm primary">
                    <i class="fas fa-file-invoice"></i>
                    ${service.hasInvoice ? 'View Invoice' : 'Generate Invoice'}
                </a>
            </div>
        `;

        container.appendChild(card);
    });

    updatePaginationUI('completed');
}

/**
 * Format date for display
 */
function formatDate(dateString) {
    if (!dateString) return 'N/A';

    try {
        const options = { year: 'numeric', month: 'long', day: 'numeric' };
        return new Date(dateString).toLocaleDateString('en-US', options);
    } catch (e) {
        console.error('Error formatting date:', e);
        return dateString;
    }
}

/**
 * Format currency for display
 */
function formatCurrency(value) {
    if (value === null || value === undefined) return '0.00';

    try {
        return Number(value).toLocaleString('en-IN', {
            maximumFractionDigits: 2,
            minimumFractionDigits: 2
        });
    } catch (e) {
        console.error('Error formatting currency:', e);
        return value.toString();
    }
}

/**
 * Show a toast notification
 */
function showToast(message, type = 'success') {
    let toastContainer = document.querySelector('.toast-container');
    if (!toastContainer) return;

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