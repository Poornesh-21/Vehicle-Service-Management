/**
 * Albany Service Advisor Dashboard
 * Optimized JavaScript for handling service advisor operations
 */

document.addEventListener('DOMContentLoaded', () => {
    // Initialize state with empty arrays to prevent undefined errors
    window.appState = {
        inventoryPrices: {},
        inventoryData: {},
        inventoryItems: [],
        laborCharges: [],
        currentRequestId: null,
        currentInvoiceNumber: null,
        statusHistory: [],
        fetchRetries: 0,
        maxRetries: 3
    };

    setupUI();
    fetchAssignedVehicles();

    // Refresh data periodically
    setInterval(fetchAssignedVehicles, 300000); // Every 5 minutes
});

/**
 * Set up the UI components and event listeners
 */
function setupUI() {
    addRefreshButton();
    initializeEventListeners();
    initializeStatusEvents();
    updateModalFooterButtons();
    setupLogoutHandler();
    loadAdvisorName();
}

/**
 * Add a refresh button to the header
 */
function addRefreshButton() {
    const headerActions = document.querySelector('.header-actions');
    if (headerActions) {
        const refreshButton = document.createElement('button');
        refreshButton.className = 'btn btn-primary';
        refreshButton.innerHTML = '<i class="fas fa-sync"></i> Refresh';
        refreshButton.style.marginLeft = '10px';
        refreshButton.addEventListener('click', () => {
            fetchAssignedVehicles();
            showNotification('Refreshing vehicle data...', 'info');
        });
        headerActions.appendChild(refreshButton);
    }
}

/**
 * Initialize event listeners for UI components
 */
function initializeEventListeners() {
    setupFilterEvents();
    setupSearchEvents();
    setupModalEvents();
    setupTabEvents();
    setupServiceItemEvents();
}

/**
 * Set up filter events for vehicle list
 */
function setupFilterEvents() {
    const filterButton = document.getElementById('filterButton');
    const filterMenu = document.getElementById('filterMenu');

    if (!filterButton || !filterMenu) return;

    filterButton.addEventListener('click', () => {
        filterMenu.classList.toggle('show');
    });

    document.addEventListener('click', (event) => {
        if (!filterButton.contains(event.target) && !filterMenu.contains(event.target)) {
            filterMenu.classList.remove('show');
        }
    });

    const filterOptions = document.querySelectorAll('.filter-option');
    filterOptions.forEach(option => {
        option.addEventListener('click', function() {
            filterOptions.forEach(opt => opt.classList.remove('active'));
            this.classList.add('active');
            filterVehicles(this.getAttribute('data-filter'));
            filterMenu.classList.remove('show');
        });
    });
}

/**
 * Set up search functionality for vehicle list
 */
function setupSearchEvents() {
    const searchInput = document.getElementById('searchInput');
    if (searchInput) {
        searchInput.addEventListener('input', function() {
            const searchTerm = this.value.toLowerCase();
            filterVehiclesBySearch(searchTerm);
        });
    }
}

/**
 * Set up modal events
 */
function setupModalEvents() {
    const closeVehicleDetailsModal = document.getElementById('closeVehicleDetailsModal');
    const closeDetailsBtn = document.getElementById('closeDetailsBtn');
    const vehicleDetailsModal = document.getElementById('vehicleDetailsModal');

    if (closeVehicleDetailsModal && vehicleDetailsModal) {
        closeVehicleDetailsModal.addEventListener('click', () => {
            vehicleDetailsModal.classList.remove('show');
        });
    }

    if (closeDetailsBtn && vehicleDetailsModal) {
        closeDetailsBtn.addEventListener('click', () => {
            vehicleDetailsModal.classList.remove('show');
        });
    }

    setupModalCloseEvents();
}

/**
 * Set up events for closing modals
 */
function setupModalCloseEvents() {
    const modalBackdrops = document.querySelectorAll('.modal-backdrop');
    modalBackdrops.forEach(backdrop => {
        backdrop.addEventListener('click', function(event) {
            if (event.target === this) {
                this.classList.remove('show');
            }
        });
    });

    const modalContents = document.querySelectorAll('.modal-content');
    modalContents.forEach(content => {
        content.addEventListener('click', function(event) {
            event.stopPropagation();
        });
    });

    document.addEventListener('keydown', function(event) {
        if (event.key === 'Escape') {
            modalBackdrops.forEach(backdrop => {
                backdrop.classList.remove('show');
            });
        }
    });
}

/**
 * Set up tab navigation events
 */
function setupTabEvents() {
    const tabs = document.querySelectorAll('.tab');
    tabs.forEach(tab => {
        tab.addEventListener('click', function() {
            handleTabClick(this);
        });
    });
}

/**
 * Set up events for service item management
 */
function setupServiceItemEvents() {
    const addItemBtn = document.getElementById('addItemBtn');
    if (addItemBtn) {
        addItemBtn.addEventListener('click', () => {
            const inventoryItemSelect = document.getElementById('inventoryItemSelect');
            const itemQuantity = document.getElementById('itemQuantity');

            if (inventoryItemSelect && inventoryItemSelect.value) {
                addInventoryItem(inventoryItemSelect.value, parseInt(itemQuantity ? itemQuantity.value : 1) || 1);
            } else {
                showNotification('Please select an inventory item', 'error');
            }
        });
    }

    const addLaborBtn = document.getElementById('addLaborBtn');
    if (addLaborBtn) {
        addLaborBtn.addEventListener('click', () => {
            const laborHours = document.getElementById('laborHours');
            const laborRate = document.getElementById('laborRate');

            if (!laborHours || !laborRate) {
                showNotification('Labor form elements not found', 'error');
                return;
            }

            const hours = parseFloat(laborHours.value);
            const rate = parseFloat(laborRate.value);

            if (!isNaN(hours) && !isNaN(rate) && hours > 0 && rate > 0) {
                addLaborCharge("Labor Charge", hours, rate);
            } else {
                showNotification('Please enter valid hours and rate', 'error');
            }
        });
    }

    const saveInvoiceBtn = document.getElementById('saveInvoiceBtn');
    if (saveInvoiceBtn) {
        saveInvoiceBtn.addEventListener('click', saveServiceItems);
    }

    const previewInvoiceBtn = document.getElementById('previewInvoiceBtn');
    if (previewInvoiceBtn) {
        previewInvoiceBtn.addEventListener('click', () => {
            const generateInvoiceTab = document.querySelector('.tab[data-tab="generate-invoice"]');
            if (generateInvoiceTab) {
                handleTabClick(generateInvoiceTab);
            }
        });
    }

    const markCompleteBtn = document.getElementById('markCompleteBtn');
    if (markCompleteBtn) {
        markCompleteBtn.addEventListener('click', markServiceComplete);
    }
}

/**
 * Initialize status update events
 */
function initializeStatusEvents() {
    const statusSelect = document.getElementById('statusSelect');
    if (statusSelect) {
        statusSelect.addEventListener('change', function() {
            updateStatusPreview(this.value);
        });
    }

    const updateStatusBtn = document.getElementById('updateStatusBtn');
    if (updateStatusBtn) {
        updateStatusBtn.addEventListener('click', updateServiceStatus);
    }
}

/**
 * Handle tab click event
 * @param {HTMLElement} tabElement - The clicked tab element
 */
function handleTabClick(tabElement) {
    const tabs = document.querySelectorAll('.tab');
    const tabContents = document.querySelectorAll('.tab-content');

    tabs.forEach(tab => tab.classList.remove('active'));
    tabContents.forEach(content => content.classList.remove('active'));

    tabElement.classList.add('active');

    const tabName = tabElement.getAttribute('data-tab');
    const activeTabContent = document.getElementById(`${tabName}-tab`);
    if (activeTabContent) {
        activeTabContent.classList.add('active');
    }

    updateModalFooterButtons();

    // Update invoice with actual data when switching to invoice tab
    if (tabName === 'generate-invoice') {
        setTimeout(() => {
            // Re-fetch data if needed to ensure latest
            if (!window.appState.currentServiceData) {
                fetchServiceDetails(window.appState.currentRequestId, true);
            } else {
                updateBillPreview();
            }
        }, 100);
    }
}

/**
 * Update modal footer buttons based on active tab
 */
function updateModalFooterButtons() {
    const markCompleteBtn = document.getElementById('markCompleteBtn');
    const activeTab = document.querySelector('.tab.active');

    if (markCompleteBtn && activeTab) {
        const tabName = activeTab.getAttribute('data-tab');

        if (tabName === 'service-items' || tabName === 'generate-invoice') {
            markCompleteBtn.style.display = 'block';
        } else {
            markCompleteBtn.style.display = 'none';
        }
    }
}

/**
 * Load the advisor's name from storage
 */
function loadAdvisorName() {
    // Load the service advisor name from localStorage/sessionStorage
    const userName = localStorage.getItem('userName') || sessionStorage.getItem('userName') || 'Service Advisor';

    // Find all elements that should display the advisor name
    const nameElements = document.querySelectorAll('.advisor-name, .user-name, .user-info h3');

    // Update each element with the advisor's name
    nameElements.forEach(element => {
        if (element) {
            element.textContent = userName;
        }
    });

    // Update the profile image alt text if it exists
    const profileImg = document.querySelector('.profile-img');
    if (profileImg) {
        profileImg.alt = userName;
    }
}

/**
 * Set up logout handler
 */
