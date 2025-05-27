/**
 * Albany Service Dashboard
 * Main dashboard module for Albany Service Admin Portal
 */

// Dashboard state management
const dashboardState = {
    stats: {
        vehiclesDue: 0,
        vehiclesInProgress: 0,
        vehiclesCompleted: 0,
        totalRevenue: 0,
        vehiclesDueList: [],
        vehiclesInServiceList: [],
        completedServicesList: []
    },
    pagination: {
        due: { current: 1, itemsPerPage: 5, total: 0 },
        inService: { current: 1, itemsPerPage: 5, total: 0 },
        completed: { current: 1, itemsPerPage: 3, total: 0 }
    },
    filters: {
        dueSearchTerm: '',
        inServiceSearchTerm: '',
        completedSearchTerm: ''
    },
    loading: false
};

// Initialize the application
document.addEventListener('DOMContentLoaded', () => {
    initializeApp();
});

/**
 * Application initialization
 */
function initializeApp() {
    setupMobileMenu();
    setupLogout();
    setupEventListeners();
    setupDateDisplay();
    setupUserName();
    loadDashboardData();
}

/**
 * Setup mobile menu toggle
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

        // Close sidebar on window resize if screen becomes large
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
 * Setup logout functionality
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
 * Setup event listeners for dashboard interactions
 */
function setupEventListeners() {
    // Setup pagination for each section
    setupPagination('dueTable', 'dueTablePagination', 'due');
    setupPagination('serviceTable', 'serviceTablePagination', 'inService');
    setupPagination('completedServicesGrid', 'completedServicesPagination', 'completed');

    // Setup search functionality
    setupSearch();

    // Setup View All buttons with correct URLs
    setupViewAllButtons();

    // Setup retry button for API errors
    const retryButton = document.getElementById('apiErrorRetry');
    if (retryButton) {
        retryButton.addEventListener('click', () => {
            hideApiError();
            loadDashboardData();
        });
    }
}

/**
 * Setup pagination controls for each section
 * @param {string} tableId - Table ID
 * @param {string} paginationId - Pagination container ID
 * @param {string} section - Section identifier (due, inService, completed)
 */
function setupPagination(tableId, paginationId, section) {
    const paginationElement = document.getElementById(paginationId);
    if (!paginationElement) return;

    // Page number buttons
    paginationElement.querySelectorAll('[data-page]').forEach(button => {
        button.addEventListener('click', function(e) {
            e.preventDefault();
            const page = parseInt(this.getAttribute('data-page'));
            changePage(page, section);
        });
    });

    // Previous button
    const prevBtn = document.getElementById(`${section}PrevBtn`);
    if (prevBtn) {
        prevBtn.addEventListener('click', function(e) {
            e.preventDefault();
            if (dashboardState.pagination[section].current > 1) {
                changePage(dashboardState.pagination[section].current - 1, section);
            }
        });
    }

    // Next button
    const nextBtn = document.getElementById(`${section}NextBtn`);
    if (nextBtn) {
        nextBtn.addEventListener('click', function(e) {
            e.preventDefault();
            const totalPages = Math.ceil(dashboardState.pagination[section].total / dashboardState.pagination[section].itemsPerPage);
            if (dashboardState.pagination[section].current < totalPages) {
                changePage(dashboardState.pagination[section].current + 1, section);
            }
        });
    }
}

/**
 * Setup search functionality for all sections
 */
function setupSearch() {
    // Map search inputs to their respective tables/grids
    const searchMappings = {
        'dueTableSearch': { section: 'due', type: 'table' },
        'serviceTableSearch': { section: 'inService', type: 'table' },
        'completedServiceSearch': { section: 'completed', type: 'grid' }
    };

    // Add event listeners to all search inputs
    Object.entries(searchMappings).forEach(([searchId, config]) => {
        const searchInput = document.getElementById(searchId);
        if (searchInput) {
            searchInput.addEventListener('keyup', function() {
                const searchTerm = this.value.trim();
                dashboardState.filters[`${config.section}SearchTerm`] = searchTerm;

                if (config.type === 'table') {
                    filterTable(config.section);
                } else {
                    filterCompletedServices(searchTerm);
                }
            });
        }
    });
}

/**
 * Filter table rows based on search term
 * @param {string} section - Section identifier (due, inService)
 */
