/**
 * Albany Vehicle Service System - Service Advisor Dashboard
 * Core functionality for managing vehicle service assignments
 */

document.addEventListener('DOMContentLoaded', () => {
    // Initialize state
    const state = {
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

    // Initialize from URL or session storage
    initializeFromURL();

    // Set up dashboard components
    setupUI();
    fetchAssignedVehicles();

    // Set up refresh interval (5 minutes)
    setInterval(fetchAssignedVehicles, 300000);
});

/**
 * Initialize state from URL parameters and session storage
 */
function initializeFromURL() {
    const urlParams = new URLSearchParams(window.location.search);
    const token = urlParams.get('token');
    if (token) {
        sessionStorage.setItem('jwt-token', token);
    }
}

/**
 * Set up all UI components and event listeners
 */
function setupUI() {
    addRefreshButton();
    initializeEventListeners();
    initializeStatusEvents();
    updateModalFooterButtons();
    addConnectionIndicator();

    // Start connection check
    checkApiConnection();
    setInterval(checkApiConnection, 30000);
}

/**
 * Add refresh button to header
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
 * Add connection status indicator to header
 * Fixes layout issues by keeping it compact and properly positioned
 */
function addConnectionIndicator() {
    const header = document.querySelector('.header-actions');
    if (!header) return;

    const statusIndicator = document.createElement('div');
    statusIndicator.id = 'connection-status';
    statusIndicator.style.display = 'inline-flex';
    statusIndicator.style.alignItems = 'center';
    statusIndicator.style.marginLeft = '10px';
    statusIndicator.style.gap = '5px';
    statusIndicator.innerHTML = `
        <span id="status-icon" class="status-indicator" 
              style="width: 8px; height: 8px; border-radius: 50%; 
                     background-color: #ccc; display: inline-block;"></span>
        <span id="status-text" style="font-size: 0.75rem; color: #666; 
                                      display: inline-block;">Checking...</span>
    `;

    header.appendChild(statusIndicator);
}

/**
 * Check API connection status
 */
function checkApiConnection() {
    const statusIcon = document.getElementById('status-icon');
    const statusText = document.getElementById('status-text');

    if (!statusIcon || !statusText) return;

    const token = getAuthToken();
    const headers = {};
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }

    fetch('/serviceAdvisor/api/assigned-vehicles', {
        method: 'HEAD',
        headers: headers
    })
        .then(response => {
            if (response.ok) {
                statusIcon.style.backgroundColor = '#38b000';
                statusText.textContent = 'Connected';
                statusText.style.color = '#38b000';
            } else {
                statusIcon.style.backgroundColor = '#ffaa00';
                statusText.textContent = 'Error';
                statusText.style.color = '#ffaa00';
            }
        })
        .catch(error => {
            statusIcon.style.backgroundColor = '#d90429';
            statusText.textContent = 'Offline';
            statusText.style.color = '#d90429';
        });
}

/**
 * Set up all event listeners for the dashboard
 */
function initializeEventListeners() {
    // Filter button events
    setupFilterEvents();

    // Search functionality
    setupSearchEvents();

    // Modal control events
    setupModalEvents();

    // Tab navigation
    setupTabEvents();

    // Service item events
    setupServiceItemEvents();
}

/**
 * Set up filter button events
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
 * Set up search functionality
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
 * Set up modal control events
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

    // Close modals with escape key and backdrop click
    setupModalCloseEvents();
}

/**
 * Set up modal close events (escape key and backdrop click)
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
 * Set up service item events (inventory and labor)
 */
function setupServiceItemEvents() {
    // Add inventory item button
    const addItemBtn = document.getElementById('addItemBtn');
    if (addItemBtn) {
        addItemBtn.addEventListener('click', () => {
            const inventoryItemSelect = document.getElementById('inventoryItemSelect');
            const itemQuantity = document.getElementById('itemQuantity');

            if (inventoryItemSelect.value) {
                addInventoryItem(inventoryItemSelect.value, parseInt(itemQuantity.value) || 1);
            } else {
                showNotification('Please select an inventory item', 'error');
            }
        });
    }

    // Add labor charge button
    const addLaborBtn = document.getElementById('addLaborBtn');
    if (addLaborBtn) {
        addLaborBtn.addEventListener('click', () => {
            const laborHours = document.getElementById('laborHours');
            const laborRate = document.getElementById('laborRate');

            const hours = parseFloat(laborHours.value);
            const rate = parseFloat(laborRate.value);

            if (!isNaN(hours) && !isNaN(rate) && hours > 0 && rate > 0) {
                addLaborCharge("Labor Charge", hours, rate);
            } else {
                showNotification('Please enter valid hours and rate', 'error');
            }
        });
    }

    // Save invoice button
    const saveInvoiceBtn = document.getElementById('saveInvoiceBtn');
    if (saveInvoiceBtn) {
        saveInvoiceBtn.addEventListener('click', saveServiceItems);
    }

    // Preview invoice button
    const previewInvoiceBtn = document.getElementById('previewInvoiceBtn');
    if (previewInvoiceBtn) {
        previewInvoiceBtn.addEventListener('click', () => {
            const generateInvoiceTab = document.querySelector('.tab[data-tab="generate-invoice"]');
            if (generateInvoiceTab) {
                handleTabClick(generateInvoiceTab);
            }
        });
    }

    // Mark complete button
    const markCompleteBtn = document.getElementById('markCompleteBtn');
    if (markCompleteBtn) {
        markCompleteBtn.addEventListener('click', markServiceComplete);
    }
}

