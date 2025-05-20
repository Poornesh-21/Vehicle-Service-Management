document.addEventListener('DOMContentLoaded', function() {
    window.inventoryPrices = {};
    window.inventoryData = {};
    window.inventoryItems = [];
    window.laborCharges = [];
    window.currentRequestId = null;
    window.currentInvoiceNumber = null;

    const urlParams = new URLSearchParams(window.location.search);
    const token = urlParams.get('token');
    if (token) {
        sessionStorage.setItem('jwt-token', token);
    }

    addRefreshButton();
    fetchAssignedVehicles();
    const refreshInterval = setInterval(fetchAssignedVehicles, 300000);
    initializeEventListeners();
    initializeStatusEvents();
    updateModalFooterButtons();
    addConnectionIndicator();
});

function addRefreshButton() {
    const headerActions = document.querySelector('.header-actions');
    if (headerActions) {
        const refreshButton = document.createElement('button');
        refreshButton.className = 'btn btn-primary';
        refreshButton.innerHTML = '<i class="fas fa-sync"></i> Refresh';
        refreshButton.style.marginLeft = '10px';
        refreshButton.addEventListener('click', function() {
            fetchAssignedVehicles();
            showNotification('Refreshing vehicle data...', 'info');
        });
        headerActions.appendChild(refreshButton);
    }
}

function addConnectionIndicator() {
    const header = document.querySelector('.main-header');
    if (header) {
        const statusIndicator = document.createElement('div');
        statusIndicator.id = 'connection-status';
        statusIndicator.style.display = 'flex';
        statusIndicator.style.alignItems = 'center';
        statusIndicator.style.marginLeft = 'auto';
        statusIndicator.style.gap = '5px';
        statusIndicator.innerHTML = `
            <span id="status-icon" class="status-indicator" style="width: 10px; height: 10px; border-radius: 50%; background-color: #ccc;"></span>
            <span id="status-text" style="font-size: 0.8rem; color: #666;">Checking connection...</span>
        `;

        const headerTitle = header.querySelector('.header-title');
        if (headerTitle) {
            header.insertBefore(statusIndicator, headerTitle.nextSibling);
        } else {
            header.appendChild(statusIndicator);
        }

        checkApiConnection();
        setInterval(checkApiConnection, 30000);
    }
}

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
                statusText.textContent = 'Connected (Error: ' + response.status + ')';
                statusText.style.color = '#ffaa00';
            }
        })
        .catch(error => {
            statusIcon.style.backgroundColor = '#d90429';
            statusText.textContent = 'Disconnected';
            statusText.style.color = '#d90429';
        });
}