function filterTable(section) {
    const searchTerm = dashboardState.filters[`${section}SearchTerm`].toLowerCase();

    // If search term is empty, reset to normal pagination view
    if (!searchTerm) {
        dashboardState.pagination[section].current = 1;
        renderSection(section);
        updatePaginationUI(section);
        return;
    }

    // Filter the data based on section
    let filteredData = [];

    if (section === 'due') {
        filteredData = dashboardState.stats.vehiclesDueList.filter(item =>
            objectContainsSearchTerm(item, searchTerm)
        );
    } else if (section === 'inService') {
        filteredData = dashboardState.stats.vehiclesInServiceList.filter(item =>
            objectContainsSearchTerm(item, searchTerm)
        );
    }

    // Update pagination for the filtered data
    dashboardState.pagination[section].total = filteredData.length;
    dashboardState.pagination[section].current = 1;

    // Render the filtered data
    if (section === 'due') {
        renderDueTable(filteredData);
    } else if (section === 'inService') {
        renderServiceTable(filteredData);
    }

    updatePaginationUI(section);
}

/**
 * Check if an object contains the search term in any of its string properties
 * @param {Object} obj - Object to search in
 * @param {string} searchTerm - Term to search for
 * @returns {boolean} - True if object contains the search term
 */
function objectContainsSearchTerm(obj, searchTerm) {
    if (!obj) return false;

    return Object.values(obj).some(value => {
        if (value === null || value === undefined) return false;

        if (typeof value === 'string') {
            return value.toLowerCase().includes(searchTerm);
        }

        if (typeof value === 'object') {
            return objectContainsSearchTerm(value, searchTerm);
        }

        return String(value).toLowerCase().includes(searchTerm);
    });
}

/**
 * Filter completed services grid based on search term
 * @param {string} searchTerm - Term to search for
 */
function filterCompletedServices(searchTerm) {
    searchTerm = searchTerm.toLowerCase();

    // If search term is empty, reset to normal pagination view
    if (!searchTerm) {
        dashboardState.pagination.completed.current = 1;
        renderCompletedServices();
        updatePaginationUI('completed');
        return;
    }

    // Filter completed services
    const filteredServices = dashboardState.stats.completedServicesList.filter(service =>
        objectContainsSearchTerm(service, searchTerm)
    );

    // Update pagination
    dashboardState.pagination.completed.total = filteredServices.length;
    dashboardState.pagination.completed.current = 1;

    // Render filtered data
    renderCompletedServices(filteredServices);
    updatePaginationUI('completed');
}

/**
 * Setup "View All" buttons to link to correct pages
 */
function setupViewAllButtons() {
    const buttonMappings = {
        'viewAllDueBtn': '/admin/service-requests?filter=due',
        'viewAllInServiceBtn': '/admin/under-service',
        'viewAllCompletedBtn': '/admin/completed-services'
    };

    Object.entries(buttonMappings).forEach(([id, url]) => {
        const button = document.getElementById(id);
        if (button) {
            button.href = url;
        }
    });
}

/**
 * Setup current date display
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
 * Setup user name display
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
 * Load dashboard data from API
 */
function loadDashboardData() {
    if (dashboardState.loading) return;

    dashboardState.loading = true;
    showSpinner();
    hideApiError();

    const token = getToken();
    if (!token) {
        console.error('No authentication token found');
        window.location.href = '/admin/login?error=session_expired';
        return;
    }

    fetch(`${window.location.origin}/admin/dashboard/api/data`, {
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
            dashboardState.loading = false;
            hideSpinner();

            if (!data) {
                throw new Error('Empty response received from server');
            }

            // Process the data to ensure proper categorization
            processAndCategorizeData(data);

            // Save processed data to state
            dashboardState.stats = data;

            // Update pagination totals
            dashboardState.pagination.due.total = data.vehiclesDueList?.length || 0;
            dashboardState.pagination.inService.total = data.vehiclesInServiceList?.length || 0;
            dashboardState.pagination.completed.total = data.completedServicesList?.length || 0;

            // Update UI
            updateDashboardUI();
        })
        .catch(error => {
            dashboardState.loading = false;
            hideSpinner();
            console.error('Error loading dashboard data:', error);
            showApiError(`Failed to load dashboard data: ${error.message}`);
        });
}

/**
 * Process and categorize service requests
 * @param {Object} data - Dashboard data from API
 */