/**
 * Initialize status events
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
 * Handle tab click
 * @param {HTMLElement} tabElement - The clicked tab element
 */
function handleTabClick(tabElement) {
    const tabs = document.querySelectorAll('.tab');
    const tabContents = document.querySelectorAll('.tab-content');

    tabs.forEach(tab => tab.classList.remove('active'));
    tabContents.forEach(content => content.classList.remove('active'));

    tabElement.classList.add('active');

    const tabName = tabElement.getAttribute('data-tab');
    document.getElementById(`${tabName}-tab`)?.classList.add('active');

    updateModalFooterButtons();

    if (tabName === 'generate-invoice') {
        setTimeout(updateBillPreview, 100);
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
 * Get authentication token from URL or session storage
 * @returns {string|null} - Auth token or null if not found
 */
function getAuthToken() {
    const urlParams = new URLSearchParams(window.location.search);
    const token = urlParams.get('token') || sessionStorage.getItem('jwt-token');
    return token;
}

/**
 * Create authorization headers for API requests
 * @returns {Object} - Headers object with Content-Type and Authorization
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
    // Global fetch retry state
    window.fetchRetries = window.fetchRetries || 0;
    const MAX_RETRIES = 3;

    const token = getAuthToken();
    const headers = {};

    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
        sessionStorage.setItem('jwt-token', token);
    }

    // Show loading state
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
                throw new Error(`HTTP error! Status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            if (Array.isArray(data)) {
                window.fetchRetries = 0;
                updateVehiclesTable(data);
            } else {
                throw new Error('Invalid data format: expected an array');
            }
        })
        .catch(error => {
            if (window.fetchRetries < MAX_RETRIES) {
                window.fetchRetries++;
                setTimeout(fetchAssignedVehicles, 1000);
                showNotification(`Retrying to load data (${window.fetchRetries}/${MAX_RETRIES})...`, 'info');
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
 * @param {Array} vehicles - Array of vehicle data
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

        // Format date
        const formattedDate = formatDate(vehicle.startDate);

        // Get status class and text
        const { statusClass, statusText } = getStatusDisplay(vehicle.status);

        // Format vehicle name correctly
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
 * Format date string for display
 * @param {string} dateString - Date string to format
 * @returns {string} - Formatted date string
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
 * @returns {Object} - Object with statusClass and statusText
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
 * Get formatted vehicle name
 * @param {Object} vehicle - Vehicle data
 * @returns {string} - Formatted vehicle name
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
    window.currentRequestId = requestId;

    const vehicleDetailsModal = document.getElementById('vehicleDetailsModal');
    vehicleDetailsModal.classList.add('show');

    // Show loading state
    const detailsTab = document.getElementById('details-tab');
    if (detailsTab) {
        detailsTab.innerHTML = `
            <div style="text-align: center; padding: 50px;">
                <i class="fas fa-spinner fa-spin" style="font-size: 32px; margin-bottom: 20px;"></i>
                <p>Loading service details...</p>
            </div>
        `;
    }

    // Reset tabs
    resetTabs();

    // Fetch service details
    fetchServiceDetails(requestId);
}

/**
 * Reset tabs to default state
 */
function resetTabs() {
    const tabs = document.querySelectorAll('.tab');
    tabs.forEach(tab => tab.classList.remove('active'));
    document.querySelector('.tab[data-tab="details"]').classList.add('active');

    const tabContents = document.querySelectorAll('.tab-content');
    tabContents.forEach(content => content.classList.remove('active'));
    document.getElementById('details-tab').classList.add('active');

    updateModalFooterButtons();
}

/**
 * Fetch service details from API
 * @param {number} requestId - Service request ID
 */
function fetchServiceDetails(requestId) {
    const token = getAuthToken();
    const headers = {};
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }

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
            // Generate invoice number if needed
            window.currentInvoiceNumber = 'INV-' + new Date().getFullYear() + '-' +
                String(Math.floor(Math.random() * 10000)).padStart(4, '0');

            // Load vehicle details
            loadVehicleDetails(data);

            // Load current bill data
            if (data.currentBill) {
                loadCurrentBill(data.currentBill);
            }


            // Initialize status history
            if (data.status) {
                window.statusHistory = [{
                    status: data.status,
                    updatedBy: data.serviceAdvisor || 'Service Advisor',
                    updatedAt: data.lastStatusUpdate || new Date().toISOString()
                }];
                updateStatusHistory();
            }

            // Fetch inventory items
            fetchInventoryItems();
        })
        .catch(error => {
            showNotification('Error loading service details: ' + error.message, 'error');
            showErrorInDetailsTab(error, requestId);
        });
}


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
 * @param {Object} data - Vehicle details data
 */
