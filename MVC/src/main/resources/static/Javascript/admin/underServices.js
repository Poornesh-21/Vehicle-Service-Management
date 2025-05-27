/**
 * Albany Vehicle Service System - Under Service Management
 * Enhanced version that only shows vehicles with assigned service advisors
 */

// Application state management
const appState = {
    vehiclesUnderService: [],
    filteredVehicles: [],
    pagination: {
        currentPage: 1,
        itemsPerPage: 5
    },
    currentServiceId: null,
    isLoading: false
};

document.addEventListener('DOMContentLoaded', () => {
    // Authentication verification
    const token = getAuthToken();
    if (!token) {
        window.location.href = '/admin/login?error=session_expired';
        return;
    }

    initializeApp();
    loadVehiclesUnderService();
});

/**
 * Initialize the application
 */
function initializeApp() {
    setupInterface();
    setupEventListeners();
}

/**
 * Set up UI components
 */
function setupInterface() {
    setupMobileMenu();
    setupLogout();
    setupDateDisplay();
    setupNavigation();
}

/**
 * Set up all event listeners
 */
function setupEventListeners() {
    // Search functionality
    const searchInput = document.getElementById('searchInput');
    if (searchInput) {
        searchInput.addEventListener('keyup', () => filterVehicles(searchInput.value));
    }

    // Table search
    const tableSearchInput = document.querySelector('.search-input-sm');
    if (tableSearchInput) {
        tableSearchInput.addEventListener('keyup', () => {
            filterTableRows(tableSearchInput.value, 'vehiclesUnderServiceTable');
        });
    }

    // Pagination
    setupPaginationControls();

    // Filters
    const applyFilterBtn = document.getElementById('applyFilterBtn');
    if (applyFilterBtn) {
        applyFilterBtn.addEventListener('click', applyFilters);
    }

    // Status update buttons
    const updateStatusBtn = document.getElementById('updateStatusBtn');
    if (updateStatusBtn) {
        updateStatusBtn.addEventListener('click', () => {
            const serviceId = document.getElementById('viewServiceId').textContent.replace('REQ-', '');
            updateServiceStatus(serviceId);
        });
    }

    const confirmUpdateStatusBtn = document.getElementById('confirmUpdateStatusBtn');
    if (confirmUpdateStatusBtn) {
        confirmUpdateStatusBtn.addEventListener('click', submitStatusUpdate);
    }

    // Table row click for viewing details
    document.addEventListener('click', handleRowClick);
}

/**
 * Get auth token from storage
 * @returns {string} Auth token
 */
function getAuthToken() {
    return sessionStorage.getItem('jwt-token') || localStorage.getItem('jwt-token') || '';
}

/**
 * Set up mobile menu toggle
 */
function setupMobileMenu() {
    const mobileMenuToggle = document.getElementById('mobileMenuToggle');
    if (mobileMenuToggle) {
        mobileMenuToggle.addEventListener('click', () => {
            document.getElementById('sidebar').classList.toggle('active');
        });
    }
}

/**
 * Set up logout functionality
 */
function setupLogout() {
    const logoutBtn = document.querySelector('.logout-btn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', () => {
            if (confirm('Are you sure you want to logout?')) {
                clearAllStorageData();
                window.location.href = '/admin/logout';
            }
        });
    }
}

/**
 * Clear all local storage data
 */
function clearAllStorageData() {
    const keys = ['jwt-token', 'user-role', 'user-name'];
    keys.forEach(key => {
        localStorage.removeItem(key);
        sessionStorage.removeItem(key);
    });
}

/**
 * Set up date display
 */
