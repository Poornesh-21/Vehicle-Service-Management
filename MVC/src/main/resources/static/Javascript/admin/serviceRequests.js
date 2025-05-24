// Service Requests management module
// Global state variables
const state = {
    serviceRequests: [],
    customers: [],
    serviceAdvisors: [],
    vehicles: [],
    selectedVehicleData: null,
    pagination: {
        currentPage: 1,
        itemsPerPage: 5,
        totalPages: 1
    },
    loading: false
};

// Initialize on document load
document.addEventListener('DOMContentLoaded', () => {
    initApp();
});

function updateNavigationLinks() {
    // Find all navigation links in the sidebar
    const navLinks = document.querySelectorAll('#sidebar a, .nav-link, .sidebar-link');

    navLinks.forEach(link => {
        // Skip links that don't have an href
        if (!link.href) return;

        // Remove token parameter if present
        let url = new URL(link.href);
        if (url.searchParams.has('token')) {
            url.searchParams.delete('token');
            link.href = url.toString();
        }
    });

    // Also update view all buttons and other special links
    const buttons = {
        'viewAllDueBtn': '/admin/service-requests',
        'viewAllInServiceBtn': '/admin/under-service',
        'viewAllCompletedBtn': '/admin/completed-services'
    };

    Object.entries(buttons).forEach(([id, path]) => {
        const button = document.getElementById(id);
        if (button) {
            button.href = path;
        }
    });
}

/**
 * Application initialization
 */
function initApp() {
    setupMobileMenu();
    setupLogout();

    // Authenticate before loading any data
    if (!authenticate()) return;

    setupDateDisplay();
    setupEventListeners();
    addMembershipFilterStyles();

    // Load data
    loadServiceRequests();

    // Initialize modals
    initializeModals();

    // Define global functions for external access
    defineGlobalFunctions();
}

/**
 * Authentication check
 * @returns {boolean} Authentication status
 */
function authenticate() {
    const token = getAuthToken();
    if (!token) {
        window.location.href = '/admin/login?error=session_expired';
        return false;
    }
    return true;
}

/**
 * Get authentication token from storage
 * @returns {string|null} Authentication token
 */
function getAuthToken() {
    return localStorage.getItem('jwt-token') || sessionStorage.getItem('jwt-token');
}

/**
 * Setup mobile menu toggle
 */
function setupMobileMenu() {
    const mobileMenuToggle = document.getElementById('mobileMenuToggle');
    if (mobileMenuToggle) {
        mobileMenuToggle.addEventListener('click', () => {
            document.getElementById('sidebar').classList.toggle('active');
            const icon = mobileMenuToggle.querySelector('i');
            if (icon) {
                icon.classList.toggle('fa-bars');
                icon.classList.toggle('fa-times');
            }
        });
    }
}

/**
 * Setup logout functionality
 */
function setupLogout() {
    const logoutBtn = document.querySelector('.logout-btn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', () => {
            if (confirm('Are you sure you want to logout?')) {
                clearAuthData();
                window.location.href = '/admin/logout';
            }
        });
    }
}

document.addEventListener('DOMContentLoaded', () => {
    // Check if URL has query parameters
    if (window.location.search) {
        // Clean the URL by removing query parameters without reloading the page
        const cleanUrl = window.location.protocol + '//' +
            window.location.host +
            window.location.pathname;

        window.history.replaceState({}, document.title, cleanUrl);
    }
});

/**
 * Clear all authentication data
 */