function loadVehicleDetails(data) {
    if (!data) {
        showNotification('Error: No data received from server', 'error');
        return;
    }

    try {
        createDetailCardsIfNeeded();

        // Vehicle information card
        const vehicleCard = document.querySelector('.detail-card:nth-of-type(1)');
        if (vehicleCard) {
            const makeModel = `${data.vehicleBrand || ''} ${data.vehicleModel || ''}`.trim();
            setDetailValue(vehicleCard, 1, makeModel || 'Not specified');
            setDetailValue(vehicleCard, 2, data.registrationNumber || 'Not specified');
            // Make sure year is displayed correctly
            setDetailValue(vehicleCard, 3, data.vehicleYear || 'Not specified');
            setDetailValue(vehicleCard, 4, data.vehicleType || 'Not specified');
        }

        // Customer information card
        const customerCard = document.querySelector('.detail-card:nth-of-type(2)');
        if (customerCard) {
            setDetailValue(customerCard, 1, data.customerName || 'Not specified');
            setDetailValue(customerCard, 2, data.customerEmail || 'Not specified');
            setDetailValue(customerCard, 3, data.customerPhone || 'Not specified');
        }

        // Service information card
        const serviceCard = document.querySelector('.detail-card:nth-of-type(3)');
        if (serviceCard) {
            setDetailValue(serviceCard, 1, data.serviceType || 'General Service');
            setDetailValue(serviceCard, 2, formatDate(data.requestDate));

            // Update status with badge
            updateStatusBadge(serviceCard, data.status);

            setDetailValue(serviceCard, 4, data.additionalDescription || 'No additional description provided.');
        }

        // Update vehicle summary elements
        updateVehicleSummary(data);
    } catch (error) {
        console.error('Error displaying vehicle details:', error);
        showNotification('Error displaying vehicle details', 'error');
    }
}

/**
 * Set detail value in card
 * @param {HTMLElement} cardElement - Card element
 * @param {number} index - Row index (1-based)
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
 * Update vehicle summary elements
 * @param {Object} data - Vehicle data
 */
function updateVehicleSummary(data) {
    // Update vehicle info summary headers
    const vehicleSummaryElements = document.querySelectorAll('.vehicle-summary .vehicle-info-summary h4');
    const vehicleInfo = buildVehicleInfoString(data);

    vehicleSummaryElements.forEach(element => {
        element.textContent = vehicleInfo;
    });

    // Update customer info in summary
    const customerElements = document.querySelectorAll('.vehicle-summary .vehicle-info-summary p');
    customerElements.forEach(element => {
        element.textContent = `Customer: ${data.customerName || 'Unknown'}`;
    });

    // Update status badges
    const statusDisplayElements = document.querySelectorAll('.vehicle-summary .status-display');
    const { statusClass, statusText } = getStatusDisplay(data.status);

    statusDisplayElements.forEach(element => {
        element.innerHTML = `
            <span class="status-badge ${statusClass}" id="currentStatusBadge">
                <i class="fas fa-circle"></i> ${statusText}
            </span>
        `;
    });

    // Update status select
    updateStatusSelect(data.status);
}

/**
 * Build vehicle info string
 * @param {Object} data - Vehicle data
 * @returns {string} - Formatted vehicle info string
 */
function buildVehicleInfoString(data) {
    const brand = data.vehicleBrand || '';
    const model = data.vehicleModel || '';
    const year = data.vehicleYear ? ` (${data.vehicleYear})` : '';
    const reg = data.registrationNumber ? ` - ${data.registrationNumber}` : '';

    return `${brand} ${model}${year}${reg}`.trim() || 'Unknown Vehicle';
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

    // Vehicle information card
    row.appendChild(createDetailCard('Vehicle Information', [
        { label: 'Make/Model:', value: 'Loading...' },
        { label: 'Registration:', value: 'Loading...' },
        { label: 'Year:', value: 'Loading...' },
        { label: 'Type:', value: 'Loading...' }
    ]));

    // Customer information card
    row.appendChild(createDetailCard('Customer Information', [
        { label: 'Name:', value: 'Loading...' },
        { label: 'Email:', value: 'Loading...' },
        { label: 'Phone:', value: 'Loading...' }
    ]));

    // Service information card
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
 * @param {Array} rows - Array of {label, value} objects
 * @returns {HTMLElement} - Card element
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
 * @returns {string} - CSS class name
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
        window.inventoryItems = [];
        window.laborCharges = [];

        // Load materials
        if (billData.materials && Array.isArray(billData.materials)) {
            billData.materials.forEach(item => {
                if (item && item.itemId) {
                    window.inventoryItems.push({
                        key: item.itemId,
                        name: item.name || `Item ${item.itemId}`,
                        price: parseFloat(item.unitPrice) || 0,
                        quantity: parseInt(item.quantity) || 1
                    });
                }
            });
        }

        // Load labor charges
        if (billData.laborCharges && Array.isArray(billData.laborCharges)) {
            billData.laborCharges.forEach(charge => {
                if (charge) {
                    window.laborCharges.push({
                        description: charge.description || 'Labor charge',
                        hours: parseFloat(charge.hours) || 0,
                        rate: parseFloat(charge.ratePerHour) || 0
                    });
                }
            });
        }

        // Render items and update UI
        renderInventoryItems();
        renderLaborCharges();
        updateBillSummaryUI(billData);

        // Set service notes
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
 * Update bill summary UI elements
 */
function updateBillSummaryUI(billData) {
    const formatCurrency = (value) => {
        const num = parseFloat(value) || 0;
        return `₹${num.toFixed(2)}`;
    };

    document.getElementById('partsSubtotal').textContent = formatCurrency(billData.partsSubtotal);
    document.getElementById('laborSubtotal').textContent = formatCurrency(billData.laborSubtotal);
    document.getElementById('subtotalAmount').textContent = formatCurrency(billData.subtotal);

}

/**
 * Fetch inventory items from API
 * @returns {Promise} - Promise that resolves with inventory items
 */
function fetchInventoryItems() {
    const token = getAuthToken();
    const headers = createAuthHeaders();
    const inventorySelect = document.getElementById('inventoryItemSelect');

    // Show loading state
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
 * Add loading option to inventory select
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
    const token = getAuthToken();
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
            console.error('Error retrying inventory fetch:', error);
        });
}

