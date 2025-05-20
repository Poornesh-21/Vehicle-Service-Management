document.addEventListener('DOMContentLoaded', function() {
    // UI Element References
    const sidebar = document.getElementById('sidebar');
    const mobileMenuToggle = document.getElementById('mobileMenuToggle');
    const spinnerOverlay = document.getElementById('spinnerOverlay');
    const searchInput = document.getElementById('customerSearch');
    const filterPills = document.querySelectorAll('.filter-pill');
    const profileTabs = document.querySelectorAll('.profile-tab');
    const addCustomerBtn = document.getElementById('addCustomerBtn');
    const saveCustomerBtn = document.getElementById('saveCustomerBtn');
    const editCustomerFromDetailsBtn = document.getElementById('editCustomerFromDetailsBtn');
    const updateCustomerBtn = document.getElementById('updateCustomerBtn');

    // API Endpoints
    const API_BASE = '/admin/customers/api';

    // Initialize Mobile Menu
    if (mobileMenuToggle) {
        mobileMenuToggle.addEventListener('click', () => sidebar.classList.toggle('active'));
    }

    // Initialize Auth - Get token from URL params first, then from storage
    const urlParams = new URLSearchParams(window.location.search);
    const urlToken = urlParams.get('token');

    // Store token from URL if available (and remove from URL)
    if (urlToken) {
        localStorage.setItem("jwt-token", urlToken);
        // Remove token from URL to prevent exposing it
        const url = new URL(window.location);
        url.searchParams.delete('token');
        window.history.replaceState({}, document.title, url);
    }

    const token = getToken();
    if (!token) {
        window.location.href = '/admin/login?error=session_expired';
        return;
    }

    // UI Helper Functions
    function showSpinner() {
        spinnerOverlay.classList.add('show');
    }

    function hideSpinner() {
        setTimeout(() => spinnerOverlay.classList.remove('show'), 300);
    }

    function showConfirmation(title, message) {
        document.getElementById('confirmationTitle').textContent = title;
        document.getElementById('confirmationMessage').textContent = message;
        new bootstrap.Modal(document.getElementById('successModal')).show();
    }

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

    // Auth Functions
    function getToken() {
        return localStorage.getItem("jwt-token") || sessionStorage.getItem("jwt-token");
    }

    function getAuthHeaders() {
        return {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + getToken()
        };
    }

    // Handle unauthorized responses
    function handleResponse(response) {
        if (response.status === 401 || response.status === 403) {
            // Token expired or invalid
            localStorage.removeItem("jwt-token");
            sessionStorage.removeItem("jwt-token");
            window.location.href = '/admin/login?error=session_expired';
            throw new Error('Authentication failed');
        }

        if (!response.ok) {
            return response.json().then(data => {
                throw new Error(data.message || `Server responded with status: ${response.status}`);
            }).catch(() => {
                throw new Error(`Server responded with status: ${response.status}`);
            });
        }

        return response.json();
    }

    // Navigation Setup
    document.querySelectorAll('.sidebar-menu-link').forEach(link => {
        const href = link.getAttribute('href');

        if (href && !href.startsWith('/admin/') && !href.startsWith('#')) {
            link.setAttribute('href', '/admin' + href);
        }

        if (href && href.includes('token=')) {
            const url = new URL(href, window.location.origin);
            url.searchParams.delete('token');
            link.setAttribute('href', url.pathname + url.search);
        }

        if (window.location.pathname.includes(href)) {
            link.classList.add('active');
        }
    });

    // Setup Filters
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

    // Setup Search
    if (searchInput) {
        searchInput.addEventListener('keyup', function() {
            const searchTerm = this.value.toLowerCase();
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

    // Setup Profile Tabs
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

    // Setup Customer Row Click Handlers
    document.querySelectorAll('.customer-row').forEach(row => {
        row.addEventListener('click', function() {
            const customerId = this.getAttribute('data-customer-id');
            fetchAndShowCustomerDetails(customerId);
        });
    });

    // Customer CRUD Functions
    function fetchAndShowCustomerDetails(customerId) {
        showSpinner();

        fetch(`${API_BASE}/${customerId}`, {
            method: 'GET',
            headers: getAuthHeaders()
        })
            .then(handleResponse)
            .then(customer => {
                populateCustomerDetails(customer);
                hideSpinner();
                new bootstrap.Modal(document.getElementById('customerDetailsModal')).show();
            })
            .catch(error => {
                hideSpinner();
                showToast('Error', error.message || 'Failed to load customer details. Please try again.', 'error');
            });
    }

    function populateCustomerDetails(customer) {
        document.getElementById('viewCustomerInitials').textContent = getInitials(customer.firstName, customer.lastName);
        document.getElementById('viewCustomerName').textContent = `${customer.firstName} ${customer.lastName}`;
        document.getElementById('viewCustomerEmail').textContent = customer.email;
        document.getElementById('viewCustomerPhone').textContent = customer.phoneNumber || 'Not provided';

        const address = [customer.street, customer.city, customer.state, customer.postalCode]
            .filter(part => part && part.trim() !== '')
            .join(', ');
        document.getElementById('viewCustomerAddress').textContent = address || 'Not provided';

        document.getElementById('viewCustomerMembership').textContent = customer.membershipStatus || 'Standard';
        document.getElementById('viewCustomerServices').textContent = customer.totalServices || '0';
        document.getElementById('viewCustomerLastService').textContent = customer.lastServiceDate
            ? new Date(customer.lastServiceDate).toLocaleDateString('en-US', { month: 'long', day: 'numeric', year: 'numeric' })
            : 'No service yet';

        document.getElementById('editCustomerFromDetailsBtn').setAttribute('data-customer-id', customer.customerId);
    }

    function getInitials(firstName, lastName) {
        const firstInitial = firstName && firstName.length > 0 ? firstName.charAt(0).toUpperCase() : '';
        const lastInitial = lastName && lastName.length > 0 ? lastName.charAt(0).toUpperCase() : '';
        return firstInitial + lastInitial;
    }

    // Form Validation
    function validateCustomerForm(formId, singleFieldId = null) {
        try {
            const prefix = formId === 'editCustomerForm' ? 'edit' : '';

            // Define field IDs exactly as they appear in the HTML
            const fieldIds = {
                firstName: prefix + 'firstName',
                lastName: prefix + 'lastName',
                email: prefix + 'email',
                phone: prefix + 'phone',
                street: prefix + 'street',
                city: prefix + 'city',
                state: prefix + 'state',
                postalCode: prefix + 'postalCode',
                membershipStatus: prefix + 'membershipStatus'
            };

            const errorIds = {};
            Object.keys(fieldIds).forEach(key => {
                errorIds[key] = fieldIds[key] + '-error';
            });

            // Reset errors
            if (singleFieldId) {
                const fieldKey = Object.keys(fieldIds).find(key => fieldIds[key] === singleFieldId);
                if (fieldKey) {
                    const errorElement = document.getElementById(errorIds[fieldKey]);
                    const inputElement = document.getElementById(singleFieldId);

                    if (errorElement) {
                        errorElement.textContent = '';
                        errorElement.style.display = 'none';
                    }

                    if (inputElement) {
                        inputElement.classList.remove('is-invalid');
                    }
                }
            } else {
                Object.values(errorIds).forEach(id => {
                    const errorElement = document.getElementById(id);
                    if (errorElement) {
                        errorElement.textContent = '';
                        errorElement.style.display = 'none';
                    }
                });

                Object.values(fieldIds).forEach(id => {
                    const inputElement = document.getElementById(id);
                    if (inputElement) {
                        inputElement.classList.remove('is-invalid');
                    }
                });
            }

            // Validation rules
            const rules = {
                firstName: {
                    required: true,
                    minLength: 1,
                    maxLength: 50,
                    pattern: /^[A-Za-z]+$/,
                    messages: {
                        required: "First name is required",
                        length: "First name must be between 1 and 50 characters",
                        pattern: "First name must contain only alphabetic characters"
                    }
                },
                lastName: {
                    required: true,
                    minLength: 1,
                    maxLength: 50,
                    pattern: /^[A-Za-z]+$/,
                    messages: {
                        required: "Last name is required",
                        length: "Last name must be between 1 and 50 characters",
                        pattern: "Last name must contain only alphabetic characters"
                    }
                },
                email: {
                    required: true,
                    maxLength: 100,
                    pattern: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
                    messages: {
                        required: "Email is required",
                        pattern: "Please enter a valid email address",
                        length: "Email must be less than 100 characters"
                    }
                },
                phone: {
                    required: true,
                    pattern: /^(\+?\d{1,3}[-\s]?)?\d{9,12}$/,
                    messages: {
                        required: "Phone number is required",
                        pattern: "Please enter a valid phone number. International format is accepted."
                    }
                },
                street: {
                    maxLength: 200,
                    messages: {
                        length: "Street address must be less than 200 characters"
                    }
                },
                city: {
                    pattern: /^[A-Za-z\s]*$/,
                    maxLength: 100,
                    messages: {
                        length: "City must be less than 100 characters",
                        pattern: "City must contain only alphabetic characters and spaces"
                    }
                },
                state: {
                    pattern: /^[A-Za-z\s]*$/,
                    maxLength: 100,
                    messages: {
                        length: "State must be less than 100 characters",
                        pattern: "State must contain only alphabetic characters and spaces"
                    }
                },
                postalCode: {
                    pattern: /^\d{6}$/,
                    messages: {
                        pattern: "Postal code must be a 6-digit number"
                    }
                },
                membershipStatus: {
                    required: true,
                    messages: {
                        required: "Membership status is required"
                    }
                }
            };

            let isValid = true;

            // Validate only one field or all fields
            const fieldsToValidate = singleFieldId
                ? [Object.keys(fieldIds).find(key => fieldIds[key] === singleFieldId)]
                : Object.keys(fieldIds);

            fieldsToValidate.forEach(field => {
                if (!field) return;

                // Check if the field element exists before trying to access its value
                const fieldElement = document.getElementById(fieldIds[field]);
                if (!fieldElement) return;

                const value = fieldElement.value.trim();
                const rule = rules[field];

                // Skip validation if the field is optional and empty
                if (!rule.required && !value) return;

                // Required check
                if (rule.required && !value) {
                    displayFieldError(fieldIds[field], errorIds[field], rule.messages.required);
                    isValid = false;
                    return;
                }

                // Length check
                if (value && ((rule.minLength && value.length < rule.minLength) ||
                    (rule.maxLength && value.length > rule.maxLength))) {
                    displayFieldError(fieldIds[field], errorIds[field], rule.messages.length);
                    isValid = false;
                    return;
                }

                // Pattern check
                if (value && rule.pattern && !rule.pattern.test(value)) {
                    displayFieldError(fieldIds[field], errorIds[field], rule.messages.pattern);
                    isValid = false;
                }
            });

            return isValid;
        } catch (error) {
            showToast("Error", "There was an error validating the form. Please check all fields and try again.", "error");
            return false;
        }
    }

    function displayFieldError(fieldId, errorId, message) {
        const field = document.getElementById(fieldId);
        const errorElement = document.getElementById(errorId);

        if (field && errorElement) {
            field.classList.add('is-invalid');
            errorElement.textContent = message;
            errorElement.style.display = 'block';
        }
    }

    function validateField(fieldId, formId) {
        validateCustomerForm(formId, fieldId);
    }

    function setupFieldValidation(formId) {
        const prefix = formId === 'editCustomerForm' ? 'edit' : '';

        const fieldIds = [
            'firstName', 'lastName', 'email', 'phone',
            'street', 'city', 'state', 'postalCode', 'membershipStatus'
        ].map(field => prefix + field);

        fieldIds.forEach(fieldId => {
            const field = document.getElementById(fieldId);
            if (field) {
                field.addEventListener('blur', () => validateField(fieldId, formId));
            }
        });
    }

    // Add Customer Button
    if (addCustomerBtn) {
        addCustomerBtn.addEventListener('click', function() {
            this.classList.add('processing');

            document.getElementById('addCustomerForm').reset();

            document.querySelectorAll('#addCustomerForm .invalid-feedback').forEach(el => {
                el.textContent = '';
                el.style.display = 'none';
            });

            document.querySelectorAll('#addCustomerForm .form-control, #addCustomerForm .form-select').forEach(el => {
                el.classList.remove('is-invalid');
            });

            new bootstrap.Modal(document.getElementById('addCustomerModal')).show();

            setTimeout(() => this.classList.remove('processing'), 300);

            setupFieldValidation('addCustomerForm');
        });
    }

    // Updated Save Customer Button handler with improved error handling
    if (saveCustomerBtn) {
        saveCustomerBtn.addEventListener('click', function() {
            this.classList.add('processing');
            this.disabled = true;

            try {
                const form = document.getElementById('addCustomerForm');

                if (!validateCustomerForm('addCustomerForm')) {
                    this.classList.remove('processing');
                    this.disabled = false;
                    return;
                }

                showSpinner();

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
                    isActive: true  // Always set to true for new customers
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

                        const modal = bootstrap.Modal.getInstance(document.getElementById('addCustomerModal'));
                        if (modal) modal.hide();

                        showConfirmation('Customer Added', 'The customer has been successfully added to the system.');

                        form.reset();

                        setTimeout(() => window.location.reload(), 800);
                    })
                    .catch(error => {
                        hideSpinner();
                        this.classList.remove('processing');
                        this.disabled = false;
                        showToast('Error', error.message || 'Failed to add customer. Please try again.', 'error');
                    });
            } catch (e) {
                hideSpinner();
                this.classList.remove('processing');
                this.disabled = false;
                showToast('Error', 'An unexpected error occurred. Please try again.', 'error');
            }
        });
    }

    // Edit Customer Button
    if (editCustomerFromDetailsBtn) {
        editCustomerFromDetailsBtn.addEventListener('click', function() {
            const customerId = this.getAttribute('data-customer-id');
            showSpinner();

            fetch(`${API_BASE}/${customerId}`, {
                method: 'GET',
                headers: getAuthHeaders()
            })
                .then(handleResponse)
                .then(customer => {
                    hideSpinner();

                    const detailsModal = bootstrap.Modal.getInstance(document.getElementById('customerDetailsModal'));
                    detailsModal.hide();

                    document.querySelectorAll('#editCustomerForm .invalid-feedback').forEach(el => {
                        el.textContent = '';
                        el.style.display = 'none';
                    });

                    document.querySelectorAll('#editCustomerForm .form-control, #editCustomerForm .form-select').forEach(el => {
                        el.classList.remove('is-invalid');
                    });

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

                    new bootstrap.Modal(document.getElementById('editCustomerModal')).show();

                    setupFieldValidation('editCustomerForm');
                })
                .catch(error => {
                    hideSpinner();
                    showToast('Error', error.message || 'Failed to load customer details for editing. Please try again.', 'error');
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
                .then(handleResponse)
                .then(data => {
                    hideSpinner();

                    const modal = bootstrap.Modal.getInstance(document.getElementById('editCustomerModal'));
                    modal.hide();

                    showConfirmation('Customer Updated', 'The customer information has been successfully updated.');

                    setTimeout(() => window.location.reload(), 1000);
                })
                .catch(error => {
                    hideSpinner();
                    showToast('Error', error.message || 'Failed to update customer. Please try again.', 'error');
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

    // Initial Load of Customers
    function loadCustomers() {
        showSpinner();

        fetch(API_BASE, {
            method: 'GET',
            headers: getAuthHeaders()
        })
            .then(handleResponse)
            .then(customers => {
                hideSpinner();
                if (customers && customers.length > 0) {
                    populateCustomersTable(customers);
                }
            })
            .catch(error => {
                hideSpinner();
                if (error.message !== 'Authentication failed') {
                    showToast('Error', error.message || 'Failed to load customers. Please refresh and try again.', 'error');
                }
            });
    }

    function populateCustomersTable(customers) {
        const tableBody = document.querySelector('.customers-table tbody');
        if (!tableBody) return;

        // Clear existing rows except any "empty" rows
        const emptyRow = tableBody.querySelector('tr[colspan="6"]');
        tableBody.innerHTML = '';

        if (customers.length === 0 && emptyRow) {
            tableBody.appendChild(emptyRow);
            return;
        }

        customers.forEach(customer => {
            const row = document.createElement('tr');
            row.className = 'customer-row';
            row.setAttribute('data-customer-id', customer.customerId);

            const initials = getInitials(customer.firstName, customer.lastName);
            const membershipClass = customer.membershipStatus === 'Premium' ? 'premium' : 'standard';
            const membershipIcon = customer.membershipStatus === 'Premium' ? 'fas fa-crown' : 'fas fa-user';
            const formattedDate = customer.lastServiceDate
                ? new Date(customer.lastServiceDate).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
                : 'No service yet';

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
                <td>${customer.totalServices || 0}</td>
                <td>
                    <span class="last-service">
                        <i class="fas fa-calendar-day"></i>
                        <span>${formattedDate}</span>
                    </span>
                </td>
            `;

            tableBody.appendChild(row);

            row.addEventListener('click', function() {
                fetchAndShowCustomerDetails(customer.customerId);
            });
        });

        // If no customers, add empty state row
        if (customers.length === 0) {
            const emptyRow = document.createElement('tr');
            emptyRow.innerHTML = `
                <td colspan="6" class="text-center py-4">
                    <div class="no-data-message">
                        <i class="fas fa-users fa-3x mb-3 text-muted"></i>
                        <h4>No customers found</h4>
                        <p class="text-muted">Add your first customer to get started</p>
                    </div>
                </td>
            `;
            tableBody.appendChild(emptyRow);
        }
    }

    // Start the application
    if (window.location.pathname.includes('/admin/customers')) {
        if (document.querySelectorAll('.customer-row').length === 0) {
            loadCustomers();
        }
    }
});