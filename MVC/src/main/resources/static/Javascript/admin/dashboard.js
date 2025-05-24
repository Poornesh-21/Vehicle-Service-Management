
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
        isLoading: false,
        apiBaseUrl: window.location.origin
    };

/**
 * Initialize the application on document ready
 */
document.addEventListener('DOMContentLoaded', () => {
    initializeApp();
});

/**
 * Initialize the application with basic setup
 */
function initializeApp() {
    // Set up UI components
    setupMobileMenu();
    setupLogout();
    setupAuthentication();
    setupDateDisplay();
    setupUserName();
    setupEventListeners();

    // Load dashboard data
    loadDashboardData();
}

/**
 * Setup mobile menu toggle functionality
 */
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

        // Close menu on window resize if desktop view
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

/**
 * Setup logout button functionality
 */
function setupLogout() {
    const logoutBtn = document.getElementById('logoutBtn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', () => {
            if (confirm('Are you sure you want to logout?')) {
                // Clear all storage
                localStorage.removeItem("jwt-token");
                sessionStorage.removeItem("jwt-token");
                localStorage.removeItem("user-role");
                localStorage.removeItem("user-name");
                sessionStorage.removeItem("user-role");
                sessionStorage.removeItem("user-name");

                // Redirect to logout page
                window.location.href = '/admin/logout';
            }
        });
    }
}

/**
 * Setup authentication and token handling
 */
function setupAuthentication() {
    const token = getToken();

    if (!token) {
        console.error('No authentication token found');
        window.location.href = '/admin/login?error=session_expired';
        return;
    }

    // Add token to all sidebar links
    document.querySelectorAll('.sidebar-menu-link').forEach(link => {
        if (link.getAttribute('href') && !link.getAttribute('href').includes('token=')) {
            const href = link.getAttribute('href');
            const separator = href.includes('?') ? '&' : '?';
            link.setAttribute('href', href + separator + 'token=' + encodeURIComponent(token));
        }
    });

    // Set up view all buttons with token
    setupViewAllButtons(token);

    // Add token to current URL if not already present
    // Add token to current URL if not already present
    if (window.location.href.indexOf('token=') === -1) {
        const separator = window.location.href.indexOf('?') === -1 ? '?' : '&';
        const newUrl = window.location.href + separator + 'token=' + encodeURIComponent(token);
        window.history.replaceState({}, document.title, newUrl);
    }
}

/**
 * Setup "View All" buttons with token
 */
function setupViewAllButtons(token) {
    if (!token) return;

    const buttons = {
        'viewAllDueBtn': '/admin/service-requests?token=${token}&filter=due',
        'viewAllInServiceBtn': '/admin/under-service?token=${token}',
        'viewAllCompletedBtn': '/admin/completed-services?token=${token}'
    };

    Object.entries(buttons).forEach(([id, url]) => {
        const button = document.getElementById(id);
        if (button) {
            button.href = url.replace('${token}', encodeURIComponent(token));
        }
    });
}

/**
 * Display the username in the sidebar
 */
function setupUserName() {
    const userNameElement = document.getElementById('userName');
    if (userNameElement) {
        const userName = localStorage.getItem("user-name") ||
            sessionStorage.getItem("user-name") ||
            "Administrator";
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
 * Setup event listeners for pagination and search
 */
function setupEventListeners() {
    // Set up pagination for each section
    setupPagination('dueTable', 'dueTablePagination', 'due');
    setupPagination('serviceTable', 'serviceTablePagination', 'inService');
    setupPagination('completedServicesGrid', 'completedServicesPagination', 'completed');

    // Set up search functionality
    setupSearch();

    // Add error retry button handler
    const retryButton = document.getElementById('apiErrorRetry');
    if (retryButton) {
        retryButton.addEventListener('click', () => {
            hideApiError();
            loadDashboardData();
        });
    }
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
            if (dashboardState.currentPage[type] > 1) {
                changePage(dashboardState.currentPage[type] - 1, type);
            }
        });
    }

    // Setup next button
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

/**
 * Setup search functionality
 */