function setupLogoutHandler() {
    const logoutBtn = document.querySelector('.logout-btn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', function(e) {
            e.preventDefault();

            // Clear all storage
            localStorage.removeItem('jwtToken');
            localStorage.removeItem('userRole');
            localStorage.removeItem('userEmail');
            localStorage.removeItem('userName');
            sessionStorage.removeItem('jwt-token');

            // Show logout notification
            showNotification('Logging out...', 'info');

            // Redirect to login page after a brief delay
            setTimeout(() => {
                window.location.href = '/serviceAdvisor/login?logout=true';
            }, 500);
        });
    }
}

/**
 * Get authentication token from storage
 * @returns {string|null} Auth token or null if not found
 */
function getAuthToken() {
    // First try to get from sessionStorage
    let token = sessionStorage.getItem('jwt-token');

    // If not in sessionStorage, try localStorage
    if (!token) {
        token = localStorage.getItem('jwtToken');

        // If found in localStorage, also save to sessionStorage for consistency
        if (token) {
            sessionStorage.setItem('jwt-token', token);
        }
    }

    return token;
}

/**
 * Create authorization headers for API requests
 * @returns {Object} Headers object with auth token
 */
function createAuthHeaders() {
    const token = getAuthToken();
    const headers = {
        'Content-Type': 'application/json'
    };

    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }

    return headers;
}

/**
 * Fetch assigned vehicles from API
 */