function clearAuthData() {
    localStorage.removeItem("jwt-token");
    sessionStorage.removeItem("jwt-token");
    localStorage.removeItem("user-role");
    localStorage.removeItem("user-name");
    sessionStorage.removeItem("user-role");
    sessionStorage.removeItem("user-name");
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
 * Setup all event listeners
 */
function setupEventListeners() {
    // Search input
    setupSearchInput();

    // Pagination
    setupPagination();

    // Customer selection
    setupCustomerSelection();

    // Vehicle management
    setupVehicleManagement();

    // Form submissions
    setupFormSubmissions();
}

/**
 * Setup search functionality
 */
function setupSearchInput() {
    const searchInput = document.getElementById('searchInput');
    if (searchInput) {
        searchInput.addEventListener('keyup', () => {
            filterServiceRequests(searchInput.value);
        });
    }

    const tableSearchInput = document.querySelector('.search-input-sm');
    if (tableSearchInput) {
        tableSearchInput.addEventListener('keyup', () => {
            filterTableRows(tableSearchInput.value);
        });
    }
}

/**
 * Setup pagination controls
 */
function setupPagination() {
    document.querySelectorAll('#pagination .page-link').forEach(link => {
        if (link.id !== 'prevBtn' && link.id !== 'nextBtn') {
            link.addEventListener('click', function(e) {
                e.preventDefault();
                const page = parseInt(this.getAttribute('data-page'));
                changePage(page);
            });
        }
    });

    const prevBtn = document.getElementById('prevBtn');
    if (prevBtn) {
        prevBtn.addEventListener('click', function(e) {
            e.preventDefault();
            if (state.pagination.currentPage > 1) {
                changePage(state.pagination.currentPage - 1);
            }
        });
    }

    const nextBtn = document.getElementById('nextBtn');
    if (nextBtn) {
        nextBtn.addEventListener('click', function(e) {
            e.preventDefault();
            if (state.pagination.currentPage < state.pagination.totalPages) {
                changePage(state.pagination.currentPage + 1);
            }
        });
    }
}

/**
 * Setup customer selection handling
 */
function setupCustomerSelection() {
    const customerIdSelect = document.getElementById('customerId');
    if (customerIdSelect) {
        customerIdSelect.addEventListener('change', function() {
            const customerId = this.value;
            const addNewVehicleBtn = document.getElementById('addNewVehicleBtn');
            const newVehicleForm = document.getElementById('newVehicleForm');

            if (customerId) {
                loadVehiclesForCustomer(customerId);
                if (addNewVehicleBtn) {
                    addNewVehicleBtn.disabled = false;
                }
            } else {
                resetVehicleDropdown();
                if (addNewVehicleBtn) {
                    addNewVehicleBtn.disabled = true;
                }
                if (newVehicleForm) {
                    newVehicleForm.style.display = 'none';
                }
            }
        });
    }
}

/**
 * Setup vehicle management controls
 */
function setupVehicleManagement() {
    // Add new vehicle button
    const addNewVehicleBtn = document.getElementById('addNewVehicleBtn');
    if (addNewVehicleBtn) {
        addNewVehicleBtn.addEventListener('click', function() {
            const newVehicleForm = document.getElementById('newVehicleForm');
            if (newVehicleForm) {
                newVehicleForm.style.display = 'block';
                const vehicleDropdown = document.getElementById('vehicleId');
                if (vehicleDropdown) vehicleDropdown.disabled = true;
            }
        });
    }

    // Cancel new vehicle button
    const cancelNewVehicleBtn = document.getElementById('cancelNewVehicleBtn');
    if (cancelNewVehicleBtn) {
        cancelNewVehicleBtn.addEventListener('click', function() {
            const newVehicleForm = document.getElementById('newVehicleForm');
            if (newVehicleForm) {
                newVehicleForm.style.display = 'none';
                const vehicleDropdown = document.getElementById('vehicleId');
                if (vehicleDropdown) vehicleDropdown.disabled = false;
            }
        });
    }

    // Vehicle dropdown change
    const vehicleDropdown = document.getElementById('vehicleId');
    if (vehicleDropdown) {
        vehicleDropdown.addEventListener('change', function() {
            const vehicleId = this.value;
            if (vehicleId) {
                fetchVehicleDetails(vehicleId);
            } else {
                state.selectedVehicleData = null;
            }
        });
    }
}

/**
 * Setup form submission handlers
 */
function setupFormSubmissions() {
    // Save new vehicle
    const saveNewVehicleBtn = document.getElementById('saveNewVehicleBtn');
    if (saveNewVehicleBtn) {
        saveNewVehicleBtn.addEventListener('click', function() {
            saveNewVehicle();
        });
    }

    // Save service request
    const saveServiceRequestBtn = document.getElementById('saveServiceRequestBtn');
    if (saveServiceRequestBtn) {
        saveServiceRequestBtn.addEventListener('click', function() {
            saveServiceRequest();
        });
    }

    // Confirm assign
    const confirmAssignBtn = document.getElementById('confirmAssignBtn');
    if (confirmAssignBtn) {
        confirmAssignBtn.addEventListener('click', function() {
            assignServiceAdvisor();
        });
    }
}

/**
 * Initialize modals event handlers
 */
function initializeModals() {
    const addServiceRequestModal = document.getElementById('addServiceRequestModal');
    if (addServiceRequestModal) {
        addServiceRequestModal.addEventListener('show.bs.modal', function() {
            loadCustomersDirectly();
        });
    }
}

/**
 * Define global functions for external access
 */
function defineGlobalFunctions() {
    // Load service advisors
    window.loadServiceAdvisors = function() {
        const token = getAuthToken();
        if (!token) {
            return Promise.resolve([]);
        }

        return makeAuthenticatedRequest('/admin/service-advisors/api/advisors')
            .then(response => {
                if (!response.ok) {
                    throw new Error(`HTTP error! Status: ${response.status}`);
                }
                return response.json();
            })
            .then(data => {
                state.serviceAdvisors = data || [];
                return state.serviceAdvisors;
            })
            .catch(error => {
                showToast("Failed to load service advisors: " + error.message, "error");
                return [];
            });
    };

    // Show service request details
    window.showServiceRequestDetails = function(requestId) {
        const request = state.serviceRequests.find(req => req.requestId === requestId);

        if (!request) {
            fetchServiceRequest(requestId);
            return;
        }

        displayServiceRequestDetails(request);
    };

    // Show assign advisor modal
    window.showAssignAdvisorModal = function(requestId) {
        const request = state.serviceRequests.find(req => req.requestId === requestId);

        if (!request) {
            showToast("Service request not found", "error");
            return;
        }

        document.getElementById('assignVehicleName').textContent = request.vehicleName || `${request.vehicleBrand} ${request.vehicleModel}`;
        document.getElementById('assignVehicleReg').textContent = request.registrationNumber || 'N/A';
        document.getElementById('assignCustomerName').textContent = request.customerName || 'Customer';
        document.getElementById('assignRequestId').textContent = `REQ-${request.requestId}`;
        document.getElementById('selectedRequestId').value = request.requestId;

        const advisorsList = document.getElementById('advisorsList');
        if (advisorsList) {
            advisorsList.innerHTML = `
                <div class="text-center py-4">
                    <div class="spinner-border text-wine" role="status"></div>
                    <p class="mt-2">Loading service advisors...</p>
                </div>
            `;
        }

        loadServiceAdvisors()
            .then(advisors => {
                populateServiceAdvisorsList(advisors);
                const modal = new bootstrap.Modal(document.getElementById('assignAdvisorModal'));
                modal.show();
            })
            .catch(error => {
                showToast("Failed to load service advisors", "error");
            });
    };

    // Populate service advisors list
    window.populateServiceAdvisorsList = function(advisors) {
        const advisorsList = document.getElementById('advisorsList');
        if (!advisorsList) return;

        if (!advisors || advisors.length === 0) {
            advisorsList.innerHTML = `
                <div class="alert alert-info">
                    <i class="fas fa-info-circle me-2"></i>
                    No service advisors available. Please add service advisors first.
                </div>
            `;
            return;
        }

        let html = '';

        advisors.forEach(advisor => {
            const workloadPercentage = advisor.workloadPercentage ||
                (advisor.activeServices ? Math.min(Math.round((advisor.activeServices / 10) * 100), 100) : 0);

            let workloadClass = 'bg-success';
            if (workloadPercentage > 75) {
                workloadClass = 'bg-danger';
            } else if (workloadPercentage > 50) {
                workloadClass = 'bg-warning';
            }

            const name = advisor.firstName && advisor.lastName ?
                `${advisor.firstName} ${advisor.lastName}` : advisor.name || 'Unnamed Advisor';

            const initials = (advisor.firstName ? advisor.firstName.charAt(0) : '') +
                (advisor.lastName ? advisor.lastName.charAt(0) : '');

            html += `
                <div class="advisor-card" data-advisor-id="${advisor.advisorId || advisor.id}">
                    <div class="advisor-avatar">
                        ${initials || 'SA'}
                    </div>
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
                document.querySelectorAll('.advisor-card').forEach(c => c.classList.remove('selected'));
                this.classList.add('selected');
            });
        });
    };
}

/**
 * Add membership filter styles
 */
function addMembershipFilterStyles() {
    if (!document.getElementById('membership-filter-styles')) {
        const style = document.createElement('style');
        style.id = 'membership-filter-styles';
        style.textContent = `
            .membership-filters {
                display: flex;
                align-items: center;
            }

            .membership-filters .btn-group {
                box-shadow: var(--shadow-sm);
                border-radius: var(--radius-lg);
                overflow: hidden;
            }

            .membership-filters .btn {
                padding: 0.4rem 0.75rem;
                font-size: 0.85rem;
                font-weight: 500;
                border-radius: 0;
            }

            .membership-filters .btn.active {
                background-color: var(--wine);
                color: white;
                border-color: var(--wine);
            }
        `;
        document.head.appendChild(style);
    }
}

/**
 * Load service requests from API
 */
function loadServiceRequests() {
    const tableBody = document.getElementById('serviceRequestsTableBody');
    if (!tableBody) return;

    const loadingRow = document.getElementById('loading-row');
    const emptyRow = document.getElementById('empty-row');

    if (loadingRow) loadingRow.style.display = 'table-row';
    if (emptyRow) emptyRow.style.display = 'none';

    state.loading = true;

    makeAuthenticatedRequest('/admin/service-requests/api')
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! Status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            if (loadingRow) loadingRow.style.display = 'none';
            state.loading = false;

            const allRequests = data || [];
            state.serviceRequests = allRequests.filter(request => !request.serviceAdvisorId);

            if (state.serviceRequests.length === 0) {
                if (emptyRow) {
                    emptyRow.style.display = 'table-row';
                    const message = emptyRow.querySelector('p');
                    if (message) {
                        message.textContent = 'No unassigned service requests found';
                    }
                }
            } else {
                renderServiceRequests();
                updatePagination();
            }
        })
        .catch(error => {
            if (loadingRow) loadingRow.style.display = 'none';
            state.loading = false;

            if (emptyRow) {
                emptyRow.style.display = 'table-row';
                const errorMessage = emptyRow.querySelector('p');
                if (errorMessage) {
                    errorMessage.textContent = 'Error loading service requests. Please try again.';
                }
            }
            console.error('Error loading service requests:', error);
        });
}

/**
 * Load customers for dropdown
 */
function loadCustomersDirectly() {
    const dropdown = document.getElementById('customerId');
    if (!dropdown) return;

    dropdown.innerHTML = '<option value="">Loading customers...</option>';
    dropdown.disabled = true;

    makeAuthenticatedRequest('/admin/customers/api')
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! Status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            dropdown.innerHTML = '<option value="">Select Customer</option>';

            if (!data || !Array.isArray(data) || data.length === 0) {
                dropdown.innerHTML += '<option value="" disabled>No customers available</option>';
                return;
            }

            state.customers = data;

            data.forEach(customer => {
                try {
                    const customerId = customer.customerId || customer.userId || customer.id;
                    const firstName = customer.firstName || '';
                    const lastName = customer.lastName || '';
                    const membershipStatus = customer.membershipStatus || 'Standard';

                    if (!customerId) return;

                    const option = document.createElement('option');
                    option.value = customerId;
                    option.textContent = `${firstName} ${lastName} (${membershipStatus})`;
                    dropdown.appendChild(option);
                } catch (e) {
                    console.error('Error processing customer data', e);
                }
            });

            dropdown.disabled = false;
        })
        .catch(error => {
            dropdown.innerHTML = '<option value="">Error loading customers</option>';
            dropdown.disabled = true;
            showToast("Failed to load customers: " + error.message, "error");
        });
}

/**
 * Load vehicles for selected customer
 * @param {string} customerId Customer ID
 */
function loadVehiclesForCustomer(customerId) {
    const vehicleDropdown = document.getElementById('vehicleId');
    if (!vehicleDropdown) return;

    vehicleDropdown.disabled = true;
    vehicleDropdown.innerHTML = '<option value="">Loading vehicles...</option>';

    makeAuthenticatedRequest(`/admin/api/customers/${customerId}/vehicles`)
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! Status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            state.vehicles = data || [];

            vehicleDropdown.innerHTML = '<option value="">Select Vehicle</option>';

            if (state.vehicles.length === 0) {
                vehicleDropdown.innerHTML += '<option value="" disabled>No vehicles found for this customer</option>';
                vehicleDropdown.disabled = true;
                return;
            }

            state.vehicles.forEach(vehicle => {
                const option = document.createElement('option');
                option.value = vehicle.vehicleId;
                option.textContent = `${vehicle.brand} ${vehicle.model} (${vehicle.registrationNumber})`;
                vehicleDropdown.appendChild(option);
            });

            vehicleDropdown.disabled = false;
        })
        .catch(error => {
            resetVehicleDropdown('Error loading vehicles. Please try again.');
            console.error('Error loading vehicles for customer:', error);
        });
}

/**
 * Reset vehicle dropdown
 * @param {string} message Message to display
 */
function resetVehicleDropdown(message = 'Select Customer First') {
    const dropdown = document.getElementById('vehicleId');
    if (dropdown) {
        dropdown.innerHTML = `<option value="">${message}</option>`;
        dropdown.disabled = true;
    }
}

/**
 * Save new vehicle
 * @param {Function} callback Optional callback after successful save
 */
function saveNewVehicle(callback) {
    const customerId = document.getElementById('customerId').value;

    if (!customerId) {
        showToast('Please select a customer first', 'error');
        return;
    }

    const brand = document.getElementById('vehicleBrand').value;
    const model = document.getElementById('vehicleModel').value;
    const regNumber = document.getElementById('vehicleRegNumber').value;
    const category = document.getElementById('vehicleCategory').value;
    const year = document.getElementById('vehicleYear').value;

    if (!brand || !model || !regNumber || !category || !year) {
        showToast('Please fill all required vehicle fields', 'error');
        return;
    }

    const vehicleData = {
        brand: brand,
        model: model,
        registrationNumber: regNumber,
        category: category,
        year: parseInt(year)
    };

    showSpinner();

    makeAuthenticatedRequest(`/admin/api/customers/${customerId}/vehicles`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(vehicleData)
    })
        .then(response => {
            if (!response.ok) {
                return response.json().then(errorData => {
                    throw new Error(errorData.error || 'Failed to create vehicle');
                });
            }
            return response.json();
        })
        .then(data => {
            document.getElementById('newVehicleForm').style.display = 'none';

            state.vehicles.push(data);

            const dropdown = document.getElementById('vehicleId');
            if (dropdown) {
                const option = document.createElement('option');
                option.value = data.vehicleId;
                option.textContent = `${data.brand} ${data.model} (${data.registrationNumber})`;
                dropdown.appendChild(option);
                dropdown.value = data.vehicleId;
                dropdown.disabled = false;
            }

            if (callback && typeof callback === 'function') {
                callback(data.vehicleId);
            } else {
                hideSpinner();
                showToast('Vehicle added successfully', 'success');
            }
        })
        .catch(error => {
            hideSpinner();
            showToast('Failed to create vehicle: ' + error.message, 'error');
        });
}

/**
 * Fetch vehicle details
 * @param {string} vehicleId Vehicle ID
 */
function fetchVehicleDetails(vehicleId) {
    showSpinner();

    makeAuthenticatedRequest(`/admin/api/vehicles/${vehicleId}`)
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! Status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            hideSpinner();
            // Store the vehicle data for later use when submitting the form
            state.selectedVehicleData = data;
            console.log('Vehicle details loaded:', state.selectedVehicleData);
        })
        .catch(error => {
            hideSpinner();
            showToast('Failed to fetch vehicle details: ' + error.message, 'error');
        });
}

/**
 * Create service request with vehicle data
 * @param {string} vehicleId Vehicle ID
 */
function createEnhancedServiceRequest(vehicleId) {
    if (!state.selectedVehicleData) {
        fetchVehicleDetails(vehicleId);
        showToast('Preparing vehicle data, please try again in a moment', 'info');
        return;
    }

    // Create service request with complete vehicle information
    const serviceRequest = {
        vehicleId: vehicleId,
        vehicleBrand: state.selectedVehicleData.brand || "Unknown Brand",
        vehicleModel: state.selectedVehicleData.model || "Unknown Model",
        vehicleRegistration: state.selectedVehicleData.registrationNumber || "Unknown", // Important: use vehicleRegistration not registrationNumber
        vehicleType: state.selectedVehicleData.category || "Car",
        serviceType: document.getElementById('serviceType').value,
        deliveryDate: document.getElementById('deliveryDate').value,
        additionalDescription: document.getElementById('description').value || "",

    };

    // Log the request payload for debugging
    console.log("Service request payload:", JSON.stringify(serviceRequest, null, 2));

    showSpinner();

    makeAuthenticatedRequest('/admin/service-requests/api', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(serviceRequest)
    })
        .then(response => {
            if (!response.ok) {
                return response.text().then(text => {
                    try {
                        const errorData = JSON.parse(text);
                        throw new Error(errorData.error || 'Failed to create service request');
                    } catch (e) {
                        throw new Error('Failed to create service request: ' + text);
                    }
                });
            }
            return response.json();
        })
        .then(data => {
            hideSpinner();

            const modal = bootstrap.Modal.getInstance(document.getElementById('addServiceRequestModal'));
            if (modal) {
                modal.hide();
            }

            document.getElementById('addServiceRequestForm').reset();
            document.getElementById('newVehicleForm').style.display = 'none';
            resetVehicleDropdown();
            state.selectedVehicleData = null;

            showSuccessModal('Service Request Created', 'The service request has been created successfully.');

            if (data) {
                state.serviceRequests.unshift(data);
                renderServiceRequests();
                updatePagination();
            } else {
                loadServiceRequests();
            }
        })
        .catch(error => {
            hideSpinner();
            showToast('Failed to create service request: ' + error.message, 'error');
        });
}

/**
 * Create service request with newly created vehicle
 * @param {string} vehicleId Vehicle ID
 */
function createServiceRequestWithVehicle(vehicleId) {
    showSpinner();

    makeAuthenticatedRequest(`/admin/api/vehicles/${vehicleId}`)
        .then(response => {
            if (!response.ok) {
                throw new Error(`Failed to get vehicle details: ${response.status}`);
            }
            return response.json();
        })
        .then(vehicleData => {
            // Store the vehicle data
            state.selectedVehicleData = vehicleData;

            // Now create the service request with complete vehicle data
            const serviceRequest = {
                vehicleId: vehicleId,
                vehicleBrand: vehicleData.brand || "Unknown Brand",
                vehicleModel: vehicleData.model || "Unknown Model",
                vehicleRegistration: vehicleData.registrationNumber || "Unknown", // FIXED: use vehicleRegistration not registrationNumber
                vehicleType: vehicleData.category || "Car",
                serviceType: document.getElementById('serviceType').value,
                deliveryDate: document.getElementById('deliveryDate').value,
                additionalDescription: document.getElementById('description').value || "",

            };

            // Log the request payload for debugging
            console.log("Service request payload:", JSON.stringify(serviceRequest, null, 2));

            return makeAuthenticatedRequest('/admin/service-requests/api', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(serviceRequest)
            });
        })
        .then(response => {
            if (!response.ok) {
                return response.text().then(text => {
                    try {
                        const errorData = JSON.parse(text);
                        throw new Error(errorData.error || 'Failed to create service request');
                    } catch (e) {
                        throw new Error('Failed to create service request: ' + text);
                    }
                });
            }
            return response.json();
        })
        .then(data => {
            hideSpinner();

            const modal = bootstrap.Modal.getInstance(document.getElementById('addServiceRequestModal'));
            if (modal) {
                modal.hide();
            }

            document.getElementById('addServiceRequestForm').reset();
            document.getElementById('newVehicleForm').style.display = 'none';
            resetVehicleDropdown();
            state.selectedVehicleData = null;

            showSuccessModal('Service Request Created', 'The service request has been created successfully.');

            if (data) {
                state.serviceRequests.unshift(data);
                renderServiceRequests();
                updatePagination();
            } else {
                loadServiceRequests();
            }
        })
        .catch(error => {
            hideSpinner();
            showToast('Failed to create service request: ' + error.message, 'error');
        });
}

/**
 * Save service request
 */
function saveServiceRequest() {
    const form = document.getElementById('addServiceRequestForm');
    if (!form) return;

    const newVehicleForm = document.getElementById('newVehicleForm');
    const isAddingNewVehicle = newVehicleForm && newVehicleForm.style.display !== 'none';
    let formValid = true;

    const customerId = document.getElementById('customerId').value;
    const serviceType = document.getElementById('serviceType').value;
    const deliveryDate = document.getElementById('deliveryDate').value;

    if (!customerId) {
        showToast('Please select a customer', 'error');
        formValid = false;
        return;
    }

    if (!serviceType) {
        showToast('Please select a service type', 'error');
        formValid = false;
        return;
    }

    if (!deliveryDate) {
        showToast('Please select a delivery date', 'error');
        formValid = false;
        return;
    }

    if (isAddingNewVehicle) {
        const brand = document.getElementById('vehicleBrand').value;
        const model = document.getElementById('vehicleModel').value;
        const regNumber = document.getElementById('vehicleRegNumber').value;
        const category = document.getElementById('vehicleCategory').value;
        const year = document.getElementById('vehicleYear').value;

        if (!brand || !model || !regNumber || !category || !year) {
            showToast('Please fill all required vehicle information', 'error');
            formValid = false;
            return;
        }

        if (formValid) {
            saveNewVehicle(function(newVehicleId) {
                createServiceRequestWithVehicle(newVehicleId);
            });
            return;
        }
    } else {
        const vehicleId = document.getElementById('vehicleId').value;
        if (!vehicleId) {
            showToast('Please select a vehicle', 'error');
            formValid = false;
            return;
        }

        if (formValid) {
            createEnhancedServiceRequest(vehicleId);
        }
    }
}

/**
 * Fetch service request details
 * @param {string} requestId Request ID
 */
function fetchServiceRequest(requestId) {
    showSpinner();

    makeAuthenticatedRequest(`/admin/service-requests/api/${requestId}`)
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! Status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            hideSpinner();
            displayServiceRequestDetails(data);
        })
        .catch(error => {
            hideSpinner();
            showToast('Failed to load service request details: ' + error.message, 'error');
        });
}

/**
 * Display service request details in modal
 * @param {Object} request Service request object
 */
function displayServiceRequestDetails(request) {
    document.getElementById('viewRequestId').textContent = `REQ-${request.requestId}`;


    document.getElementById('viewCreatedDate').textContent = formatDate(request.createdAt || new Date().toISOString());

    const vehicleName = request.vehicleName ||
        (request.vehicleBrand && request.vehicleModel ?
            `${request.vehicleBrand} ${request.vehicleModel}` :
            (request.vehicleModel || 'Unknown Vehicle'));
    document.getElementById('viewVehicleName').textContent = vehicleName;

    const regNumber = request.registrationNumber || request.vehicleRegistration || 'No Registration';
    document.getElementById('viewRegistrationNumber').textContent = regNumber;

    document.getElementById('viewVehicleCategory').textContent = request.vehicleCategory || request.vehicleType || 'Car';
    document.getElementById('viewCustomerName').textContent = request.customerName || 'N/A';

    const contactInfo = request.customerPhone ||
        request.phoneNumber ||
        request.customerContact ||
        request.customerEmail ||
        'N/A';
    document.getElementById('viewCustomerContact').textContent = contactInfo;

    const membershipElement = document.getElementById('viewMembership');
    const membershipStatus = request.membershipStatus || 'Standard';
    membershipElement.innerHTML = `
        <span class="membership-badge membership-${membershipStatus.toLowerCase()}">
            <i class="fas fa-${membershipStatus === 'Premium' ? 'crown' : 'user'}"></i>
            ${membershipStatus}
        </span>
    `;

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

    const modal = new bootstrap.Modal(document.getElementById('viewServiceRequestModal'));
    modal.show();
}

/**
 * Populate materials table in service request details modal
 * @param {Array} materials Materials list
 */
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
            <td>${escapeHTML(material.name)}</td>
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

/**
 * Assign service advisor to request
 */
function assignServiceAdvisor() {
    const requestId = document.getElementById('selectedRequestId').value;
    const selectedAdvisor = document.querySelector('.advisor-card.selected');

    if (!selectedAdvisor) {
        showToast('Please select a service advisor', 'error');
        return;
    }

    const advisorId = selectedAdvisor.getAttribute('data-advisor-id');
    if (!advisorId) {
        showToast('Invalid advisor selection', 'error');
        return;
    }

    showSpinner();

    makeAuthenticatedRequest(`/admin/service-requests/api/${requestId}/assign`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ advisorId: parseInt(advisorId) })
    })
        .then(response => {
            if (!response.ok) {
                return response.text().then(text => {
                    try {
                        const errorData = JSON.parse(text);
                        throw new Error(errorData.error || 'Failed to assign service advisor');
                    } catch (e) {
                        throw new Error('Failed to assign service advisor: ' + text);
                    }
                });
            }
            return response.json();
        })
        .then(data => {
            hideSpinner();

            const modal = bootstrap.Modal.getInstance(document.getElementById('assignAdvisorModal'));
            if (modal) {
                modal.hide();
            }

            showSuccessModal('Service Advisor Assigned', 'The service advisor has been assigned successfully.');

            const index = state.serviceRequests.findIndex(req => req.requestId === parseInt(requestId));
            if (index !== -1) {
                state.serviceRequests.splice(index, 1);
                renderServiceRequests();
                updatePagination();
            } else {
                loadServiceRequests();
            }
        })
        .catch(error => {
            hideSpinner();
            showToast('Failed to assign service advisor: ' + error.message, 'error');
        });
}

/**
 * Render service requests list
 */
function renderServiceRequests() {
    const tableBody = document.getElementById('serviceRequestsTableBody');
    if (!tableBody) return;

    const rows = tableBody.querySelectorAll('tr:not(#loading-row):not(#empty-row)');
    rows.forEach(row => row.remove());

    if (!state.serviceRequests || state.serviceRequests.length === 0) {
        const emptyRow = document.getElementById('empty-row');
        if (emptyRow) {
            emptyRow.style.display = 'table-row';
        }
        return;
    }

    const emptyRow = document.getElementById('empty-row');
    if (emptyRow) {
        emptyRow.style.display = 'none';
    }

    const startIndex = (state.pagination.currentPage - 1) * state.pagination.itemsPerPage;
    const endIndex = Math.min(startIndex + state.pagination.itemsPerPage, state.serviceRequests.length);
    const displayedRequests = state.serviceRequests.slice(startIndex, endIndex);

    displayedRequests.forEach(request => {
        const status = request.status || "Unknown";
        const membershipStatus = request.membershipStatus || 'Standard';

        const vehicleName = request.vehicleName ||
            (request.vehicleBrand && request.vehicleModel ?
                `${request.vehicleBrand} ${request.vehicleModel}` :
                (request.vehicleModel || 'Unknown Vehicle'));

        const regNumber = request.registrationNumber ||
            request.vehicleRegistration ||
            'No Registration';

        const row = document.createElement('tr');
        row.className = 'active-page';
        row.innerHTML = `
            <td>REQ-${request.requestId || request.serviceId}</td>
            <td>
                <div class="vehicle-info">
                    <div class="vehicle-icon">
                        <i class="fas fa-${request.vehicleCategory === 'Bike' ? 'motorcycle' : 'car-side'}"></i>
                    </div>
                    <div class="vehicle-details">
                        <h5>${escapeHTML(vehicleName)}</h5>
                        <p>${escapeHTML(regNumber)}</p>
                    </div>
                </div>
            </td>
            <td>
                <div class="person-info">
                    <div class="person-details">
                        <h5>${escapeHTML(request.customerName || 'N/A')}</h5>
                        <p>${escapeHTML(request.customerEmail || '')}</p>
                    </div>
                </div>
            </td>
            <td>
                <span class="membership-badge membership-${membershipStatus.toLowerCase()}">
                    <i class="fas fa-${membershipStatus.toLowerCase() === 'premium' ? 'crown' : 'user'}"></i>
                    ${escapeHTML(membershipStatus)}
                </span>
            </td>
            <td>${escapeHTML(request.serviceType || 'N/A')}</td>
            
            <td>${formatDate(request.deliveryDate)}</td>
            <td>${escapeHTML(request.serviceAdvisorName || 'Not Assigned')}</td>
            <td class="table-actions-cell">
                <button class="btn-table-action view-btn" title="View Details" onclick="showServiceRequestDetails(${request.requestId})">
                    <i class="fas fa-eye"></i>
                </button>
                ${!request.serviceAdvisorId ?
            `<button class="btn-assign" title="Assign Advisor" onclick="showAssignAdvisorModal(${request.requestId})">
                        <i class="fas fa-user-plus"></i> Assign
                    </button>` :
            `<button class="btn-table-action edit-btn" title="Edit Request">
                        <i class="fas fa-edit"></i>
                    </button>`
        }
            </td>
        `;

        tableBody.appendChild(row);
    });
}

/**
 * Filter service requests by search term
 * @param {string} searchTerm Search term
 */
function filterServiceRequests(searchTerm) {
    if (!searchTerm) {
        renderServiceRequests();
        return;
    }

    searchTerm = searchTerm.toLowerCase();

    const filteredRequests = state.serviceRequests.filter(request => {
        return (
            `req-${request.requestId}`.toLowerCase().includes(searchTerm) ||
            (request.customerName && request.customerName.toLowerCase().includes(searchTerm)) ||
            (request.registrationNumber && request.registrationNumber.toLowerCase().includes(searchTerm)) ||
            (request.vehicleName && request.vehicleName.toLowerCase().includes(searchTerm)) ||
            (request.vehicleBrand && request.vehicleBrand.toLowerCase().includes(searchTerm)) ||
            (request.vehicleModel && request.vehicleModel.toLowerCase().includes(searchTerm)) ||
            (request.serviceType && request.serviceType.toLowerCase().includes(searchTerm)) ||
            (request.status && request.status.toLowerCase().includes(searchTerm))
        );
    });

    state.pagination.currentPage = 1;
    renderServiceRequests(filteredRequests);
    updatePagination();
}

/**
 * Filter table rows by search term
 * @param {string} searchTerm Search term
 */
function filterTableRows(searchTerm) {
    if (!searchTerm) {
        document.querySelectorAll('#serviceRequestsTable tbody tr').forEach(row => {
            row.style.display = '';
        });
        return;
    }

    searchTerm = searchTerm.toLowerCase();

    document.querySelectorAll('#serviceRequestsTable tbody tr').forEach(row => {
        const text = row.textContent.toLowerCase();
        if (text.includes(searchTerm)) {
            row.style.display = '';
        } else {
            row.style.display = 'none';
        }
    });
}

/**
 * Update pagination UI
 */
function updatePagination() {
    state.pagination.totalPages = Math.ceil(state.serviceRequests.length / state.pagination.itemsPerPage);
    const pagination = document.getElementById('pagination');
    if (!pagination) return;

    const pageLinks = pagination.querySelectorAll('.page-link:not(#prevBtn):not(#nextBtn)');
    pageLinks.forEach(link => link.parentElement.remove());

    const prevBtn = document.getElementById('prevBtn');
    const prevBtnParent = prevBtn ? prevBtn.parentElement : null;
    const nextBtn = document.getElementById('nextBtn');
    const nextBtnParent = nextBtn ? nextBtn.parentElement : null;

    if (prevBtnParent && nextBtnParent) {
        for (let i = 1; i <= state.pagination.totalPages; i++) {
            const li = document.createElement('li');
            li.className = `page-item ${i === state.pagination.currentPage ? 'active' : ''}`;
            li.innerHTML = `<a class="page-link" href="#" data-page="${i}">${i}</a>`;

            li.querySelector('.page-link').addEventListener('click', function(e) {
                e.preventDefault();
                changePage(i);
            });

            pagination.insertBefore(li, nextBtnParent);
        }

        prevBtnParent.classList.toggle('disabled', state.pagination.currentPage === 1);
        nextBtnParent.classList.toggle('disabled', state.pagination.currentPage === state.pagination.totalPages || state.pagination.totalPages === 0);
    }
}

/**
 * Change current page
 * @param {number} page Page number
 */
function changePage(page) {
    state.pagination.currentPage = page;
    renderServiceRequests();
    updatePaginationUI();
}

/**
 * Update pagination UI after page change
 */
function updatePaginationUI() {
    const pagination = document.getElementById('pagination');
    if (!pagination) return;

    const pageItems = pagination.querySelectorAll('.page-item');
    pageItems.forEach(item => {
        const pageLink = item.querySelector('.page-link');
        if (pageLink && pageLink.id !== 'prevBtn' && pageLink.id !== 'nextBtn') {
            const page = parseInt(pageLink.getAttribute('data-page'));
            item.classList.toggle('active', page === state.pagination.currentPage);
        }
    });

    const prevBtn = document.getElementById('prevBtn');
    const nextBtn = document.getElementById('nextBtn');

    if (prevBtn) {
        prevBtn.parentElement.classList.toggle('disabled', state.pagination.currentPage === 1);
    }

    if (nextBtn) {
        nextBtn.parentElement.classList.toggle('disabled', state.pagination.currentPage === state.pagination.totalPages || state.pagination.totalPages === 0);
    }
}

/**
 * Format date for display
 * @param {string} dateString Date string
 * @returns {string} Formatted date
 */
function formatDate(dateString) {
    if (!dateString) return 'N/A';

    const options = { year: 'numeric', month: 'long', day: 'numeric' };
    try {
        return new Date(dateString).toLocaleDateString('en-US', options);
    } catch (e) {
        return dateString || 'N/A';
    }
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
}

/**
 * Show toast notification
 * @param {string} message Message to display
 * @param {string} type Type of toast (success, error, info)
 */
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
                <strong class="me-auto">${type === 'success' ? 'Success' : type === 'error' ? 'Error' : 'Information'}</strong>
                <button type="button" class="btn-close" data-bs-dismiss="toast" aria-label="Close"></button>
            </div>
            <div class="toast-body">
                ${escapeHTML(message)}
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

/**
 * Show success modal
 * @param {string} title Modal title
 * @param {string} message Modal message
 */
function showSuccessModal(title, message) {
    document.getElementById('successTitle').textContent = title;
    document.getElementById('successMessage').textContent = message;

    const modal = new bootstrap.Modal(document.getElementById('successModal'));
    modal.show();
}

/**
 * Make authenticated request to API
 * @param {string} url Request URL
 * @param {Object} options Request options
 * @returns {Promise} Fetch promise
 */
function makeAuthenticatedRequest(url, options = {}) {
    const token = getAuthToken();
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

/**
 * Escape HTML special characters
 * @param {string} str String to escape
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
document.addEventListener('DOMContentLoaded', updateNavigationLinks);