function setupDateDisplay() {
    const dateElement = document.getElementById('current-date');
    if (dateElement) {
        const options = { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' };
        dateElement.textContent = new Date().toLocaleDateString('en-US', options);
    }
}

/**
 * Set up navigation links
 */
function setupNavigation() {
    const currentPath = window.location.pathname;
    document.querySelectorAll('.sidebar-menu-link').forEach(link => {
        const href = link.getAttribute('href');

        // Clean up URLs - remove any token parameters
        if (href && href.includes('token=')) {
            const cleanHref = href.split('?')[0];
            link.setAttribute('href', cleanHref);
        }

        // Set active link
        if (href && currentPath.includes(href.split('?')[0])) {
            link.classList.add('active');
        } else {
            link.classList.remove('active');
        }
    });
}

/**
 * Set up pagination controls
 */
function setupPaginationControls() {
    // Page number buttons
    document.querySelectorAll('#paginationService .page-link[data-page]').forEach(link => {
        link.addEventListener('click', e => {
            e.preventDefault();
            if (!link.parentElement.classList.contains('disabled')) {
                const page = parseInt(link.getAttribute('data-page'), 10);
                if (page) {
                    appState.pagination.currentPage = page;
                    updatePagination();
                    showCurrentPage();
                }
            }
        });
    });

    // Previous button
    const prevBtn = document.getElementById('prevBtnService');
    if (prevBtn) {
        prevBtn.addEventListener('click', e => {
            e.preventDefault();
            if (appState.pagination.currentPage > 1) {
                appState.pagination.currentPage--;
                updatePagination();
                showCurrentPage();
            }
        });
    }

    // Next button
    const nextBtn = document.getElementById('nextBtnService');
    if (nextBtn) {
        nextBtn.addEventListener('click', e => {
            e.preventDefault();
            const totalPages = Math.ceil(appState.filteredVehicles.length / appState.pagination.itemsPerPage);
            if (appState.pagination.currentPage < totalPages) {
                appState.pagination.currentPage++;
                updatePagination();
                showCurrentPage();
            }
        });
    }
}

/**
 * Load vehicles under service from API
 */
function loadVehiclesUnderService() {
    const loadingRow = document.getElementById('loading-row-service');
    const emptyRow = document.getElementById('empty-row-service');

    if (loadingRow) loadingRow.style.display = '';
    if (emptyRow) emptyRow.style.display = 'none';

    appState.isLoading = true;

    apiRequest('/admin/api/vehicle-tracking/under-service')
        .then(data => {
            appState.isLoading = false;
            if (loadingRow) loadingRow.style.display = 'none';

            // Filter out vehicles without assigned service advisors
            const vehiclesWithAdvisors = data.filter(vehicle =>
                vehicle.serviceAdvisorName && vehicle.serviceAdvisorName !== 'Not Assigned'
            );

            appState.vehiclesUnderService = vehiclesWithAdvisors;
            appState.filteredVehicles = vehiclesWithAdvisors;

            if (vehiclesWithAdvisors.length > 0) {
                updatePagination();
                showCurrentPage();
            } else {
                showEmptyState(emptyRow);
            }
        })
        .catch(error => {
            appState.isLoading = false;
            if (loadingRow) loadingRow.style.display = 'none';
            showErrorState(emptyRow, error.message);
        });
}

/**
 * Make authenticated API request
 * @param {string} url - API endpoint
 * @param {string} method - HTTP method
 * @param {Object} body - Request body
 * @returns {Promise} API response
 */
function apiRequest(url, method = 'GET', body = null) {
    const token = getAuthToken();
    if (!token) {
        window.location.href = '/admin/login?error=session_expired';
        return Promise.reject(new Error('No authentication token found'));
    }

    const options = {
        method,
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        }
    };

    if (body) {
        options.body = JSON.stringify(body);
    }

    return fetch(url, options)
        .then(response => {
            if (response.status === 401 || response.status === 403) {
                window.location.href = '/admin/login?error=session_expired';
                throw new Error('Session expired');
            }
            if (!response.ok) {
                throw new Error(`API request failed: ${response.status}`);
            }
            return response.json();
        });
}

/**
 * Show empty state
 * @param {HTMLElement} emptyRow - Empty row element
 */
function showEmptyState(emptyRow) {
    if (!emptyRow) return;

    emptyRow.style.display = '';
    emptyRow.innerHTML = `
    <td colspan="10" class="text-center py-5">
      <div class="my-5">
        <i class="fas fa-car-alt fa-4x text-muted mb-4" style="opacity: 0.3;"></i>
        <h4 class="text-wine">No Vehicles With Assigned Service Advisors</h4>
        <p class="text-muted mt-3">
          There are no vehicles currently undergoing service with assigned advisors.
          <br>Vehicles will appear here when they are assigned to a service advisor.
        </p>
        <button class="btn-premium primary mt-3" onclick="refreshVehicleData()">
          <i class="fas fa-sync-alt"></i>
          Refresh Data
        </button>
      </div>
    </td>
  `;
}