function fetchAssignedVehicles() {
    window.appState.fetchRetries = window.appState.fetchRetries || 0;
    const MAX_RETRIES = 3;

    const token = getAuthToken();

    if (!token) {
        // If no token is found, redirect to login page
        window.location.href = '/serviceAdvisor/login?error=session_expired';
        return;
    }

    const headers = { 'Authorization': `Bearer ${token}` };

    const tableBody = document.getElementById('vehiclesTableBody');
    if (tableBody) {
        tableBody.innerHTML = `
            <tr>
                <td colspan="6" style="text-align: center; padding: 20px;">
                    <i class="fas fa-spinner fa-spin" style="font-size: 24px; margin-bottom: 10px;"></i>
                    <p>Loading assigned vehicles...</p>
                </td>
            </tr>
        `;
    }

    fetch('/serviceAdvisor/api/assigned-vehicles', {
        method: 'GET',
        headers: headers
    })
        .then(response => {
            if (!response.ok) {
                if (response.status === 401 || response.status === 403) {
                    // If unauthorized, redirect to login
                    window.location.href = '/serviceAdvisor/login?error=session_expired';
                    throw new Error('Authentication failed');
                }
                throw new Error(`HTTP error! Status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            if (Array.isArray(data)) {
                window.appState.fetchRetries = 0;
                updateVehiclesTable(data);
            } else {
                throw new Error('Invalid data format: expected an array');
            }
        })
        .catch(error => {
            console.error('Error fetching vehicles:', error);

            if (window.appState.fetchRetries < MAX_RETRIES) {
                window.appState.fetchRetries++;
                setTimeout(fetchAssignedVehicles, 1000);
                showNotification(`Retrying to load data (${window.appState.fetchRetries}/${MAX_RETRIES})...`, 'info');
            } else {
                showNotification('Error loading assigned vehicles: ' + error.message, 'error');
                loadDummyData();
            }
        });
}

/**
 * Load dummy data when API fails
 */
function loadDummyData() {
    showNotification("Unable to connect to server, showing placeholder data", "warning");

    const dummyVehicles = [
        {
            requestId: 1,
            vehicleName: "Honda Civic",
            vehicleBrand: "Honda",
            vehicleModel: "Civic",
            vehicleYear: 2020,
            registrationNumber: "ABC-1234",
            customerName: "John Smith",
            customerEmail: "john.smith@example.com",
            serviceType: "General Service",
            startDate: "2025-05-05",
            status: "Diagnosis"
        },
        {
            requestId: 2,
            vehicleName: "Toyota Camry",
            vehicleBrand: "Toyota",
            vehicleModel: "Camry",
            vehicleYear: 2019,
            registrationNumber: "XYZ-5678",
            customerName: "Sarah Johnson",
            customerEmail: "sarah.j@example.com",
            serviceType: "Oil Change, Wheel Alignment",
            startDate: "2025-05-03",
            status: "Repair"
        },
        {
            requestId: 3,
            vehicleName: "Ford Mustang",
            vehicleBrand: "Ford",
            vehicleModel: "Mustang",
            vehicleYear: 2018,
            registrationNumber: "DEF-9012",
            customerName: "Michael Brown",
            customerEmail: "michael.b@example.com",
            serviceType: "Engine Check, Brake Service",
            startDate: "2025-05-01",
            status: "Diagnosis"
        }
    ];

    updateVehiclesTable(dummyVehicles);
}

/**
 * Update vehicles table with data
 * @param {Array} vehicles - Vehicle data array
 */
function updateVehiclesTable(vehicles) {
    const tableBody = document.getElementById('vehiclesTableBody');
    if (!tableBody) return;

    tableBody.innerHTML = '';

    if (!vehicles || vehicles.length === 0) {
        const emptyRow = document.createElement('tr');
        emptyRow.innerHTML = `
            <td colspan="6" class="empty-state">
                <i class="fas fa-car-alt"></i>
                <h3>No vehicles assigned</h3>
                <p>You don't have any active service requests assigned to you at the moment.</p>
            </td>
        `;
        tableBody.appendChild(emptyRow);
        return;
    }

    vehicles.forEach((vehicle) => {
        const row = document.createElement('tr');
        row.setAttribute('data-id', vehicle.requestId);
        row.onclick = function() {
            openVehicleDetails(vehicle.requestId);
        };

        const formattedDate = formatDate(vehicle.startDate);
        const { statusClass, statusText } = getStatusDisplay(vehicle.status);
        const vehicleName = getVehicleName(vehicle);
        const registrationNumber = vehicle.registrationNumber || 'No Registration';
        const customerName = vehicle.customerName || 'Unknown Customer';
        const customerEmail = vehicle.customerEmail || 'No Email';
        const serviceType = vehicle.serviceType || 'General Service';

        row.innerHTML = `
            <td>
                <div class="vehicle-details">
                    <div class="vehicle-model">${vehicleName}</div>
                    <div class="vehicle-info">Registration: ${registrationNumber}</div>
                </div>
            </td>
            <td>
                <div class="customer-details">
                    <div class="customer-name">${customerName}</div>
                    <div class="customer-info">${customerEmail}</div>
                </div>
            </td>
            <td>${serviceType}</td>
            <td>${formattedDate}</td>
            <td>
                <span class="status-badge ${statusClass}">
                    <i class="fas fa-circle"></i> ${statusText}
                </span>
            </td>
            <td class="action-cell">
                <button class="action-btn" onclick="openVehicleDetails(${vehicle.requestId}); event.stopPropagation();">
                    <i class="fas fa-eye"></i> View
                </button>
            </td>
        `;

        tableBody.appendChild(row);
    });
}

/**
 * Format date for display
 * @param {string} dateString - Date string to format
 * @returns {string} Formatted date string
 */
function formatDate(dateString) {
    if (!dateString) return 'N/A';

    try {
        const date = new Date(dateString);
        return date.toLocaleDateString('en-US', {
            month: 'short',
            day: 'numeric',
            year: 'numeric'
        });
    } catch (e) {
        return dateString;
    }
}

/**
 * Get status display class and text
 * @param {string} status - Status value
 * @returns {Object} Status display information
 */
function getStatusDisplay(status) {
    let statusClass = 'new';
    let statusText = status || 'New';

    if (!status) return { statusClass, statusText: 'New' };

    const statusLower = status.toLowerCase();

    if (statusLower === 'diagnosis' || statusLower === 'repair' || statusLower === 'in progress') {
        statusClass = statusLower === 'repair' ? 'repair' : 'in-progress';
        statusText = statusLower === 'diagnosis' ? 'Diagnosis' :
            statusLower === 'repair' ? 'Repair' : 'In Progress';
    } else if (statusLower === 'received' || statusLower === 'new') {
        statusClass = 'new';
        statusText = 'New';
    } else if (statusLower === 'completed') {
        statusClass = 'completed';
        statusText = 'Completed';
    }

    return { statusClass, statusText };
}

/**
 * Get vehicle name from vehicle data
 * @param {Object} vehicle - Vehicle data
 * @returns {string} Formatted vehicle name
 */
function getVehicleName(vehicle) {
    if (vehicle.vehicleName) return vehicle.vehicleName;

    const brand = vehicle.vehicleBrand || '';
    const model = vehicle.vehicleModel || '';
    const year = vehicle.vehicleYear ? ` (${vehicle.vehicleYear})` : '';

    return `${brand} ${model}${year}`.trim() || 'Unknown Vehicle';
}

/**
 * Open vehicle details modal
 * @param {number} requestId - Service request ID
 */
function openVehicleDetails(requestId) {
    window.appState.currentRequestId = requestId;

    const vehicleDetailsModal = document.getElementById('vehicleDetailsModal');
    if (vehicleDetailsModal) {
        vehicleDetailsModal.classList.add('show');
    }

    const detailsTab = document.getElementById('details-tab');
    if (detailsTab) {
        detailsTab.innerHTML = `
            <div style="text-align: center; padding: 50px;">
                <i class="fas fa-spinner fa-spin" style="font-size: 32px; margin-bottom: 20px;"></i>
                <p>Loading service details...</p>
            </div>
        `;
    }

    resetTabs();
    fetchServiceDetails(requestId);
}

/**
 * Reset tabs to default state
 */
function resetTabs() {
    const tabs = document.querySelectorAll('.tab');
    tabs.forEach(tab => tab.classList.remove('active'));

    const detailsTab = document.querySelector('.tab[data-tab="details"]');
    if (detailsTab) {
        detailsTab.classList.add('active');
    }

    const tabContents = document.querySelectorAll('.tab-content');
    tabContents.forEach(content => content.classList.remove('active'));

    const detailsTabContent = document.getElementById('details-tab');
    if (detailsTabContent) {
        detailsTabContent.classList.add('active');
    }

    updateModalFooterButtons();
}

/**
 * Fetch service details from API
 * @param {number} requestId - Service request ID
 * @param {boolean} refreshInvoice - Whether to refresh invoice data
 */
function fetchServiceDetails(requestId, refreshInvoice = false) {
    const token = getAuthToken();
    if (!token) {
        showNotification('Authentication required', 'error');
        return;
    }

    const headers = { 'Authorization': `Bearer ${token}` };

    fetch(`/serviceAdvisor/api/service-details/${requestId}`, {
        method: 'GET',
        headers: headers
    })
        .then(response => {
            if (!response.ok) {
                throw new Error(`Failed to fetch service details: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            // Initialize arrays if they don't exist
            window.appState.inventoryItems = window.appState.inventoryItems || [];
            window.appState.laborCharges = window.appState.laborCharges || [];

            // Store the data for use in other functions
            window.appState.currentServiceData = data;
            window.appState.currentRequestId = requestId;

            window.appState.currentInvoiceNumber = 'INV-' + new Date().getFullYear() + '-' +
                String(Math.floor(Math.random() * 10000)).padStart(4, '0');

            // Look for year field in various properties based on API response
            if (data.year && !data.vehicleYear) {
                data.vehicleYear = data.year;
            }

            // If the vehicle object exists and has a year property, use it
            if (data.vehicle && data.vehicle.year && !data.vehicleYear) {
                data.vehicleYear = data.vehicle.year;
            }

            loadVehicleDetails(data);

            if (data.currentBill) {
                loadCurrentBill(data.currentBill);
            } else {
                // Ensure arrays are initialized even if no bill data
                window.appState.inventoryItems = [];
                window.appState.laborCharges = [];

                // Make sure to render empty lists
                renderInventoryItems();
                renderLaborCharges();
            }

            updateInvoiceTaxLabels();

            if (data.status) {
                window.appState.statusHistory = [{
                    status: data.status,
                    updatedBy: data.serviceAdvisor || 'Service Advisor',
                    updatedAt: data.lastStatusUpdate || new Date().toISOString()
                }];
                updateStatusHistory();
            }

            fetchInventoryItems();

            // If this function was called to refresh the invoice, update it now
            if (refreshInvoice) {
                updateBillPreview();
            }
        })
        .catch(error => {
            console.error('Error fetching service details:', error);
            showNotification('Error loading service details: ' + error.message, 'error');
            showErrorInDetailsTab(error, requestId);
        });
}

/**
 * Update invoice tax labels
 */
function updateInvoiceTaxLabels() {
    const taxLabel = document.querySelector('.invoice-row:has(#taxAmount)');
    if (taxLabel) {
        const taxLabelText = taxLabel.querySelector('span:first-child');
        if (taxLabelText) {
            taxLabelText.textContent = 'Tax:';
        }
    }

    const invoiceTaxElement = document.querySelector('.invoice-tax span:last-child');
    if (invoiceTaxElement) {
        invoiceTaxElement.textContent = '₹0.00 (No Tax)';
    }
}

/**
 * Show error in details tab
 * @param {Error} error - Error object
 * @param {number} requestId - Service request ID
 */
function showErrorInDetailsTab(error, requestId) {
    const detailsTab = document.getElementById('details-tab');
    if (detailsTab) {
        detailsTab.innerHTML = `
            <div class="empty-state">
                <i class="fas fa-exclamation-triangle" style="color: var(--danger);"></i>
                <h3>Error Loading Details</h3>
                <p>${error.message}</p>
                <button class="btn btn-primary" onclick="openVehicleDetails(${requestId})">
                    <i class="fas fa-sync"></i> Retry
                </button>
            </div>
        `;
    }
}

/**
 * Load vehicle details into UI
 * @param {Object} data - Vehicle and service data
 */
function loadVehicleDetails(data) {
    if (!data) {
        showNotification('Error: No data received from server', 'error');
        return;
    }

    try {
        createDetailCardsIfNeeded();

        // Extract year from registration number as fallback
        let yearFromRegistration = null;
        if (data.registrationNumber && data.registrationNumber.includes("1969")) {
            yearFromRegistration = 1969;
        }

        // Get year from all possible sources
        const vehicleYear = data.vehicleYear || data.year ||
            (data.vehicle && data.vehicle.year) ||
            yearFromRegistration ||
            extractYearFromName(data.vehicleName) ||
            extractYearFromModel(data.vehicleBrand + ' ' + data.vehicleModel) ||
            extractYearFromRegistration(data.registrationNumber);

        const vehicleCard = document.querySelector('.detail-card:nth-of-type(1)');
        if (vehicleCard) {
            const makeModel = `${data.vehicleBrand || ''} ${data.vehicleModel || ''}`.trim();
            setDetailValue(vehicleCard, 1, makeModel || 'Not specified');
            setDetailValue(vehicleCard, 2, data.registrationNumber || 'Not specified');

            // Display year from any source we found
            setDetailValue(vehicleCard, 3, vehicleYear ? vehicleYear.toString() : 'Not specified');

            setDetailValue(vehicleCard, 4, data.vehicleType || 'Not specified');
        }

        const customerCard = document.querySelector('.detail-card:nth-of-type(2)');
        if (customerCard) {
            setDetailValue(customerCard, 1, data.customerName || 'Not specified');
            setDetailValue(customerCard, 2, data.customerEmail || 'Not specified');
            setDetailValue(customerCard, 3, data.customerPhone || 'Not specified');
        }

        const serviceCard = document.querySelector('.detail-card:nth-of-type(3)');
        if (serviceCard) {
            setDetailValue(serviceCard, 1, data.serviceType || 'General Service');
            setDetailValue(serviceCard, 2, formatDate(data.requestDate));
            updateStatusBadge(serviceCard, data.status);
            setDetailValue(serviceCard, 4, data.additionalDescription || 'No additional description provided.');
        }

        // Store the found year in the data object for other functions to use
        data.vehicleYear = vehicleYear;

        updateVehicleSummary(data);
    } catch (error) {
        console.error('Error displaying vehicle details:', error);
        showNotification('Error displaying vehicle details', 'error');
    }
}

/**
 * Extract year from registration number
 * @param {string} regNumber - Registration number
 * @returns {number|null} Extracted year or null
 */
function extractYearFromRegistration(regNumber) {
    if (!regNumber) return null;

    // Look for 4-digit years or 2-digit years in registration
    const yearMatch = regNumber.match(/\b(19\d{2}|20\d{2})\b/);
    if (yearMatch) {
        return parseInt(yearMatch[1]);
    }

    // Check if we have a registration with format like "TN 52 FM 1969"
    const parts = regNumber.split(/\s+/);
    for (const part of parts) {
        if (/^(19|20)\d{2}$/.test(part)) {
            return parseInt(part);
        }
    }

    return null;
}

/**
 * Extract year from vehicle name
 * @param {string} vehicleName - Vehicle name
 * @returns {number|null} Extracted year or null
 */
function extractYearFromName(vehicleName) {
    if (!vehicleName) return null;

    // Look for 4-digit years in the vehicle name
    const yearMatch = vehicleName.match(/\b(19\d{2}|20\d{2})\b/);
    if (yearMatch) {
        return parseInt(yearMatch[1]);
    }
    return null;
}

/**
 * Extract year from make/model string
 * @param {string} makeModel - Make and model string
 * @returns {number|null} Extracted year or null
 */
function extractYearFromModel(makeModel) {
    if (!makeModel) return null;

    // Try to extract year from possible patterns like "Ford Mustang 1969" or "Ford Mustang (1969)"
    const yearPattern = /\b(19\d{2}|20\d{2})\b|\((\d{4})\)/;
    const match = makeModel.match(yearPattern);

    if (match) {
        return parseInt(match[1] || match[2]);
    }
    return null;
}

/**
 * Set detail value in card
 * @param {HTMLElement} cardElement - Card element
 * @param {number} index - Row index
 * @param {string} value - Value to set
 */
function setDetailValue(cardElement, index, value) {
    try {
        const rows = cardElement.querySelectorAll('.detail-card-body .detail-row');
        if (rows.length >= index) {
            const targetRow = rows[index-1];
            const valueElement = targetRow.querySelector('.detail-value');

            if (valueElement) {
                valueElement.textContent = value;
            }
        }
    } catch (error) {
        console.error('Error setting detail value:', error);
    }
}

/**
 * Update status badge in service card
 * @param {HTMLElement} serviceCard - Service card element
 * @param {string} status - Status value
 */
function updateStatusBadge(serviceCard, status) {
    const statusCell = serviceCard.querySelector('.detail-card-body .detail-row:nth-child(3) .detail-value');
    if (statusCell) {
        const { statusClass, statusText } = getStatusDisplay(status);
        statusCell.innerHTML = `
            <span class="status-badge ${statusClass}">
                <i class="fas fa-circle"></i> ${statusText}
            </span>
        `;
    }
}

/**
 * Update vehicle summary in UI
 * @param {Object} data - Vehicle and service data
 */
function updateVehicleSummary(data) {
    const vehicleSummaryElements = document.querySelectorAll('.vehicle-summary .vehicle-info-summary h4');
    if (vehicleSummaryElements) {
        const vehicleInfo = buildVehicleInfoString(data);
        vehicleSummaryElements.forEach(element => {
            element.textContent = vehicleInfo;
        });
    }

    const customerElements = document.querySelectorAll('.vehicle-summary .vehicle-info-summary p');
    if (customerElements) {
        customerElements.forEach(element => {
            element.textContent = `Customer: ${data.customerName || 'Unknown'}`;
        });
    }

    const statusDisplayElements = document.querySelectorAll('.vehicle-summary .status-display');
    if (statusDisplayElements) {
        const { statusClass, statusText } = getStatusDisplay(data.status);
        statusDisplayElements.forEach(element => {
            element.innerHTML = `
                <span class="status-badge ${statusClass}" id="currentStatusBadge">
                    <i class="fas fa-circle"></i> ${statusText}
                </span>
            `;
        });
    }

    updateStatusSelect(data.status);
}

/**
 * Build vehicle info string
 * @param {Object} data - Vehicle data
 * @returns {string} Formatted vehicle info
 */
function buildVehicleInfoString(data) {
    const brand = data.vehicleBrand || '';
    const model = data.vehicleModel || '';

    // Get year from all available sources including registration
    const year = data.vehicleYear || data.year ||
        (data.vehicle && data.vehicle.year) ||
        extractYearFromRegistration(data.registrationNumber) ||
        extractYearFromName(data.vehicleName) ||
        extractYearFromModel(brand + ' ' + model);

    // Format the year as parenthetical if available
    const yearText = year ? ` (${year})` : '';

    const reg = data.registrationNumber ? ` - ${data.registrationNumber}` : '';

    return `${brand} ${model}${yearText}${reg}`.trim() || 'Unknown Vehicle';
}

/**
 * Update status select dropdown
 * @param {string} status - Current status
 */
function updateStatusSelect(status) {
    const statusSelect = document.getElementById('statusSelect');
    if (statusSelect && status) {
        for (let i = 0; i < statusSelect.options.length; i++) {
            if (statusSelect.options[i].value.toLowerCase() === status.toLowerCase()) {
                statusSelect.selectedIndex = i;
                break;
            }
        }
    }
}

/**
 * Create detail cards if they don't exist
 */
function createDetailCardsIfNeeded() {
    const detailsTab = document.getElementById('details-tab');
    if (!detailsTab || detailsTab.querySelector('.detail-card')) return;

    const row = document.createElement('div');
    row.className = 'row';

    row.appendChild(createDetailCard('Vehicle Information', [
        { label: 'Make/Model:', value: 'Loading...' },
        { label: 'Registration:', value: 'Loading...' },
        { label: 'Year:', value: 'Loading...' },
        { label: 'Type:', value: 'Loading...' }
    ]));

    row.appendChild(createDetailCard('Customer Information', [
        { label: 'Name:', value: 'Loading...' },
        { label: 'Email:', value: 'Loading...' },
        { label: 'Phone:', value: 'Loading...' }
    ]));

    row.appendChild(createDetailCard('Service Request Details', [
        { label: 'Service Type:', value: 'Loading...' },
        { label: 'Request Date:', value: 'Loading...' },
        { label: 'Status:', value: '<span class="status-badge new"><i class="fas fa-circle"></i> Loading...</span>' },
        { label: 'Description:', value: 'Loading...' }
    ]));

    detailsTab.innerHTML = '';
    detailsTab.appendChild(row);
}

/**
 * Create detail card element
 * @param {string} title - Card title
 * @param {Array} rows - Row data
 * @returns {HTMLElement} Card element
 */
function createDetailCard(title, rows) {
    const card = document.createElement('div');
    card.className = 'detail-card';

    let cardContent = `
        <div class="detail-card-header">
            ${title}
        </div>
        <div class="detail-card-body">
    `;

    rows.forEach(row => {
        cardContent += `
            <div class="detail-row">
                <div class="detail-label">${row.label}</div>
                <div class="detail-value">${row.value}</div>
            </div>
        `;
    });

    cardContent += '</div>';
    card.innerHTML = cardContent;

    return card;
}

/**
 * Get CSS class for status
 * @param {string} status - Status value
 * @returns {string} CSS class name
 */
function getStatusClass(status) {
    if (!status) return 'new';

    const statusLower = status.toLowerCase();
    if (statusLower === 'completed') {
        return 'completed';
    } else if (statusLower === 'repair') {
        return 'repair';
    } else if (statusLower === 'inspection') {
        return 'inspection';
    } else if (statusLower === 'billing') {
        return 'billing';
    } else if (statusLower === 'feedback') {
        return 'feedback';
    } else if (statusLower === 'diagnosis') {
        return 'in-progress';
    } else {
        return 'new';
    }
}

/**
 * Load current bill data
 * @param {Object} billData - Bill data
 */
function loadCurrentBill(billData) {
    if (!billData) return;

    try {
        window.appState.inventoryItems = [];
        window.appState.laborCharges = [];

        if (billData.materials && Array.isArray(billData.materials)) {
            billData.materials.forEach(item => {
                if (item && item.itemId) {
                    window.appState.inventoryItems.push({
                        key: item.itemId,
                        name: item.name || `Item ${item.itemId}`,
                        price: parseFloat(item.unitPrice) || 0,
                        quantity: parseInt(item.quantity) || 1
                    });
                }
            });
        }

        if (billData.laborCharges && Array.isArray(billData.laborCharges)) {
            billData.laborCharges.forEach(charge => {
                if (charge) {
                    window.appState.laborCharges.push({
                        description: charge.description || 'Labor charge',
                        hours: parseFloat(charge.hours) || 0,
                        rate: parseFloat(charge.ratePerHour || charge.rate) || 0
                    });
                }
            });
        }

        renderInventoryItems();
        renderLaborCharges();
        updateBillSummary();

        const serviceNotesTextarea = document.getElementById('serviceNotes');
        if (serviceNotesTextarea && billData.notes) {
            serviceNotesTextarea.value = billData.notes;
        }
    } catch (error) {
        console.error('Error loading bill information:', error);
        showNotification('Error loading bill information', 'error');
    }
}

/**
 * Fetch inventory items from API
 * @returns {Promise} Promise that resolves with inventory data
 */
function fetchInventoryItems() {
    const headers = createAuthHeaders();
    const inventorySelect = document.getElementById('inventoryItemSelect');

    if (inventorySelect) {
        clearInventorySelect(inventorySelect);
        addLoadingOption(inventorySelect);
    }

    return fetch('/serviceAdvisor/api/inventory-items', {
        method: 'GET',
        headers: headers
    })
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! Status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            populateInventoryDropdown(data);
            return data;
        })
        .catch(error => {
            handleInventoryFetchError(error, inventorySelect);
            throw error;
        });
}

