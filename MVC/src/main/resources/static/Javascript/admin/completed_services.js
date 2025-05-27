let completedServices = [];
let currentServiceId = null;
let currentService = null;

document.addEventListener('DOMContentLoaded', function() {
    initializeEventListeners();
    loadCompletedServices();

    // Initialize search functionality
    initializeSearch();

    // Listen for all modal hidden events to clean up backdrops
    document.addEventListener('hidden.bs.modal', function(event) {
        cleanupModalBackdrops();
    });
});

// Function to properly clean up modal backdrops
function cleanupModalBackdrops() {
    // Remove all modal backdrops
    const backdrops = document.querySelectorAll('.modal-backdrop');
    backdrops.forEach(backdrop => backdrop.remove());

    // Reset body styles
    document.body.classList.remove('modal-open');
    document.body.style.removeProperty('padding-right');
}

// Function to close all modals properly before opening a new one
function closeAllModals() {
    const modals = document.querySelectorAll('.modal.show');
    modals.forEach(modal => {
        const modalInstance = bootstrap.Modal.getInstance(modal);
        if (modalInstance) {
            modalInstance.hide();
        }
    });

    // Ensure backdrops are cleaned up
    cleanupModalBackdrops();
}

// Function to safely open a modal
function safelyOpenModal(modalId) {
    // First close any open modals
    closeAllModals();

    // Then open the new modal
    const modalElement = document.getElementById(modalId);
    if (modalElement) {
        const modal = new bootstrap.Modal(modalElement);
        modal.show();
    }
}
function extractLaborData(data) {
    if (!data) return null;

    // Create an object to store the extracted labor data
    let laborData = {
        minutes: 0,
        cost: 0,
        description: '',
        found: false
    };

    // Case 1: Direct properties (most common case)
    if (data.labor_minutes !== undefined && data.labor_cost !== undefined) {
        laborData.minutes = data.labor_minutes;
        laborData.cost = data.labor_cost;
        laborData.description = data.work_description || data.serviceType || 'Service Labor';
        laborData.found = true;
        console.log("Found labor data in root object:", laborData);
        return laborData;
    }

    // Case 2: Check for data in the serviceTracking object
    if (data.serviceTracking) {
        const tracking = data.serviceTracking;
        if (tracking.laborMinutes !== undefined && tracking.laborCost !== undefined) {
            laborData.minutes = tracking.laborMinutes;
            laborData.cost = tracking.laborCost;
            laborData.description = tracking.workDescription || data.serviceType || 'Service Labor';
            laborData.found = true;
            console.log("Found labor data in serviceTracking:", laborData);
            return laborData;
        }

        // Check alternate field names
        if (tracking.labor_minutes !== undefined && tracking.labor_cost !== undefined) {
            laborData.minutes = tracking.labor_minutes;
            laborData.cost = tracking.labor_cost;
            laborData.description = tracking.workDescription || data.serviceType || 'Service Labor';
            laborData.found = true;
            console.log("Found labor data in serviceTracking with alternate field names:", laborData);
            return laborData;
        }
    }

    // Case 3: Check for data in serviceTrackings array (most recent entry)
    if (data.serviceTrackings && Array.isArray(data.serviceTrackings) && data.serviceTrackings.length > 0) {
        const tracking = data.serviceTrackings[0]; // Most recent tracking

        // Check standard field names
        if (tracking.laborMinutes !== undefined && tracking.laborCost !== undefined) {
            laborData.minutes = tracking.laborMinutes;
            laborData.cost = tracking.laborCost;
            laborData.description = tracking.workDescription || data.serviceType || 'Service Labor';
            laborData.found = true;
            console.log("Found labor data in serviceTrackings array:", laborData);
            return laborData;
        }

        // Check alternate field names
        if (tracking.labor_minutes !== undefined && tracking.labor_cost !== undefined) {
            laborData.minutes = tracking.labor_minutes;
            laborData.cost = tracking.labor_cost;
            laborData.description = tracking.workDescription || data.serviceType || 'Service Labor';
            laborData.found = true;
            console.log("Found labor data in serviceTrackings array with alternate field names:", laborData);
            return laborData;
        }
    }

    // Case 4: Check for laborCost and laborMinutes fields directly
    if (data.laborMinutes !== undefined && data.laborCost !== undefined) {
        laborData.minutes = data.laborMinutes;
        laborData.cost = data.laborCost;
        laborData.description = data.workDescription || data.serviceType || 'Service Labor';
        laborData.found = true;
        console.log("Found labor data with camelCase field names:", laborData);
        return laborData;
    }

    // Case 5: Deep scan for any object containing labor information
    for (const key in data) {
        if (typeof data[key] === 'object' && data[key] !== null) {
            const obj = data[key];

            // Check for labor_minutes and labor_cost
            if (obj.labor_minutes !== undefined && obj.labor_cost !== undefined) {
                laborData.minutes = obj.labor_minutes;
                laborData.cost = obj.labor_cost;
                laborData.description = obj.work_description || data.serviceType || 'Service Labor';
                laborData.found = true;
                console.log(`Found labor data in nested object ${key}:`, laborData);
                return laborData;
            }

            // Check for laborMinutes and laborCost
            if (obj.laborMinutes !== undefined && obj.laborCost !== undefined) {
                laborData.minutes = obj.laborMinutes;
                laborData.cost = obj.laborCost;
                laborData.description = obj.workDescription || data.serviceType || 'Service Labor';
                laborData.found = true;
                console.log(`Found labor data in nested object ${key} with camelCase:`, laborData);
                return laborData;
            }
        }
    }

    // No labor data found in any expected location
    console.warn("No labor data found in response:", data);
    return null;
}

// Search functionality
function initializeSearch() {
    // Create and add the search bar to the DOM
    const tableHeader = document.querySelector('.table-header');
    if (tableHeader) {
        const searchContainer = document.createElement('div');
        searchContainer.className = 'search-container';
        searchContainer.innerHTML = `
            <div class="input-group">
                <span class="input-group-text">
                    <i class="fas fa-search"></i>
                </span>
                <input type="text" class="form-control" id="serviceSearchInput" placeholder="Search services...">
            </div>
        `;
        tableHeader.appendChild(searchContainer);

        // Add styles for the search container
        const style = document.createElement('style');
        style.textContent = `
            .table-header {
                display: flex;
                justify-content: space-between;
                align-items: center;
                margin-bottom: 1rem;
            }
            .search-container {
                width: 300px;
            }
            @media (max-width: 768px) {
                .table-header {
                    flex-direction: column;
                    align-items: flex-start;
                }
                .search-container {
                    width: 100%;
                    margin-top: 1rem;
                }
            }
        `;
        document.head.appendChild(style);

        // Add event listener for search input
        const searchInput = document.getElementById('serviceSearchInput');
        searchInput.addEventListener('input', function() {
            filterServicesTable(this.value.toLowerCase().trim());
        });
    }
}

// Filter table based on search term
function filterServicesTable(searchTerm) {
    const rows = document.querySelectorAll('#completedServicesTableBody tr:not(.no-results-row)');
    let visibleCount = 0;

    rows.forEach(row => {
        const text = row.textContent.toLowerCase();
        if (searchTerm === '' || text.includes(searchTerm)) {
            row.style.display = '';
            visibleCount++;
        } else {
            row.style.display = 'none';
        }
    });

    // Show or hide "no results" message
    const tableBody = document.getElementById('completedServicesTableBody');
    if (tableBody) {
        // Remove any existing no-results rows
        document.querySelectorAll('.no-results-row').forEach(el => el.remove());

        if (visibleCount === 0 && searchTerm !== '') {
            const noResultsRow = document.createElement('tr');
            noResultsRow.className = 'no-results-row';
            noResultsRow.innerHTML = `
                <td colspan="9" class="text-center py-4">
                    <div class="text-muted">
                        <i class="fas fa-search fa-2x mb-3"></i>
                        <p>No services found matching "${searchTerm}"</p>
                    </div>
                </td>
            `;
            tableBody.appendChild(noResultsRow);
        }
    }
}

function isMembershipPremium(service) {
    if (service.membershipStatus) {
        const status = String(service.membershipStatus).toLowerCase().trim();
        if (status.includes('premium')) {
            return true;
        }
    }
    if (service.customer && service.customer.membershipStatus) {
        const status = String(service.customer.membershipStatus).toLowerCase().trim();
        if (status.includes('premium')) {
            return true;
        }
    }
    if (service.isPremium || service.premium || service.isPremiumMember) {
        return true;
    }
    return false;
}

function getMembershipStatus(service) {
    const isPremium = isMembershipPremium(service);
    return isPremium ? 'Premium' : 'Standard';
}