function initializeEventListeners() {
    const filterButton = document.getElementById('filterButton');
    const filterMenu = document.getElementById('filterMenu');

    if (filterButton && filterMenu) {
        filterButton.addEventListener('click', function() {
            filterMenu.classList.toggle('show');
        });

        document.addEventListener('click', function(event) {
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

    const searchInput = document.getElementById('searchInput');
    if (searchInput) {
        searchInput.addEventListener('input', function() {
            const searchTerm = this.value.toLowerCase();
            filterVehiclesBySearch(searchTerm);
        });
    }

    const closeVehicleDetailsModal = document.getElementById('closeVehicleDetailsModal');
    const closeDetailsBtn = document.getElementById('closeDetailsBtn');
    const vehicleDetailsModal = document.getElementById('vehicleDetailsModal');

    if (closeVehicleDetailsModal && vehicleDetailsModal) {
        closeVehicleDetailsModal.addEventListener('click', function() {
            vehicleDetailsModal.classList.remove('show');
        });
    }

    if (closeDetailsBtn && vehicleDetailsModal) {
        closeDetailsBtn.addEventListener('click', function() {
            vehicleDetailsModal.classList.remove('show');
        });
    }

    const tabs = document.querySelectorAll('.tab');
    tabs.forEach(tab => {
        tab.addEventListener('click', function() {
            handleTabClick(this);
        });
    });

    const addItemBtn = document.getElementById('addItemBtn');
    if (addItemBtn) {
        addItemBtn.addEventListener('click', function() {
            const inventoryItemSelect = document.getElementById('inventoryItemSelect');
            const itemQuantity = document.getElementById('itemQuantity');

            if (inventoryItemSelect.value) {
                addInventoryItem(inventoryItemSelect.value, parseInt(itemQuantity.value) || 1);
            } else {
                showNotification('Please select an inventory item', 'error');
            }
        });
    }

    const addLaborBtn = document.getElementById('addLaborBtn');
    if (addLaborBtn) {
        addLaborBtn.addEventListener('click', function() {
            const hours = parseFloat(document.getElementById('laborHours').value);
            const rate = parseFloat(document.getElementById('laborRate').value);

            if (!isNaN(hours) && !isNaN(rate) && hours > 0 && rate > 0) {
                addLaborCharge("Labor Charge", hours, rate);
                document.getElementById('laborHours').value = '1';
                document.getElementById('laborRate').value = '65';
            } else {
                showNotification('Please enter valid hours and rate', 'error');
            }
        });
    }

    const saveInvoiceBtn = document.getElementById('saveInvoiceBtn');
    if (saveInvoiceBtn) {
        saveInvoiceBtn.addEventListener('click', function() {
            saveServiceItems();
        });
    }

    const previewInvoiceBtn = document.getElementById('previewInvoiceBtn');
    if (previewInvoiceBtn) {
        previewInvoiceBtn.addEventListener('click', function() {
            const generateInvoiceTab = document.querySelector('.tab[data-tab="generate-invoice"]');
            if (generateInvoiceTab) {
                handleTabClick(generateInvoiceTab);
            }
        });
    }

    const markCompleteBtn = document.getElementById('markCompleteBtn');
    if (markCompleteBtn) {
        markCompleteBtn.addEventListener('click', function() {
            markServiceComplete();
        });
    }

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

function getAuthToken() {
    const urlParams = new URLSearchParams(window.location.search);
    const token = urlParams.get('token') || sessionStorage.getItem('jwt-token');
    return token;
}

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

let fetchRetries = 0;
const MAX_RETRIES = 3;

function fetchAssignedVehicles() {
    const token = getAuthToken();
    const headers = {};

    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
        sessionStorage.setItem('jwt-token', token);
    }

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
                fetchRetries = 0;
                updateVehiclesTable(data);
            } else {
                throw new Error('Invalid data format: expected an array');
            }
        })
        .catch(error => {
            if (fetchRetries < MAX_RETRIES) {
                fetchRetries++;
                setTimeout(fetchAssignedVehicles, 1000);
                showNotification(`Retrying to load data (${fetchRetries}/${MAX_RETRIES})...`, 'info');
            } else {
                showNotification('Error loading assigned vehicles: ' + error.message, 'error');
                loadDummyData();
            }
        });
}