/**
 * Clear inventory select dropdown
 * @param {HTMLElement} inventorySelect - Select element
 */
function clearInventorySelect(inventorySelect) {
    while (inventorySelect.options.length > 1) {
        inventorySelect.remove(1);
    }
}

/**
 * Add loading option to select
 * @param {HTMLElement} inventorySelect - Select element
 */
function addLoadingOption(inventorySelect) {
    const loadingOption = document.createElement('option');
    loadingOption.disabled = true;
    loadingOption.textContent = 'Loading inventory items...';
    inventorySelect.appendChild(loadingOption);
    inventorySelect.selectedIndex = 1;
}

/**
 * Handle inventory fetch error
 * @param {Error} error - Error object
 * @param {HTMLElement} inventorySelect - Select element
 */
function handleInventoryFetchError(error, inventorySelect) {
    console.error('Error fetching inventory items:', error);
    showNotification('Error loading inventory items. Please try again.', 'error');

    if (inventorySelect) {
        clearInventorySelect(inventorySelect);

        const errorOption = document.createElement('option');
        errorOption.disabled = true;
        errorOption.textContent = 'Error loading items. Please try again.';
        inventorySelect.appendChild(errorOption);
    }

    setTimeout(retryFetchInventoryItems, 3000);
}

/**
 * Retry fetching inventory items
 */
function retryFetchInventoryItems() {
    const headers = createAuthHeaders();

    fetch('/serviceAdvisor/api/inventory-items', {
        method: 'GET',
        headers: headers
    })
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! Status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            populateInventoryDropdown(data);
            showNotification('Inventory items loaded successfully', 'info');
        })
        .catch(error => {
            // Silent error - already showed notification
            console.error('Retry error:', error);
        });
}

/**
 * Populate inventory dropdown with data
 * @param {Array} items - Inventory items
 */
function populateInventoryDropdown(items) {
    const inventorySelect = document.getElementById('inventoryItemSelect');
    if (!inventorySelect) return;

    clearInventorySelect(inventorySelect);

    window.appState.inventoryPrices = {};
    window.appState.inventoryData = {};

    if (!items || !Array.isArray(items) || items.length === 0) {
        const noItemsOption = document.createElement('option');
        noItemsOption.disabled = true;
        noItemsOption.textContent = 'No inventory items available';
        inventorySelect.appendChild(noItemsOption);
        return;
    }

    items.forEach(item => {
        if (!item.currentStock || parseFloat(item.currentStock) <= 0) {
            return;
        }

        const option = document.createElement('option');
        option.value = item.itemId;

        const formattedPrice = new Intl.NumberFormat('en-IN', {
            style: 'currency',
            currency: 'INR',
            minimumFractionDigits: 2
        }).format(item.unitPrice);

        option.textContent = `${item.name} - ${formattedPrice} (${item.currentStock} in stock)`;
        inventorySelect.appendChild(option);

        window.appState.inventoryPrices[item.itemId] = parseFloat(item.unitPrice);

        window.appState.inventoryData[item.itemId] = {
            name: item.name,
            price: parseFloat(item.unitPrice),
            stock: parseFloat(item.currentStock),
            category: item.category || 'General'
        };
    });

    inventorySelect.selectedIndex = 0;
}