function processAndCategorizeData(data) {
    console.log("Original data from server:", JSON.stringify(data));

    // Initialize arrays if they don't exist
    if (!data.vehiclesDueList) data.vehiclesDueList = [];
    if (!data.vehiclesInServiceList) data.vehiclesInServiceList = [];
    if (!data.completedServicesList) data.completedServicesList = [];

    // Track completed services separately - they should remain in completedServicesList
    const completedServices = [...data.completedServicesList];

    // Combine all non-completed service requests into a single array for reprocessing
    const allServiceRequests = [];

    // Add all requests from vehiclesDueList
    if (Array.isArray(data.vehiclesDueList)) {
        data.vehiclesDueList.forEach(request => {
            // Skip if it's already in completed services
            if (request.status === 'Completed') {
                if (!completedServices.some(s => s.requestId === request.requestId)) {
                    completedServices.push(request);
                }
            } else {
                allServiceRequests.push({...request, source: 'due'});
            }
        });
    }

    // Add all requests from vehiclesInServiceList
    if (Array.isArray(data.vehiclesInServiceList)) {
        data.vehiclesInServiceList.forEach(request => {
            // Skip if it's already in completed services
            if (request.status === 'Completed') {
                if (!completedServices.some(s => s.requestId === request.requestId)) {
                    completedServices.push(request);
                }
            } else if (!allServiceRequests.some(r => r.requestId === request.requestId)) {
                allServiceRequests.push({...request, source: 'inService'});
            }
        });
    }

    console.log("Combined service requests:", allServiceRequests.length);

    // Create new empty lists
    const newDueList = [];
    const newInServiceList = [];

    // Process each request and categorize it correctly
    allServiceRequests.forEach(request => {
        // Check for service advisor assignment - CRITICAL PART
        // If service_advisor_id exists OR serviceAdvisorId exists OR serviceAdvisor object exists,
        // then the request should go to Under Service
        const hasServiceAdvisor =
            // Check for service_advisor_id (database field) which is critical
            request.service_advisor_id !== undefined && request.service_advisor_id !== null ||
            // Direct property checks for variations
            request.serviceAdvisorId !== undefined && request.serviceAdvisorId !== null ||
            // Check for serviceAdvisorName (but exclude "Not Assigned")
            (typeof request.serviceAdvisorName === 'string' &&
                !request.serviceAdvisorName.toLowerCase().includes('not assigned') &&
                request.serviceAdvisorName.trim() !== '') ||
            // Check for serviceAdvisor object
            (request.serviceAdvisor && (
                request.serviceAdvisor.advisorId ||
                request.serviceAdvisor.id ||
                (typeof request.serviceAdvisor === 'number' && request.serviceAdvisor > 0) ||
                (request.serviceAdvisor.user && (
                    request.serviceAdvisor.user.firstName ||
                    request.serviceAdvisor.user.lastName
                ))
            ));

        console.log(`Request ID: ${request.requestId}, Has Service Advisor: ${hasServiceAdvisor}, Raw service_advisor_id: ${request.service_advisor_id}, serviceAdvisorId: ${request.serviceAdvisorId}`);

        // Properly categorize
        if (hasServiceAdvisor) {
            // Add to under service list
            if (!newInServiceList.some(r => r.requestId === request.requestId)) {
                newInServiceList.push(request);
            }
        } else {
            // Add to due for service list
            if (!newDueList.some(r => r.requestId === request.requestId)) {
                newDueList.push(request);
            }
        }
    });

    console.log("New categorization - Due for service: " + newDueList.length + ", Under service: " + newInServiceList.length);

    // Replace the original lists with our newly categorized lists
    data.vehiclesDueList = newDueList;
    data.vehiclesInServiceList = newInServiceList;
    data.completedServicesList = completedServices;

    // Update the counts
    data.vehiclesDue = newDueList.length;
    data.vehiclesInProgress = newInServiceList.length;
    data.vehiclesCompleted = completedServices.length;

    console.log("Recategorized data:", {
        dueCount: data.vehiclesDue,
        inProgressCount: data.vehiclesInProgress,
        completedCount: data.vehiclesCompleted
    });
}

/**
 * Update dashboard UI with loaded data
 */