function loadDummyData() {
    showNotification("Unable to connect to server, showing placeholder data", "warning");

    const dummyVehicles = [
        {
            requestId: 1,
            vehicleName: "Honda Civic (2020)",
            registrationNumber: "ABC-1234",
            customerName: "John Smith",
            customerEmail: "john.smith@example.com",
            serviceType: "General Service",
            startDate: "2025-05-05",
            status: "Diagnosis"
        },
        {
            requestId: 2,
            vehicleName: "Toyota Camry (2019)",
            registrationNumber: "XYZ-5678",
            customerName: "Sarah Johnson",
            customerEmail: "sarah.j@example.com",
            serviceType: "Oil Change, Wheel Alignment",
            startDate: "2025-05-03",
            status: "Repair"
        },
        {
            requestId: 3,
            vehicleName: "Ford Mustang (2018)",
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

    vehicles.forEach((vehicle, index) => {
        const row = document.createElement('tr');
        row.setAttribute('data-id', vehicle.requestId);
        row.onclick = function() {
            openVehicleDetails(vehicle.requestId);
        };

        let formattedDate = 'N/A';
        if (vehicle.startDate) {
            try {
                const requestDate = new Date(vehicle.startDate);
                formattedDate = requestDate.toLocaleDateString('en-US', {
                    month: 'short',
                    day: 'numeric',
                    year: 'numeric'
                });
            } catch (e) {
                formattedDate = vehicle.startDate;
            }
        }

        let statusClass = 'new';
        let statusText = vehicle.status || 'New';

        if (statusText.toLowerCase() === 'diagnosis' ||
            statusText.toLowerCase() === 'repair' ||
            statusText.toLowerCase() === 'in progress') {
            statusClass = 'in-progress';
            if (statusText.toLowerCase() === 'diagnosis') {
                statusText = 'Diagnosis';
            } else if (statusText.toLowerCase() === 'repair') {
                statusText = 'Repair';
            } else {
                statusText = 'In Progress';
            }
        } else if (statusText.toLowerCase() === 'received' ||
            statusText.toLowerCase() === 'new') {
            statusClass = 'new';
            statusText = 'New';
        } else if (statusText.toLowerCase() === 'completed') {
            statusClass = 'completed';
            statusText = 'Completed';
        }

        const vehicleName = vehicle.vehicleName ||
            `${vehicle.vehicleBrand || ''} ${vehicle.vehicleModel || ''}`.trim() ||
            'Unknown Vehicle';
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

function openVehicleDetails(requestId) {
    window.currentRequestId = requestId;

    const vehicleDetailsModal = document.getElementById('vehicleDetailsModal');
    vehicleDetailsModal.classList.add('show');

    const detailsTab = document.getElementById('details-tab');
    if (detailsTab) {
        detailsTab.innerHTML = `
            <div style="text-align: center; padding: 50px;">
                <i class="fas fa-spinner fa-spin" style="font-size: 32px; margin-bottom: 20px;"></i>
                <p>Loading service details...</p>
            </div>
        `;
    }

    const tabs = document.querySelectorAll('.tab');
    tabs.forEach(tab => tab.classList.remove('active'));
    document.querySelector('.tab[data-tab="details"]').classList.add('active');

    const tabContents = document.querySelectorAll('.tab-content');
    tabContents.forEach(content => content.classList.remove('active'));
    document.getElementById('details-tab').classList.add('active');

    updateModalFooterButtons();

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
            window.currentInvoiceNumber = 'INV-' + new Date().getFullYear() + '-' +
                String(Math.floor(Math.random() * 10000)).padStart(4, '0');

            loadVehicleDetails(data);

            if (data.currentBill) {
                loadCurrentBill(data.currentBill);
            }

            fetchInventoryItems();
        })
        .catch(error => {
            showNotification('Error loading service details: ' + error.message, 'error');

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
        });
}

function loadVehicleDetails(data) {
    if (!data) {
        showNotification('Error: No data received from server', 'error');
        return;
    }

    try {
        createDetailCardsIfNeeded();

        const vehicleCard = document.querySelector('.detail-card:nth-of-type(1)');
        if (vehicleCard) {
            const makeModel = `${data.vehicleBrand || ''} ${data.vehicleModel || ''}`.trim();
            setDetailValueFixed(vehicleCard, 1, makeModel || 'Not specified');
            setDetailValueFixed(vehicleCard, 2, data.registrationNumber || 'Not specified');
            setDetailValueFixed(vehicleCard, 3, data.vehicleYear || 'Not specified');
            setDetailValueFixed(vehicleCard, 4, data.vehicleType || 'Not specified');
        }

        const customerCard = document.querySelector('.detail-card:nth-of-type(2)');
        if (customerCard) {
            setDetailValueFixed(customerCard, 1, data.customerName || 'Not specified');
            setDetailValueFixed(customerCard, 2, data.customerEmail || 'Not specified');
            setDetailValueFixed(customerCard, 3, data.customerPhone || 'Not specified');
        }

        const serviceCard = document.querySelector('.detail-card:nth-of-type(3)');
        if (serviceCard) {
            setDetailValueFixed(serviceCard, 1, data.serviceType || 'General Service');

            let formattedDate = 'Not specified';
            if (data.requestDate) {
                try {
                    const requestDate = new Date(data.requestDate);
                    formattedDate = requestDate.toLocaleDateString('en-US', {
                        month: 'short',
                        day: 'numeric',
                        year: 'numeric'
                    });
                } catch (e) {
                    formattedDate = data.requestDate;
                }
            }
            setDetailValueFixed(serviceCard, 2, formattedDate);

            const statusCell = serviceCard.querySelector('.detail-card-body .detail-row:nth-child(3) .detail-value');
            if (statusCell) {
                const status = data.status || 'New';
                let statusClass = getStatusClass(status);

                statusCell.innerHTML = `
                    <span class="status-badge ${statusClass}">
                        <i class="fas fa-circle"></i> ${status}
                    </span>
                `;
            }

            setDetailValueFixed(serviceCard, 4, data.additionalDescription || 'No additional description provided.');
        }

        const vehicleSummaryElements = document.querySelectorAll('.vehicle-summary .vehicle-info-summary h4');
        const vehicleInfo = `${data.vehicleBrand || ''} ${data.vehicleModel || ''} (${data.registrationNumber || 'Unknown'})`.trim();
        vehicleSummaryElements.forEach(element => {
            element.textContent = vehicleInfo;
        });

        const customerElements = document.querySelectorAll('.vehicle-summary .vehicle-info-summary p');
        customerElements.forEach(element => {
            element.textContent = `Customer: ${data.customerName || 'Unknown'}`;
        });

        const statusDisplayElements = document.querySelectorAll('.vehicle-summary .status-display');
        const status = data.status || 'New';
        let statusClass = getStatusClass(status);

        statusDisplayElements.forEach(element => {
            element.innerHTML = `
                <span class="status-badge ${statusClass}" id="currentStatusBadge">
                    <i class="fas fa-circle"></i> ${status}
                </span>
            `;
        });

        const statusSelect = document.getElementById('statusSelect');
        if (statusSelect) {
            for (let i = 0; i < statusSelect.options.length; i++) {
                if (statusSelect.options[i].value.toLowerCase() === status.toLowerCase()) {
                    statusSelect.selectedIndex = i;
                    break;
                }
            }
        }
    } catch (error) {
        showNotification('Error displaying vehicle details: ' + error.message, 'error');
    }
}

function setDetailValueFixed(cardElement, index, value) {
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
    }
}

function createDetailCardsIfNeeded() {
    const detailsTab = document.getElementById('details-tab');
    if (!detailsTab) return;

    if (detailsTab.querySelector('.detail-card')) return;

    const row = document.createElement('div');
    row.className = 'row';

    const vehicleCard = document.createElement('div');
    vehicleCard.className = 'detail-card';
    vehicleCard.innerHTML = `
        <div class="detail-card-header">
            Vehicle Information
        </div>
        <div class="detail-card-body">
            <div class="detail-row">
                <div class="detail-label">Make/Model:</div>
                <div class="detail-value">Loading...</div>
            </div>
            <div class="detail-row">
                <div class="detail-label">Registration:</div>
                <div class="detail-value">Loading...</div>
            </div>
            <div class="detail-row">
                <div class="detail-label">Year:</div>
                <div class="detail-value">Loading...</div>
            </div>
            <div class="detail-row">
                <div class="detail-label">Type:</div>
                <div class="detail-value">Loading...</div>
            </div>
        </div>
    `;

    const customerCard = document.createElement('div');
    customerCard.className = 'detail-card';
    customerCard.innerHTML = `
        <div class="detail-card-header">
            Customer Information
        </div>
        <div class="detail-card-body">
            <div class="detail-row">
                <div class="detail-label">Name:</div>
                <div class="detail-value">Loading...</div>
            </div>
            <div class="detail-row">
                <div class="detail-label">Email:</div>
                <div class="detail-value">Loading...</div>
            </div>
            <div class="detail-row">
                <div class="detail-label">Phone:</div>
                <div class="detail-value">Loading...</div>
            </div>
        </div>
    `;

    const serviceCard = document.createElement('div');
    serviceCard.className = 'detail-card';
    serviceCard.innerHTML = `
        <div class="detail-card-header">
            Service Request Details
        </div>
        <div class="detail-card-body">
            <div class="detail-row">
                <div class="detail-label">Service Type:</div>
                <div class="detail-value">Loading...</div>
            </div>
            <div class="detail-row">
                <div class="detail-label">Request Date:</div>
                <div class="detail-value">Loading...</div>
            </div>
            <div class="detail-row">
                <div class="detail-label">Status:</div>
                <div class="detail-value">
                    <span class="status-badge new">
                        <i class="fas fa-circle"></i> Loading...
                    </span>
                </div>
            </div>
            <div class="detail-row">
                <div class="detail-label">Description:</div>
                <div class="detail-value">Loading...</div>
            </div>
        </div>
    `;

    row.appendChild(vehicleCard);
    row.appendChild(customerCard);
    row.appendChild(serviceCard);

    detailsTab.innerHTML = '';
    detailsTab.appendChild(row);
}

function getStatusClass(status) {
    const statusLower = status.toLowerCase();
    if (statusLower === 'completed') {
        return 'completed';
    } else if (statusLower === 'diagnosis' || statusLower === 'repair') {
        return 'in-progress';
    } else {
        return 'new';
    }
}

function loadCurrentBill(billData) {
    if (!billData) {
        return;
    }

    try {
        window.inventoryItems = [];
        window.laborCharges = [];

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

        renderInventoryItems();
        renderLaborCharges();

        const formatCurrency = (value) => {
            const num = parseFloat(value) || 0;
            return `₹${num.toFixed(2)}`;
        };

        document.getElementById('partsSubtotal').textContent = formatCurrency(billData.partsSubtotal);
        document.getElementById('laborSubtotal').textContent = formatCurrency(billData.laborSubtotal);
        document.getElementById('subtotalAmount').textContent = formatCurrency(billData.subtotal);
        document.getElementById('taxAmount').textContent = formatCurrency(billData.tax);
        document.getElementById('totalAmount').textContent = formatCurrency(billData.total);

        const serviceNotesTextarea = document.getElementById('serviceNotes');
        if (serviceNotesTextarea && billData.notes) {
            serviceNotesTextarea.value = billData.notes;
        }
    } catch (error) {
        showNotification('Error loading bill information', 'error');
    }
}

function fetchInventoryItems() {
    const token = getAuthToken();
    const headers = createAuthHeaders();

    const inventorySelect = document.getElementById('inventoryItemSelect');
    if (inventorySelect) {
        while (inventorySelect.options.length > 1) {
            inventorySelect.remove(1);
        }

        const loadingOption = document.createElement('option');
        loadingOption.disabled = true;
        loadingOption.textContent = 'Loading inventory items...';
        inventorySelect.appendChild(loadingOption);
        inventorySelect.selectedIndex = 1;
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
            showNotification('Error loading inventory items. Please try again.', 'error');

            if (inventorySelect) {
                while (inventorySelect.options.length > 1) {
                    inventorySelect.remove(1);
                }

                const errorOption = document.createElement('option');
                errorOption.disabled = true;
                errorOption.textContent = 'Error loading items. Please try again.';
                inventorySelect.appendChild(errorOption);
            }

            setTimeout(() => {
                retryFetchInventoryItems();
            }, 3000);

            throw error;
        });
}

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
        });
}