function initializeEventListeners() {
    document.addEventListener('click', function(e) {
        if (e.target.classList.contains('view-service-btn') || e.target.closest('.view-service-btn')) {
            const btn = e.target.classList.contains('view-service-btn') ? e.target : e.target.closest('.view-service-btn');
            const serviceId = btn.getAttribute('data-id');
            viewServiceDetails(serviceId);
        }
    });

    document.getElementById('generateInvoiceBtn')?.addEventListener('click', function() {
        openGenerateInvoiceModal();
    });

    document.getElementById('confirmGenerateInvoiceBtn')?.addEventListener('click', function() {
        generateInvoice();
    });

    document.getElementById('confirmPaymentBtn')?.addEventListener('click', function() {
        processPayment();
    });

    document.getElementById('customerPickup')?.addEventListener('change', function() {
        if (this.checked) {
            document.getElementById('pickupFields').style.display = 'block';
            document.getElementById('deliveryFields').style.display = 'none';
            document.getElementById('confirmDeliveryBtnText').textContent = 'Confirm Pickup';
        }
    });

    document.getElementById('homeDelivery')?.addEventListener('change', function() {
        if (this.checked) {
            document.getElementById('pickupFields').style.display = 'none';
            document.getElementById('deliveryFields').style.display = 'block';
            document.getElementById('confirmDeliveryBtnText').textContent = 'Confirm Delivery';

            // Auto-fill customer address if available
            if (currentService) {
                const customerAddress = getCustomerAddress(currentService);
                if (customerAddress) {
                    document.getElementById('deliveryAddress').value = customerAddress;
                }
            }
        }
    });

    document.getElementById('confirmDeliveryBtn')?.addEventListener('click', function() {
        processDelivery();
    });

    document.getElementById('paymentMethod')?.addEventListener('change', function() {
        const transactionIdGroup = document.getElementById('transactionIdGroup');
        // Hide transaction ID field when Cash is selected
        if (this.value === 'Cash') {
            transactionIdGroup.style.display = 'none';
            document.getElementById('transactionId').value = 'CASH-' + Date.now().toString().slice(-6);
            document.getElementById('transactionId').disabled = true;
        } else {
            transactionIdGroup.style.display = 'block';
            document.getElementById('transactionId').value = '';
            document.getElementById('transactionId').disabled = false;
        }
    });
}

function normalizeServiceData(service) {
    if (!service) return;
    const isPremium = isMembershipPremium(service);
    service.membershipStatus = isPremium ? 'Premium' : 'Standard';

    if (!service.customerName || service.customerName === 'Unknown Customer') {
        if (service.customer) {
            if (typeof service.customer === 'object') {
                if (service.customer.firstName && service.customer.lastName) {
                    service.customerName = `${service.customer.firstName} ${service.customer.lastName}`;
                }
                else if (service.customer.user && service.customer.user.firstName && service.customer.user.lastName) {
                    service.customerName = `${service.customer.user.firstName} ${service.customer.user.lastName}`;
                }
                else if (service.customer.name) {
                    service.customerName = service.customer.name;
                }
            }
        }
        else if (service.firstName && service.lastName) {
            service.customerName = `${service.firstName} ${service.lastName}`;
        }
        else if (service.user && service.user.firstName && service.user.lastName) {
            service.customerName = `${service.user.firstName} ${service.user.lastName}`;
        }
    }

    if (!service.registrationNumber) {
        if (service.vehicleRegistration) {
            service.registrationNumber = service.vehicleRegistration;
        } else if (service.vehicle && service.vehicle.registrationNumber) {
            service.registrationNumber = service.vehicle.registrationNumber;
        }
    }
}