function setupSearch() {
    // Map of search input IDs to their respective table/container IDs
    const searchMappings = {
        'dueTableSearch': { tableId: 'dueTable', type: 'table' },
        'serviceTableSearch': { tableId: 'serviceTable', type: 'table' },
        'completedServiceSearch': { tableId: 'completedServicesGrid', type: 'grid' }
    };

    // Set up each search input
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

/**
 * Load dashboard data from the API
 */
function loadDashboardData() {
    if (dashboardState.isLoading) return;

    dashboardState.isLoading = true;
    showSpinner();
    hideApiError();

    // Check token validity
    const token = getToken();
    if (!token) {
        console.error('No authentication token found when loading data');
        window.location.href = '/admin/login?error=session_expired';
        return;
    }

    console.log('Starting API request to dashboard data endpoint');

    // Make the API request - Fixed: Changed URL to the correct endpoint path
    // The key issue is likely in how we're forming the URL
    const url = `${window.location.origin}/admin/dashboard/api/data`;
    console.log('API Request URL:', url);

    fetch(url, {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + token
        },
        credentials: 'same-origin'
    })
        .then(response => {
            console.log('API Response Status:', response.status);
            console.log('API Response Headers:', [...response.headers.entries()]);

            if (response.status === 401 || response.status === 403) {
                console.error('Authentication error:', response.status);
                window.location.href = '/admin/login?error=session_expired';
                throw new Error('Session expired');
            }

            if (!response.ok) {
                return response.text().then(text => {
                    console.error('API Error Response:', text);
                    try {
                        const errorData = JSON.parse(text);
                        throw new Error(errorData.message || errorData.error || `Server error: ${response.status}`);
                    } catch (e) {
                        throw new Error(`Server error: ${response.status} - ${text || response.statusText}`);
                    }
                });
            }

            console.log('API response received successfully');
            return response.json();
        })
        .then(data => {
            console.log('API Data received:', data ? 'Valid data object' : 'Null or undefined data');
            dashboardState.isLoading = false;
            hideSpinner();

            if (!data) {
                throw new Error('Empty response received from server');
            }

            // Store the data and update the UI
            dashboardState.stats = data;
            updateDashboardUI();
        })
        .catch(error => {
            dashboardState.isLoading = false;
            hideSpinner();

            console.error('Error loading dashboard data:', error);
            showApiError(`Failed to load dashboard data: ${error.message}`);

            // Show mock data for development/debugging
            if (confirm('Error loading data. Would you like to load sample data for testing?')) {
                loadMockData();
            }
        });
}

/**
 * Handle API response with proper error checking
 */
function handleApiResponse(response) {
    // Handle authentication errors
    if (response.status === 401 || response.status === 403) {
        window.location.href = '/admin/login?error=session_expired';
        throw new Error('Session expired');
    }

    // Handle other errors
    if (!response.ok) {
        return response.text().then(text => {
            let errorMessage = `Server returned ${response.status}: ${response.statusText}`;

            // Try to parse as JSON to get more detailed error
            try {
                const errorData = JSON.parse(text);
                if (errorData.message || errorData.error) {
                    errorMessage = errorData.message || errorData.error;
                }
            } catch (e) {
                // If not JSON, use the raw text if it exists
                if (text) errorMessage = text;
            }

            throw new Error(errorMessage);
        });
    }

    // Parse JSON response
    return response.json().catch(error => {
        throw new Error(`Invalid JSON response: ${error.message}`);
    });
}

/**
 * Update all dashboard UI elements with the loaded data
 */
function updateDashboardUI() {
    if (!dashboardState.stats) {
        console.error('No dashboard stats available to update UI');
        return;
    }

    // Update counters and summary information
    updateDashboardStats();

    // Update tables and grids
    renderDueTable();
    renderServiceTable();
    renderCompletedServices();

    // Initialize pagination
    updateAllPagination();
}

/**
 * Update dashboard statistics
 */
