/**
 * Customer Management JavaScript
 * Handles all functionality for the customer management page
 */
document.addEventListener('DOMContentLoaded', function() {
    // ===== Core Elements =====
    const sidebar = document.getElementById('sidebar');
    const mobileMenuToggle = document.getElementById('mobileMenuToggle');
    const spinnerOverlay = document.getElementById('spinnerOverlay');
    const customerSearch = document.getElementById('customerSearch');
    const filterPills = document.querySelectorAll('.filter-pill');
    const addCustomerBtn = document.getElementById('addCustomerBtn');
    const saveCustomerBtn = document.getElementById('saveCustomerBtn');
    const editCustomerFromDetailsBtn = document.getElementById('editCustomerFromDetailsBtn');
    const updateCustomerBtn = document.getElementById('updateCustomerBtn');
    const profileTabs = document.querySelectorAll('.profile-tab');
    const paginationLinks = document.querySelectorAll('.pagination .page-link');
    const CUSTOMERS_PER_PAGE = 10;

    // ===== API Endpoints =====
    const API_BASE = '/admin/customers/api';
    const SERVICE_API = '/admin/service-requests/api';

    // ===== State Management =====
    let allCustomers = [];
    let currentPage = 1;
    let totalPages = 1;
    let selectedCustomerId = null;
    let serviceHistory = [];

    // ===== Authentication =====
    // Get auth token from localStorage or sessionStorage
    function getToken() {
        return localStorage.getItem("jwt-token") || sessionStorage.getItem("jwt-token");
    }

    // Check token and redirect if not authenticated
    function checkAuth() {
        const token = getToken();
        if (!token) {
            window.location.href = '/admin/login?error=session_expired';
            return false;
        }
        return true;
    }

    // Get Authorization headers for API requests
    function getAuthHeaders() {
        return {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + getToken()
        };
    }

    // ===== UI Helpers =====
    // Show loading spinner
    function showSpinner() {
        spinnerOverlay.classList.add('show');
    }

    // Hide loading spinner
    function hideSpinner() {
        setTimeout(() => spinnerOverlay.classList.remove('show'), 300);
    }

    // Show toast notification
    function showToast(title, message, type = 'info') {
        let toastContainer = document.getElementById('toast-container');
        if (!toastContainer) {
            toastContainer = document.createElement('div');
            toastContainer.id = 'toast-container';
            toastContainer.className = 'position-fixed top-0 end-0 p-3';
            toastContainer.style.zIndex = '1050';
            document.body.appendChild(toastContainer);
        }

        const toastId = 'toast-' + Date.now();
        const toast = document.createElement('div');
        toast.id = toastId;
        toast.className = `toast align-items-center ${type === 'error' ? 'bg-danger' : type === 'success' ? 'bg-success' : 'bg-info'} text-white border-0`;
        toast.setAttribute('role', 'alert');
        toast.setAttribute('aria-live', 'assertive');
        toast.setAttribute('aria-atomic', 'true');

        toast.innerHTML = `
        <div class="d-flex">
          <div class="toast-body">
            <strong>${title}</strong>: ${message}
          </div>
          <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
        </div>`;

        toastContainer.appendChild(toast);

        const bsToast = new bootstrap.Toast(toast, {
            autohide: true,
            delay: 5000
        });
        bsToast.show();

        toast.addEventListener('hidden.bs.toast', () => toast.remove());
    }

    // Show success confirmation modal
    function showConfirmation(title, message) {
        document.getElementById('confirmationTitle').textContent = title;
        document.getElementById('confirmationMessage').textContent = message;
        new bootstrap.Modal(document.getElementById('successModal')).show();
    }

    // Generate initials from name
    function getInitials(firstName, lastName) {
        const firstInitial = firstName && firstName.length > 0 ? firstName.charAt(0).toUpperCase() : '';
        const lastInitial = lastName && lastName.length > 0 ? lastName.charAt(0).toUpperCase() : '';
        return firstInitial + lastInitial;
    }

    // Format date for display
    function formatDate(dateString) {
        if (!dateString) return 'Not available';
        try {
            const date = new Date(dateString);
            return date.toLocaleDateString('en-US', { month: 'long', day: 'numeric', year: 'numeric' });
        } catch (e) {
            return 'Invalid date';
        }
    }

    // ===== Data Loading =====
    // Load all customers from API
    function loadCustomers() {
        if (!checkAuth()) return;

        showSpinner();

        fetch(API_BASE, {
            method: 'GET',
            headers: getAuthHeaders()
        })
            .then(response => {
                if (!response.ok) {
                    if (response.status === 401 || response.status === 403) {
                        localStorage.removeItem("jwt-token");
                        sessionStorage.removeItem("jwt-token");
                        window.location.href = '/admin/login?error=session_expired';
                        throw new Error('Authentication failed');
                    }
                    throw new Error(`Server responded with status: ${response.status}`);
                }
                return response.json();
            })
            .then(customers => {
                hideSpinner();
                allCustomers = customers || [];
                totalPages = Math.ceil(allCustomers.length / CUSTOMERS_PER_PAGE);
                updatePagination();
                renderCustomers(currentPage);
            })
            .catch(error => {
                hideSpinner();
                if (error.message !== 'Authentication failed') {
                    showToast('Error', `Failed to load customers: ${error.message}`, 'error');
                }
            });
    }

    // Fetch customer details by ID
    function fetchCustomerDetails(customerId) {
        if (!checkAuth()) return Promise.reject(new Error('Authentication failed'));

        return fetch(`${API_BASE}/${customerId}`, {
            method: 'GET',
            headers: getAuthHeaders()
        })
            .then(response => {
                if (!response.ok) {
                    if (response.status === 401 || response.status === 403) {
                        localStorage.removeItem("jwt-token");
                        sessionStorage.removeItem("jwt-token");
                        window.location.href = '/admin/login?error=session_expired';
                        throw new Error('Authentication failed');
                    }
                    throw new Error(`Failed to load customer details: ${response.status}`);
                }
                return response.json();
            });
    }

    // Fetch service history for a customer
    function fetchServiceHistory(customerId) {
        if (!checkAuth() || !customerId) return Promise.resolve([]);

        // Use the standard service requests API with status filter
        return fetch(`${SERVICE_API}?customerId=${customerId}`, {
            method: 'GET',
            headers: getAuthHeaders()
        })
            .then(response => {
                if (!response.ok) {
                    if (response.status === 401 || response.status === 403) {
                        throw new Error('Authentication failed');
                    }
                    return []; // Return empty array on error
                }
                return response.json();
            })
            .then(services => {
                // Filter for completed services only
                if (Array.isArray(services)) {
                    return services.filter(service =>
                        service.status === 'Completed' ||
                        service.state === 'Completed' ||
                        service.serviceStatus === 'Completed'
                    );
                }
                return [];
            })
            .catch(error => {
                console.error('Error fetching service history:', error);
                return []; // Return empty array on any error
            });
    }

    // ===== UI Rendering =====
    // Render customers table with pagination
    function renderCustomers(page = 1) {
        const tableBody = document.querySelector('.customers-table tbody');
        if (!tableBody) return;

        // Clear existing rows
        tableBody.innerHTML = '';

        if (allCustomers.length === 0) {
            tableBody.innerHTML = `
                <tr>
                    <td colspan="6" class="text-center py-4">
                        <div class="no-data-message">
                            <i class="fas fa-users fa-3x mb-3 text-muted"></i>
                            <h4>No customers found</h4>
                            <p class="text-muted">Add your first customer to get started</p>
                        </div>
                    </td>
                </tr>`;
            return;
        }

        // Calculate slice of customers to show based on pagination
        const start = (page - 1) * CUSTOMERS_PER_PAGE;
        const end = start + CUSTOMERS_PER_PAGE;
        const customersToShow = allCustomers.slice(start, end);

        // Create and append rows
        customersToShow.forEach(customer => {
            const row = document.createElement('tr');
            row.className = 'customer-row';
            row.setAttribute('data-customer-id', customer.customerId);

            const initials = getInitials(customer.firstName, customer.lastName);
            const membershipClass = customer.membershipStatus === 'Premium' ? 'premium' : 'standard';
            const membershipIcon = customer.membershipStatus === 'Premium' ? 'fas fa-crown' : 'fas fa-user';

            // Ensure proper formatting for service count and last service date
            const serviceCount = customer.totalServices && !isNaN(parseInt(customer.totalServices))
                ? parseInt(customer.totalServices)
                : 0;

            const lastServiceDate = formatDate(customer.lastServiceDate);

            row.innerHTML = `
                <td>
                    <div class="customer-cell">
                        <div class="customer-avatar">${initials}</div>
                        <div class="customer-info">
                            <div class="customer-name">${customer.firstName} ${customer.lastName}</div>
                        </div>
                    </div>
                </td>
                <td>${customer.email}</td>
                <td>
                    <span class="phone-number">
                        <i class="fas fa-phone-alt"></i>
                        <span>${customer.phoneNumber || 'Not provided'}</span>
                    </span>
                </td>
                <td>
                    <span class="membership-badge ${membershipClass}">
                        <i class="${membershipIcon}"></i>
                        <span>${customer.membershipStatus || 'Standard'}</span>
                    </span>
                </td>
                <td>${serviceCount}</td>
                <td>
                    <span class="last-service">
                        <i class="fas fa-calendar-day"></i>
                        <span>${lastServiceDate}</span>
                    </span>
                </td>
            `;

            tableBody.appendChild(row);

            // Add click event listener to row
            row.addEventListener('click', () => {
                selectedCustomerId = customer.customerId;
                openCustomerDetailsModal(customer.customerId);
            });
        });
    }

    // Update pagination controls based on data
    function updatePagination() {
        const pagination = document.querySelector('.pagination');
        if (!pagination) return;

        // Get existing elements
        const items = pagination.querySelectorAll('.page-item');
        const prevBtn = items[0];
        const nextBtn = items[items.length - 1];

        // Clear all items except first and last
        while (pagination.children.length > 2) {
            pagination.removeChild(pagination.children[1]);
        }

        // Insert active page button
        const pageItem = document.createElement('li');
        pageItem.className = 'page-item active';
        pageItem.innerHTML = `<a class="page-link" href="#">1</a>`;
        pagination.insertBefore(pageItem, nextBtn);

        // Disable navigation buttons since we only have one page
        prevBtn.classList.add('disabled');
        nextBtn.classList.add('disabled');

        // Update event listeners for page button
        pageItem.querySelector('.page-link').addEventListener('click', (e) => {
            e.preventDefault();
            // Already on page 1, nothing to do
        });
    }

    // Display customer details in modal
    function populateCustomerDetails(customer) {
        // Basic info
        document.getElementById('viewCustomerInitials').textContent = getInitials(customer.firstName, customer.lastName);
        document.getElementById('viewCustomerName').textContent = `${customer.firstName} ${customer.lastName}`;
        document.getElementById('viewCustomerEmail').textContent = customer.email;
        document.getElementById('viewCustomerPhone').textContent = customer.phoneNumber || 'Not provided';

        // Address info
        const address = [customer.street, customer.city, customer.state, customer.postalCode]
            .filter(part => part && part.trim() !== '')
            .join(', ');
        document.getElementById('viewCustomerAddress').textContent = address || 'Not provided';

        // Membership & service info
        document.getElementById('viewCustomerMembership').textContent = customer.membershipStatus || 'Standard';
        document.getElementById('viewCustomerServices').textContent = customer.totalServices || '0';
        document.getElementById('viewCustomerLastService').textContent = formatDate(customer.lastServiceDate);

        // Set customer ID for edit button
        document.getElementById('editCustomerFromDetailsBtn').setAttribute('data-customer-id', customer.customerId);
    }

    // Populate service history in modal
    function populateServiceHistory(services) {
        const tableBody = document.getElementById('serviceHistoryTableBody');
        if (!tableBody) return;

        tableBody.innerHTML = '';

        if (!services || services.length === 0) {
            tableBody.innerHTML = `
                <tr>
                    <td colspan="5" class="text-center py-4">
                        <div class="no-data-message">
                            <i class="fas fa-tools fa-3x mb-3 text-muted"></i>
                            <h4>No service history found</h4>
                            <p class="text-muted">This customer has not had any completed services yet</p>
                        </div>
                    </td>
                </tr>`;
            return;
        }

        // Get the customer's membership status for discount calculation
        const membershipStatus = document.getElementById('viewCustomerMembership').textContent.trim();
        const isPremium = membershipStatus.toLowerCase() === 'premium';

        // Sort services by date (newest first)
        const sortedServices = [...services].sort((a, b) => {
            const dateA = a.updatedAt || a.completionDate || a.createdAt;
            const dateB = b.updatedAt || b.completionDate || b.createdAt;
            return new Date(dateB) - new Date(dateA);
        });

        sortedServices.forEach(service => {
            const row = document.createElement('tr');

            // Get vehicle name from available properties
            let vehicleName = 'Unknown vehicle';
            if (service.vehicleBrand && service.vehicleModel) {
                vehicleName = `${service.vehicleBrand} ${service.vehicleModel}`;
            } else if (service.vehicle && service.vehicle.brand && service.vehicle.model) {
                vehicleName = `${service.vehicle.brand} ${service.vehicle.model}`;
            } else if (service.vehicleName) {
                vehicleName = service.vehicleName;
            }

            // Use completion date for completed services
            const serviceDate = formatDate(service.updatedAt || service.completionDate || service.createdAt);

            // Calculate service amount with labor discount for premium customers
            // Extract labor and material costs
            let laborCost = 0;
            try {
                laborCost = parseFloat(service.laborCost || 0);
            } catch (e) {
                console.error('Error parsing laborCost:', e);
            }

            let materialCost = 0;
            try {
                materialCost = parseFloat(service.totalMaterialCost || 0);
            } catch (e) {
                console.error('Error parsing materialCost:', e);
            }

            // Apply premium discount to labor if applicable
            if (isPremium && laborCost > 0) {
                laborCost = laborCost * 0.7; // 30% discount
            }

            // Calculate total
            let totalAmount = laborCost + materialCost;

            // If we still don't have a valid amount, try other properties
            if (totalAmount <= 0) {
                if (typeof service.totalAmount !== 'undefined' && service.totalAmount !== null) {
                    try {
                        totalAmount = parseFloat(service.totalAmount);
                    } catch (e) {}
                } else if (typeof service.calculatedTotal !== 'undefined' && service.calculatedTotal !== null) {
                    try {
                        totalAmount = parseFloat(service.calculatedTotal);
                    } catch (e) {}
                } else if (service.invoice && service.invoice.totalAmount) {
                    try {
                        totalAmount = parseFloat(service.invoice.totalAmount);
                    } catch (e) {}
                }
            }

            // If still no amount, use fixed default
            if (totalAmount <= 0 || isNaN(totalAmount)) {
                totalAmount = 3000;
            }

            // Format the amount
            const amount = `₹${totalAmount.toFixed(2)}`;

            row.innerHTML = `
                <td>${vehicleName}</td>
                <td>${service.serviceType || 'General Service'}</td>
                <td>${serviceDate}</td>
                <td><span class="service-status completed">Completed</span></td>
                <td>${amount}</td>
            `;

            tableBody.appendChild(row);
        });
    }

    // Open customer details modal
    function openCustomerDetailsModal(customerId) {
        showSpinner();
        let customerData;

        // Fetch customer details
        fetchCustomerDetails(customerId)
            .then(customer => {
                customerData = customer;
                populateCustomerDetails(customer);

                // After populating details, fetch service history
                return fetchServiceHistory(customerId);
            })
            .then(services => {
                serviceHistory = services || [];

                // Update customer total services count with actual completed services
                if (customerData && serviceHistory.length > 0) {
                    // Update service count display
                    document.getElementById('viewCustomerServices').textContent = serviceHistory.length;

                    // Update Total Services in table as well if this row is visible
                    const customerRow = document.querySelector(`.customer-row[data-customer-id="${customerId}"]`);
                    if (customerRow) {
                        const totalServicesCell = customerRow.querySelector('td:nth-child(5)');
                        if (totalServicesCell) {
                            totalServicesCell.textContent = serviceHistory.length;
                        }
                    }

                    // Find most recent service for last service date
                    const sortedServices = [...serviceHistory].sort((a, b) =>
                        new Date(b.updatedAt || b.createdAt) - new Date(a.updatedAt || a.createdAt)
                    );

                    if (sortedServices[0]) {
                        const lastServiceDate = sortedServices[0].updatedAt || sortedServices[0].createdAt;
                        const formattedDate = formatDate(lastServiceDate);

                        // Update in modal
                        document.getElementById('viewCustomerLastService').textContent = formattedDate;

                        // Update in table if this row is visible
                        if (customerRow) {
                            const lastServiceCell = customerRow.querySelector('td:nth-child(6) .last-service span');
                            if (lastServiceCell) {
                                lastServiceCell.textContent = formattedDate;
                            }
                        }

                        // Also update the customer data in our local store
                        const customerIndex = allCustomers.findIndex(c => c.customerId === parseInt(customerId));
                        if (customerIndex !== -1) {
                            allCustomers[customerIndex].totalServices = serviceHistory.length;
                            allCustomers[customerIndex].lastServiceDate = lastServiceDate;
                        }
                    }
                }

                populateServiceHistory(serviceHistory);
                hideSpinner();
                new bootstrap.Modal(document.getElementById('customerDetailsModal')).show();
            })
            .catch(error => {
                hideSpinner();
                showToast('Error', error.message || 'Failed to load customer details', 'error');
            });
    }

    // ===== Form Validation =====
    function validateCustomerForm(formId, singleFieldId = null) {
        const prefix = formId === 'editCustomerForm' ? 'edit' : '';

        // Define field IDs with their corresponding validation rules
        const fields = {
            firstName: {
                id: prefix + 'firstName',
                required: true,
                pattern: /^[A-Za-z]+$/,
                minLength: 1,
                maxLength: 50,
                errorId: prefix + 'firstName-error',
                messages: {
                    required: "First name is required",
                    pattern: "First name must contain only alphabetic characters",
                    length: "First name must be between 1 and 50 characters"
                }
            },
            lastName: {
                id: prefix + 'lastName',
                required: true,
                pattern: /^[A-Za-z]+$/,
                minLength: 1,
                maxLength: 50,
                errorId: prefix + 'lastName-error',
                messages: {
                    required: "Last name is required",
                    pattern: "Last name must contain only alphabetic characters",
                    length: "Last name must be between 1 and 50 characters"
                }
            },
            email: {
                id: prefix + 'email',
                required: true,
                pattern: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
                maxLength: 100,
                errorId: prefix + 'email-error',
                messages: {
                    required: "Email is required",
                    pattern: "Please enter a valid email address",
                    length: "Email must be less than 100 characters"
                }
            },
            phone: {
                id: prefix + 'phone',
                required: true,
                pattern: /^(\+?\d{1,3}[-\s]?)?\d{9,12}$/,
                errorId: prefix + 'phone-error',
                messages: {
                    required: "Phone number is required",
                    pattern: "Please enter a valid phone number"
                }
            },
            street: {
                id: prefix + 'street',
                maxLength: 200,
                errorId: prefix + 'street-error',
                messages: {
                    length: "Street address must be less than 200 characters"
                }
            },
            city: {
                id: prefix + 'city',
                pattern: /^[A-Za-z\s]*$/,
                maxLength: 100,
                errorId: prefix + 'city-error',
                messages: {
                    pattern: "City must contain only alphabetic characters and spaces",
                    length: "City must be less than 100 characters"
                }
            },
            state: {
                id: prefix + 'state',
                pattern: /^[A-Za-z\s]*$/,
                maxLength: 100,
                errorId: prefix + 'state-error',
                messages: {
                    pattern: "State must contain only alphabetic characters and spaces",
                    length: "State must be less than 100 characters"
                }
            },
            postalCode: {
                id: prefix + 'postalCode',
                pattern: /^\d{6}$/,
                errorId: prefix + 'postalCode-error',
                messages: {
                    pattern: "Postal code must be a 6-digit number"
                }
            },
            membershipStatus: {
                id: prefix + 'membershipStatus',
                required: true,
                errorId: prefix + 'membershipStatus-error',
                messages: {
                    required: "Membership status is required"
                }
            }
        };

        // Reset validation state
        const fieldsToValidate = singleFieldId
            ? [Object.values(fields).find(f => f.id === singleFieldId)]
            : Object.values(fields);

        fieldsToValidate.forEach(field => {
            if (!field) return;

            const inputElement = document.getElementById(field.id);
            const errorElement = document.getElementById(field.errorId);

            if (inputElement) inputElement.classList.remove('is-invalid');
            if (errorElement) {
                errorElement.textContent = '';
                errorElement.style.display = 'none';
            }
        });

        // Validate each field
        let isValid = true;

        fieldsToValidate.forEach(field => {
            if (!field) return;

            const inputElement = document.getElementById(field.id);
            if (!inputElement) return;

            const value = inputElement.value.trim();

            // Skip validation if field is optional and empty
            if (!field.required && !value) return;

            // Required check
            if (field.required && !value) {
                displayFieldError(field.id, field.errorId, field.messages.required);
                isValid = false;
                return;
            }

            // Length check
            if (value && (
                (field.minLength && value.length < field.minLength) ||
                (field.maxLength && value.length > field.maxLength)
            )) {
                displayFieldError(field.id, field.errorId, field.messages.length);
                isValid = false;
                return;
            }

            // Pattern check
            if (value && field.pattern && !field.pattern.test(value)) {
                displayFieldError(field.id, field.errorId, field.messages.pattern);
                isValid = false;
            }
        });

        return isValid;
    }

    // Display field validation error
    function displayFieldError(fieldId, errorId, message) {
        const field = document.getElementById(fieldId);
        const errorElement = document.getElementById(errorId);

        if (field && errorElement) {
            field.classList.add('is-invalid');
            errorElement.textContent = message;
            errorElement.style.display = 'block';
        }
    }

    // ===== Event Handlers =====
    // Mobile menu toggle
    if (mobileMenuToggle) {
        mobileMenuToggle.addEventListener('click', () => {
            sidebar.classList.toggle('active');
        });
    }

    // Customer search
    if (customerSearch) {
        customerSearch.addEventListener('input', () => {
            const searchTerm = customerSearch.value.toLowerCase();
            const rows = document.querySelectorAll('.customer-row');

            rows.forEach(row => {
                const customerName = row.querySelector('.customer-name').textContent.toLowerCase();
                const email = row.querySelector('td:nth-child(2)').textContent.toLowerCase();
                const phone = row.querySelector('.phone-number').textContent.toLowerCase();

                row.style.display = (customerName.includes(searchTerm) ||
                    email.includes(searchTerm) ||
                    phone.includes(searchTerm)) ? '' : 'none';
            });
        });
    }

    // Filter pills
    if (filterPills.length) {
        filterPills.forEach(pill => {
            pill.addEventListener('click', function() {
                filterPills.forEach(p => p.classList.remove('active'));
                this.classList.add('active');

                const filter = this.textContent.trim();
                const rows = document.querySelectorAll('.customer-row');

                rows.forEach(row => {
                    if (filter === 'All Customers') {
                        row.style.display = '';
                    } else if (filter === 'Premium Members') {
                        const membershipBadge = row.querySelector('.membership-badge');
                        row.style.display = (membershipBadge && membershipBadge.classList.contains('premium')) ? '' : 'none';
                    } else if (filter === 'Standard Members') {
                        const membershipBadge = row.querySelector('.membership-badge');
                        row.style.display = (membershipBadge && membershipBadge.classList.contains('standard')) ? '' : 'none';
                    }
                });
            });
        });
    }

    // Profile tabs
    if (profileTabs.length) {
        profileTabs.forEach(tab => {
            tab.addEventListener('click', function() {
                const tabId = this.getAttribute('data-tab');

                profileTabs.forEach(t => t.classList.remove('active'));
                document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));

                this.classList.add('active');
                document.getElementById(`${tabId}-tab`).classList.add('active');
            });
        });
    }

    // Setup field validation on blur
    function setupFieldValidation(formId) {
        const prefix = formId === 'editCustomerForm' ? 'edit' : '';

        const fieldIds = [
            'firstName', 'lastName', 'email', 'phone',
            'street', 'city', 'state', 'postalCode', 'membershipStatus'
        ].map(field => prefix + field);

        fieldIds.forEach(fieldId => {
            const field = document.getElementById(fieldId);
            if (field) {
                field.addEventListener('blur', () => {
                    validateCustomerForm(formId, fieldId);
                });
            }
        });
    }

    // Add Customer Button
    if (addCustomerBtn) {
        addCustomerBtn.addEventListener('click', function() {
            this.classList.add('processing');

            document.getElementById('addCustomerForm').reset();

            // Reset validation state
            document.querySelectorAll('#addCustomerForm .invalid-feedback').forEach(el => {
                el.textContent = '';
                el.style.display = 'none';
            });

            document.querySelectorAll('#addCustomerForm .form-control, #addCustomerForm .form-select').forEach(el => {
                el.classList.remove('is-invalid');
            });

            new bootstrap.Modal(document.getElementById('addCustomerModal')).show();
            setupFieldValidation('addCustomerForm');

            setTimeout(() => this.classList.remove('processing'), 300);
        });
    }

    // Save Customer Button
    if (saveCustomerBtn) {
        saveCustomerBtn.addEventListener('click', function() {
            if (!validateCustomerForm('addCustomerForm')) return;

            this.classList.add('processing');
            this.disabled = true;
            showSpinner();

            // Prepare form data
            const formData = {
                firstName: document.getElementById('firstName').value.trim(),
                lastName: document.getElementById('lastName').value.trim(),
                email: document.getElementById('email').value.trim(),
                phoneNumber: document.getElementById('phone').value.trim(),
                street: document.getElementById('street').value.trim(),
                city: document.getElementById('city').value.trim(),
                state: document.getElementById('state').value.trim(),
                postalCode: document.getElementById('postalCode').value.trim(),
                membershipStatus: document.getElementById('membershipStatus').value,
                isActive: true
            };

            fetch(API_BASE, {
                method: 'POST',
                headers: getAuthHeaders(),
                body: JSON.stringify(formData)
            })
                .then(response => {
                    if (!response.ok) {
                        return response.json().then(data => {
                            throw new Error(data.message || `Error creating customer: ${response.status}`);
                        }).catch(e => {
                            // If JSON parsing fails, handle the text response
                            return response.text().then(text => {
                                throw new Error(text || `Error creating customer: ${response.status}`);
                            });
                        });
                    }
                    return response.json();
                })
                .then(data => {
                    hideSpinner();

                    bootstrap.Modal.getInstance(document.getElementById('addCustomerModal')).hide();
                    document.getElementById('addCustomerForm').reset();

                    showConfirmation('Customer Added', 'The customer has been successfully added to the system.');

                    // Refresh customer list
                    setTimeout(() => {
                        loadCustomers();
                    }, 800);
                })
                .catch(error => {
                    hideSpinner();
                    this.classList.remove('processing');
                    this.disabled = false;
                    showToast('Error', error.message || 'Failed to add customer. Please try again.', 'error');
                });
        });
    }

    // Edit Customer Button in Details Modal
    if (editCustomerFromDetailsBtn) {
        editCustomerFromDetailsBtn.addEventListener('click', function() {
            const customerId = this.getAttribute('data-customer-id');
            showSpinner();

            fetchCustomerDetails(customerId)
                .then(customer => {
                    hideSpinner();

                    bootstrap.Modal.getInstance(document.getElementById('customerDetailsModal')).hide();

                    // Reset validation state
                    document.querySelectorAll('#editCustomerForm .invalid-feedback').forEach(el => {
                        el.textContent = '';
                        el.style.display = 'none';
                    });

                    document.querySelectorAll('#editCustomerForm .form-control, #editCustomerForm .form-select').forEach(el => {
                        el.classList.remove('is-invalid');
                    });

                    // Populate form
                    document.getElementById('editCustomerId').value = customer.customerId;
                    document.getElementById('editUserId').value = customer.userId;
                    document.getElementById('editFirstName').value = customer.firstName;
                    document.getElementById('editLastName').value = customer.lastName;
                    document.getElementById('editEmail').value = customer.email;
                    document.getElementById('editPhone').value = customer.phoneNumber;
                    document.getElementById('editStreet').value = customer.street || '';
                    document.getElementById('editCity').value = customer.city || '';
                    document.getElementById('editState').value = customer.state || '';
                    document.getElementById('editPostalCode').value = customer.postalCode || '';
                    document.getElementById('editMembershipStatus').value = customer.membershipStatus || 'Standard';

                    setupFieldValidation('editCustomerForm');
                    new bootstrap.Modal(document.getElementById('editCustomerModal')).show();
                })
                .catch(error => {
                    hideSpinner();
                    showToast('Error', error.message || 'Failed to load customer details', 'error');
                });
        });
    }

    // Update Customer Button
    if (updateCustomerBtn) {
        updateCustomerBtn.addEventListener('click', function() {
            if (!validateCustomerForm('editCustomerForm')) return;

            const customerId = document.getElementById('editCustomerId').value;
            showSpinner();

            const formData = {
                customerId: customerId,
                userId: document.getElementById('editUserId').value,
                firstName: document.getElementById('editFirstName').value.trim(),
                lastName: document.getElementById('editLastName').value.trim(),
                email: document.getElementById('editEmail').value.trim(),
                phoneNumber: document.getElementById('editPhone').value.trim(),
                street: document.getElementById('editStreet').value.trim(),
                city: document.getElementById('editCity').value.trim(),
                state: document.getElementById('editState').value.trim(),
                postalCode: document.getElementById('editPostalCode').value.trim(),
                membershipStatus: document.getElementById('editMembershipStatus').value,
                isActive: true
            };

            fetch(`${API_BASE}/${customerId}`, {
                method: 'PUT',
                headers: getAuthHeaders(),
                body: JSON.stringify(formData)
            })
                .then(response => {
                    if (!response.ok) {
                        return response.json().then(data => {
                            throw new Error(data.message || `Error updating customer: ${response.status}`);
                        }).catch(e => {
                            return response.text().then(text => {
                                throw new Error(text || `Error updating customer: ${response.status}`);
                            });
                        });
                    }
                    return response.json();
                })
                .then(data => {
                    hideSpinner();

                    bootstrap.Modal.getInstance(document.getElementById('editCustomerModal')).hide();

                    showConfirmation('Customer Updated', 'The customer information has been successfully updated.');

                    // Refresh customer list
                    setTimeout(() => {
                        loadCustomers();

                        // If customer details modal was open, refresh it
                        if (selectedCustomerId === parseInt(customerId)) {
                            openCustomerDetailsModal(customerId);
                        }
                    }, 800);
                })
                .catch(error => {
                    hideSpinner();
                    showToast('Error', error.message || 'Failed to update customer', 'error');
                });
        });
    }

    // Logout Button
    document.querySelector('.logout-btn').addEventListener('click', function(e) {
        e.preventDefault();
        localStorage.removeItem("jwt-token");
        sessionStorage.removeItem("jwt-token");
        window.location.href = '/admin/logout';
    });

    // ===== Initialization =====
    // Check URL parameters for token
    const urlParams = new URLSearchParams(window.location.search);
    const urlToken = urlParams.get('token');

    // Store token from URL if available (and remove from URL)
    if (urlToken) {
        localStorage.setItem("jwt-token", urlToken);
        const url = new URL(window.location);
        url.searchParams.delete('token');
        window.history.replaceState({}, document.title, url);
    }

    if (checkAuth()) {
        loadCustomers();
    }
});