/**
 * Add inventory item to bill
 * @param {string|number} itemId - Item ID
 * @param {number} quantity - Quantity to add
 */
function addInventoryItem(itemId, quantity = 1) {
    if (!itemId) {
        showNotification('Please select an inventory item', 'error');
        return;
    }

    const parsedItemId = Number(itemId);

    if (!quantity || quantity <= 0) {
        showNotification('Quantity must be greater than zero', 'error');
        return;
    }

    if (!window.appState.inventoryData || !window.appState.inventoryData[parsedItemId]) {
        showNotification('Item data not found. Please refresh the page.', 'error');
        return;
    }

    const itemData = window.appState.inventoryData[parsedItemId];
    const existingItemIndex = window.appState.inventoryItems.findIndex(item => Number(item.key) === parsedItemId);

    const newTotalQuantity = existingItemIndex >= 0
        ? window.appState.inventoryItems[existingItemIndex].quantity + quantity
        : quantity;

    if (newTotalQuantity > itemData.stock) {
        showNotification(`Not enough stock. Only ${itemData.stock} available for ${itemData.name}`, 'error');
        return;
    }

    if (existingItemIndex >= 0) {
        window.appState.inventoryItems[existingItemIndex].quantity += quantity;
        showNotification(`Updated quantity for ${itemData.name}`, 'info');
    } else {
        window.appState.inventoryItems.push({
            key: parsedItemId,
            name: itemData.name,
            price: itemData.price,
            quantity: quantity
        });
        showNotification(`Added ${itemData.name} to service items`, 'info');
    }

    const inventorySelect = document.getElementById('inventoryItemSelect');
    const quantityInput = document.getElementById('itemQuantity');
    if (inventorySelect) inventorySelect.selectedIndex = 0;
    if (quantityInput) quantityInput.value = 1;

    renderInventoryItems();
    updateBillSummary();
}

/**
 * Render inventory items in the UI
 */
function renderInventoryItems() {
    const inventoryItemsList = document.getElementById('inventoryItemsList');
    if (!inventoryItemsList) return;

    inventoryItemsList.innerHTML = '';

    if (!window.appState.inventoryItems || window.appState.inventoryItems.length === 0) {
        const emptyRow = document.createElement('tr');
        emptyRow.innerHTML = `
            <td colspan="5" style="text-align: center; padding: 20px;">
                No inventory items added yet.
            </td>
        `;
        inventoryItemsList.appendChild(emptyRow);
        return;
    }

    const formatter = new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR',
        minimumFractionDigits: 2
    });

    window.appState.inventoryItems.forEach((item, index) => {
        const row = document.createElement('tr');
        const total = item.price * item.quantity;

        row.innerHTML = `
            <td>${item.name}</td>
            <td>${formatter.format(item.price)}</td>
            <td>
                <div class="quantity-control">
                    <button class="quantity-btn" onclick="decrementInventoryQuantity(${index})">-</button>
                    <input type="number" class="quantity-input" value="${item.quantity}" min="1" data-index="${index}" onchange="updateInventoryQuantity(this)">
                    <button class="quantity-btn" onclick="incrementInventoryQuantity(${index})">+</button>
                </div>
            </td>
            <td>${formatter.format(total)}</td>
            <td style="text-align: center;">
                <button class="btn-remove" onclick="removeInventoryItem(${index})">
                    <i class="fas fa-times"></i>
                </button>
            </td>
        `;

        inventoryItemsList.appendChild(row);
    });
}

/**
 * Increment inventory item quantity
 * @param {number} index - Item index
 */
function incrementInventoryQuantity(index) {
    if (!window.appState.inventoryItems || !window.appState.inventoryItems[index]) return;

    const item = window.appState.inventoryItems[index];
    const itemId = Number(item.key);

    if (window.appState.inventoryData && window.appState.inventoryData[itemId]) {
        const availableStock = window.appState.inventoryData[itemId].stock;

        if (item.quantity >= availableStock) {
            showNotification(`Cannot add more. Only ${availableStock} available for ${item.name}`, 'error');
            return;
        }
    }

    window.appState.inventoryItems[index].quantity++;
    renderInventoryItems();
    updateBillSummary();
}

/**
 * Decrement inventory item quantity
 * @param {number} index - Item index
 */
function decrementInventoryQuantity(index) {
    if (!window.appState.inventoryItems || !window.appState.inventoryItems[index]) return;

    if (window.appState.inventoryItems[index].quantity > 1) {
        window.appState.inventoryItems[index].quantity--;
        renderInventoryItems();
        updateBillSummary();
    }
}

/**
 * Update inventory item quantity
 * @param {HTMLElement} input - Input element
 */
function updateInventoryQuantity(input) {
    if (!input) return;

    const index = parseInt(input.getAttribute('data-index'));
    const quantity = parseInt(input.value) || 1;

    if (!window.appState.inventoryItems || !window.appState.inventoryItems[index]) return;

    const item = window.appState.inventoryItems[index];
    const itemId = Number(item.key);

    if (window.appState.inventoryData && window.appState.inventoryData[itemId]) {
        const availableStock = window.appState.inventoryData[itemId].stock;

        if (quantity > availableStock) {
            showNotification(`Cannot set quantity to ${quantity}. Only ${availableStock} available for ${item.name}`, 'error');
            input.value = item.quantity;
            return;
        }
    }

    if (quantity > 0) {
        window.appState.inventoryItems[index].quantity = quantity;
        renderInventoryItems();
        updateBillSummary();
    }
}

/**
 * Remove inventory item
 * @param {number} index - Item index
 */
function removeInventoryItem(index) {
    if (!window.appState.inventoryItems || !window.appState.inventoryItems[index]) return;

    window.appState.inventoryItems.splice(index, 1);
    renderInventoryItems();
    updateBillSummary();
}

/**
 * Add labor charge to bill
 * @param {string} description - Charge description
 * @param {number} hours - Hours worked
 * @param {number} rate - Hourly rate
 */
function addLaborCharge(description, hours, rate) {
    hours = parseFloat(hours) || 0;
    rate = parseFloat(rate) || 0;

    if (hours <= 0) {
        showNotification('Hours must be greater than zero', 'error');
        return;
    }

    if (rate <= 0) {
        showNotification('Rate must be greater than zero', 'error');
        return;
    }

    const addLaborBtn = document.getElementById('addLaborBtn');
    if (addLaborBtn) {
        addLaborBtn.disabled = true;
        addLaborBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Adding...';
    }

    const laborCharge = {
        description: description || "Labor Charge",
        hours: hours,
        rate: rate
    };

    window.appState.laborCharges = [laborCharge];

    renderLaborCharges();
    updateBillSummary();

    saveLaborChargeToAPI(laborCharge, addLaborBtn);
}

/**
 * Save labor charge to API
 * @param {Object} laborCharge - Labor charge data
 * @param {HTMLElement} addLaborBtn - Add button element
 */
function saveLaborChargeToAPI(laborCharge, addLaborBtn) {
    const token = getAuthToken();
    if (!token) {
        showNotification('Authentication required', 'error');
        if (addLaborBtn) {
            addLaborBtn.disabled = false;
            addLaborBtn.innerHTML = '<i class="fas fa-plus"></i> Add';
        }
        return;
    }

    const headers = {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
    };

    if (!window.appState.currentRequestId) {
        showNotification('No service request selected', 'error');
        if (addLaborBtn) {
            addLaborBtn.disabled = false;
            addLaborBtn.innerHTML = '<i class="fas fa-plus"></i> Add';
        }
        return;
    }

    const formattedLabor = [{
        description: laborCharge.description,
        hours: laborCharge.hours,
        rate: laborCharge.rate
    }];

    fetch(`/serviceAdvisor/api/service/${window.appState.currentRequestId}/labor-charges`, {
        method: 'POST',
        headers: headers,
        body: JSON.stringify(formattedLabor)
    })
        .then(response => {
            if (!response.ok) {
                return response.text().then(text => {
                    try {
                        return JSON.parse(text);
                    } catch (e) {
                        throw new Error(`Failed to save labor charge: ${response.status} - ${text}`);
                    }
                });
            }
            return response.json();
        })
        .then(data => {
            if (data.error) {
                throw new Error(data.error);
            }

            showNotification('Labor charge added successfully', 'success');

            const laborHours = document.getElementById('laborHours');
            const laborRate = document.getElementById('laborRate');

            if (laborHours) laborHours.value = '1';
            if (laborRate) laborRate.value = '65';
        })
        .catch(error => {
            console.error('Error saving labor charge:', error);
            showNotification('Error saving labor charge: ' + error.message, 'error');
        })
        .finally(() => {
            if (addLaborBtn) {
                addLaborBtn.disabled = false;
                addLaborBtn.innerHTML = '<i class="fas fa-plus"></i> Add';
            }
        });
}

/**
 * Render labor charges in the UI
 */