function loadCompletedServices() {
    const tableBody = document.getElementById('completedServicesTableBody');
    tableBody.innerHTML = `
        <tr>
            <td colspan="10" class="text-center py-4">
                <div class="spinner-border text-wine" role="status"></div>
                <p class="mt-2">Loading completed services...</p>
            </td>
        </tr>
    `;
    const token = getAuthToken();
    completedServices = [];
    fetch('/admin/api/completed-services', {
        method: 'GET',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        }
    })
        .then(response => {
            if (!response.ok) {
                throw new Error(`Failed to fetch completed services: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            if (!Array.isArray(data)) {
                throw new Error('Invalid data format received from API');
            }
            completedServices = data;

            return Promise.all(completedServices.map(service => {
                normalizeServiceData(service);
                return enhanceCustomerInfo(service);
            }));
        })
        .then(() => {
            renderCompletedServicesTable();
        })
        .catch(error => {
            tableBody.innerHTML = `
                <tr>
                    <td colspan="10" class="text-center py-4">
                        <div class="text-danger mb-3">
                            <i class="fas fa-exclamation-circle fa-2x"></i>
                        </div>
                        <p>Error loading completed services: ${error.message}</p>
                        <button class="btn-premium primary mt-3" onclick="loadCompletedServices()">
                            <i class="fas fa-sync-alt"></i> Try Again
                        </button>
                    </td>
                </tr>
            `;
        });
}

function enhanceCustomerInfo(service) {
    return new Promise((resolve, reject) => {
        try {
            normalizeServiceData(service);
            let customerId = service.customerId;
            if (!customerId) {
                if (service.customer && service.customer.customerId) {
                    customerId = service.customer.customerId;
                } else if (service.vehicle && service.vehicle.customer && service.vehicle.customer.customerId) {
                    customerId = service.vehicle.customer.customerId;
                } else if (service.userId) {
                    customerId = service.userId;
                }
            }

            if (!customerId) {
                const isPremium = isMembershipPremium(service);
                service.membershipStatus = isPremium ? 'Premium' : 'Standard';
                resolve(service);
                return;
            }

            const token = getAuthToken();
            fetch(`/admin/customers/api/${customerId}`, {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                }
            })
                .then(response => {
                    if (response.ok) return response.json();
                    throw new Error('Failed to fetch customer details');
                })
                .then(customerData => {
                    if (customerData) {
                        if (customerData.firstName && customerData.lastName) {
                            service.customerName = `${customerData.firstName} ${customerData.lastName}`;
                        }
                        if (customerData.email) {
                            service.customerEmail = customerData.email;
                        }
                        if (customerData.phoneNumber) {
                            service.customerPhone = customerData.phoneNumber;
                        }
                        if (customerData.membershipStatus) {
                            service.rawCustomerMembershipStatus = customerData.membershipStatus;
                            const status = String(customerData.membershipStatus).trim().toLowerCase();
                            if (status.includes('premium')) {
                                service.membershipStatus = 'Premium';
                            }
                            else if (service.membershipStatus !== 'Premium') {
                                service.membershipStatus = 'Standard';
                            }
                        }
                        service.enhancedCustomerData = customerData;
                    }
                    const isPremium = isMembershipPremium(service);
                    service.membershipStatus = isPremium ? 'Premium' : 'Standard';
                    resolve(service);
                })
                .catch(error => {
                    const isPremium = isMembershipPremium(service);
                    service.membershipStatus = isPremium ? 'Premium' : 'Standard';
                    resolve(service);
                });
        } catch (error) {
            if (service) {
                const isPremium = isMembershipPremium(service);
                service.membershipStatus = isPremium ? 'Premium' : 'Standard';
            }
            resolve(service);
        }
    });
}

function renderCompletedServicesTable() {
    const tableBody = document.getElementById('completedServicesTableBody');
    tableBody.innerHTML = '';
    if (!completedServices || completedServices.length === 0) {
        tableBody.innerHTML = `
            <tr>
                <td colspan="10" class="text-center py-5">
                    <div class="my-4">
                        <i class="fas fa-check-circle fa-3x text-muted mb-3" style="opacity: 0.3;"></i>
                        <h5>No Completed Services</h5>
                        <p class="text-muted">Completed vehicle services will appear here once they're finished.</p>
                    </div>
                </td>
            </tr>
        `;
        return;
    }

    completedServices.forEach(service => {
        const row = createServiceTableRow(service);
        tableBody.appendChild(row);
    });
}

function createServiceTableRow(service) {
    const row = document.createElement('tr');
    const completionDate = new Date(service.completionDate || service.completedDate || service.updatedAt);
    const formattedDate = completionDate.toLocaleDateString();
    const formatter = new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR',
        minimumFractionDigits: 2
    });
    const invoiceStatus = service.hasInvoice ?
        `<span class="status-badge status-completed"><i class="fas fa-check-circle"></i> Generated</span>` :
        `<span class="status-badge status-pending"><i class="fas fa-clock"></i> Pending</span>`;
    const paymentStatus = service.isPaid || service.paid ?
        `<span class="status-badge status-paid"><i class="fas fa-check-circle"></i> Paid</span>` :
        `<span class="status-badge status-pending"><i class="fas fa-clock"></i> Pending</span>`;
    const deliveryStatus = service.isDelivered || service.delivered ?
        `<span class="status-badge status-completed"><i class="fas fa-check-circle"></i> Completed</span>` :
        `<span class="status-badge status-pending"><i class="fas fa-clock"></i> Pending</span>`;
    const vehicleType = (service.vehicleType || service.category || '').toString().toLowerCase();
    const vehicleIcon = vehicleType.includes('bike') || vehicleType === 'bike' ?
        'fas fa-motorcycle' :
        vehicleType.includes('truck') || vehicleType === 'truck' ?
            'fas fa-truck' :
            'fas fa-car';
    const vehicleName = service.vehicleName ||
        (service.vehicleBrand && service.vehicleModel ?
            `${service.vehicleBrand} ${service.vehicleModel}` :
            'Unknown Vehicle');

    const isPremium = isMembershipPremium(service);
    const membershipStatus = isPremium ? 'Premium' : 'Standard';
    const membershipClass = isPremium ? 'membership-premium' : 'membership-standard';

    const customerName = service.customerName || 'Unknown Customer';
    row.innerHTML = `
        <td>REQ-${service.requestId || service.serviceId}</td>
        <td>
            <div class="vehicle-info">
                <div class="vehicle-icon">
                    <i class="${vehicleIcon}"></i>
                </div>
                <div class="vehicle-details">
                    <h5>${vehicleName}</h5>
                    <p>${service.registrationNumber || 'Unknown'}</p>
                </div>
            </div>
        </td>
        <td>${customerName}</td>
        <td>${formattedDate}</td>
        <td>${formatter.format(service.totalAmount || service.totalCost || service.calculatedTotal || 0)}</td>
        <td>${invoiceStatus}</td>
        <td>${paymentStatus}</td>
        <td>${deliveryStatus}</td>
        <td>
            <div class="table-actions-cell">
                <button class="btn-table-action view-service-btn" data-id="${service.requestId || service.serviceId}">
                    <i class="fas fa-eye"></i>
                </button>
            </div>
        </td>
    `;
    return row;
}

function viewServiceDetails(serviceId) {
    closeAllModals();
    currentServiceId = serviceId;
    document.getElementById('viewServiceId').textContent = `REQ-${serviceId}`;
    document.getElementById('viewVehicleName').textContent = 'Loading...';
    document.getElementById('viewRegistrationNumber').textContent = 'Loading...';
    document.getElementById('viewCustomerName').textContent = 'Loading...';
    document.getElementById('viewMembership').textContent = 'Loading...';
    document.getElementById('viewCompletionDate').textContent = 'Loading...';

    // Show the modal before fetching data for better UX
    const modal = new bootstrap.Modal(document.getElementById('viewServiceDetailsModal'));
    modal.show();

    loadBasicServiceDetails(serviceId);
    loadInvoiceData(serviceId);
}

function loadBasicServiceDetails(serviceId) {
    const token = getAuthToken();
    const serviceUrls = [
        `/admin/api/completed-services/${serviceId}`,
        `/admin/api/services/${serviceId}/details`,
        `/admin/api/service-details/${serviceId}`
    ];
    tryFetchUrls(serviceUrls, token)
        .then(service => {
            currentService = service;
            document.getElementById('viewServiceId').textContent = `REQ-${service.requestId || service.serviceId || serviceId}`;
            document.getElementById('viewVehicleName').textContent = getVehicleName(service);
            document.getElementById('viewRegistrationNumber').textContent = getRegistrationNumber(service);
            document.getElementById('viewCustomerName').textContent = service.customerName || 'Unknown Customer';
            document.getElementById('viewMembership').textContent = getMembershipStatus(service);
            document.getElementById('viewCompletionDate').textContent = getFormattedDate(service);
            updateWorkflowSteps(service);
            updateFooterButtons(service);
        })
        .catch(error => {
            document.getElementById('viewServiceId').textContent = `REQ-${serviceId}`;
            document.getElementById('viewVehicleName').textContent = 'Error loading details';
            document.getElementById('viewRegistrationNumber').textContent = 'Error loading details';
            document.getElementById('viewCustomerName').textContent = 'Error loading details';
            document.getElementById('viewMembership').textContent = 'Error loading details';
            document.getElementById('viewCompletionDate').textContent = 'Error loading details';
            showToast('Error loading service details: ' + error.message, 'error');
        });
}

function getRegistrationNumber(service) {
    if (service.registrationNumber) {
        return service.registrationNumber;
    }
    if (service.vehicleRegistration) {
        return service.vehicleRegistration;
    }
    if (service.vehicle && service.vehicle.registrationNumber) {
        return service.vehicle.registrationNumber;
    }
    return 'Unknown';
}

function tryFetchUrls(urls, token) {
    return urls.reduce((promise, url) => {
        return promise.catch(() => {
            return fetch(url, {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                }
            }).then(response => {
                if (!response.ok) {
                    throw new Error(`Failed to fetch from ${url}: ${response.status}`);
                }
                return response.json();
            });
        });
    }, Promise.reject(new Error('Starting URL chain')));
}

function getVehicleName(service) {
    if (service.vehicleName) {
        return service.vehicleName;
    }
    if (service.vehicleBrand && service.vehicleModel) {
        return `${service.vehicleBrand} ${service.vehicleModel}`;
    }
    if (service.vehicle) {
        const vehicle = service.vehicle;
        if (vehicle.brand && vehicle.model) {
            return `${vehicle.brand} ${vehicle.model}`;
        }
    }
    return 'Unknown Vehicle';
}

function getFormattedDate(service) {
    const dateFields = [
        'completionDate', 'completedDate', 'updatedAt',
        'formattedCompletedDate', 'formattedCompletionDate'
    ];
    let dateValue = null;
    for (const field of dateFields) {
        if (service[field]) {
            dateValue = service[field];
            break;
        }
    }
    if (!dateValue) {
        return new Date().toLocaleDateString();
    }
    if (typeof dateValue === 'string' && dateValue.includes(',')) {
        return dateValue;
    }
    try {
        const date = new Date(dateValue);
        if (!isNaN(date.getTime())) {
            return date.toLocaleDateString('en-US', {
                month: 'short',
                day: 'numeric',
                year: 'numeric'
            });
        }
    } catch (e) {
    }
    return dateValue;
}

function populateMaterialsTable(materials) {
    const tableBody = document.getElementById('materialsTableBody');
    tableBody.innerHTML = '';
    if (!materials || materials.length === 0) {
        tableBody.innerHTML = `
            <tr><td colspan="4" class="text-center">No materials used in this service</td></tr>
        `;
        return;
    }
    const formatter = new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR',
        minimumFractionDigits: 2
    });
    let materialTotal = 0;
    materials.forEach(material => {
        if (!material) return;
        const row = document.createElement('tr');
        const itemName = material.name || 'Unknown Item';
        const quantity = parseFloatSafe(material.quantity, 1);
        const unitPrice = parseFloatSafe(material.unitPrice, 0);
        let total;
        if (material.total) {
            total = parseFloatSafe(material.total, 0);
        } else {
            total = quantity * unitPrice;
        }
        materialTotal += total;
        row.innerHTML = `
            <td>${itemName}</td>
            <td>${quantity}</td>
            <td>${formatter.format(unitPrice)}</td>
            <td>${formatter.format(total)}</td>
        `;
        tableBody.appendChild(row);
    });
    if (materials.length > 1) {
        const totalRow = document.createElement('tr');
        totalRow.innerHTML = `
            <td colspan="3" class="text-end fw-bold">Total</td>
            <td class="fw-bold">${formatter.format(materialTotal)}</td>
        `;
        tableBody.appendChild(totalRow);
    }
}

function getAuthToken() {
    const urlParams = new URLSearchParams(window.location.search);
    const tokenParam = urlParams.get('token');
    if (tokenParam) return tokenParam;
    const sessionToken = sessionStorage.getItem('jwt-token');
    if (sessionToken) return sessionToken;
    const localToken = localStorage.getItem('jwt-token');
    if (localToken) return localToken;
    if (typeof token !== 'undefined') return token;
    return '';
}

function parseFloatSafe(value, defaultValue) {
    if (value === null || value === undefined) {
        return defaultValue;
    }
    if (typeof value === 'number') {
        return value;
    }
    try {
        const parsed = parseFloat(value);
        return isNaN(parsed) ? defaultValue : parsed;
    } catch (e) {
        return defaultValue;
    }
}

function populateLaborChargesTable(laborCharges) {
    const tableBody = document.getElementById('laborChargesTableBody');
    tableBody.innerHTML = '';

    if (!laborCharges || laborCharges.length === 0) {
        tableBody.innerHTML = `
            <tr><td colspan="4" class="text-center">No labor charges recorded for this service</td></tr>
        `;
        return;
    }

    const formatter = new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR',
        minimumFractionDigits: 2
    });

    let laborTotal = 0;
    laborCharges.forEach(charge => {
        if (!charge) return;

        const row = document.createElement('tr');
        const description = charge.description || 'Service Labor';

        // Get hours
        const hours = parseFloatSafe(charge.hours, 0);

        // IMPORTANT: Use 'rate' to match backend field name
        // Fall back to 'ratePerHour' for backward compatibility
        const ratePerHour = parseFloatSafe(charge.rate || charge.ratePerHour, 0);

        // Get or calculate total
        let total = parseFloatSafe(charge.total, hours * ratePerHour);

        laborTotal += total;

        row.innerHTML = `
            <td>${description}</td>
            <td>${hours.toFixed(2)}</td>
            <td>${formatter.format(ratePerHour)}/hr</td>
            <td>${formatter.format(total)}</td>
        `;
        tableBody.appendChild(row);
    });

    if (laborCharges.length > 1) {
        const totalRow = document.createElement('tr');
        totalRow.innerHTML = `
            <td colspan="3" class="text-end fw-bold">Total</td>
            <td class="fw-bold">${formatter.format(laborTotal)}</td>
        `;
        tableBody.appendChild(totalRow);
    }
}

function formatCurrency(amount) {
    return new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR',
        minimumFractionDigits: 2
    }).format(amount);
}

function updateInvoiceSummary(data) {
    const formatter = new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR',
        minimumFractionDigits: 2
    });

    if (!data) {
        document.getElementById('summaryMaterialsTotal').textContent = formatter.format(0);
        document.getElementById('summaryLaborTotal').textContent = formatter.format(0);
        document.getElementById('premiumDiscountRow').style.display = 'none';
        document.getElementById('summarySubtotal').textContent = formatter.format(0);
        document.getElementById('summaryGST').textContent = formatter.format(0);
        document.getElementById('summaryGrandTotal').textContent = formatter.format(0);
        return;
    }

    // Materials total
    const materialsTotal = parseFloatSafe(
        data.materialsTotal || data.calculatedMaterialsTotal || data.total_material_cost,
        0
    );

    // Labor total
    const laborTotal = parseFloatSafe(
        data.laborTotal || data.calculatedLaborTotal || data.labor_cost,
        0
    );

    // Calculate premium discount (30% of labor only)
    let discount = 0;

    if (data.membershipStatus &&
        (data.membershipStatus.toLowerCase() === 'premium' ||
            data.membershipStatus.toLowerCase().includes('premium'))) {
        // Apply 30% discount to labor only
        discount = parseFloat((laborTotal * 0.3).toFixed(2));
    }

    // Calculate subtotal
    const subtotal = parseFloat((materialsTotal + laborTotal - discount).toFixed(2));

    // Calculate tax (18% GST)
    const tax = parseFloat((subtotal * 0.18).toFixed(2));

    // Calculate grand total
    const grandTotal = parseFloat((subtotal + tax).toFixed(2));

    // Update UI
    document.getElementById('summaryMaterialsTotal').textContent = formatter.format(materialsTotal);
    document.getElementById('summaryLaborTotal').textContent = formatter.format(laborTotal);

    if (discount > 0) {
        // Update discount label
        if (document.getElementById('premiumDiscountLabel')) {
            document.getElementById('premiumDiscountLabel').textContent = 'Premium Discount (30% off labor)';
        }

        document.getElementById('summaryDiscount').textContent = `-${formatter.format(discount)}`;
        document.getElementById('premiumDiscountRow').style.display = '';
    } else {
        document.getElementById('premiumDiscountRow').style.display = 'none';
    }

    document.getElementById('summarySubtotal').textContent = formatter.format(subtotal);
    document.getElementById('summaryGST').textContent = formatter.format(tax);
    document.getElementById('summaryGrandTotal').textContent = formatter.format(grandTotal);

    // Store calculated values for later use
    data.calculatedMaterialsTotal = materialsTotal;
    data.calculatedLaborTotal = laborTotal;
    data.calculatedDiscount = discount;
    data.calculatedSubtotal = subtotal;
    data.calculatedTax = tax;
    data.calculatedTotal = grandTotal;
}

function updateWorkflowSteps(service) {
    document.querySelectorAll('.workflow-step').forEach(step => {
        step.classList.remove('active', 'completed');
    });
    const hasInvoice = service.hasInvoice || service.invoiceId ||
        (service.invoice && service.invoice.invoiceId) || false;
    const isPaid = service.isPaid || service.paid ||
        (service.payment && service.payment.status === 'Completed') || false;
    const isDelivered = service.isDelivered || service.delivered || false;
    if (hasInvoice) {
        document.getElementById('stepInvoice').classList.add('completed');
        if (isPaid) {
            document.getElementById('stepPayment').classList.add('completed');
            if (isDelivered) {
                document.getElementById('stepDelivery').classList.add('completed');
            } else {
                document.getElementById('stepDelivery').classList.add('active');
            }
        } else {
            document.getElementById('stepPayment').classList.add('active');
        }
    } else {
        document.getElementById('stepInvoice').classList.add('active');
    }
    service.hasInvoice = hasInvoice;
    service.isPaid = isPaid;
    service.isDelivered = isDelivered;
}

function updateFooterButtons(service) {
    const footer = document.getElementById('serviceDetailsFooter');
    const actionButtons = footer.querySelectorAll('button:not([data-bs-dismiss="modal"])');
    actionButtons.forEach(button => button.remove());
    if (!service.hasInvoice) {
        const generateInvoiceBtn = document.createElement('button');
        generateInvoiceBtn.type = 'button';
        generateInvoiceBtn.className = 'btn-premium primary';
        generateInvoiceBtn.innerHTML = '<i class="fas fa-file-invoice"></i> Generate Invoice';
        generateInvoiceBtn.addEventListener('click', openGenerateInvoiceModal);
        footer.appendChild(generateInvoiceBtn);
    } else if (!service.isPaid) {
        const processPaymentBtn = document.createElement('button');
        processPaymentBtn.type = 'button';
        processPaymentBtn.className = 'btn-premium primary';
        processPaymentBtn.innerHTML = '<i class="fas fa-money-bill-wave"></i> Process Payment';
        processPaymentBtn.addEventListener('click', openPaymentModal);
        footer.appendChild(processPaymentBtn);
    } else if (!service.isDelivered) {
        const deliveryBtn = document.createElement('button');
        deliveryBtn.type = 'button';
        deliveryBtn.className = 'btn-premium primary';
        deliveryBtn.innerHTML = '<i class="fas fa-truck"></i> Schedule Delivery';
        deliveryBtn.addEventListener('click', openDeliveryModal);
        footer.appendChild(deliveryBtn);
    }
    if (service.hasInvoice) {
        const downloadInvoiceBtn = document.createElement('button');
        downloadInvoiceBtn.type = 'button';
        downloadInvoiceBtn.className = 'btn-premium secondary me-2';
        downloadInvoiceBtn.innerHTML = '<i class="fas fa-download"></i> Download Invoice';
        downloadInvoiceBtn.addEventListener('click', () => downloadInvoice(service.requestId || service.serviceId));
        footer.insertBefore(downloadInvoiceBtn, footer.firstChild);
    }
}

function loadInvoiceData(serviceId) {
    document.getElementById('materialsTableBody').innerHTML = `
        <tr><td colspan="4" class="text-center"><div class="spinner-border spinner-border-sm text-wine" role="status"></div> Loading materials...</td></tr>
    `;
    document.getElementById('laborChargesTableBody').innerHTML = `
        <tr><td colspan="4" class="text-center"><div class="spinner-border spinner-border-sm text-wine" role="status"></div> Loading labor charges...</td></tr>
    `;

    const token = getAuthToken();

    // Use endpoints that might have the labor data
    const invoiceUrls = [
        `/admin/api/completed-services/${serviceId}/invoice-details`,
        `/admin/api/vehicle-tracking/service-request/${serviceId}`,
        `/admin/service-requests/api/${serviceId}`,
        `/serviceAdvisor/api/service-details/${serviceId}`,
        `/admin/api/service/${serviceId}/labor-charges`
    ];

    console.log(`Loading invoice data for service ID: ${serviceId}`);

    tryFetchUrls(invoiceUrls, token)
        .then(data => {
            console.log("Raw data from API:", data);

            if (!data) {
                data = createDefaultInvoiceData(serviceId);
            }

            // Extract labor data using our enhanced function
            const laborData = extractLaborData(data);

            if (laborData && laborData.found) {
                console.log("Labor data extracted successfully:", laborData);

                // Convert minutes to hours (ensure floating point division)
                const laborHours = laborData.minutes / 60;

                // Calculate hourly rate (avoid division by zero)
                const hourlyRate = laborHours > 0 ? parseFloat((laborData.cost / laborHours).toFixed(2)) : 0;

                // Create labor charge object with correct values
                const laborCharge = {
                    description: laborData.description || 'Service Labor',
                    hours: parseFloat(laborHours.toFixed(2)),
                    rate: hourlyRate,
                    total: parseFloat(laborData.cost)
                };

                console.log("Created labor charge from extracted data:", laborCharge);

                // Override any existing labor charges
                data.laborCharges = [laborCharge];
                data.laborTotal = parseFloat(laborData.cost);
            }
            // If no labor data was found but laborCharges exists
            else if (data.laborCharges && data.laborCharges.length > 0) {
                console.log("Using existing labor charges:", data.laborCharges);

                // Calculate labor total
                let total = 0;
                data.laborCharges.forEach(charge => {
                    // Important: use 'rate' (backend field) instead of 'ratePerHour'
                    const hours = parseFloatSafe(charge.hours, 0);
                    const rate = parseFloatSafe(charge.rate || charge.ratePerHour, 0);

                    // Ensure each charge has a total
                    if (charge.total === undefined) {
                        charge.total = hours * rate;
                    }

                    total += parseFloatSafe(charge.total, 0);
                });

                data.laborTotal = total;
            }
            // If no labor data was found at all, create default labor charge
            else if (data.serviceType || data.service_type) {
                console.log("No labor data found, creating default labor charge");

                const serviceType = data.serviceType || data.service_type || "Service";

                // Create a default labor charge
                data.laborCharges = [{
                    description: `Service: ${serviceType}`,
                    hours: 0,
                    rate: 0,
                    total: 0
                }];

                data.laborTotal = 0;
            }

            const materials = data.materials || [];
            populateMaterialsTable(materials);
            populateLaborChargesTable(data.laborCharges || []);
            updateInvoiceSummary(data);
        })
        .catch(error => {
            console.error("Error loading invoice data:", error);
            document.getElementById('materialsTableBody').innerHTML = `
                <tr><td colspan="4" class="text-center">No materials data available</td></tr>
            `;
            document.getElementById('laborChargesTableBody').innerHTML = `
                <tr><td colspan="4" class="text-center">No labor charges data available</td></tr>
            `;
            updateInvoiceSummary(createDefaultInvoiceData(serviceId));
        });
}

function createDefaultInvoiceData(serviceId) {
    return {
        requestId: serviceId,
        serviceId: serviceId,
        materialsTotal: 0,
        laborTotal: 0,
        discount: 0,
        subtotal: 0,
        tax: 0,
        grandTotal: 0,
        materials: [],
        laborCharges: []
    };
}
function numberToWords(num) {
    const ones = ['', 'One', 'Two', 'Three', 'Four', 'Five', 'Six', 'Seven', 'Eight', 'Nine',
        'Ten', 'Eleven', 'Twelve', 'Thirteen', 'Fourteen', 'Fifteen', 'Sixteen',
        'Seventeen', 'Eighteen', 'Nineteen'];
    const tens = ['', '', 'Twenty', 'Thirty', 'Forty', 'Fifty', 'Sixty', 'Seventy', 'Eighty', 'Ninety'];

    function convertLessThanOneThousand(num) {
        if (num === 0) {
            return '';
        }
        if (num < 20) {
            return ones[num];
        }

        const digit = num % 10;
        if (num < 100) {
            return tens[Math.floor(num / 10)] + (digit ? ' ' + ones[digit] : '');
        }

        return ones[Math.floor(num / 100)] + ' Hundred' + (num % 100 ? ' and ' + convertLessThanOneThousand(num % 100) : '');
    }

    if (num === 0) {
        return 'Zero';
    }

    let result = '';

    // Handle crores (10 million)
    if (num >= 10000000) {
        result += convertLessThanOneThousand(Math.floor(num / 10000000)) + ' Crore ';
        num %= 10000000;
    }

    // Handle lakhs (100 thousand)
    if (num >= 100000) {
        result += convertLessThanOneThousand(Math.floor(num / 100000)) + ' Lakh ';
        num %= 100000;
    }

    // Handle thousands
    if (num >= 1000) {
        result += convertLessThanOneThousand(Math.floor(num / 1000)) + ' Thousand ';
        num %= 1000;
    }

    // Handle hundreds and remaining
    if (num > 0) {
        result += convertLessThanOneThousand(num);
    }

    return result.trim();
}
function generateInvoicePDF(data, serviceId) {
    try {
        // Check if jsPDF is available
        if (typeof jspdf === 'undefined' || typeof jspdf.jsPDF === 'undefined') {
            throw new Error("jsPDF library not loaded");
        }

        // Initialize jsPDF
        const { jsPDF } = jspdf;
        const doc = new jsPDF({
            orientation: 'portrait',
            unit: 'mm',
            format: 'a4'
        });

        // Set document properties
        doc.setProperties({
            title: `Invoice REQ-${serviceId}`,
            subject: 'Vehicle Service Invoice',
            author: 'Albany ',
            creator: 'Albany'
        });

        // Get data needed for invoice
        const invoiceNumber = `INV-${data.invoiceId || serviceId}`;
        const currentDate = new Date().toLocaleDateString('en-US', {
            year: 'numeric', month: 'long', day: 'numeric'
        });

        // Extract customer information
        const customerName = data.customerName || 'Valued Customer';
        const vehicleInfo = getVehicleInfo(data);
        const registrationNumber = data.registrationNumber || data.vehicleRegistration || 'N/A';

        // Formatting helper function
        const formatCurrency = (amount) => {
            return new Intl.NumberFormat('en-IN', {
                style: 'currency',
                currency: 'INR',
                minimumFractionDigits: 2
            }).format(amount || 0);
        };

        // Set font styles
        doc.setFont('helvetica', 'bold');
        doc.setFontSize(20);
        doc.setTextColor(114, 47, 55); // Wine color

        // Add company header
        doc.text('ALBANY', 105, 20, { align: 'center' });

        doc.setFontSize(10);
        doc.setTextColor(100, 100, 100);
        doc.text('123 Service Road, Salem, TamilNadu - 636015', 105, 25, { align: 'center' });
        doc.text('Phone: +91 9827372810 | Email: info@info.albanyservice@gmail.com', 105, 30, { align: 'center' });

        // Add horizontal line
        doc.setDrawColor(114, 47, 55);
        doc.setLineWidth(0.5);
        doc.line(15, 35, 195, 35);

        // Invoice title and details
        doc.setFontSize(16);
        doc.setTextColor(0, 0, 0);
        doc.text('TAX INVOICE', 105, 45, { align: 'center' });

        doc.setFontSize(10);
        doc.setFont('helvetica', 'normal');

        // Invoice details box (left side)
        doc.roundedRect(15, 50, 85, 30, 2, 2);
        doc.setFont('helvetica', 'bold');
        doc.text('Invoice Number:', 20, 58);
        doc.text('Invoice Date:', 20, 66);
        doc.text('Service ID:', 20, 74);

        doc.setFont('helvetica', 'normal');
        doc.text(invoiceNumber, 60, 58);
        doc.text(currentDate, 60, 66);
        doc.text(`REQ-${serviceId}`, 60, 74);

        // Customer details box (right side)
        doc.roundedRect(105, 50, 85, 30, 2, 2);
        doc.setFont('helvetica', 'bold');
        doc.text('Customer:', 110, 58);
        doc.text('Vehicle:', 110, 66);
        doc.text('Registration:', 110, 74);

        doc.setFont('helvetica', 'normal');
        doc.text(customerName, 140, 58);
        doc.text(vehicleInfo, 140, 66);
        doc.text(registrationNumber, 140, 74);

        // Add invoice items section title
        doc.setFont('helvetica', 'bold');
        doc.setFontSize(12);
        doc.text('INVOICE DETAILS', 15, 90);

        // Materials table
        const materials = data.materials || [];
        if (materials.length > 0) {
            const materialRows = materials.map(item => {
                if (!item) return null;
                const quantity = parseFloatSafe(item.quantity, 1);
                const unitPrice = parseFloatSafe(item.unitPrice, 0);
                const total = parseFloatSafe(item.total, quantity * unitPrice);

                return [
                    item.name || 'Unknown Item',
                    quantity,
                    formatCurrency(unitPrice).replace('₹', ''),
                    formatCurrency(total).replace('₹', '')
                ];
            }).filter(row => row !== null);

            doc.autoTable({
                head: [['Material', 'Quantity', 'Unit Price (₹)', 'Amount (₹)']],
                body: materialRows,
                startY: 95,
                theme: 'grid',
                headStyles: {
                    fillColor: [114, 47, 55],
                    textColor: [255, 255, 255],
                    fontStyle: 'bold'
                },
                styles: {
                    fontSize: 9
                },
                columnStyles: {
                    0: { cellWidth: 80 },
                    1: { cellWidth: 25, halign: 'right' },
                    2: { cellWidth: 35, halign: 'right' },
                    3: { cellWidth: 35, halign: 'right' }
                }
            });
        } else {
            doc.setFont('helvetica', 'normal');
            doc.setFontSize(10);
            doc.text('No materials used in this service.', 15, 100);
            doc.autoTable({ startY: 105 }); // This sets the cursor position
        }

        // Labor charges table
        const laborCharges = data.laborCharges || [];
        if (laborCharges.length > 0) {
            doc.setFont('helvetica', 'bold');
            doc.setFontSize(12);
            doc.text('LABOR CHARGES', 15, doc.previousAutoTable.finalY + 10);

            const laborRows = laborCharges.map(charge => {
                if (!charge) return null;
                const hours = parseFloatSafe(charge.hours, 0);
                const rate = parseFloatSafe(charge.rate || charge.ratePerHour, 0);
                const total = parseFloatSafe(charge.total, hours * rate);

                return [
                    charge.description || 'Service Labor',
                    hours.toFixed(2),
                    formatCurrency(rate).replace('₹', ''),
                    formatCurrency(total).replace('₹', '')
                ];
            }).filter(row => row !== null);

            doc.autoTable({
                head: [['Description', 'Hours', 'Rate/Hour (₹)', 'Amount (₹)']],
                body: laborRows,
                startY: doc.previousAutoTable.finalY + 15,
                theme: 'grid',
                headStyles: {
                    fillColor: [114, 47, 55],
                    textColor: [255, 255, 255],
                    fontStyle: 'bold'
                },
                styles: {
                    fontSize: 9
                },
                columnStyles: {
                    0: { cellWidth: 80 },
                    1: { cellWidth: 25, halign: 'right' },
                    2: { cellWidth: 35, halign: 'right' },
                    3: { cellWidth: 35, halign: 'right' }
                }
            });
        } else {
            // If no previous table, set a default Y position
            const startY = doc.previousAutoTable ? doc.previousAutoTable.finalY + 15 : 110;
            doc.setFont('helvetica', 'bold');
            doc.setFontSize(12);
            doc.text('LABOR CHARGES', 15, startY);

            doc.setFont('helvetica', 'normal');
            doc.setFontSize(10);
            doc.text('No labor charges recorded for this service.', 15, startY + 10);

            // Set a position for the next section
            doc.autoTable({
                startY: startY + 15,
                body: [['', '', '', '']]
            });
        }

        // Calculate totals
        const materialsTotal = parseFloatSafe(data.calculatedMaterialsTotal || data.materialsTotal, 0);
        const laborTotal = parseFloatSafe(data.calculatedLaborTotal || data.laborTotal, 0);
        const discount = parseFloatSafe(data.calculatedDiscount || data.discount, 0);
        const subtotal = parseFloatSafe(data.calculatedSubtotal || data.subtotal, materialsTotal + laborTotal - discount);
        const tax = parseFloatSafe(data.calculatedTax || data.tax, subtotal * 0.18);
        const total = parseFloatSafe(data.calculatedTotal || data.totalAmount || data.grandTotal, subtotal + tax);

        // Summary table
        const summaryData = [];
        summaryData.push(['Materials Total', '', '', formatCurrency(materialsTotal).replace('₹', '')]);
        summaryData.push(['Labor Total', '', '', formatCurrency(laborTotal).replace('₹', '')]);

        if (discount > 0) {
            summaryData.push(['Premium Discount', '', '', '-' + formatCurrency(discount).replace('₹', '')]);
        }

        summaryData.push(['Subtotal', '', '', formatCurrency(subtotal).replace('₹', '')]);
        summaryData.push(['GST (18%)', '', '', formatCurrency(tax).replace('₹', '')]);

        doc.autoTable({
            body: summaryData,
            startY: doc.previousAutoTable.finalY + 10,
            theme: 'plain',
            styles: {
                fontSize: 9
            },
            columnStyles: {
                0: { cellWidth: 80 },
                1: { cellWidth: 25 },
                2: { cellWidth: 35 },
                3: { cellWidth: 35, halign: 'right' }
            }
        });

        // Grand total
        doc.autoTable({
            body: [['GRAND TOTAL', '', '', formatCurrency(total).replace('₹', '')]],
            startY: doc.previousAutoTable.finalY + 1,
            theme: 'grid',
            styles: {
                fontSize: 10,
                fontStyle: 'bold',
                halign: 'right'
            },
            columnStyles: {
                0: { cellWidth: 80, halign: 'left' },
                1: { cellWidth: 25 },
                2: { cellWidth: 35 },
                3: { cellWidth: 35, halign: 'right' }
            },
            headStyles: {
                fillColor: [114, 47, 55]
            },
            bodyStyles: {
                fillColor: [240, 240, 240]
            }
        });

        // Amount in words
        const amountInWords = numberToWords(Math.round(total));
        doc.setFontSize(9);
        doc.setFont('helvetica', 'bold');
        doc.text('Amount in words:', 15, doc.previousAutoTable.finalY + 10);
        doc.setFont('helvetica', 'normal');
        doc.text(`${amountInWords} Rupees Only`, 50, doc.previousAutoTable.finalY + 10);

        // Payment status
        const isPaid = data.isPaid || data.paid || false;
        if (isPaid) {
            doc.setFontSize(14);
            doc.setFont('helvetica', 'bold');
            doc.setTextColor(39, 174, 96); // Green color
            doc.text('PAID', 105, doc.previousAutoTable.finalY + 20, { align: 'center' });
        }

        // Add terms and conditions
        doc.setFontSize(9);
        doc.setFont('helvetica', 'bold');
        doc.setTextColor(0, 0, 0);
        doc.text('Terms & Conditions:', 15, doc.previousAutoTable.finalY + 25);

        doc.setFont('helvetica', 'normal');
        const terms = [
            '1. All services come with a 3-month warranty for parts and labor.',
            '2. Please retain this invoice for warranty claims.',
            '3. Any complaints regarding service should be reported within 7 days.',
            '4. All prices are inclusive of GST.'
        ];

        let yPos = doc.previousAutoTable.finalY + 30;
        terms.forEach(term => {
            doc.text(term, 15, yPos);
            yPos += 5;
        });

        // Add footer
        const pageCount = doc.internal.getNumberOfPages();
        for (let i = 1; i <= pageCount; i++) {
            doc.setPage(i);
            doc.setFontSize(8);
            doc.setTextColor(100, 100, 100);
            doc.text(
                'Thank you for choosing Albany ',
                105,
                doc.internal.pageSize.height - 10,
                { align: 'center' }
            );
            doc.text(
                `Page ${i} of ${pageCount}`,
                doc.internal.pageSize.width - 20,
                doc.internal.pageSize.height - 10
            );
        }

        // Save the PDF
        const fileName = `Invoice_REQ-${serviceId}_${new Date().toISOString().split('T')[0]}.pdf`;
        doc.save(fileName);

        showToast('Invoice PDF downloaded successfully', 'success');
    } catch (error) {
        console.error("Error generating PDF:", error);
        showToast('Error generating PDF: ' + error.message, 'error');

        // Fallback to server-side PDF generation
        const token = getAuthToken();
        fallbackToPDFDownload(serviceId, token);
    }
}
function fallbackToPDFDownload(serviceId, token) {
    const downloadUrl = `/admin/api/completed-services/${serviceId}/invoice/download?token=${token}`;
    window.open(downloadUrl, '_blank');
}
function getVehicleInfo(data) {
    if (data.vehicleName) {
        return data.vehicleName;
    }

    let vehicleInfo = '';

    if (data.vehicleBrand) {
        vehicleInfo += data.vehicleBrand;
    }

    if (data.vehicleModel) {
        vehicleInfo += vehicleInfo ? ' ' + data.vehicleModel : data.vehicleModel;
    }

    if (data.vehicleYear) {
        vehicleInfo += vehicleInfo ? ` (${data.vehicleYear})` : data.vehicleYear;
    }

    if (!vehicleInfo && data.vehicle) {
        const vehicle = data.vehicle;
        vehicleInfo = `${vehicle.brand || ''} ${vehicle.model || ''} ${vehicle.year ? '(' + vehicle.year + ')' : ''}`.trim();
    }

    return vehicleInfo || 'Unknown Vehicle';
}


function downloadInvoice(serviceId) {
    // Show loading indicator
    showToast('Generating invoice PDF...', 'info');

    const token = getAuthToken();

    // First try to get the invoice data
    fetch(`/admin/api/completed-services/${serviceId}/invoice-details`, {
        method: 'GET',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        }
    })
        .then(response => {
            if (!response.ok) {
                throw new Error(`Failed to fetch invoice data: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            console.log("Invoice data for PDF:", data);
            generateInvoicePDF(data, serviceId);
        })
        .catch(error => {
            console.error("Error fetching invoice data:", error);
            showToast('Error generating invoice PDF: ' + error.message, 'error');

            // Fallback to server-side PDF generation
            fallbackToPDFDownload(serviceId, token);
        });
}

function openGenerateInvoiceModal() {
    if (!currentService) {
        showToast('Service data not available', 'error');
        return;
    }

    // Close service details modal first
    closeAllModals();

    // Format currency for display
    const formatter = new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR',
        minimumFractionDigits: 2
    });

    // Set service and customer information
    document.getElementById('invoiceServiceId').textContent = `REQ-${currentService.requestId || currentService.serviceId}`;
    document.getElementById('invoiceCustomerName').textContent = currentService.customerName || 'Unknown Customer';

    // Pre-fill customer email if available
    if (currentService.customerEmail) {
        document.getElementById('customerEmail').value = currentService.customerEmail;
    } else if (currentService.enhancedCustomerData && currentService.enhancedCustomerData.email) {
        document.getElementById('customerEmail').value = currentService.enhancedCustomerData.email;
    } else {
        // Try to get email from different locations in the object
        const email = findEmailInObject(currentService);
        document.getElementById('customerEmail').value = email || '';
    }

    // Set invoice amounts
    let materialsTotal = currentService.calculatedMaterialsTotal || 0;
    let laborTotal = currentService.calculatedLaborTotal || 0;
    let discount = currentService.calculatedDiscount || 0;
    let subtotal = currentService.calculatedSubtotal || 0;
    let tax = currentService.calculatedTax || 0;
    let total = currentService.calculatedTotal || 0;

    // Handle premium status and discount display
    const isPremium = isMembershipPremium(currentService);
    const premiumDiscountRow = document.getElementById('invoicePremiumDiscountRow');
    const premiumBadge = document.getElementById('invoicePremiumBadge');

    if (isPremium) {
        premiumBadge.style.display = '';
        if (discount > 0) {
            document.getElementById('invoiceDiscount').textContent = `-${formatter.format(discount)}`;
            premiumDiscountRow.style.display = '';
        } else {
            premiumDiscountRow.style.display = 'none';
        }
    } else {
        premiumDiscountRow.style.display = 'none';
        premiumBadge.style.display = 'none';
    }

    // Set amount displays
    document.getElementById('invoiceMaterialsTotal').textContent = formatter.format(materialsTotal);
    document.getElementById('invoiceLaborTotal').textContent = formatter.format(laborTotal);
    document.getElementById('invoiceSubtotal').textContent = formatter.format(subtotal);
    document.getElementById('invoiceGST').textContent = formatter.format(tax);
    document.getElementById('invoiceGrandTotal').textContent = formatter.format(total);

    // Check if send email checkbox should be checked by default
    const emailInput = document.getElementById('customerEmail');
    const sendEmailCheckbox = document.getElementById('sendInvoiceEmail');
    sendEmailCheckbox.checked = emailInput.value.trim() !== '';

    // Show invoice generation modal
    safelyOpenModal('generateInvoiceModal');
}