/**
 * Show error state
 * @param {HTMLElement} emptyRow - Empty row element
 * @param {string} errorMessage - Error message
 */
function showErrorState(emptyRow, errorMessage) {
    if (!emptyRow) return;

    emptyRow.style.display = '';
    emptyRow.innerHTML = `
    <td colspan="10" class="text-center py-5">
      <div class="my-5">
        <i class="fas fa-exclamation-triangle fa-4x text-danger mb-4"></i>
        <h4 class="text-danger">Error Loading Vehicles</h4>
        <p class="text-muted mt-3">
          We encountered a problem while loading vehicles under service.
          <br>Error: ${errorMessage}
        </p>
        <button class="btn-premium primary mt-3" onclick="refreshVehicleData()">
          <i class="fas fa-sync-alt"></i>
          Try Again
        </button>
      </div>
    </td>
  `;
}

/**
 * Refresh vehicle data
 */
function refreshVehicleData() {
    const loadingRow = document.getElementById('loading-row-service');
    const emptyRow = document.getElementById('empty-row-service');

    if (loadingRow) loadingRow.style.display = '';
    if (emptyRow) emptyRow.style.display = 'none';

    loadVehiclesUnderService();
    showToastNotification('Refreshing Data', 'Fetching the latest vehicle service information...');
}

/**
 * Filter vehicles based on search text
 * @param {string} searchText - Search text
 */
function filterVehicles(searchText) {
    if (!searchText || searchText.trim() === '') {
        appState.filteredVehicles = appState.vehiclesUnderService;
        appState.pagination.currentPage = 1;
        updatePagination();
        showCurrentPage();
        return;
    }

    searchText = searchText.toLowerCase().trim();

    appState.filteredVehicles = appState.vehiclesUnderService.filter(vehicle => {
        return (
            (vehicle.vehicleName && vehicle.vehicleName.toLowerCase().includes(searchText)) ||
            (vehicle.registrationNumber && vehicle.registrationNumber.toLowerCase().includes(searchText)) ||
            (vehicle.customerName && vehicle.customerName.toLowerCase().includes(searchText)) ||
            (vehicle.serviceType && vehicle.serviceType.toLowerCase().includes(searchText)) ||
            (vehicle.status && vehicle.status.toLowerCase().includes(searchText)) ||
            (vehicle.serviceAdvisorName && vehicle.serviceAdvisorName.toLowerCase().includes(searchText))
        );
    });

    appState.pagination.currentPage = 1;
    updatePagination();
    showCurrentPage();
}

/**
 * Filter table rows based on search text
 * @param {string} searchText - Search text
 * @param {string} tableId - Table ID
 */
function filterTableRows(searchText, tableId) {
    const table = document.getElementById(tableId);
    if (!table) return;

    const rows = table.querySelectorAll('tbody tr');
    if (!rows.length) return;

    searchText = searchText.toLowerCase().trim();

    rows.forEach(row => {
        if (row.id === 'loading-row-service' || row.id === 'empty-row-service') {
            return;
        }

        const text = row.textContent.toLowerCase();
        row.style.display = !searchText || text.includes(searchText) ? '' : 'none';
    });
}

/**
 * Show current page of vehicles
 */
function showCurrentPage() {
    const startIndex = (appState.pagination.currentPage - 1) * appState.pagination.itemsPerPage;
    const endIndex = Math.min(startIndex + appState.pagination.itemsPerPage, appState.filteredVehicles.length);
    const vehiclesToShow = appState.filteredVehicles.slice(startIndex, endIndex);

    renderVehicles(vehiclesToShow);
}

/**
 * Render vehicles to the table
 * @param {Array} vehicles - Vehicles to render
 */