function populateInventoryDropdown(items) {
    const inventorySelect = document.getElementById('inventoryItemSelect');
    if (!inventorySelect) return;

    while (inventorySelect.options.length > 1) {
        inventorySelect.remove(1);
    }

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
    const itemName = itemData.name;
    const itemPrice = itemData.price;
    const itemStock = itemData.stock;

    const existingItemIndex = window.inventoryItems.findIndex(item => Number(item.key) === parsedItemId);

    let newTotalQuantity = quantity;
    if (existingItemIndex >= 0) {
        newTotalQuantity += window.inventoryItems[existingItemIndex].quantity;
    }

    if (newTotalQuantity > itemStock) {
        showNotification(`Not enough stock. Only ${itemStock} available for ${itemName}`, 'error');
        return;
    }

    if (existingItemIndex >= 0) {
        window.inventoryItems[existingItemIndex].quantity += quantity;
        showNotification(`Updated quantity for ${itemName}`, 'info');
    } else {
        window.inventoryItems.push({
            key: parsedItemId,
            name: itemName,
            price: itemPrice,
            quantity: quantity
        });
        showNotification(`Added ${itemName} to service items`, 'info');
    }

    const inventorySelect = document.getElementById('inventoryItemSelect');
    const quantityInput = document.getElementById('itemQuantity');
    if (inventorySelect) inventorySelect.selectedIndex = 0;
    if (quantityInput) quantityInput.value = 1;

    renderInventoryItems();
    updateBillSummary();
}

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