/**
 * Load existing labor charges from API
 */
function loadExistingLaborCharges() {
    if (!window.currentRequestId) return;

    const token = getAuthToken();
    const headers = {
        'Authorization': token ? `Bearer ${token}` : ''
    };

    fetch(`/serviceAdvisor/api/service/${window.currentRequestId}/labor-charges`, {
        method: 'GET',
        headers: headers
    })
        .then(response => {
            if (!response.ok) {
                throw new Error(`Failed to load labor charges: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            if (data.laborMinutes > 0 && data.laborCost > 0) {
                // Calculate hours from minutes
                const hours = data.laborHours || (data.laborMinutes / 60);
                const rate = data.hourlyRate || (data.laborCost / hours);

                // Add to local array
                window.laborCharges = [{
                    description: "Labor Charge",
                    hours: hours,
                    rate: rate
                }];

                renderLaborCharges();
                updateBillSummary();
            }
        })
        .catch(error => {
            console.error('Error loading labor charges:', error);
        });
}

/**
 * Populate inventory dropdown with items
 * @param {Array} items - Inventory items
 */
function populateInventoryDropdown(items) {
    const inventorySelect = document.getElementById('inventoryItemSelect');
    if (!inventorySelect) return;

    clearInventorySelect(inventorySelect);

    window.inventoryPrices = {};
    window.inventoryData = {};

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

        window.inventoryPrices[item.itemId] = parseFloat(item.unitPrice);

        window.inventoryData[item.itemId] = {
            name: item.name,
            price: parseFloat(item.unitPrice),
            stock: parseFloat(item.currentStock),
            category: item.category || 'General'
        };
    });

    inventorySelect.selectedIndex = 0;
}

/**
 * Add inventory item to service
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

    if (!window.inventoryData || !window.inventoryData[parsedItemId]) {
        showNotification('Item data not found. Please refresh the page.', 'error');
        return;
    }

    const itemData = window.inventoryData[parsedItemId];
    const existingItemIndex = window.inventoryItems.findIndex(item => Number(item.key) === parsedItemId);

    // Check stock limits
    const newTotalQuantity = existingItemIndex >= 0
        ? window.inventoryItems[existingItemIndex].quantity + quantity
        : quantity;

    if (newTotalQuantity > itemData.stock) {
        showNotification(`Not enough stock. Only ${itemData.stock} available for ${itemData.name}`, 'error');
        return;
    }

    // Update or add item
    if (existingItemIndex >= 0) {
        window.inventoryItems[existingItemIndex].quantity += quantity;
        showNotification(`Updated quantity for ${itemData.name}`, 'info');
    } else {
        window.inventoryItems.push({
            key: parsedItemId,
            name: itemData.name,
            price: itemData.price,
            quantity: quantity
        });
        showNotification(`Added ${itemData.name} to service items`, 'info');
    }

    // Reset form
    const inventorySelect = document.getElementById('inventoryItemSelect');
    const quantityInput = document.getElementById('itemQuantity');
    if (inventorySelect) inventorySelect.selectedIndex = 0;
    if (quantityInput) quantityInput.value = 1;

    // Update UI
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

    if (window.inventoryItems.length === 0) {
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

    window.inventoryItems.forEach((item, index) => {
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
 * Increment inventory quantity
 * @param {number} index - Item index
 */
function incrementInventoryQuantity(index) {
    if (!window.inventoryItems[index]) return;

    const item = window.inventoryItems[index];
    const itemId = Number(item.key);

    if (window.inventoryData && window.inventoryData[itemId]) {
        const availableStock = window.inventoryData[itemId].stock;

        if (item.quantity >= availableStock) {
            showNotification(`Cannot add more. Only ${availableStock} available for ${item.name}`, 'error');
            return;
        }
    }

    window.inventoryItems[index].quantity++;
    renderInventoryItems();
    updateBillSummary();
}

/**
 * Decrement inventory quantity
 * @param {number} index - Item index
 */
function decrementInventoryQuantity(index) {
    if (window.inventoryItems[index].quantity > 1) {
        window.inventoryItems[index].quantity--;
        renderInventoryItems();
        updateBillSummary();
    }
}

/**
 * Update inventory quantity
 * @param {HTMLElement} input - Input element
 */
function updateInventoryQuantity(input) {
    const index = parseInt(input.getAttribute('data-index'));
    const quantity = parseInt(input.value) || 1;

    if (!window.inventoryItems[index]) return;

    const item = window.inventoryItems[index];
    const itemId = Number(item.key);

    if (window.inventoryData && window.inventoryData[itemId]) {
        const availableStock = window.inventoryData[itemId].stock;

        if (quantity > availableStock) {
            showNotification(`Cannot set quantity to ${quantity}. Only ${availableStock} available for ${item.name}`, 'error');
            input.value = item.quantity;
            return;
        }
    }

    if (quantity > 0) {
        window.inventoryItems[index].quantity = quantity;
        renderInventoryItems();
        updateBillSummary();
    }
}

/**
 * Remove inventory item
 * @param {number} index - Item index
 */
function removeInventoryItem(index) {
    window.inventoryItems.splice(index, 1);
    renderInventoryItems();
    updateBillSummary();
}

/**
 * Add labor charge
 * @param {string} description - Labor description
 * @param {number} hours - Hours
 * @param {number} rate - Hourly rate
 */
function addLaborCharge(description, hours, rate) {
    // Validate inputs
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

    // Show loading state
    const addLaborBtn = document.getElementById('addLaborBtn');
    if (addLaborBtn) {
        addLaborBtn.disabled = true;
        addLaborBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Adding...';
    }

    // Create labor charge object
    const laborCharge = {
        description: description || "Labor Charge",
        hours: hours,
        rate: rate
    };

    // Add to local array (replace existing)
    window.laborCharges = [laborCharge];

    // Update UI immediately
    renderLaborCharges();
    updateBillSummary();

    // Save to API
    saveLaborChargeToAPI(laborCharge, addLaborBtn);
}

/**
 * Save labor charge to API
 * @param {Object} laborCharge - Labor charge object
 * @param {HTMLElement} addLaborBtn - Add button element
 */
function saveLaborChargeToAPI(laborCharge, addLaborBtn) {
    const token = getAuthToken();
    const headers = {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : ''
    };

    // Format for API
    const formattedLabor = [{
        description: laborCharge.description,
        hours: laborCharge.hours,
        rate: laborCharge.rate
    }];

    fetch(`/serviceAdvisor/api/service/${window.currentRequestId}/labor-charges`, {
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

            // Reset form
            document.getElementById('laborHours').value = '1';
            document.getElementById('laborRate').value = '65';
        })
        .catch(error => {
            console.error('Labor charge save error:', error);
            showNotification('Error saving labor charge: ' + error.message, 'error');
        })
        .finally(() => {
            // Reset button
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

    if (window.laborCharges.length === 0) {
        laborChargesList.innerHTML = '<div style="padding: 10px 0; color: var(--gray);">No labor charges added yet.</div>';
        return;
    }

    const formatter = new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR',
        minimumFractionDigits: 2
    });

    window.laborCharges.forEach((charge, index) => {
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
    if (index < 0 || index >= window.laborCharges.length) {
        return;
    }

    // Show loading indicator
    const loadingIcon = document.createElement('i');
    loadingIcon.className = 'fas fa-spinner fa-spin';
    loadingIcon.style.marginLeft = '10px';

    const laborList = document.getElementById('laborChargesList');
    if (laborList) {
        laborList.appendChild(loadingIcon);
    }

    // Remove from local array
    window.laborCharges.splice(index, 1);

    // Update UI immediately
    renderLaborCharges();
    updateBillSummary();

    // If there are no more labor charges, just show completed UI state
    if (window.laborCharges.length === 0) {
        showNotification('Labor charge removed', 'info');
        if (laborList && loadingIcon.parentNode === laborList) {
            laborList.removeChild(loadingIcon);
        }

        // Update API with empty array
        updateLaborChargesAPI([], laborList, loadingIcon);
        return;
    }

    // Update API with remaining charges
    updateLaborChargesAPI(window.laborCharges, laborList, loadingIcon);
}

/**
 * Update labor charges in API
 * @param {Array} laborCharges - Labor charges
 * @param {HTMLElement} laborList - Labor list element
 * @param {HTMLElement} loadingIcon - Loading icon element
 */
function updateLaborChargesAPI(laborCharges, laborList, loadingIcon) {
    const token = getAuthToken();
    const headers = {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : ''
    };

    // Format for API
    const formattedCharges = laborCharges.map(charge => {
        return {
            description: charge.description || 'Labor Charge',
            hours: parseFloat(charge.hours) || 0,
            rate: parseFloat(charge.rate) || 0
        };
    });

    fetch(`/serviceAdvisor/api/service/${window.currentRequestId}/labor-charges`, {
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
            // Remove loading icon
            if (laborList && loadingIcon.parentNode === laborList) {
                laborList.removeChild(loadingIcon);
            }
        });
}

/**
 * Update bill summary totals
 */
function updateBillSummary() {
    // Calculate parts subtotal
    let partsSubtotal = 0;
    window.inventoryItems.forEach(item => {
        partsSubtotal += item.price * item.quantity;
    });

    // Calculate labor subtotal
    let laborSubtotal = 0;
    window.laborCharges.forEach(charge => {
        laborSubtotal += charge.hours * charge.rate;
    });


    // Format currency
    const formatter = new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR',
        minimumFractionDigits: 2
    });

    // Update UI
    document.getElementById('partsSubtotal').textContent = formatter.format(partsSubtotal);
    document.getElementById('laborSubtotal').textContent = formatter.format(laborSubtotal);
    document.getElementById('subtotalAmount').textContent = formatter.format(subtotal);
    document.getElementById('totalAmount').textContent = formatter.format(total);
}

/**
 * Update invoice preview
 */
function updateBillPreview() {
    const invoiceItemsList = document.getElementById('invoiceItemsList');
    if (!invoiceItemsList) return;

    invoiceItemsList.innerHTML = '';

    const formatter = new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR',
        minimumFractionDigits: 2
    });

    // Add inventory items
    window.inventoryItems.forEach(item => {
        const row = document.createElement('tr');
        const total = item.price * item.quantity;

        row.innerHTML = `
            <td>${item.name} (Parts)</td>
            <td>${item.quantity}</td>
            <td>${formatter.format(item.price)}</td>
            <td>${formatter.format(total)}</td>
        `;

        invoiceItemsList.appendChild(row);
    });

    // Add labor charges
    window.laborCharges.forEach(charge => {
        const row = document.createElement('tr');
        const total = charge.hours * charge.rate;

        row.innerHTML = `
            <td>Labor Charge</td>
            <td>${charge.hours} hrs</td>
            <td>${formatter.format(charge.rate)}/hr</td>
            <td>${formatter.format(total)}</td>
        `;

        invoiceItemsList.appendChild(row);
    });

    // Show empty state if no items
    if (window.inventoryItems.length === 0 && window.laborCharges.length === 0) {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td colspan="4" style="text-align: center; padding: 20px;">
                No service items added yet.
            </td>
        `;
        invoiceItemsList.appendChild(row);
    }

    let subtotal = 0;
    window.inventoryItems.forEach(item => {
        subtotal += item.price * item.quantity;
    });

    window.laborCharges.forEach(charge => {
        subtotal += charge.hours * charge.rate;
    });


    // Update totals in UI
    const invoiceSubtotal = document.getElementById('invoiceSubtotal');
    const invoiceTotal = document.getElementById('invoiceTotal');

    if (invoiceSubtotal) invoiceSubtotal.textContent = formatter.format(subtotal);
    if (invoiceTotal) invoiceTotal.textContent = formatter.format(total);

    // Update invoice info fields
    updateInvoiceInfoFields();
}

/**
 * Update invoice info fields
 */
function updateInvoiceInfoFields() {
    // Get customer info
    const customerName = document.querySelector('.vehicle-summary .vehicle-info-summary p')?.textContent?.replace('Customer: ', '') || 'Unknown Customer';
    const customerEmailElement = document.querySelector('.detail-card:nth-of-type(2) .detail-row:nth-child(2) .detail-value');
    const customerPhoneElement = document.querySelector('.detail-card:nth-of-type(2) .detail-row:nth-child(3) .detail-value');
    const customerEmail = customerEmailElement?.textContent || 'Not available';
    const customerPhone = customerPhoneElement?.textContent || 'Not available';

    // Get vehicle info
    const vehicleInfoElement = document.querySelector('.vehicle-summary .vehicle-info-summary h4');
    let vehicleModel = 'Unknown Vehicle';
    let registrationNumber = 'Unknown';

    if (vehicleInfoElement) {
        const vehicleText = vehicleInfoElement.textContent;
        const regMatch = vehicleText.match(/\(([^)]+)\)/);
        if (regMatch && regMatch[1]) {
            registrationNumber = regMatch[1];
            vehicleModel = vehicleText.replace(/\s*\([^)]+\)/, '').trim();
        } else {
            vehicleModel = vehicleText;
        }
    }

    // Try to get registration from details if not found
    if (registrationNumber === 'Unknown') {
        const regElement = document.querySelector('.detail-card:nth-of-type(1) .detail-row:nth-child(2) .detail-value');
        if (regElement) {
            registrationNumber = regElement.textContent.trim();
        }
    }

    // Generate invoice date and number
    const today = new Date();
    const formattedDate = today.toLocaleDateString('en-US', {
        month: 'short',
        day: 'numeric',
        year: 'numeric'
    });

    if (!window.currentInvoiceNumber) {
        window.currentInvoiceNumber = 'INV-' + today.getFullYear() + '-' + String(Math.floor(Math.random() * 10000)).padStart(4, '0');
    }

    // Update customer info
    updateInvoiceField('.invoice-customer .invoice-detail:nth-child(2)', `<span>Name:</span> ${customerName}`);
    updateInvoiceField('.invoice-customer .invoice-detail:nth-child(3)', `<span>Email:</span> ${customerEmail}`);
    updateInvoiceField('.invoice-customer .invoice-detail:nth-child(4)', `<span>Phone:</span> ${customerPhone}`);

    // Update vehicle info
    updateInvoiceField('.invoice-service .invoice-detail:nth-child(2)', `<span>Vehicle:</span> ${vehicleModel}`);
    updateInvoiceField('.invoice-service .invoice-detail:nth-child(3)', `<span>Registration:</span> ${registrationNumber}`);
    updateInvoiceField('.invoice-service .invoice-detail:nth-child(4)', `<span>Invoice Date:</span> ${formattedDate}`);
    updateInvoiceField('.invoice-service .invoice-detail:nth-child(5)', `<span>Invoice #:</span> ${window.currentInvoiceNumber}`);
}

/**
 * Update invoice field
 * @param {string} selector - Element selector
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
 * @param {string} status - Status value
 */
function updateStatusPreview(status) {
    const currentStatusBadge = document.getElementById('currentStatusBadge');
    if (currentStatusBadge) {
        currentStatusBadge.classList.remove('new', 'completed', 'diagnosis', 'repair');

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
    const headers = createAuthHeaders();

    // Get service advisor name
    const serviceAdvisorName = document.querySelector('.user-info h3')?.textContent || 'Service Advisor';

    const statusData = {
        status: status,
        notes: document.getElementById('serviceNotes')?.value || "",
        notifyCustomer: false,
        updatedBy: serviceAdvisorName,
        updatedAt: new Date().toISOString()
    };

    // Update local status history
    if (window.statusHistory === undefined) {
        window.statusHistory = [];
    }

    // Add the new status to the history
    window.statusHistory.push(statusData);

    // Update the status history display
    updateStatusHistory();

    return fetch(`/serviceAdvisor/api/service/${window.currentRequestId}/status`, {
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

            updateStatusInTable(window.currentRequestId, status);
            updateAllStatusBadges(status);

            return data;
        })
        .catch(error => {
            showNotification('Error updating status: ' + error.message, 'error');

            if (saveButton) saveButton.disabled = false;

            throw error;
        });
}

/**
 * Update all status badges
 * @param {string} status - Status value
 */
function updateAllStatusBadges(status) {
    let statusClass = getStatusClass(status);

    // Update main status badge
    const currentStatusBadge = document.getElementById('currentStatusBadge');
    if (currentStatusBadge) {
        updateStatusBadgeClasses(currentStatusBadge, statusClass);
        currentStatusBadge.innerHTML = `<i class="fas fa-circle"></i> ${status}`;
    }

    // Update status display badges
    const statusDisplays = document.querySelectorAll('.status-display .status-badge');
    statusDisplays.forEach(badge => {
        updateStatusBadgeClasses(badge, statusClass);
        badge.innerHTML = `<i class="fas fa-circle"></i> ${status}`;
    });

    // Update detail status badge
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

    // Clear existing history items
    statusHistoryContainer.innerHTML = '';

    // Initialize status history if needed
    if (!window.statusHistory || window.statusHistory.length === 0) {
        const initialStatus = {
            status: 'New',
            updatedBy: 'System',
            updatedAt: new Date().toISOString()
        };
        window.statusHistory = [initialStatus];
    }

    // Define the standard service flow
    const serviceFlow = [
        { status: 'New', label: 'New' },
        { status: 'Diagnosis', label: 'Diagnosis' },
        { status: 'Repair', label: 'Repair' },
        { status: 'Inspection', label: 'Inspection' },
        { status: 'Billing', label: 'Billing' },
        { status: 'Feedback', label: 'Feedback' },
        { status: 'Completed', label: 'Completed' }
    ];

    // Get current status and its index
    const currentStatus = window.statusHistory[window.statusHistory.length - 1].status;
    const currentStatusIndex = serviceFlow.findIndex(step => step.status === currentStatus);

    // Create timeline
    createStatusTimeline(statusHistoryContainer, serviceFlow, currentStatusIndex);

    // Create history list
    createStatusHistoryList(statusHistoryContainer);
}

/**
 * Create status timeline
 * @param {HTMLElement} container - Container element
 * @param {Array} serviceFlow - Service flow steps
 * @param {number} currentStatusIndex - Current status index
 */
function createStatusTimeline(container, serviceFlow, currentStatusIndex) {
    // Create timeline container
    const timelineContainer = document.createElement('div');
    timelineContainer.className = 'status-timeline-graph';
    container.appendChild(timelineContainer);

    // Add each step to timeline
    serviceFlow.forEach((step, index) => {
        // Determine step status
        let stepStatus = index < currentStatusIndex ? 'completed' :
            index === currentStatusIndex ? 'in-progress' : 'upcoming';

        // Find history entry for this step
        const historyEntry = window.statusHistory.find(entry => entry.status === step.status);

        // Create step element
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
 * Get step icon class
 * @param {string} stepStatus - Step status
 * @returns {string} - Icon class
 */
function getStepIcon(stepStatus) {
    return stepStatus === 'completed' ? 'fa-check' :
        stepStatus === 'in-progress' ? 'fa-sync' : 'fa-clock';
}

/**
 * Create status history list
 * @param {HTMLElement} container - Container element
 */
function createStatusHistoryList(container) {
    // Add title
    const historyTitle = document.createElement('h4');
    historyTitle.className = 'section-title';
    historyTitle.textContent = 'Status History';
    container.appendChild(historyTitle);

    // Create list container
    const historyList = document.createElement('div');
    historyList.className = 'status-history-list';
    container.appendChild(historyList);

    // Add history items (newest first)
    for (let i = window.statusHistory.length - 1; i >= 0; i--) {
        const statusData = window.statusHistory[i];
        const statusClass = getStatusClass(statusData.status);
        const formattedDateTime = formatDateTimeForHistory(statusData.updatedAt);

        // Create history item
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
 * Format date and time for history display
 * @param {string} dateTimeString - ISO date string
 * @returns {string} - Formatted date and time
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
 * Update status in vehicles table
 * @param {number} requestId - Request ID
 * @param {string} status - Status value
 */
function updateStatusInTable(requestId, status) {
    // Find row with matching ID
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
        // Row not found, refresh table
        console.log(`Row with requestId ${requestId} not found, refreshing table`);
        fetchAssignedVehicles();
    }
}

/**
 * Save service items (labor and inventory)
 */
function saveServiceItems() {
    const promises = [];

    // Save labor charges
    const laborPromise = saveLaborCharges().catch(error => {
        console.error("Error saving labor charges:", error);
        return null; // Continue with other operations even if labor charges fail
    });
    promises.push(laborPromise);

    // Save inventory items if there are any
    if (window.inventoryItems.length > 0) {
        const inventoryPromise = saveInventoryItems();
        promises.push(inventoryPromise);
    }

    // Wait for all promises to complete
    Promise.all(promises)
        .then(results => {
            showNotification('All service items saved successfully!', 'success');
            setTimeout(() => {
                openVehicleDetails(window.currentRequestId);
            }, 1000);
        })
        .catch(error => {
            showNotification('Error: ' + error.message, 'error');
        });
}

/**
 * Save labor charges
 * @returns {Promise} - Promise that resolves when labor charges are saved
 */
function saveLaborCharges() {
    if (window.laborCharges.length === 0) {
        showNotification('No labor charges to save', 'info');
        return Promise.resolve(); // Return a resolved promise for chaining
    }

    showNotification('Saving labor charges...', 'info');

    // Disable save button during save
    const saveButton = document.getElementById('saveInvoiceBtn');
    if (saveButton) saveButton.disabled = true;

    const token = getAuthToken();
    const headers = {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : ''
    };

    // Format labor charges properly
    const formattedCharges = window.laborCharges.map(charge => {
        return {
            description: charge.description || 'Labor Charge',
            hours: parseFloat(charge.hours) || 0,
            rate: parseFloat(charge.rate) || 0
        };
    });

    // Filter out invalid charges
    const validCharges = formattedCharges.filter(
        charge => charge.hours > 0 && charge.rate > 0
    );

    if (validCharges.length === 0) {
        showNotification('No valid labor charges to save', 'warning');
        if (saveButton) saveButton.disabled = false;
        return Promise.resolve();
    }

    // Make API call
    return fetch(`/serviceAdvisor/api/service/${window.currentRequestId}/labor-charges`, {
        method: 'POST',
        headers: headers,
        body: JSON.stringify(validCharges)
    })
        .then(response => {
            // Check response status
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
            console.error('Labor charge error:', error);
            showNotification('Error saving labor charges: ' + error.message, 'error');
            if (saveButton) saveButton.disabled = false;
            throw error;
        });
}

/**
 * Save inventory items
 * @returns {Promise} - Promise that resolves when inventory items are saved
 */
function saveInventoryItems() {
    const items = window.inventoryItems.map(item => {
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

    return fetch(`/serviceAdvisor/api/service/${window.currentRequestId}/inventory-items`, {
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
            console.log('Inventory items saved successfully:', data);
            return data;
        })
        .catch(error => {
            console.error('Inventory error:', error);
            showNotification('Error saving inventory items: ' + error.message, 'error');
            throw error;
        });
}

/**
 * Mark service as complete
 */
function markServiceComplete() {
    const requestId = window.currentRequestId;
    if (!requestId) {
        showNotification('No service selected', 'error');
        return;
    }

    showNotification('Marking service as completed...', 'info');

    const token = getAuthToken();
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
                document.getElementById('vehicleDetailsModal').classList.remove('show');
                fetchAssignedVehicles();
            }, 1500);
        })
        .catch(error => {
            showNotification('Error: ' + error.message, 'error');
        });
}

/**
 * Filter vehicles by selected filter
 * @param {string} filter - Filter value
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

    // Update filter button text
    updateFilterButtonText(filter);
}

/**
 * Update filter button text
 * @param {string} filter - Filter value
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
 * Show notification message
 * @param {string} message - Message to show
 * @param {string} type - Notification type (success, error, info, warning)
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

// Export functions for global access
window.openVehicleDetails = openVehicleDetails;
window.incrementInventoryQuantity = incrementInventoryQuantity;
window.decrementInventoryQuantity = decrementInventoryQuantity;
window.updateInventoryQuantity = updateInventoryQuantity;
window.removeInventoryItem = removeInventoryItem;
window.removeLaborCharge = removeLaborCharge;
window.handleTabClick = handleTabClick;
window.updateBillPreview = updateBillPreview;
window.updateInvoiceInfoFields = updateInvoiceInfoFields;