function renderVehicles(vehicles) {
    const tableBody = document.getElementById('vehiclesUnderServiceTableBody');
    if (!tableBody) return;

    // Preserve special rows
    const loadingRow = document.getElementById('loading-row-service');
    const emptyRow = document.getElementById('empty-row-service');

    // Clear table except for special rows
    tableBody.innerHTML = '';
    if (loadingRow) tableBody.appendChild(loadingRow);
    if (emptyRow) tableBody.appendChild(emptyRow);

    if (loadingRow) loadingRow.style.display = 'none';

    if (vehicles.length === 0) {
        if (emptyRow) {
            emptyRow.style.display = '';
            emptyRow.innerHTML = `
        <td colspan="10" class="text-center py-4">
          <div class="my-5">
            <i class="fas fa-car-alt fa-3x text-muted mb-3"></i>
            <h5>No matching vehicles found</h5>
            <p class="text-muted">Try adjusting your search criteria</p>
          </div>
        </td>
      `;
        }
        return;
    }

    if (emptyRow) emptyRow.style.display = 'none';

    // Create rows for each vehicle
    vehicles.forEach(vehicle => {
        const row = createVehicleRow(vehicle);
        tableBody.appendChild(row);
    });

    // Add event listeners to new buttons
    document.querySelectorAll('.assign-btn').forEach(btn => {
        btn.addEventListener('click', e => {
            e.stopPropagation();
            const row = btn.closest('tr');
            const serviceId = row.getAttribute('data-id');
            // Handle assignment (could be implemented separately)
        });
    });
}

/**
 * Create a table row for a vehicle
 * @param {Object} vehicle - Vehicle data
 * @returns {HTMLElement} Table row
 */