function validateInventoryQuantities() {
    let isValid = true;
    const errorMessages = [];

    if (!window.inventoryData) {
        return { isValid: false, message: 'Inventory data not available. Please refresh the page.' };
    }

    window.inventoryItems.forEach(item => {
        const itemId = Number(item.key);
        const quantity = Number(item.quantity);

        if (!window.inventoryData[itemId]) {
            return;
        }

        const availableStock = window.inventoryData[itemId].stock;

        if (quantity > availableStock) {
            isValid = false;
            errorMessages.push(`Not enough stock for ${item.name}. Available: ${availableStock}, Requested: ${quantity}`);
        }
    });

    return {
        isValid,
        message: errorMessages.length > 0 ? errorMessages.join('\n') : ''
    };
}

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

function decrementInventoryQuantity(index) {
    if (window.inventoryItems[index].quantity > 1) {
        window.inventoryItems[index].quantity--;
        renderInventoryItems();
        updateBillSummary();
    }
}

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

function removeInventoryItem(index) {
    window.inventoryItems.splice(index, 1);
    renderInventoryItems();
    updateBillSummary();
}

function addLaborCharge(description, hours, rate) {
    hours = parseFloat(hours) || 0;
    rate = parseFloat(rate) || 0;

    if (hours <= 0 || rate <= 0) {
        showNotification('Hours and rate must be greater than zero', 'error');
        return;
    }

    window.laborCharges.push({
        description: description || "Labor Charge",
        hours: hours,
        rate: rate
    });

    renderLaborCharges();
    updateBillSummary();
}

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

function removeLaborCharge(index) {
    window.laborCharges.splice(index, 1);
    renderLaborCharges();
    updateBillSummary();
}

function updateBillSummary() {
    let partsSubtotal = 0;
    window.inventoryItems.forEach(item => {
        partsSubtotal += item.price * item.quantity;
    });

    let laborSubtotal = 0;
    window.laborCharges.forEach(charge => {
        laborSubtotal += charge.hours * charge.rate;
    });

    const subtotal = partsSubtotal + laborSubtotal;
    const tax = subtotal * 0.07;
    const total = subtotal + tax;

    const formatter = new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR',
        minimumFractionDigits: 2
    });

    document.getElementById('partsSubtotal').textContent = formatter.format(partsSubtotal);
    document.getElementById('laborSubtotal').textContent = formatter.format(laborSubtotal);
    document.getElementById('subtotalAmount').textContent = formatter.format(subtotal);
    document.getElementById('taxAmount').textContent = formatter.format(tax);
    document.getElementById('totalAmount').textContent = formatter.format(total);
}