function updateDashboardStats() {
    if (!dashboardState.stats) return;

    // Get stat elements
    const elements = {
        vehiclesDue: document.getElementById('vehiclesDueCount'),
        vehiclesInProgress: document.getElementById('vehiclesInProgressCount'),
        vehiclesCompleted: document.getElementById('vehiclesCompletedCount'),
        totalRevenue: document.getElementById('totalRevenueAmount')
    };

    // Update each element if it exists
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

/**
 * Update pagination for all sections
 */
function updateAllPagination() {
    updatePaginationUI('due');
    updatePaginationUI('inService');
    updatePaginationUI('completed');
}

/**
 * Render the due vehicles table
 */
function renderDueTable() {
    const tableBody = document.getElementById('dueTableBody');
    if (!tableBody) return;

    // Remove loading indicator
    const loadingRow = document.getElementById('dueTableLoading');
    if (loadingRow) {
        loadingRow.remove();
    }

    // Get due vehicles list
    const dueList = dashboardState.stats?.vehiclesDueList || [];

    // If no data, show empty state
    if (dueList.length === 0) {
        tableBody.innerHTML = getEmptyStateHTML('No vehicles due for service', 'car',
            'All vehicles are currently serviced or no pending service requests.');
        return;
    }

    // Clear existing rows
    tableBody.innerHTML = '';

    // Create rows for each vehicle
    dueList.forEach((vehicle, index) => {
        const isActivePage = index < dashboardState.itemsPerPage.due;
        const token = getToken();

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
        <a href="/admin/service-requests/${vehicle.requestId}?token=${encodeURIComponent(token)}" 
           class="btn-premium sm primary">
          <i class="fas fa-eye"></i>
          View Details
        </a>
      </td>
    `;

        tableBody.appendChild(row);
    });
}

/**
 * Render the vehicles under service table
 */
function renderServiceTable() {
    const tableBody = document.getElementById('serviceTableBody');
    if (!tableBody) return;

    // Remove loading indicator
    const loadingRow = document.getElementById('serviceTableLoading');
    if (loadingRow) {
        loadingRow.remove();
    }

    // Get in-service vehicles list
    const inServiceList = dashboardState.stats?.vehiclesInServiceList || [];

    // If no data, show empty state
    if (inServiceList.length === 0) {
        tableBody.innerHTML = getEmptyStateHTML('No vehicles currently in service', 'wrench',
            'There are no vehicles currently being serviced.');
        return;
    }

    // Clear existing rows
    tableBody.innerHTML = '';

    // Create rows for each vehicle
    inServiceList.forEach((vehicle, index) => {
        const isActivePage = index < dashboardState.itemsPerPage.inService;
        const token = getToken();

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
        <a href="/admin/under-service/${vehicle.requestId}?token=${encodeURIComponent(token)}" 
           class="btn-premium sm primary">
          <i class="fas fa-eye"></i>
          View Details
        </a>
      </td>
    `;

        tableBody.appendChild(row);
    });
}

/**
 * Render the completed services grid
 */
function renderCompletedServices() {
    const container = document.getElementById('completedServicesGrid');
    if (!container) return;

    // Remove loading indicator
    const loadingElement = document.getElementById('completedServicesLoading');
    if (loadingElement) {
        loadingElement.remove();
    }

    // Get completed services list
    const completedList = dashboardState.stats?.completedServicesList || [];

    // If no data, show empty state
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

    // Clear existing cards
    container.innerHTML = '';

    // Create cards for each completed service
    completedList.forEach((service, index) => {
        const isActivePage = index < dashboardState.itemsPerPage.completed;
        const token = getToken();

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
 * Get total items count for the given type
 */
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

/**
 * Change the current page for pagination
 */
function changePage(page, type) {
    // Update current page
    dashboardState.currentPage[type] = page;

    // Update UI based on type
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

    // Update pagination UI
    updatePaginationUI(type);
}

function updateDueTablePage() {
    const tableRows = document.querySelectorAll('#dueTable tbody tr.data-row');
    const startIndex = (dashboardState.currentPage.due - 1) * dashboardState.itemsPerPage.due;
    const endIndex = startIndex + dashboardState.itemsPerPage.due;

    // Hide all rows first
    tableRows.forEach(row => {
        row.classList.remove('active-page');
    });

    // Show rows for current page
    for (let i = startIndex; i < endIndex && i < tableRows.length; i++) {
        tableRows[i].classList.add('active-page');
    }
}

/**
 * Update the service table page
 */
function updateServiceTablePage() {
    const tableRows = document.querySelectorAll('#serviceTable tbody tr.data-row');
    const startIndex = (dashboardState.currentPage.inService - 1) * dashboardState.itemsPerPage.inService;
    const endIndex = startIndex + dashboardState.itemsPerPage.inService;

    // Hide all rows first
    tableRows.forEach(row => {
        row.classList.remove('active-page');
    });

    // Show rows for current page
    for (let i = startIndex; i < endIndex && i < tableRows.length; i++) {
        tableRows[i].classList.add('active-page');
    }
}

/**
 * Update the completed services page
 */
function updateCompletedServicesPage() {
    const cards = document.querySelectorAll('#completedServicesGrid .service-card');
    const startIndex = (dashboardState.currentPage.completed - 1) * dashboardState.itemsPerPage.completed;
    const endIndex = startIndex + dashboardState.itemsPerPage.completed;

    // Hide all cards first
    cards.forEach(card => {
        card.classList.remove('active-page');
    });

    // Show cards for current page
    for (let i = startIndex; i < endIndex && i < cards.length; i++) {
        cards[i].classList.add('active-page');
    }
}

/**
 * Update the pagination UI
 */
function updatePaginationUI(type) {
    const totalItems = getTotalItemsForType(type);
    const totalPages = Math.ceil(totalItems / dashboardState.itemsPerPage[type]);

    // Find pagination element
    const paginationElement = document.getElementById(`${type}TablePagination`) ||
        document.getElementById(`${type}ServicesPagination`);

    if (!paginationElement) return;

    // Update page buttons
    paginationElement.querySelectorAll('[data-page]').forEach(button => {
        const buttonPage = parseInt(button.getAttribute('data-page'));
        button.classList.toggle('active', buttonPage === dashboardState.currentPage[type]);

        // Hide page buttons that are out of range
        if (buttonPage > totalPages) {
            button.style.display = 'none';
        } else {
            button.style.display = '';
        }
    });

    // Update previous button
    const prevBtn = document.getElementById(`${type}PrevBtn`);
    if (prevBtn) {
        prevBtn.classList.toggle('disabled', dashboardState.currentPage[type] === 1);
    }

    // Update next button
    const nextBtn = document.getElementById(`${type}NextBtn`);
    if (nextBtn) {
        nextBtn.classList.toggle('disabled', dashboardState.currentPage[type] === totalPages || totalPages === 0);
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

function hideSpinner() {
    const spinnerOverlay = document.getElementById('spinnerOverlay');
    if (spinnerOverlay) {
        // First hide it visually for immediate effect
        spinnerOverlay.style.display = 'none';

        // Then completely remove it from the DOM
        spinnerOverlay.parentNode.removeChild(spinnerOverlay);
    }

    // Also remove any loading indicator elements
    const loadingElements = document.querySelectorAll('.loading-data, .loading-message');
    loadingElements.forEach(element => {
        if (element && element.parentNode) {
            element.parentNode.removeChild(element);
        }
    });
}
function showApiError(message) {
    // Check if error container exists, create if not
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
          <button id="apiErrorMock" type="button" class="btn btn-sm btn-outline-secondary">
            <i class="fas fa-database me-1"></i>Use Test Data
          </button>
          <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
      </div>
    `;

        // Insert at top of main content
        const mainContent = document.querySelector('.main-content');
        if (mainContent) {
            mainContent.insertBefore(errorContainer, mainContent.firstChild.nextSibling);
        } else {
            document.body.insertBefore(errorContainer, document.body.firstChild);
        }

        // Set up retry button
        const retryButton = document.getElementById('apiErrorRetry');
        if (retryButton) {
            retryButton.addEventListener('click', () => {
                hideApiError();
                loadDashboardData();
            });
        }

        // Set up mock data button
        const mockButton = document.getElementById('apiErrorMock');
        if (mockButton) {
            mockButton.addEventListener('click', () => {
                hideApiError();
                loadMockData();
            });
        }
    }

    // Update error message
    const errorMessage = document.getElementById('apiErrorMessage');
    if (errorMessage) {
        errorMessage.textContent = message;
    }

    // Show the error container
    errorContainer.style.display = 'block';

    // Log error to console
    console.error('API Error:', message);
}

/**
 * Hide API error message
 */
function hideApiError() {
    const errorContainer = document.getElementById('apiErrorContainer');
    if (errorContainer) {
        errorContainer.style.display = 'none';
    }
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

    // Initialize and show the toast
    const toastElement = document.getElementById(toastId);
    if (toastElement && typeof bootstrap !== 'undefined') {
        const toast = new bootstrap.Toast(toastElement, {
            autohide: true,
            delay: 5000
        });

        toast.show();

        // Remove toast element after it's hidden
        toastElement.addEventListener('hidden.bs.toast', function() {
            toastElement.remove();
        });
    }
}

/**
 * Get the authentication token
 */
function getToken() {
    return localStorage.getItem('jwt-token') || sessionStorage.getItem('jwt-token');
}

/**
 * Escape HTML to prevent XSS
 */
function escapeHTML(str) {
    if (!str) return '';
    return str.toString()
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

/**
 * Ping the API to check connectivity and diagnose issues
 */
function checkApiConnectivity() {
    const token = getToken();
    if (!token) {
        showToast('No authentication token found', 'error');
        return;
    }

    // Create a detailed diagnostics report
    console.log('Running API connectivity diagnostics...');
    console.log('Token present:', !!token);
    console.log('Token length:', token ? token.length : 0);

    // First test a simple endpoint
    const testUrl = `${window.location.origin}/admin/dashboard/api/data`;
    console.log('Testing API endpoint:', testUrl);

    showToast('Testing API connection...', 'info');

    fetch(testUrl, {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + token
        },
        credentials: 'same-origin'
    })
        .then(response => {
            console.log('Response status:', response.status);
            console.log('Response OK:', response.ok);
            console.log('Response headers:', [...response.headers.entries()]);

            if (response.ok) {
                console.log('API connection successful');
                showToast('API connection successful! Status: ' + response.status, 'success');
                return response.json();
            } else {
                console.error('API connection failed:', response.status);
                showToast('API connection failed: ' + response.status, 'error');

                // Try to get error details
                return response.text().then(text => {
                    try {
                        const data = JSON.parse(text);
                        console.error('Error details:', data);
                    } catch (e) {
                        console.error('Raw error response:', text);
                    }
                    throw new Error('API request failed with status: ' + response.status);
                });
            }
        })
        .then(data => {
            console.log('API data structure:', Object.keys(data || {}));

            // Check if data structure is as expected
            const requiredKeys = ['vehiclesDue', 'vehiclesInProgress', 'vehiclesCompleted'];
            const missingKeys = requiredKeys.filter(key => !(key in data));

            if (missingKeys.length) {
                console.warn('API response is missing expected keys:', missingKeys);
                showToast('API response is missing data: ' + missingKeys.join(', '), 'warning');
            } else {
                console.log('API response structure is valid');
                showToast('API data structure is valid', 'success');
            }
        })
        .catch(error => {
            console.error('API connectivity test error:', error);
            showToast('API connectivity error: ' + error.message, 'error');

            // Create detailed error report
            const diagnosticInfo = {
                url: testUrl,
                token: token ? (token.substring(0, 10) + '...' + token.substring(token.length - 5)) : 'none',
                userAgent: navigator.userAgent,
                timestamp: new Date().toISOString(),
                error: error.toString()
            };

            console.error('Diagnostic information:', diagnosticInfo);
        });
}

// Export diagnostic function for console debugging
window.diagnostics = {
    checkApiConnection: checkApiConnectivity,
    reloadDashboard: loadDashboardData,
    getState: () => dashboardState,
    loadMockData: loadMockData,
    fixApiUrl: () => {
        // This function tries to fix potential URL issues
        if (window.location.hostname === 'localhost') {
            dashboardState.apiBaseUrl = window.location.origin;
            console.log('API URL set to:', dashboardState.apiBaseUrl);
            showToast('API URL set to: ' + dashboardState.apiBaseUrl, 'info');
            return dashboardState.apiBaseUrl;
        } else {
            // Try to fix domain issues by detecting correct URL
            const urlParts = window.location.origin.split('.');
            if (urlParts.length > 2) {
                const possibleApiUrl = urlParts[0] + '-api.' + urlParts.slice(1).join('.');
                if (confirm(`Try API URL: ${possibleApiUrl}?`)) {
                    dashboardState.apiBaseUrl = possibleApiUrl;
                    console.log('API URL set to:', dashboardState.apiBaseUrl);
                    showToast('API URL set to: ' + dashboardState.apiBaseUrl, 'info');
                    return dashboardState.apiBaseUrl;
                }
            }
            return dashboardState.apiBaseUrl;
        }
    },
    inspectToken: () => {
        const token = getToken();
        if (!token) {
            console.error('No token found!');
            return 'No token found!';
        }

        // Don't print the full token for security, just show parts
        const tokenPreview = token.substring(0, 10) + '...' + token.substring(token.length - 5);
        console.log('Token preview:', tokenPreview);

        // Try to decode JWT parts
        try {
            const parts = token.split('.');
            if (parts.length === 3) {
                const header = JSON.parse(atob(parts[0]));
                const payload = JSON.parse(atob(parts[1]));

                console.log('Token header:', header);
                console.log('Token payload:', payload);

                // Check expiration
                if (payload.exp) {
                    const expDate = new Date(payload.exp * 1000);
                    const now = new Date();
                    console.log('Token expires:', expDate);
                    console.log('Token expired:', expDate < now);

                    return {
                        valid: expDate > now,
                        expiresAt: expDate.toLocaleString(),
                        subject: payload.sub,
                        issuer: payload.iss
                    };
                }
            }
        } catch (e) {
            console.error('Error decoding token:', e);
        }

        return { tokenPreview };
    }
};