function renderLaborCharges() {
    const laborChargesList = document.getElementById('laborChargesList');
    if (!laborChargesList) return;

    laborChargesList.innerHTML = '';

    if (!window.appState.laborCharges || window.appState.laborCharges.length === 0) {
        laborChargesList.innerHTML = '<div style="padding: 10px 0; color: var(--gray);">No labor charges added yet.</div>';
        return;
    }

    const formatter = new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR',
        minimumFractionDigits: 2
    });

    window.appState.laborCharges.forEach((charge, index) => {
        const laborItem = document.createElement('div');
        laborItem.className = 'labor-item';

        const total = charge.hours * charge.rate;

        laborItem.innerHTML = `
            <div class="labor-details">
                <div class="labor-title">Labor Charge</div>
                <div class="labor-subtitle">${charge.hours} hours @ ${formatter.format(charge.rate)}/hr</div>
            </div>
            <div class="labor-price">${formatter.format(total)}</div>
            <div class="labor-actions">
                <button class="btn-remove" onclick="removeLaborCharge(${index})">
                    <i class="fas fa-times"></i>
                </button>
            </div>
        `;

        laborChargesList.appendChild(laborItem);
    });
}

/**
 * Remove labor charge
 * @param {number} index - Charge index
 */
function removeLaborCharge(index) {
    if (!window.appState.laborCharges || index < 0 || index >= window.appState.laborCharges.length) {
        return;
    }

    const loadingIcon = document.createElement('i');
    loadingIcon.className = 'fas fa-spinner fa-spin';
    loadingIcon.style.marginLeft = '10px';

    const laborList = document.getElementById('laborChargesList');
    if (laborList) {
        laborList.appendChild(loadingIcon);
    }

    window.appState.laborCharges.splice(index, 1);

    renderLaborCharges();
    updateBillSummary();

    if (window.appState.laborCharges.length === 0) {
        showNotification('Labor charge removed', 'info');
        if (laborList && loadingIcon.parentNode === laborList) {
            laborList.removeChild(loadingIcon);
        }

        updateLaborChargesAPI([], laborList, loadingIcon);
        return;
    }

    updateLaborChargesAPI(window.appState.laborCharges, laborList, loadingIcon);
}

/**
 * Update labor charges in API
 * @param {Array} laborCharges - Labor charges array
 * @param {HTMLElement} laborList - Labor list element
 * @param {HTMLElement} loadingIcon - Loading icon element
 */
function updateLaborChargesAPI(laborCharges, laborList, loadingIcon) {
    const token = getAuthToken();
    if (!token || !window.appState.currentRequestId) {
        if (laborList && loadingIcon && loadingIcon.parentNode === laborList) {
            laborList.removeChild(loadingIcon);
        }
        return;
    }

    const headers = {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : ''
    };

    const formattedCharges = laborCharges.map(charge => {
        return {
            description: charge.description || 'Labor Charge',
            hours: parseFloat(charge.hours) || 0,
            rate: parseFloat(charge.rate) || 0
        };
    });

    fetch(`/serviceAdvisor/api/service/${window.appState.currentRequestId}/labor-charges`, {
        method: 'POST',
        headers: headers,
        body: JSON.stringify(formattedCharges)
    })
        .then(response => {
            if (!response.ok) {
                return response.text().then(text => {
                    try {
                        return JSON.parse(text);
                    } catch (e) {
                        throw new Error(`Failed to update labor charges: ${response.status} - ${text}`);
                    }
                });
            }
            return response.json();
        })
        .then(data => {
            if (data.error) {
                throw new Error(data.error);
            }

            showNotification('Labor charges updated', 'info');
        })
        .catch(error => {
            console.error('Error updating labor charges:', error);
            showNotification('Error updating labor charges: ' + error.message, 'error');
        })
        .finally(() => {
            if (laborList && loadingIcon && loadingIcon.parentNode === laborList) {
                laborList.removeChild(loadingIcon);
            }
        });
}

/**
 * Update bill summary totals
 */
function updateBillSummary() {
    let partsSubtotal = 0;
    if (window.appState.inventoryItems && Array.isArray(window.appState.inventoryItems)) {
        window.appState.inventoryItems.forEach(item => {
            partsSubtotal += (item.price || 0) * (item.quantity || 0);
        });
    }

    let laborSubtotal = 0;
    if (window.appState.laborCharges && Array.isArray(window.appState.laborCharges)) {
        window.appState.laborCharges.forEach(charge => {
            laborSubtotal += (charge.hours || 0) * (charge.rate || 0);
        });
    }

    const subtotal = partsSubtotal + laborSubtotal;
    const total = subtotal;

    const formatter = new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR',
        minimumFractionDigits: 2
    });

    // Safely update elements if they exist
    safelyUpdateElementText('partsSubtotal', formatter.format(partsSubtotal));
    safelyUpdateElementText('laborSubtotal', formatter.format(laborSubtotal));
    safelyUpdateElementText('subtotalAmount', formatter.format(subtotal));
    safelyUpdateElementText('taxAmount', formatter.format(0));
    safelyUpdateElementText('totalAmount', formatter.format(total));
}

/**
 * Safely update element text content
 * @param {string} elementId - Element ID
 * @param {string} text - Text to set
 */
function safelyUpdateElementText(elementId, text) {
    const element = document.getElementById(elementId);
    if (element) {
        element.textContent = text;
    }
}

/**
 * Update bill preview in invoice tab
 */
function updateBillPreview() {
    // Initialize arrays if they don't exist
    window.appState.inventoryItems = window.appState.inventoryItems || [];
    window.appState.laborCharges = window.appState.laborCharges || [];

    // Store current vehicle and customer data globally to ensure it's available for invoice
    if (!window.appState.currentServiceData) {
        // Fetch fresh data to ensure we have the latest information
        fetchServiceDetails(window.appState.currentRequestId, true);
        return;
    }

    const invoiceItemsList = document.getElementById('invoiceItemsList');
    if (!invoiceItemsList) return;

    invoiceItemsList.innerHTML = '';

    const formatter = new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR',
        minimumFractionDigits: 2
    });

    // Safely iterate through inventory items
    if (Array.isArray(window.appState.inventoryItems)) {
        window.appState.inventoryItems.forEach(item => {
            const row = document.createElement('tr');
            const total = (item.price || 0) * (item.quantity || 0);

            row.innerHTML = `
                <td>${item.name} (Parts)</td>
                <td>${item.quantity}</td>
                <td>${formatter.format(item.price || 0)}</td>
                <td>${formatter.format(total)}</td>
            `;

            invoiceItemsList.appendChild(row);
        });
    }

    // Safely iterate through labor charges
    if (Array.isArray(window.appState.laborCharges)) {
        window.appState.laborCharges.forEach(charge => {
            const row = document.createElement('tr');
            const total = (charge.hours || 0) * (charge.rate || 0);

            row.innerHTML = `
                <td>Labor Charge</td>
                <td>${charge.hours} hrs</td>
                <td>${formatter.format(charge.rate || 0)}/hr</td>
                <td>${formatter.format(total)}</td>
            `;

            invoiceItemsList.appendChild(row);
        });
    }

    // Show empty state if no items
    if ((!Array.isArray(window.appState.inventoryItems) || window.appState.inventoryItems.length === 0) &&
        (!Array.isArray(window.appState.laborCharges) || window.appState.laborCharges.length === 0)) {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td colspan="4" style="text-align: center; padding: 20px;">
                No service items added yet.
            </td>
        `;
        invoiceItemsList.appendChild(row);
    }

    // Calculate totals
    let subtotal = 0;

    if (Array.isArray(window.appState.inventoryItems)) {
        window.appState.inventoryItems.forEach(item => {
            subtotal += (item.price || 0) * (item.quantity || 0);
        });
    }

    if (Array.isArray(window.appState.laborCharges)) {
        window.appState.laborCharges.forEach(charge => {
            subtotal += (charge.hours || 0) * (charge.rate || 0);
        });
    }

    const total = subtotal;

    // Update invoice totals
    safelyUpdateElementText('invoiceSubtotal', formatter.format(subtotal));
    safelyUpdateElementText('invoiceTotal', formatter.format(total));

    // Update invoice info fields
    if (document.querySelector('.invoice-tax span:last-child')) {
        document.querySelector('.invoice-tax span:last-child').textContent = formatter.format(0);
    }

    updateInvoiceInfoFields(window.appState.currentServiceData);
}

/**
 * Update invoice info fields
 * @param {Object} serviceData - Service data
 */
function updateInvoiceInfoFields(serviceData) {
    // Use the provided service data or try to get it from the global state
    const data = serviceData || window.appState.currentServiceData;

    if (!data) {
        console.error('No service data available for invoice');
        return;
    }

    // Use the actual data from the vehicle
    const customerName = data.customerName || 'Unknown Customer';
    const customerEmail = data.customerEmail || 'Not available';
    const customerPhone = data.customerPhone || 'Not available';

    // Get vehicle information
    const vehicleBrand = data.vehicleBrand || '';
    const vehicleModel = data.vehicleModel || '';
    const vehicleYear = data.vehicleYear || '';

    // Build the vehicle description
    let vehicleDescription = vehicleBrand;
    if (vehicleModel) {
        vehicleDescription += ' ' + vehicleModel;
    }
    if (vehicleYear) {
        vehicleDescription += ' (' + vehicleYear + ')';
    }

    const registrationNumber = data.registrationNumber || data.vehicleRegistration || 'Unknown';

    // Generate invoice date and number
    const today = new Date();
    const formattedDate = today.toLocaleDateString('en-US', {
        month: 'short',
        day: 'numeric',
        year: 'numeric'
    });

    if (!window.appState.currentInvoiceNumber) {
        window.appState.currentInvoiceNumber = 'INV-' + today.getFullYear() + '-' + String(Math.floor(Math.random() * 10000)).padStart(4, '0');
    }

    // Update customer info
    updateInvoiceField('.invoice-customer .invoice-detail:nth-child(2)', `<span>Name:</span> ${customerName}`);
    updateInvoiceField('.invoice-customer .invoice-detail:nth-child(3)', `<span>Email:</span> ${customerEmail}`);
    updateInvoiceField('.invoice-customer .invoice-detail:nth-child(4)', `<span>Phone:</span> ${customerPhone}`);

    // Update vehicle info
    updateInvoiceField('.invoice-service .invoice-detail:nth-child(2)', `<span>Vehicle:</span> ${vehicleDescription}`);
    updateInvoiceField('.invoice-service .invoice-detail:nth-child(3)', `<span>Registration:</span> ${registrationNumber}`);
    updateInvoiceField('.invoice-service .invoice-detail:nth-child(4)', `<span>Invoice Date:</span> ${formattedDate}`);
    updateInvoiceField('.invoice-service .invoice-detail:nth-child(5)', `<span>Invoice #:</span> ${window.appState.currentInvoiceNumber}`);

    // Update invoice tax line to say "No Tax"
    updateInvoiceField('.invoice-tax', `<span>Tax:</span><span id="invoiceTax">₹0.00 (No Tax)</span>`);
}