function updateBillPreview() {
    const invoiceItemsList = document.getElementById('invoiceItemsList');
    if (!invoiceItemsList) {
        return;
    }

    invoiceItemsList.innerHTML = '';

    const formatter = new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR',
        minimumFractionDigits: 2
    });

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

    const tax = subtotal * 0.07;
    const total = subtotal + tax;

    const invoiceSubtotal = document.getElementById('invoiceSubtotal');
    const invoiceTax = document.getElementById('invoiceTax');
    const invoiceTotal = document.getElementById('invoiceTotal');

    if (invoiceSubtotal) invoiceSubtotal.textContent = formatter.format(subtotal);
    if (invoiceTax) invoiceTax.textContent = formatter.format(tax);
    if (invoiceTotal) invoiceTotal.textContent = formatter.format(total);

    updateInvoiceInfoFields();
}

function updateInvoiceInfoFields() {
    const customerName = document.querySelector('.vehicle-summary .vehicle-info-summary p')?.textContent?.replace('Customer: ', '') || 'Unknown Customer';

    const customerEmailElement = document.querySelector('.detail-card:nth-of-type(2) .detail-row:nth-child(2) .detail-value');
    const customerPhoneElement = document.querySelector('.detail-card:nth-of-type(2) .detail-row:nth-child(3) .detail-value');

    const customerEmail = customerEmailElement?.textContent || 'Not available';
    const customerPhone = customerPhoneElement?.textContent || 'Not available';

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

    if (registrationNumber === 'Unknown') {
        const regElement = document.querySelector('.detail-card:nth-of-type(1) .detail-row:nth-child(2) .detail-value');
        if (regElement) {
            registrationNumber = regElement.textContent.trim();
        }
    }

    const today = new Date();
    const formattedDate = today.toLocaleDateString('en-US', {
        month: 'short',
        day: 'numeric',
        year: 'numeric'
    });

    if (!window.currentInvoiceNumber) {
        window.currentInvoiceNumber = 'INV-' + today.getFullYear() + '-' + String(Math.floor(Math.random() * 10000)).padStart(4, '0');
    }

    const customerNameElement = document.querySelector('.invoice-customer .invoice-detail:nth-child(2)');
    if (customerNameElement) {
        customerNameElement.innerHTML = `<span>Name:</span> ${customerName}`;
    }

    const customerEmailInvoiceElement = document.querySelector('.invoice-customer .invoice-detail:nth-child(3)');
    if (customerEmailInvoiceElement) {
        customerEmailInvoiceElement.innerHTML = `<span>Email:</span> ${customerEmail}`;
    }

    const customerPhoneInvoiceElement = document.querySelector('.invoice-customer .invoice-detail:nth-child(4)');
    if (customerPhoneInvoiceElement) {
        customerPhoneInvoiceElement.innerHTML = `<span>Phone:</span> ${customerPhone}`;
    }

    const vehicleInfoInvoiceElement = document.querySelector('.invoice-service .invoice-detail:nth-child(2)');
    if (vehicleInfoInvoiceElement) {
        vehicleInfoInvoiceElement.innerHTML = `<span>Vehicle:</span> ${vehicleModel}`;
    }

    const regInvoiceElement = document.querySelector('.invoice-service .invoice-detail:nth-child(3)');
    if (regInvoiceElement) {
        regInvoiceElement.innerHTML = `<span>Registration:</span> ${registrationNumber}`;
    }

    const invoiceDateElement = document.querySelector('.invoice-service .invoice-detail:nth-child(4)');
    if (invoiceDateElement) {
        invoiceDateElement.innerHTML = `<span>Invoice Date:</span> ${formattedDate}`;
    }

    const invoiceNumberElement = document.querySelector('.invoice-service .invoice-detail:nth-child(5)');
    if (invoiceNumberElement) {
        invoiceNumberElement.innerHTML = `<span>Invoice #:</span> ${window.currentInvoiceNumber}`;
    }
}

function initializeStatusEvents() {
    const statusSelect = document.getElementById('statusSelect');
    if (statusSelect) {
        statusSelect.addEventListener('change', function() {
            updateStatusPreview(this.value);
        });
    }
}