function getCustomerAddress(service) {
    let address = '';

    // Try to get address from different possible locations in the service object
    if (service.enhancedCustomerData) {
        const customer = service.enhancedCustomerData;
        const addressParts = [];

        if (customer.street) addressParts.push(customer.street);
        if (customer.city) addressParts.push(customer.city);
        if (customer.state) addressParts.push(customer.state);
        if (customer.postalCode) addressParts.push(customer.postalCode);

        if (addressParts.length > 0) {
            return addressParts.join(', ');
        }
    }

    // Try alternate locations for address
    if (service.customer) {
        const customer = service.customer;
        const addressParts = [];

        if (customer.street) addressParts.push(customer.street);
        if (customer.city) addressParts.push(customer.city);
        if (customer.state) addressParts.push(customer.state);
        if (customer.postalCode) addressParts.push(customer.postalCode);

        if (addressParts.length > 0) {
            return addressParts.join(', ');
        }
    }

    // If no address found, return empty string
    return address;
}

function showSuccessMessage(title, message, onClose) {
    // First ensure any existing modals are properly closed
    closeAllModals();

    // Get or create success modal
    const successModal = document.getElementById('successModal');

    // Update modal content
    document.getElementById('successTitle').textContent = title;
    document.getElementById('successMessage').textContent = message;

    // Get or create modal instance
    let modalInstance = bootstrap.Modal.getInstance(successModal);
    if (!modalInstance) {
        modalInstance = new bootstrap.Modal(successModal);
    }

    // Setup close handler
    if (typeof onClose === 'function') {
        successModal.addEventListener('hidden.bs.modal', function handler() {
            successModal.removeEventListener('hidden.bs.modal', handler);
            setTimeout(onClose, 100);
        });
    }

    // Show the modal
    modalInstance.show();
}