/**
 * Update invoice field content
 * @param {string} selector - CSS selector
 * @param {string} html - HTML content
 */
function updateInvoiceField(selector, html) {
    const element = document.querySelector(selector);
    if (element) {
        element.innerHTML = html;
    }
}

/**
 * Update status preview
 * @param {string} status - New status
 */
function updateStatusPreview(status) {
    const currentStatusBadge = document.getElementById('currentStatusBadge');
    if (currentStatusBadge) {
        currentStatusBadge.classList.remove('new', 'completed', 'diagnosis', 'repair', 'in-progress');

        let statusClass = getStatusClass(status);
        currentStatusBadge.classList.add(statusClass);

        let statusText = status.replace(/-/g, ' ');
        statusText = statusText.charAt(0).toUpperCase() + statusText.slice(1);
        currentStatusBadge.innerHTML = `<i class="fas fa-circle"></i> ${statusText}`;
    }
}

/**
 * Update service status
 */
function updateServiceStatus() {
    const statusSelect = document.getElementById('statusSelect');
    if (!statusSelect) return;

    const status = statusSelect.value;
    showNotification('Updating status...', 'info');

    const saveButton = document.getElementById('saveServiceItemsBtn');
    if (saveButton) saveButton.disabled = true;

    const token = getAuthToken();
    if (!token || !window.appState.currentRequestId) {
        showNotification('Authentication or request ID missing', 'error');
        if (saveButton) saveButton.disabled = false;
        return Promise.reject(new Error('Authentication or request ID missing'));
    }

    const headers = createAuthHeaders();

    const serviceAdvisorName = document.querySelector('.user-info h3')?.textContent || 'Service Advisor';

    const statusData = {
        status: status,
        notes: document.getElementById('serviceNotes')?.value || "",
        notifyCustomer: false,
        updatedBy: serviceAdvisorName,
        updatedAt: new Date().toISOString()
    };

    if (window.appState.statusHistory === undefined) {
        window.appState.statusHistory = [];
    }

    window.appState.statusHistory.push(statusData);
    updateStatusHistory();

    return fetch(`/serviceAdvisor/api/service/${window.appState.currentRequestId}/status`, {
        method: 'PUT',
        headers: headers,
        body: JSON.stringify(statusData)
    })
        .then(response => {
            if (!response.ok) {
                throw new Error(`Failed to update status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            showNotification(`Service status updated to ${status}!`);

            if (saveButton) saveButton.disabled = false;

            updateStatusInTable(window.appState.currentRequestId, status);
            updateAllStatusBadges(status);

            return data;
        })
        .catch(error => {
            console.error('Error updating status:', error);
            showNotification('Error updating status: ' + error.message, 'error');

            if (saveButton) saveButton.disabled = false;

            throw error;
        });
}

/**
 * Update all status badges
 * @param {string} status - New status
 */
function updateAllStatusBadges(status) {
    let statusClass = getStatusClass(status);

    const currentStatusBadge = document.getElementById('currentStatusBadge');
    if (currentStatusBadge) {
        updateStatusBadgeClasses(currentStatusBadge, statusClass);
        currentStatusBadge.innerHTML = `<i class="fas fa-circle"></i> ${status}`;
    }

    const statusDisplays = document.querySelectorAll('.status-display .status-badge');
    statusDisplays.forEach(badge => {
        updateStatusBadgeClasses(badge, statusClass);
        badge.innerHTML = `<i class="fas fa-circle"></i> ${status}`;
    });

    const detailStatus = document.querySelector('.detail-card:nth-of-type(3) .detail-row:nth-child(3) .detail-value .status-badge');
    if (detailStatus) {
        updateStatusBadgeClasses(detailStatus, statusClass);
        detailStatus.innerHTML = `<i class="fas fa-circle"></i> ${status}`;
    }
}

/**
 * Update status badge classes
 * @param {HTMLElement} badge - Badge element
 * @param {string} newClass - New class to add
 */
function updateStatusBadgeClasses(badge, newClass) {
    badge.classList.remove('new', 'in-progress', 'completed', 'repair', 'inspection', 'billing', 'feedback');
    badge.classList.add(newClass.toLowerCase());
}

/**
 * Update status history display
 */
function updateStatusHistory() {
    const statusHistoryContainer = document.getElementById('statusHistory');
    if (!statusHistoryContainer) return;

    statusHistoryContainer.innerHTML = '';

    if (!window.appState.statusHistory || window.appState.statusHistory.length === 0) {
        const initialStatus = {
            status: 'New',
            updatedBy: 'System',
            updatedAt: new Date().toISOString()
        };
        window.appState.statusHistory = [initialStatus];
    }

    const serviceFlow = [
        { status: 'New', label: 'New' },
        { status: 'Diagnosis', label: 'Diagnosis' },
        { status: 'Repair', label: 'Repair' },
        { status: 'Inspection', label: 'Inspection' },
        { status: 'Billing', label: 'Billing' },
        { status: 'Feedback', label: 'Feedback' },
        { status: 'Completed', label: 'Completed' }
    ];

    const currentStatus = window.appState.statusHistory[window.appState.statusHistory.length - 1].status;
    const currentStatusIndex = serviceFlow.findIndex(step => step.status === currentStatus);

    createStatusTimeline(statusHistoryContainer, serviceFlow, currentStatusIndex);
    createStatusHistoryList(statusHistoryContainer);
}

/**
 * Create status timeline UI
 * @param {HTMLElement} container - Container element
 * @param {Array} serviceFlow - Service flow steps
 * @param {number} currentStatusIndex - Current status index
 */
function createStatusTimeline(container, serviceFlow, currentStatusIndex) {
    const timelineContainer = document.createElement('div');
    timelineContainer.className = 'status-timeline-graph';
    container.appendChild(timelineContainer);

    serviceFlow.forEach((step, index) => {
        let stepStatus = index < currentStatusIndex ? 'completed' :
            index === currentStatusIndex ? 'in-progress' : 'upcoming';

        const historyEntry = window.appState.statusHistory.find(entry => entry.status === step.status);

        const stepElement = document.createElement('div');
        stepElement.className = `timeline-step ${stepStatus}`;

        let dateInfo = '';
        if (historyEntry) {
            dateInfo = formatDateTimeForHistory(historyEntry.updatedAt);
        }

        stepElement.innerHTML = `
            <div class="timeline-step-connector ${index === 0 ? 'first' : ''}"></div>
            <div class="timeline-step-badge">
                <i class="fas ${getStepIcon(stepStatus)}"></i>
            </div>
            <div class="timeline-step-content">
                <div class="timeline-step-title">${step.label}</div>
                ${dateInfo ? `<div class="timeline-step-date">${dateInfo}</div>` : ''}
            </div>
            ${index < serviceFlow.length - 1 ? '<div class="timeline-step-connector"></div>' : ''}
        `;

        timelineContainer.appendChild(stepElement);
    });
}

/**
 * Get icon for timeline step
 * @param {string} stepStatus - Step status
 * @returns {string} Icon class
 */
function getStepIcon(stepStatus) {
    return stepStatus === 'completed' ? 'fa-check' :
        stepStatus === 'in-progress' ? 'fa-sync' : 'fa-clock';
}

/**
 * Create status history list UI
 * @param {HTMLElement} container - Container element
 */
function createStatusHistoryList(container) {
    const historyTitle = document.createElement('h4');
    historyTitle.className = 'section-title';
    historyTitle.textContent = 'Status History';
    container.appendChild(historyTitle);

    const historyList = document.createElement('div');
    historyList.className = 'status-history-list';
    container.appendChild(historyList);

    if (!window.appState.statusHistory || !Array.isArray(window.appState.statusHistory)) {
        return;
    }

    for (let i = window.appState.statusHistory.length - 1; i >= 0; i--) {
        const statusData = window.appState.statusHistory[i];
        const statusClass = getStatusClass(statusData.status);
        const formattedDateTime = formatDateTimeForHistory(statusData.updatedAt);

        const historyItem = document.createElement('div');
        historyItem.className = 'status-history-item';
        historyItem.innerHTML = `
            <div class="status-history-badge ${statusClass.toLowerCase()}">
                <i class="fas fa-circle"></i>
            </div>
            <div class="status-history-content">
                <div class="status-history-title">${statusData.status}</div>
                <div class="status-history-meta">
                    <span class="status-history-time">${formattedDateTime}</span>
                    <span class="status-history-user">${statusData.updatedBy}</span>
                </div>
            </div>
        `;

        historyList.appendChild(historyItem);
    }
}

/**
 * Format date time for history display
 * @param {string} dateTimeString - Date time string
 * @returns {string} Formatted date time
 */
function formatDateTimeForHistory(dateTimeString) {
    try {
        const date = new Date(dateTimeString);
        const formattedDate = date.toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric'
        });
        const formattedTime = date.toLocaleTimeString('en-US', {
            hour: '2-digit',
            minute: '2-digit'
        });
        return `${formattedDate} ${formattedTime}`;
    } catch (e) {
        return dateTimeString || 'Unknown date';
    }
}

/**
 * Update status in table
 * @param {number} requestId - Request ID
 * @param {string} status - New status
 */
function updateStatusInTable(requestId, status) {
    const row = document.querySelector(`#vehiclesTableBody tr[data-id="${requestId}"]`);
    if (row) {
        const statusCell = row.querySelector('td:nth-child(5)');
        if (statusCell) {
            const { statusClass, statusText } = getStatusDisplay(status);

            statusCell.innerHTML = `
                <span class="status-badge ${statusClass}">
                    <i class="fas fa-circle"></i> ${statusText}
                </span>
            `;
        }
    } else {
        fetchAssignedVehicles();
    }
}

/**
 * Save service items
 */
function saveServiceItems() {
    const promises = [];

    const laborPromise = saveLaborCharges().catch(error => {
        console.error("Error saving labor charges:", error);
        return null;
    });
    promises.push(laborPromise);

    if (window.appState.inventoryItems && window.appState.inventoryItems.length > 0) {
        const inventoryPromise = saveInventoryItems().catch(error => {
            console.error("Error saving inventory items:", error);
            return null;
        });
        promises.push(inventoryPromise);
    }

    Promise.all(promises)
        .then(results => {
            showNotification('All service items saved successfully!', 'success');
            setTimeout(() => {
                if (window.appState.currentRequestId) {
                    openVehicleDetails(window.appState.currentRequestId);
                }
            }, 1000);
        })
        .catch(error => {
            showNotification('Error: ' + error.message, 'error');
        });
}

/**
 * Save labor charges
 * @returns {Promise} Promise that resolves when charges are saved
 */
function saveLaborCharges() {
    if (!window.appState.laborCharges || window.appState.laborCharges.length === 0) {
        showNotification('No labor charges to save', 'info');
        return Promise.resolve();
    }

    showNotification('Saving labor charges...', 'info');

    const saveButton = document.getElementById('saveInvoiceBtn');
    if (saveButton) saveButton.disabled = true;

    const token = getAuthToken();
    if (!token || !window.appState.currentRequestId) {
        showNotification('Authentication or request ID missing', 'error');
        if (saveButton) saveButton.disabled = false;
        return Promise.reject(new Error('Authentication or request ID missing'));
    }

    const headers = {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
    };

    const formattedCharges = window.appState.laborCharges.map(charge => {
        return {
            description: charge.description || 'Labor Charge',
            hours: parseFloat(charge.hours) || 0,
            rate: parseFloat(charge.rate) || 0
        };
    });

    const validCharges = formattedCharges.filter(
        charge => charge.hours > 0 && charge.rate > 0
    );

    if (validCharges.length === 0) {
        showNotification('No valid labor charges to save', 'warning');
        if (saveButton) saveButton.disabled = false;
        return Promise.resolve();
    }

    return fetch(`/serviceAdvisor/api/service/${window.appState.currentRequestId}/labor-charges`, {
        method: 'POST',
        headers: headers,
        body: JSON.stringify(validCharges)
    })
        .then(response => {
            if (!response.ok) {
                return response.text().then(text => {
                    throw new Error(`Failed to save labor charges: ${response.status} - ${text}`);
                });
            }
            return response.json();
        })
        .then(data => {
            showNotification('Labor charges saved successfully!', 'success');
            if (saveButton) saveButton.disabled = false;
            return data;
        })
        .catch(error => {
            showNotification('Error saving labor charges: ' + error.message, 'error');
            if (saveButton) saveButton.disabled = false;
            throw error;
        });
}

/**
 * Save inventory items
 * @returns {Promise} Promise that resolves when items are saved
 */
function saveInventoryItems() {
    if (!window.appState.inventoryItems || !window.appState.currentRequestId) {
        return Promise.reject(new Error('Missing inventory items or request ID'));
    }

    const items = window.appState.inventoryItems.map(item => {
        return {
            itemId: Number(item.key),
            name: item.name,
            quantity: Number(item.quantity),
            unitPrice: Number(item.price)
        };
    });

    const materialsRequest = {
        items: items,
        replaceExisting: true
    };

    return fetch(`/serviceAdvisor/api/service/${window.appState.currentRequestId}/inventory-items`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${getAuthToken()}`
        },
        body: JSON.stringify(materialsRequest)
    })
        .then(response => {
            if (!response.ok) {
                return response.text().then(text => {
                    throw new Error(`Failed to save inventory items: ${response.status} - ${text}`);
                });
            }
            return response.json();
        })
        .then(data => {
            return data;
        })
        .catch(error => {
            showNotification('Error saving inventory items: ' + error.message, 'error');
            throw error;
        });
}

/**
 * Mark service as complete
 */
function markServiceComplete() {
    const requestId = window.appState.currentRequestId;
    if (!requestId) {
        showNotification('No service selected', 'error');
        return;
    }

    showNotification('Marking service as completed...', 'info');

    const token = getAuthToken();
    if (!token) {
        showNotification('Authentication required', 'error');
        return;
    }

    const headers = {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
    };

    const data = {
        status: "Completed",
        notes: "Service completed by " + document.querySelector('.user-info h3').textContent
    };

    fetch(`/serviceAdvisor/api/service/${requestId}/status`, {
        method: 'PUT',
        headers: headers,
        body: JSON.stringify(data)
    })
        .then(response => {
            if (!response.ok) {
                throw new Error(`Failed to update status: ${response.status}`);
            }
            return response.json();
        })
        .then(result => {
            showNotification('Service marked as completed!', 'success');

            updateAllStatusBadges("Completed");

            setTimeout(() => {
                const vehicleDetailsModal = document.getElementById('vehicleDetailsModal');
                if (vehicleDetailsModal) {
                    vehicleDetailsModal.classList.remove('show');
                }
                fetchAssignedVehicles();
            }, 1500);
        })
        .catch(error => {
            console.error('Error marking service complete:', error);
            showNotification('Error: ' + error.message, 'error');
        });
}

/**
 * Filter vehicles by type
 * @param {string} filter - Filter type
 */
function filterVehicles(filter) {
    const rows = document.querySelectorAll('#vehiclesTableBody tr');

    rows.forEach(row => {
        const statusBadge = row.querySelector('.status-badge');
        if (!statusBadge) {
            row.style.display = filter === 'all' ? '' : 'none';
            return;
        }

        const status = statusBadge.textContent.trim().toLowerCase();

        if (filter === 'all') {
            row.style.display = '';
        } else if (filter === 'new' && (status.includes('new') || status.includes('received'))) {
            row.style.display = '';
        } else if (filter === 'in-progress' && (status.includes('diagnosis') || status.includes('repair') || status.includes('in progress'))) {
            row.style.display = '';
        } else {
            row.style.display = 'none';
        }
    });

    updateFilterButtonText(filter);
}

/**
 * Update filter button text
 * @param {string} filter - Selected filter
 */
function updateFilterButtonText(filter) {
    const filterButton = document.getElementById('filterButton');
    if (filterButton) {
        const filterTexts = {
            'all': 'All Vehicles',
            'new': 'New Assignments',
            'in-progress': 'In Progress'
        };

        filterButton.innerHTML = `<i class="fas fa-filter"></i> ${filterTexts[filter] || 'Filter'} <i class="fas fa-chevron-down" style="font-size: 0.8rem;"></i>`;
    }
}

/**
 * Filter vehicles by search term
 * @param {string} searchTerm - Search term
 */
function filterVehiclesBySearch(searchTerm) {
    const rows = document.querySelectorAll('#vehiclesTableBody tr');

    rows.forEach(row => {
        const textContent = row.textContent.toLowerCase();

        if (textContent.includes(searchTerm)) {
            row.style.display = '';
        } else {
            row.style.display = 'none';
        }
    });
}

/**
 * Show notification
 * @param {string} message - Message to show
 * @param {string} type - Notification type
 */
function showNotification(message, type = 'success') {
    const notification = document.getElementById('successNotification');
    if (!notification) return;

    notification.className = 'notification';
    notification.classList.add(type);

    const notificationMessage = document.getElementById('notificationMessage');
    if (notificationMessage) {
        notificationMessage.textContent = message;
    }

    notification.classList.add('show');

    setTimeout(() => {
        notification.classList.remove('show');
    }, 3000);
}

// Expose necessary functions to global scope
window.openVehicleDetails = openVehicleDetails;
window.incrementInventoryQuantity = incrementInventoryQuantity;
window.decrementInventoryQuantity = decrementInventoryQuantity;
window.updateInventoryQuantity = updateInventoryQuantity;
window.removeInventoryItem = removeInventoryItem;
window.removeLaborCharge = removeLaborCharge;
window.handleTabClick = handleTabClick;
window.updateBillPreview = updateBillPreview;
window.updateInvoiceInfoFields = updateInvoiceInfoFields;