function updateStatusPreview(status) {
    const currentStatusBadge = document.getElementById('currentStatusBadge');
    if (currentStatusBadge) {
        currentStatusBadge.classList.remove('new', 'in-progress', 'completed', 'diagnosis', 'pending-parts', 'repair', 'quality-check');

        let statusClass = 'new';
        if (status === 'Diagnosis' || status === 'Repair') {
            statusClass = 'in-progress';
        } else if (status === 'Completed') {
            statusClass = 'completed';
        }

        currentStatusBadge.classList.add(statusClass);

        let statusText = status.replace(/-/g, ' ');
        statusText = statusText.charAt(0).toUpperCase() + statusText.slice(1);
        currentStatusBadge.innerHTML = `<i class="fas fa-circle"></i> ${statusText}`;
    }
}

function updateServiceStatus() {
    const statusSelect = document.getElementById('statusSelect');

    if (!statusSelect) {
        return;
    }

    const status = statusSelect.value;
    showNotification('Updating status...', 'info');

    const saveButton = document.getElementById('saveServiceItemsBtn');
    if (saveButton) saveButton.disabled = true;

    const token = getAuthToken();
    const headers = createAuthHeaders();

    const statusData = {
        status: status,
        notes: document.getElementById('serviceNotes')?.value || "",
        notifyCustomer: false
    };

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

function updateAllStatusBadges(status) {
    let statusClass = getStatusClass(status);

    const currentStatusBadge = document.getElementById('currentStatusBadge');
    if (currentStatusBadge) {
        currentStatusBadge.classList.remove('new', 'in-progress', 'completed');
        currentStatusBadge.classList.add(statusClass);
        currentStatusBadge.innerHTML = `<i class="fas fa-circle"></i> ${status}`;
    }

    const statusDisplays = document.querySelectorAll('.status-display .status-badge');
    statusDisplays.forEach(badge => {
        badge.classList.remove('new', 'in-progress', 'completed');
        badge.classList.add(statusClass);
        badge.innerHTML = `<i class="fas fa-circle"></i> ${status}`;
    });

    const detailStatus = document.querySelector('.detail-card:nth-of-type(3) .detail-value:nth-of-type(3) .status-badge');
    if (detailStatus) {
        detailStatus.classList.remove('new', 'in-progress', 'completed');
        detailStatus.classList.add(statusClass);
        detailStatus.innerHTML = `<i class="fas fa-circle"></i> ${status}`;
    }
}

function updateStatusInTable(requestId, status) {
    const row = document.querySelector(`#vehiclesTableBody tr[data-id="${requestId}"]`);
    if (row) {
        const statusCell = row.querySelector('td:nth-child(5)');
        if (statusCell) {
            let statusClass = 'new';
            if (status === 'Diagnosis' || status === 'Repair') {
                statusClass = 'in-progress';
            } else if (status === 'Completed') {
                statusClass = 'completed';
            }

            statusCell.innerHTML = `
                <span class="status-badge ${statusClass}">
                    <i class="fas fa-circle"></i> ${status}
                </span>
            `;
        }
    }
}

function saveServiceItems() {
    if (window.inventoryItems.length === 0 && window.laborCharges.length === 0) {
        showNotification('No service items to save', 'info');
        return;
    }

    showNotification('Saving service items...', 'info');

    const saveButton = document.getElementById('saveInvoiceBtn');
    if (saveButton) saveButton.disabled = true;

    const token = getAuthToken();
    const headers = createAuthHeaders();

    const promises = [];

    if (window.laborCharges.length > 0) {
        const charges = window.laborCharges.map(charge => {
            return {
                description: charge.description || 'Labor Charge',
                hours: parseFloat(charge.hours) || 0,
                ratePerHour: parseFloat(charge.rate) || 0,
                total: parseFloat(charge.hours * charge.rate) || 0
            };
        });

        const laborPromise = fetch(`/serviceAdvisor/api/service/${window.currentRequestId}/labor-charges`, {
            method: 'POST',
            headers: headers,
            body: JSON.stringify(charges)
        })
            .then(response => {
                if (!response.ok) {
                    return response.text().then(text => {
                        throw new Error(`Failed to save labor charges: ${response.status} - ${text}`);
                    });
                }
                return response.json();
            });

        promises.push(laborPromise);
    }

    if (window.inventoryItems.length > 0) {
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

        const inventoryPromise = fetch(`/serviceAdvisor/api/service/${window.currentRequestId}/inventory-items`, {
            method: 'POST',
            headers: headers,
            body: JSON.stringify(materialsRequest)
        })
            .then(response => {
                if (!response.ok) {
                    return response.text().then(text => {
                        throw new Error(`Failed to save inventory items: ${response.status} - ${text}`);
                    });
                }
                return response.json();
            });

        promises.push(inventoryPromise);
    }

    Promise.all(promises)
        .then(results => {
            showNotification('Service items saved successfully!', 'success');

            if (saveButton) saveButton.disabled = false;

            setTimeout(() => {
                openVehicleDetails(window.currentRequestId);
            }, 1000);
        })
        .catch(error => {
            showNotification('Error: ' + error.message, 'error');

            if (saveButton) saveButton.disabled = false;
        });
}

function generateBill() {
    const serviceNotes = document.getElementById('serviceNotes').value;

    const validation = validateInventoryQuantities();
    if (!validation.isValid) {
        showNotification(validation.message, 'error');
        return;
    }

    showNotification('Generating bill...', 'info');

    const saveButton = document.getElementById('saveServiceItemsBtn');
    if (saveButton) saveButton.disabled = true;

    const formatter = new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR',
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    });

    const billRequest = {
        materials: window.inventoryItems.map(item => {
            return {
                itemId: Number(item.key),
                name: item.name,
                quantity: Number(item.quantity),
                unitPrice: Number(item.price),
                total: Number(item.price * item.quantity)
            };
        }),
        laborCharges: window.laborCharges.map(charge => {
            return {
                description: charge.description || 'Labor Charge',
                hours: Number(charge.hours),
                ratePerHour: Number(charge.rate),
                total: Number(charge.hours * charge.rate)
            };
        }),
        materialsTotal: Number(window.inventoryItems.reduce((sum, item) => sum + (item.price * item.quantity), 0)),
        laborTotal: Number(window.laborCharges.reduce((sum, charge) => sum + (charge.hours * charge.rate), 0)),
        subtotal: Number(
            window.inventoryItems.reduce((sum, item) => sum + (item.price * item.quantity), 0) +
            window.laborCharges.reduce((sum, charge) => sum + (charge.hours * charge.rate), 0)
        ),
        notes: serviceNotes,
        sendEmail: true
    };

    billRequest.gst = Number(billRequest.subtotal * 0.07);
    billRequest.grandTotal = Number(billRequest.subtotal + billRequest.gst);

    const token = getAuthToken();
    const headers = createAuthHeaders();

    fetch(`/serviceAdvisor/api/service/${window.currentRequestId}/generate-bill`, {
        method: 'POST',
        headers: headers,
        body: JSON.stringify(billRequest)
    })
        .then(response => {
            if (!response.ok) {
                return response.text().then(text => {
                    throw new Error(`Failed to generate bill: ${response.status} - ${text}`);
                });
            }
            return response.json();
        })
        .then(data => {
            showNotification('Bill generated successfully!');

            if (data.emailSent) {
                setTimeout(() => {
                    showNotification('Bill email sent to customer');
                }, 3000);
            }

            if (saveButton) saveButton.disabled = false;

            updateBillPreview();
            fetchInventoryItems();
        })
        .catch(error => {
            showNotification('Error generating bill: ' + error.message, 'error');

            if (saveButton) saveButton.disabled = false;
        });
}

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

    const filterButton = document.getElementById('filterButton');
    if (filterButton) {
        if (filter === 'all') {
            filterButton.innerHTML = '<i class="fas fa-filter"></i> All Vehicles <i class="fas fa-chevron-down" style="font-size: 0.8rem;"></i>';
        } else if (filter === 'new') {
            filterButton.innerHTML = '<i class="fas fa-filter"></i> New Assignments <i class="fas fa-chevron-down" style="font-size: 0.8rem;"></i>';
        } else if (filter === 'in-progress') {
            filterButton.innerHTML = '<i class="fas fa-filter"></i> In Progress <i class="fas fa-chevron-down" style="font-size: 0.8rem;"></i>';
        }
    }
}

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

function showNotification(message, type = 'success') {
    const notification = document.getElementById('successNotification');

    notification.className = 'notification';
    notification.classList.add(type);

    document.getElementById('notificationMessage').textContent = message;
    notification.classList.add('show');

    setTimeout(() => {
        notification.classList.remove('show');
    }, 3000);
}

window.openVehicleDetails = openVehicleDetails;
window.incrementInventoryQuantity = incrementInventoryQuantity;
window.decrementInventoryQuantity = decrementInventoryQuantity;
window.updateInventoryQuantity = updateInventoryQuantity;
window.removeInventoryItem = removeInventoryItem;
window.removeLaborCharge = removeLaborCharge;
window.handleTabClick = handleTabClick;
window.sendBillToAdmin = sendBillToAdmin;
window.updateBillPreview = updateBillPreview;
window.updateInvoiceInfoFields = updateInvoiceInfoFields;