function createVehicleRow(vehicle) {
    const row = document.createElement('tr');
    row.setAttribute('data-id', vehicle.requestId);
    row.classList.add('active-page');

    const startDate = formatDate(vehicle.startDate);
    const estimatedCompletion = formatDate(vehicle.estimatedCompletionDate);
    const vehicleName = vehicle.vehicleName ||
        (vehicle.vehicleBrand && vehicle.vehicleModel ? `${vehicle.vehicleBrand} ${vehicle.vehicleModel}` : 'Unknown Vehicle');
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
          <h5>${escapeHTML(vehicleName)}</h5>
          <p>${escapeHTML(registrationNumber)}</p>
        </div>
      </div>
    </td>
    <td>
      <div class="person-info">
        <h5>${escapeHTML(customerName)}</h5>
      </div>
    </td>
    <td>
      ${membershipStatus === 'Premium' ?
        '<span class="membership-badge membership-premium"><i class="fas fa-crown"></i> Premium</span>' :
        '<span class="membership-badge membership-standard"><i class="fas fa-user"></i> Standard</span>'}
    </td>
    <td>${escapeHTML(serviceType)}</td>
    <td>${escapeHTML(serviceAdvisorName)}</td>
    <td>${startDate}</td>
    <td>${estimatedCompletion}</td>
    <td>
      <div class="table-actions-cell">
        <button class="btn-table-action" id="viewServiceBtn" title="View Details">
          <i class="fas fa-eye"></i>
        </button>
      </div>
    </td>
  `;

    return row;
}

/**
 * Format date for display
 * @param {string|Date} dateInput - Date to format
 * @returns {string} Formatted date
 */
function formatDate(dateInput) {
    if (!dateInput) return 'Not set';

    try {
        const date = new Date(dateInput);
        return date.toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric'
        });
    } catch (e) {
        return 'Invalid date';
    }
}

/**
 * Update pagination UI
 */
function updatePagination() {
    const totalItems = appState.filteredVehicles.length;
    const { itemsPerPage, currentPage } = appState.pagination;
    const totalPages = Math.ceil(totalItems / itemsPerPage);

    const pagination = document.getElementById('paginationService');
    if (!pagination) return;

    const pageLinks = pagination.querySelectorAll('.page-link[data-page]');

    pageLinks.forEach((link, index) => {
        const pageNumber = index + 1;
        const pageItem = link.parentElement;

        if (pageNumber <= totalPages) {
            pageItem.style.display = '';
            pageItem.classList.toggle('active', pageNumber === currentPage);
        } else {
            pageItem.style.display = 'none';
        }
    });

    const prevBtn = document.getElementById('prevBtnService');
    const nextBtn = document.getElementById('nextBtnService');

    if (prevBtn) {
        prevBtn.parentElement.classList.toggle('disabled', currentPage === 1);
    }

    if (nextBtn) {
        nextBtn.parentElement.classList.toggle('disabled', currentPage === totalPages || totalPages === 0);
    }
}

/**
 * Apply filters from the filter modal
 */
function applyFilters() {
    const filterCriteria = {
        vehicleType: getFilterValue('filterByVehicleType'),
        serviceType: getFilterValue('filterByServiceType'),
        status: getFilterValue('filterByStatus'),
        dateFrom: getFilterValue('filterDateFrom'),
        dateTo: getFilterValue('filterDateTo'),
        serviceAdvisor: getFilterValue('filterByServiceAdvisor')
    };

    const loadingRow = document.getElementById('loading-row-service');
    const emptyRow = document.getElementById('empty-row-service');

    if (loadingRow) loadingRow.style.display = '';
    if (emptyRow) emptyRow.style.display = 'none';

    apiRequest('/admin/api/vehicle-tracking/under-service/filter', 'POST', filterCriteria)
        .then(data => {
            // Filter to only include vehicles with assigned service advisors
            const vehiclesWithAdvisors = data.filter(vehicle =>
                vehicle.serviceAdvisorName && vehicle.serviceAdvisorName !== 'Not Assigned'
            );

            appState.vehiclesUnderService = vehiclesWithAdvisors;
            appState.filteredVehicles = vehiclesWithAdvisors;
            appState.pagination.currentPage = 1;

            if (loadingRow) loadingRow.style.display = 'none';

            if (vehiclesWithAdvisors.length > 0) {
                updatePagination();
                showCurrentPage();
            } else if (emptyRow) {
                emptyRow.style.display = '';
            }

            $('#filterVehiclesModal').modal('hide');
        })
        .catch(error => {
            if (loadingRow) loadingRow.style.display = 'none';
            if (emptyRow) {
                emptyRow.style.display = '';
                showErrorState(emptyRow, error.message);
            }
            showToastNotification('Error', 'Failed to apply filters: ' + error.message);
        });
}

/**
 * Get filter value from input element
 * @param {string} id - Element ID
 * @returns {string|null} Filter value
 */
function getFilterValue(id) {
    const element = document.getElementById(id);
    return element && element.value ? element.value : null;
}

/**
 * View service details
 * @param {string} serviceId - Service ID
 */
function viewServiceDetails(serviceId) {
    document.getElementById('spinnerOverlay')?.classList.add('show');
    appState.currentServiceId = serviceId;

    apiRequest(`/admin/api/vehicle-tracking/service-request/${serviceId}`)
        .then(service => {
            const vehicle = appState.vehiclesUnderService.find(v => v.requestId === parseInt(serviceId));
            const mergedService = { ...vehicle, ...service };
            document.getElementById('spinnerOverlay')?.classList.remove('show');

            populateServiceDetailsModal(mergedService, serviceId);

            const modal = document.getElementById('viewServiceDetailsModal');
            if (modal) {
                $(modal).modal('show');
                setTimeout(() => {
                    modal.style.zIndex = "1060";
                    document.querySelector('.modal-backdrop').style.zIndex = "1050";
                }, 100);
            }
        })
        .catch(error => {
            document.getElementById('spinnerOverlay')?.classList.remove('show');
            showToastNotification('Error', 'Failed to load service details: ' + error.message);
        });
}

/**
 * Populate service details modal
 * @param {Object} service - Service data
 * @param {string} serviceId - Service ID
 */
function populateServiceDetailsModal(service, serviceId) {
    // Set basic info
    setElementText('viewServiceId', 'REQ-' + serviceId);
    setElementText('viewStartDate', formatDate(service.startDate || service.createdAt));
    setElementText('viewVehicleName', service.vehicleName || service.vehicleModel || 'Vehicle');
    setElementText('viewRegistrationNumber', service.registrationNumber || 'Not specified');
    setElementText('viewVehicleCategory', service.category || service.vehicleType || 'Car');
    setElementText('viewCustomerName', service.customerName || 'Customer');
    setElementText('viewCustomerContact', service.customerPhone || service.customerEmail || 'Not available');
    setElementText('viewServiceType', service.serviceType || 'Regular Service');
    setElementText('viewEstimatedCompletion', formatDate(service.estimatedCompletionDate || service.deliveryDate));
    setElementText('viewServiceAdvisor', service.serviceAdvisorName || 'Not Assigned');
    setElementText('viewServiceDescription', service.additionalDescription || 'No description provided.');
    setElementText('viewProgressNotes', getProgressNotes(service) || 'No progress notes available.');

    // Set status badge
    const status = (service.status || 'Received').toLowerCase();
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
    setElementHTML('viewStatus', statusBadge);

    // Set membership badge
    let membershipBadge = '';
    if ((service.membershipStatus || '') === 'Premium') {
        membershipBadge = '<span class="membership-badge membership-premium"><i class="fas fa-crown"></i> Premium</span>';
    } else {
        membershipBadge = '<span class="membership-badge membership-standard"><i class="fas fa-user"></i> Standard</span>';
    }
    setElementHTML('viewMembership', membershipBadge);

    // Update progress bar
    const progressBar = document.querySelector('.progress-bar');
    if (progressBar) {
        const progressPercentage = calculateProgressPercentage(service.status);
        progressBar.style.width = progressPercentage + '%';
        progressBar.textContent = progressPercentage + '%';
        progressBar.setAttribute('aria-valuenow', progressPercentage);
    }
}

/**
 * Set element text content
 * @param {string} id - Element ID
 * @param {string} text - Text content
 */
function setElementText(id, text) {
    const element = document.getElementById(id);
    if (element) element.textContent = text;
}

/**
 * Set element HTML content
 * @param {string} id - Element ID
 * @param {string} html - HTML content
 */
function setElementHTML(id, html) {
    const element = document.getElementById(id);
    if (element) element.innerHTML = html;
}

/**
 * Calculate progress percentage based on status
 * @param {string} status - Service status
 * @returns {number} Progress percentage
 */
function calculateProgressPercentage(status) {
    switch ((status || '').toLowerCase()) {
        case 'received': return 10;
        case 'diagnosis': return 40;
        case 'repair': return 70;
        case 'completed': return 100;
        default: return 0;
    }
}

/**
 * Get progress notes based on service status
 * @param {Object} service - Service data
 * @returns {string} Progress notes
 */
function getProgressNotes(service) {
    switch ((service.status || '').toLowerCase()) {
        case 'received':
            return 'Vehicle received and awaiting diagnosis. Service advisor assigned.';
        case 'diagnosis':
            return `Diagnosis in progress by ${service.serviceAdvisorName}. Checking for issues related to ${service.serviceType}.`;
        case 'repair':
            return `Repair work in progress. Estimated completion on ${formatDate(service.estimatedCompletionDate || service.deliveryDate)}.`;
        case 'completed':
            return 'Service completed. Vehicle ready for pickup.';
        default:
            return 'Status updates will appear here.';
    }
}

/**
 * Update service status
 * @param {string} serviceId - Service ID
 */
function updateServiceStatus(serviceId) {
    const currentStatusElement = document.getElementById('viewStatus');
    const currentStatusText = currentStatusElement ? currentStatusElement.textContent.trim() : '';

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

    appState.currentServiceId = serviceId;

    // Determine recommended next status
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

    // Update status modal
    setElementText('updateServiceId', 'REQ-' + serviceId);
    setElementText('currentStatusText', currentStatus);
    setElementText('newStatusText', recommendedStatus);

    const statusSelect = document.getElementById('serviceStatusSelect');
    if (statusSelect) {
        statusSelect.value = recommendedStatus;

        // Disable inappropriate status options
        for (let i = 0; i < statusSelect.options.length; i++) {
            const option = statusSelect.options[i];

            if (option.value === 'Completed' && currentStatus !== 'Repair') {
                option.disabled = true;
            } else if (option.value === 'Repair' && currentStatus === 'Received') {
                option.disabled = true;
            } else {
                option.disabled = false;
            }
        }

        statusSelect.onchange = function() {
            setElementText('newStatusText', this.value);
        };
    }

    // Close service details modal
    $('#viewServiceDetailsModal').modal('hide');

    // Show status update modal after a brief delay
    setTimeout(() => {
        $('.modal-backdrop').remove();
        $('body').removeClass('modal-open');
        $('body').css('padding-right', '');
        $('#updateStatusModal').modal('show');
    }, 100);
}

/**
 * Submit status update
 */
function submitStatusUpdate() {
    const spinnerOverlay = document.getElementById('spinnerOverlay');
    if (spinnerOverlay) spinnerOverlay.classList.add('show');

    const serviceId = appState.currentServiceId;
    const statusSelect = document.getElementById('serviceStatusSelect');
    if (!statusSelect) return;

    const newStatus = statusSelect.value;

    // Close modal
    $('#updateStatusModal').modal('hide');

    setTimeout(() => {
        $('.modal-backdrop').remove();
        $('body').removeClass('modal-open');
        $('body').css('padding-right', '');
    }, 100);

    // Call API to update status
    apiRequest(`/admin/api/vehicle-tracking/service-request/${serviceId}/status`, 'PUT', { status: newStatus })
        .then(data => {
            if (spinnerOverlay) spinnerOverlay.classList.remove('show');

            if (newStatus === 'Completed') {
                // Remove completed vehicle from list
                appState.vehiclesUnderService = appState.vehiclesUnderService.filter(v => v.requestId !== parseInt(serviceId));
                appState.filteredVehicles = appState.filteredVehicles.filter(v => v.requestId !== parseInt(serviceId));

                updatePagination();
                showCurrentPage();

                // Show success modal
                const successTitle = document.getElementById('successTitle');
                const successMessage = document.getElementById('successMessage');

                if (successTitle) successTitle.textContent = 'Service Completed!';
                if (successMessage) {
                    successMessage.textContent = `Service REQ-${serviceId} has been marked as completed and moved to Completed Services.`;
                }

                $('#successModal').modal('show');
            } else {
                // Update vehicle status in the list
                const serviceIndex = appState.vehiclesUnderService.findIndex(v => v.requestId === parseInt(serviceId));
                if (serviceIndex !== -1) {
                    appState.vehiclesUnderService[serviceIndex].status = newStatus;

                    // Also update in filtered list if present
                    const filteredIndex = appState.filteredVehicles.findIndex(v => v.requestId === parseInt(serviceId));
                    if (filteredIndex !== -1) {
                        appState.filteredVehicles[filteredIndex].status = newStatus;
                    }

                    showCurrentPage();
                }

                showToastNotification('Status Updated!', `Service REQ-${serviceId} status updated to ${newStatus}.`);
            }
        })
        .catch(error => {
            if (spinnerOverlay) spinnerOverlay.classList.remove('show');
            showToastNotification('Error', 'Failed to update status: ' + error.message);
        });
}

/**
 * Show toast notification
 * @param {string} title - Notification title
 * @param {string} message - Notification message
 * @param {string} type - Notification type (success, error, info)
 */
function showToastNotification(title, message, type = 'success') {
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
          <strong>${escapeHTML(title)}</strong> ${escapeHTML(message)}
        </div>
        <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
      </div>
    </div>
  `;

    toastContainer.insertAdjacentHTML('beforeend', toastHtml);

    const toastElement = document.getElementById(toastId);
    if (toastElement && typeof bootstrap !== 'undefined') {
        const toast = new bootstrap.Toast(toastElement, {
            animation: true,
            autohide: true,
            delay: 3000
        });

        toast.show();

        toastElement.addEventListener('hidden.bs.toast', () => {
            toastElement.remove();
        });
    }
}

/**
 * Handle row click event
 * @param {Event} e - Click event
 */
function handleRowClick(e) {
    // View service button click
    if (e.target && (e.target.id === 'viewServiceBtn' || e.target.closest('#viewServiceBtn'))) {
        const row = e.target.closest('tr');
        if (row) {
            const serviceId = row.getAttribute('data-id');
            if (serviceId) {
                viewServiceDetails(serviceId);
            }
        }
        return;
    }

    // Row click (except for buttons and special rows)
    if (e.target &&
        e.target.tagName !== 'BUTTON' &&
        e.target.closest('tr') &&
        !e.target.closest('thead') &&
        !e.target.closest('.table-actions-cell') &&
        !e.target.closest('button')) {

        const row = e.target.closest('tr');
        if (row.id !== 'loading-row-service' && row.id !== 'empty-row-service') {
            const serviceId = row.getAttribute('data-id');
            if (serviceId) {
                viewServiceDetails(serviceId);
            }
        }
    }
}

/**
 * Escape HTML to prevent XSS
 * @param {string} str - String to escape
 * @returns {string} Escaped string
 */
function escapeHTML(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}