function generateInvoice() {
    if (!currentService) {
        showToast('Service data not available', 'error');
        return;
    }

    // Get email address from input field
    const email = document.getElementById('customerEmail').value.trim();

    // Validate email - this is just basic validation
    if (!email) {
        showToast('Please enter customer email', 'error');
        return;
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
        showToast('Please enter a valid email address', 'error');
        return;
    }

    // Get whether to send the email or not
    const sendEmail = document.getElementById('sendInvoiceEmail').checked;

    // Disable button to prevent double-clicks
    const confirmBtn = document.getElementById('confirmGenerateInvoiceBtn');
    confirmBtn.disabled = true;
    confirmBtn.innerHTML = '<div class="spinner-border spinner-border-sm me-2" role="status"></div> Generating...';

    // Close the modal
    closeAllModals();

    // Prepare request data
    const invoiceRequest = {
        serviceId: currentService.requestId || currentService.serviceId,
        emailAddress: email,
        sendEmail: sendEmail,
        notes: "Generated from admin portal",
        materialsTotal: currentService.calculatedMaterialsTotal,
        laborTotal: currentService.calculatedLaborTotal,
        discount: currentService.calculatedDiscount,
        subtotal: currentService.calculatedSubtotal,
        tax: currentService.calculatedTax,
        total: currentService.calculatedTotal,
        membershipStatus: getMembershipStatus(currentService)
    };

    // Get authentication token
    const token = getAuthToken();

    // Make API call to generate invoice
    fetch(`/admin/api/invoices/service-request/${currentService.requestId || currentService.serviceId}/generate`, {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(invoiceRequest)
    })
        .then(response => {
            if (!response.ok) {
                return response.json().then(data => {
                    throw new Error(data.error || 'Failed to generate invoice');
                });
            }
            return response.json();
        })
        .then(data => {
            // Reset button state
            confirmBtn.disabled = false;
            confirmBtn.innerHTML = '<i class="fas fa-file-invoice"></i> Generate & Send';

            // Update local service data
            if (currentService) {
                currentService.hasInvoice = true;
                currentService.invoiceId = data.invoiceId;
            }

            // Refresh the service list
            loadCompletedServices();

            // Show success message
            showSuccessMessage(
                'Invoice Generated',
                sendEmail ? `Invoice has been generated and sent to ${email}` : 'Invoice has been generated successfully',
                () => openPaymentModal()
            );
        })
        .catch(error => {
            // Reset button state
            confirmBtn.disabled = false;
            confirmBtn.innerHTML = '<i class="fas fa-file-invoice"></i> Generate & Send';

            // Show error message
            showToast('Error: ' + error.message, 'error');
        });
}