function updateDashboardUI() {
    if (!dashboardState.stats) {
        console.error('No dashboard stats available to update UI');
        return;
    }

    // Update statistics
    updateDashboardStats();

    // Render all sections
    renderDueTable();
    renderServiceTable();
    renderCompletedServices();

    // Update pagination for all sections
    updatePaginationUI('due');
    updatePaginationUI('inService');
    updatePaginationUI('completed');
}

/**
 * Update dashboard statistics counters
 */
function updateDashboardStats() {
    if (!dashboardState.stats) return;

    // Map element IDs to stat properties
    const elements = {
        vehiclesDue: document.getElementById('vehiclesDueCount'),
        vehiclesInProgress: document.getElementById('vehiclesInProgressCount'),
        vehiclesCompleted: document.getElementById('vehiclesCompletedCount'),
        totalRevenue: document.getElementById('totalRevenueAmount')
    };

    // Update each element with its corresponding stat
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
 * Render the due vehicles table
 * @param {Array} customData - Optional filtered data to display
 */
function renderDueTable(customData) {
    const tableBody = document.getElementById('dueTableBody');
    if (!tableBody) return;

    // Remove loading indicator
    const loadingRow = document.getElementById('dueTableLoading');
    if (loadingRow) {
        loadingRow.remove();
    }

    // Get data to display (either filtered or all)
    const dueList = customData || dashboardState.stats?.vehiclesDueList || [];

    // Show empty state if no data
    if (dueList.length === 0) {
        tableBody.innerHTML = getEmptyStateHTML('No vehicles due for service', 'car',
            'All vehicles are currently serviced or no pending service requests.');
        return;
    }

    // Clear previous content
    tableBody.innerHTML = '';

    // Calculate pagination
    const { current, itemsPerPage } = dashboardState.pagination.due;
    const startIndex = (current - 1) * itemsPerPage;
    const endIndex = Math.min(startIndex + itemsPerPage, dueList.length);
    const itemsToShow = dueList.slice(startIndex, endIndex);

    // Create rows for each item
    itemsToShow.forEach(vehicle => {
        const row = document.createElement('tr');
        row.className = 'data-row active-page';

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
    `;

        tableBody.appendChild(row);
    });
}

/**
 * Render the vehicles under service table
 * @param {Array} customData - Optional filtered data to display
 */
function renderServiceTable(customData) {
    const tableBody = document.getElementById('serviceTableBody');
    if (!tableBody) return;

    // Remove loading indicator
    const loadingRow = document.getElementById('serviceTableLoading');
    if (loadingRow) {
        loadingRow.remove();
    }

    // Get data to display (either filtered or all)
    const inServiceList = customData || dashboardState.stats?.vehiclesInServiceList || [];

    // Show empty state if no data
    if (inServiceList.length === 0) {
        tableBody.innerHTML = getEmptyStateHTML('No vehicles currently in service', 'wrench',
            'There are no vehicles currently being serviced.');
        return;
    }

    // Clear previous content
    tableBody.innerHTML = '';

    // Calculate pagination
    const { current, itemsPerPage } = dashboardState.pagination.inService;
    const startIndex = (current - 1) * itemsPerPage;
    const endIndex = Math.min(startIndex + itemsPerPage, inServiceList.length);
    const itemsToShow = inServiceList.slice(startIndex, endIndex);

    // Create rows for each item
    itemsToShow.forEach(vehicle => {
        const row = document.createElement('tr');
        row.className = 'data-row active-page';

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
    `;

        tableBody.appendChild(row);
    });
}

/**
 * Render the completed services grid
 * @param {Array} customData - Optional filtered data to display
 */
function renderCompletedServices(customData) {
    const container = document.getElementById('completedServicesGrid');
    if (!container) return;

    // Remove loading indicator
    const loadingElement = document.getElementById('completedServicesLoading');
    if (loadingElement) {
        loadingElement.remove();
    }

    // Get data to display (either filtered or all)
    const completedList = customData || dashboardState.stats?.completedServicesList || [];

    // Show empty state if no data
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

    // Clear previous content
    container.innerHTML = '';

    // Calculate pagination
    const { current, itemsPerPage } = dashboardState.pagination.completed;
    const startIndex = (current - 1) * itemsPerPage;
    const endIndex = Math.min(startIndex + itemsPerPage, completedList.length);
    const itemsToShow = completedList.slice(startIndex, endIndex);

    // Create cards for each item
    itemsToShow.forEach(service => {
        const card = document.createElement('div');
        card.className = 'service-card active-page';

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
    `;

        container.appendChild(card);
    });
}

/**
 * Change current page for a section
 * @param {number} page - Page number to navigate to
 * @param {string} section - Section identifier (due, inService, completed)
 */
function changePage(page, section) {
    dashboardState.pagination[section].current = page;
    renderSection(section);
    updatePaginationUI(section);
}

/**
 * Render a specific section based on section name
 * @param {string} section - Section to render (due, inService, completed)
 */
function renderSection(section) {
    switch(section) {
        case 'due':
            renderDueTable();
            break;
        case 'inService':
            renderServiceTable();
            break;
        case 'completed':
            renderCompletedServices();
            break;
        default:
            console.error('Unknown section:', section);
    }
}

/**
 * Update pagination UI for a section
 * @param {string} section - Section identifier (due, inService, completed)
 */
function updatePaginationUI(section) {
    const { current, itemsPerPage, total } = dashboardState.pagination[section];
    const totalPages = Math.ceil(total / itemsPerPage) || 1;

    // Get pagination container
    const paginationId = section === 'completed' ?
        'completedServicesPagination' :
        `${section}TablePagination`;
    const paginationElement = document.getElementById(paginationId);
    if (!paginationElement) return;

    // Update page number buttons
    paginationElement.querySelectorAll('[data-page]').forEach(button => {
        const buttonPage = parseInt(button.getAttribute('data-page'));
        button.classList.toggle('active', buttonPage === current);

        // Hide page numbers beyond total pages
        if (buttonPage > totalPages) {
            button.style.display = 'none';
        } else {
            button.style.display = '';
        }
    });

    // Update previous button state
    const prevBtn = document.getElementById(`${section}PrevBtn`);
    if (prevBtn) {
        prevBtn.classList.toggle('disabled', current === 1);
    }

    // Update next button state
    const nextBtn = document.getElementById(`${section}NextBtn`);
    if (nextBtn) {
        nextBtn.classList.toggle('disabled', current === totalPages || totalPages === 0);
    }
}

/**
 * Get CSS class for status badge
 * @param {string} status - Status value
 * @returns {string} - CSS class name
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
 * @param {string} status - Status value
 * @returns {string} - Font Awesome icon name
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
 * Get HTML for empty state display
 * @param {string} title - Empty state title
 * @param {string} icon - Font Awesome icon name
 * @param {string} message - Empty state message
 * @returns {string} - HTML string
 */
function getEmptyStateHTML(title, icon, message) {
    return `
    <tr>
      <td colspan="5" class="text-center py-4">
        <div class="no-data-message">
          <i class="fas fa-${icon} fa-3x mb-3 text-muted"></i>
          <h5>${title}</h5>
          <p class="text-muted">${message}</p>
        </div>
      </td>
    </tr>
  `;
}

/**
 * Show loading spinner
 */
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

/**
 * Hide loading spinner
 */
function hideSpinner() {
    const spinnerOverlay = document.getElementById('spinnerOverlay');
    if (spinnerOverlay) {
        spinnerOverlay.style.display = 'none';
    }

    // Also hide any inline loading elements
    const loadingElements = document.querySelectorAll('.loading-data, .loading-message');
    loadingElements.forEach(element => {
        if (element && element.parentNode) {
            element.parentNode.removeChild(element);
        }
    });
}

/**
 * Show API error message
 * @param {string} message - Error message to display
 */
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
 * @param {string} dateString - Date string to format
 * @returns {string} - Formatted date string
 */
function formatDate(dateString) {
    if (!dateString) return 'N/A';

    try {
        const options = { year: 'numeric', month: 'long', day: 'numeric' };
        return new Date(dateString).toLocaleDateString('en-US', options);
    } catch (e) {
        return dateString;
    }
}

/**
 * Format currency value
 * @param {number} value - Currency value to format
 * @returns {string} - Formatted currency string
 */
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

/**
 * Show toast notification
 * @param {string} message - Message to display
 * @param {string} type - Toast type (success, error, info)
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

/**
 * Get authentication token
 * @returns {string|null} - JWT token
 */
function getToken() {
    return localStorage.getItem('jwt-token') || sessionStorage.getItem('jwt-token');
}

/**
 * Escape HTML special characters to prevent XSS
 * @param {string} str - String to escape
 * @returns {string} - Escaped string
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