function fetchLaborCharges(serviceId, token) {
    return fetch(`/admin/api/service/${serviceId}/labor-charges`, {
        method: 'GET',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        }
    })
        .then(response => {
            if (!response.ok) {
                throw new Error(`Failed to fetch labor charges: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            console.log("Labor charges from dedicated endpoint:", data);

            // Extract labor charges from the response
            if (data.laborHours !== undefined && data.hourlyRate !== undefined) {
                return {
                    laborCharges: [{
                        description: 'Service Labor',
                        hours: data.laborHours,
                        rate: data.hourlyRate,
                        total: data.laborHours * data.hourlyRate
                    }],
                    laborTotal: data.laborHours * data.hourlyRate
                };
            }

            return null;
        })
        .catch(error => {
            console.error("Error fetching labor charges:", error);
            return null;
        });
}
function openPaymentModal() {
    if (!currentService) return;

    // Close any open modals first
    closeAllModals();

    document.getElementById('paymentServiceId').textContent = `REQ-${currentService.requestId || currentService.serviceId}`;
    document.getElementById('paymentCustomerName').textContent = currentService.customerName;
    const total = currentService.calculatedTotal || 0;
    document.getElementById('paidAmount').value = total.toFixed(2);

    // Show the payment modal
    safelyOpenModal('paymentModal');
}

function processPayment() {
    if (!currentService) return;
    const paymentMethod = document.getElementById('paymentMethod').value;
    const transactionId = document.getElementById('transactionId').value;
    const paidAmount = document.getElementById('paidAmount').value;
    if (!paymentMethod) {
        showToast('Please select a payment method', 'error');
        return;
    }
    if (!paidAmount || paidAmount <= 0) {
        showToast('Please enter a valid amount', 'error');
        return;
    }
    const confirmBtn = document.getElementById('confirmPaymentBtn');
    confirmBtn.disabled = true;
    confirmBtn.innerHTML = '<div class="spinner-border spinner-border-sm me-2" role="status"></div> Processing...';

    // Close the modal
    closeAllModals();

    const paymentRequest = {
        serviceId: currentService.requestId || currentService.serviceId,
        paymentMethod: paymentMethod,
        transactionId: transactionId,
        amount: parseFloat(paidAmount),
        notes: "Payment processed by admin"
    };
    const token = getAuthToken();
    const paymentEndpoints = [
        `/admin/api/vehicle-tracking/process-payment`,
        `/admin/api/vehicle-tracking/service-request/${currentService.requestId || currentService.serviceId}/payment`,
        `/admin/api/completed-services/${currentService.requestId || currentService.serviceId}/payment`
    ];
    tryPostUrls(paymentEndpoints, paymentRequest, token)
        .then(data => {
            confirmBtn.disabled = false;
            confirmBtn.innerHTML = '<i class="fas fa-check-circle"></i> Confirm Payment';
            if (currentService) {
                currentService.isPaid = true;
                currentService.paid = true;
            }
            loadCompletedServices();

            // Show success message and then open delivery modal
            showSuccessMessage(
                'Payment Processed',
                'Payment has been processed successfully',
                () => openDeliveryModal()
            );
        })
        .catch(error => {
            confirmBtn.disabled = false;
            confirmBtn.innerHTML = '<i class="fas fa-check-circle"></i> Confirm Payment';
            showToast('Error processing payment: ' + error.message, 'error');
        });
}

function tryPostUrls(urls, requestData, token) {
    return urls.reduce((promise, url) => {
        return promise.catch(() => {
            return fetch(url, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(requestData)
            }).then(response => {
                if (!response.ok) {
                    throw new Error(`Failed to POST to ${url}: ${response.status}`);
                }
                return response.json();
            });
        });
    }, Promise.reject(new Error('Starting POST URL chain')));
}

function openDeliveryModal() {
    if (!currentService) return;

    // Close any open modals first
    closeAllModals();

    document.getElementById('deliveryServiceId').textContent = `REQ-${currentService.requestId || currentService.serviceId}`;
    document.getElementById('pickupPerson').value = '';
    document.getElementById('pickupTime').value = '';
    document.getElementById('deliveryAddress').value = '';
    document.getElementById('deliveryDate').value = '';
    document.getElementById('deliveryContact').value = '';
    document.getElementById('customerPickup').checked = true;
    document.getElementById('pickupFields').style.display = 'block';
    document.getElementById('deliveryFields').style.display = 'none';
    document.getElementById('confirmDeliveryBtnText').textContent = 'Confirm Pickup';

    // Pre-fetch customer address for later use
    if (currentService) {
        currentService.customerAddress = getCustomerAddress(currentService);
    }

    // Show the delivery modal
    safelyOpenModal('deliveryModal');
}

function processDelivery() {
    if (!currentService) return;
    const deliveryMethod = document.querySelector('input[name="deliveryMethod"]:checked').value;
    if (deliveryMethod === 'pickup') {
        const pickupPerson = document.getElementById('pickupPerson').value;
        const pickupTime = document.getElementById('pickupTime').value;
        if (!pickupPerson) {
            showToast('Please enter pickup person name', 'error');
            return;
        }
        if (!pickupTime) {
            showToast('Please select pickup time', 'error');
            return;
        }
    } else {
        const deliveryAddress = document.getElementById('deliveryAddress').value;
        const deliveryDate = document.getElementById('deliveryDate').value;
        const deliveryContact = document.getElementById('deliveryContact').value;
        if (!deliveryAddress) {
            showToast('Please enter delivery address', 'error');
            return;
        }
        if (!deliveryDate) {
            showToast('Please select delivery date', 'error');
            return;
        }
        if (!deliveryContact) {
            showToast('Please enter contact number', 'error');
            return;
        }
    }
    const confirmBtn = document.getElementById('confirmDeliveryBtn');
    confirmBtn.disabled = true;
    confirmBtn.innerHTML = '<div class="spinner-border spinner-border-sm me-2" role="status"></div> Processing...';

    // Close the modal
    closeAllModals();

    const deliveryRequest = {
        serviceId: currentService.requestId || currentService.serviceId,
        deliveryType: deliveryMethod,
        notes: "Processed by admin"
    };
    if (deliveryMethod === 'pickup') {
        deliveryRequest.pickupPerson = document.getElementById('pickupPerson').value;
        deliveryRequest.pickupTime = document.getElementById('pickupTime').value;
    } else {
        deliveryRequest.deliveryAddress = document.getElementById('deliveryAddress').value;
        deliveryRequest.deliveryDate = document.getElementById('deliveryDate').value;
        deliveryRequest.contactNumber = document.getElementById('deliveryContact').value;
    }
    const token = getAuthToken();
    const deliveryEndpoints = [
        `/admin/api/vehicle-tracking/service-request/${currentService.requestId || currentService.serviceId}/dispatch`,
        `/admin/api/completed-services/${currentService.requestId || currentService.serviceId}/dispatch`,
        `/admin/api/delivery/service-request/${currentService.requestId || currentService.serviceId}`
    ];
    tryPostUrls(deliveryEndpoints, deliveryRequest, token)
        .then(data => {
            confirmBtn.disabled = false;
            confirmBtn.innerHTML = `<i class="fas fa-check-circle"></i> ${deliveryMethod === 'pickup' ? 'Confirm Pickup' : 'Confirm Delivery'}`;
            if (currentService) {
                currentService.isDelivered = true;
                currentService.delivered = true;
            }
            loadCompletedServices();

            // Show success message
            showSuccessMessage(
                'Delivery Scheduled',
                deliveryMethod === 'pickup' ?
                    'Vehicle pickup has been scheduled successfully' :
                    'Vehicle delivery has been scheduled successfully'
            );
        })
        .catch(error => {
            confirmBtn.disabled = false;
            confirmBtn.innerHTML = `<i class="fas fa-check-circle"></i> ${deliveryMethod === 'pickup' ? 'Confirm Pickup' : 'Confirm Delivery'}`;
            showToast('Error processing delivery: ' + error.message, 'error');
        });
}

function showToast(message, type = 'success') {
    const toastContainer = document.querySelector('.toast-container');
    if (!toastContainer) return;
    const toastEl = document.createElement('div');
    toastEl.className = `toast align-items-center text-white bg-${type === 'error' ? 'danger' : (type === 'warning' ? 'warning' : 'success')} border-0`;
    toastEl.setAttribute('role', 'alert');
    toastEl.setAttribute('aria-live', 'assertive');
    toastEl.setAttribute('aria-atomic', 'true');
    toastEl.innerHTML = `
        <div class="d-flex">
            <div class="toast-body">
                ${message}
            </div>
            <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
        </div>
    `;
    toastContainer.appendChild(toastEl);
    const toast = new bootstrap.Toast(toastEl, {
        autohide: true,
        delay: 3000
    });
    toast.show();
    toastEl.addEventListener('hidden.bs.toast', function() {
        toastEl.remove();
    });
}

// Helper function to find email in nested objects
function findEmailInObject(obj) {
    if (!obj) return null;

    // Direct check
    if (obj.email) return obj.email;
    if (obj.customerEmail) return obj.customerEmail;

    // Check in customer object
    if (obj.customer && obj.customer.email) return obj.customer.email;

    // Check in user object
    if (obj.user && obj.user.email) return obj.user.email;

    // Check in enhancedCustomerData
    if (obj.enhancedCustomerData && obj.enhancedCustomerData.email) {
        return obj.enhancedCustomerData.email;
    }

    return null;
}