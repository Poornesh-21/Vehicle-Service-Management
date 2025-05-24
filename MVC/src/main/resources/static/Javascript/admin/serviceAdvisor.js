let serviceAdvisors = [];
let currentPage = 1;
const itemsPerPage = 8;
let currentFilter = 'all';

document.addEventListener('DOMContentLoaded', function() {
    initializeApp();
    setupEventListeners();
    loadServiceAdvisors();
});

function initializeApp() {
    setupMobileMenu();
    setupLogout();
    setupAuthentication();
    setupNavigation();
}

function setupMobileMenu() {
    const mobileMenuToggle = document.getElementById('mobileMenuToggle');
    if (mobileMenuToggle) {
        mobileMenuToggle.addEventListener('click', function() {
            document.getElementById('sidebar').classList.toggle('active');
        });
    }
}

function setupLogout() {
    const logoutBtn = document.getElementById('logoutBtn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', function() {
            localStorage.removeItem("jwt-token");
            sessionStorage.removeItem("jwt-token");
            localStorage.removeItem("user-role");
            localStorage.removeItem("user-name");
            sessionStorage.removeItem("user-role");
            sessionStorage.removeItem("user-name");
            window.location.href = '/admin/logout';
        });
    }
}

function setupAuthentication() {
    const token = getToken();
    if (!token) {
        window.location.href = '/admin/login?error=session_expired';
        return;
    }
}

function setupNavigation() {
    const currentPath = window.location.pathname;
    document.querySelectorAll('.sidebar-menu-link').forEach(link => {
        const href = link.getAttribute('href');

        // Clean up URLs with token parameters
        if (href && href.includes('token=')) {
            const cleanHref = href.split('?')[0];
            link.setAttribute('href', cleanHref);
        }

        if (href && currentPath.includes(href.split('?')[0])) {
            link.classList.add('active');
        } else {
            link.classList.remove('active');
        }
    });
}

function setupEventListeners() {
    const addAdvisorModalEl = document.getElementById('addAdvisorModal');
    if (addAdvisorModalEl) {
        addAdvisorModalEl.addEventListener('hidden.bs.modal', function() {
            clearErrorMessage('addAdvisorForm');
            const form = document.getElementById('addAdvisorForm');
            if (form) {
                form.querySelectorAll('.is-invalid').forEach(input => {
                    input.classList.remove('is-invalid');
                });
                form.querySelectorAll('.error-message').forEach(error => {
                    error.classList.remove('show');
                    error.textContent = '';
                });
            }
        });
    }

    const editAdvisorModalEl = document.getElementById('editAdvisorModal');
    if (editAdvisorModalEl) {
        editAdvisorModalEl.addEventListener('hidden.bs.modal', function() {
            clearErrorMessage('editAdvisorForm');
            const form = document.getElementById('editAdvisorForm');
            if (form) {
                form.querySelectorAll('.is-invalid').forEach(input => {
                    input.classList.remove('is-invalid');
                });
                form.querySelectorAll('.error-message').forEach(error => {
                    error.classList.remove('show');
                    error.textContent = '';
                });
            }
        });
    }

    const addAdvisorBtn = document.getElementById('addAdvisorBtn');
    if (addAdvisorBtn) {
        addAdvisorBtn.addEventListener('click', function() {
            const form = document.getElementById('addAdvisorForm');
            form.reset();
            clearErrorMessage('addAdvisorForm');
            form.querySelectorAll('.is-invalid').forEach(input => {
                input.classList.remove('is-invalid');
            });
            form.querySelectorAll('.error-message').forEach(error => {
                error.classList.remove('show');
                error.textContent = '';
            });
            const addAdvisorModal = new bootstrap.Modal(document.getElementById('addAdvisorModal'));
            addAdvisorModal.show();
        });
    }

    const saveAdvisorBtn = document.getElementById('saveAdvisorBtn');
    if (saveAdvisorBtn) {
        saveAdvisorBtn.addEventListener('click', saveServiceAdvisor);
    }

    const editAdvisorFromDetailsBtn = document.getElementById('editAdvisorFromDetailsBtn');
    if (editAdvisorFromDetailsBtn) {
        editAdvisorFromDetailsBtn.addEventListener('click', function() {
            const advisorId = this.getAttribute('data-advisor-id');
            showEditModal(advisorId);
        });
    }

    const updateAdvisorBtn = document.getElementById('updateAdvisorBtn');
    if (updateAdvisorBtn) {
        updateAdvisorBtn.addEventListener('click', updateServiceAdvisor);
    }

    const resetPasswordBtn = document.getElementById('resetPasswordBtn');
    if (resetPasswordBtn) {
        resetPasswordBtn.addEventListener('click', function() {
            const generatedPassword = generateRandomPassword();
            document.getElementById('editPassword').value = generatedPassword;
            showConfirmation(
                "Password Reset",
                "A new temporary password has been generated and will be sent to the service advisor's email."
            );
        });
    }

    const profileTabs = document.querySelectorAll('.profile-tab');
    profileTabs.forEach(tab => {
        tab.addEventListener('click', function() {
            const tabId = this.getAttribute('data-tab');
            profileTabs.forEach(t => t.classList.remove('active'));
            document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
            this.classList.add('active');
            document.getElementById(`${tabId}-tab`).classList.add('active');
        });
    });

    const searchInput = document.getElementById('advisorSearch');
    if (searchInput) {
        searchInput.addEventListener('keyup', function() {
            filterServiceAdvisors(this.value);
        });
    }

    const filterPills = document.querySelectorAll('.filter-pill');
    filterPills.forEach(pill => {
        pill.addEventListener('click', function() {
            filterPills.forEach(p => p.classList.remove('active'));
            this.classList.add('active');
            currentFilter = this.textContent.trim().toLowerCase().replace(' ', '-');
            currentPage = 1;
            renderServiceAdvisors();
        });
    });

    const prevBtn = document.getElementById('prevBtn');
    if (prevBtn) {
        prevBtn.addEventListener('click', function(e) {
            e.preventDefault();
            if (currentPage > 1) {
                currentPage--;
                renderServiceAdvisors();
                updatePaginationUI();
            }
        });
    }

    const nextBtn = document.getElementById('nextBtn');
    if (nextBtn) {
        nextBtn.addEventListener('click', function(e) {
            e.preventDefault();
            const filteredAdvisors = getFilteredAdvisors();
            const totalPages = Math.ceil(filteredAdvisors.length / itemsPerPage);
            if (currentPage < totalPages) {
                currentPage++;
                renderServiceAdvisors();
                updatePaginationUI();
            }
        });
    }


}

function getToken() {
    return localStorage.getItem('jwt-token') || sessionStorage.getItem('jwt-token');
}

function showSpinner() {
    const spinnerOverlay = document.getElementById('spinnerOverlay');
    if (spinnerOverlay) {
        spinnerOverlay.classList.add('show');
    }
}

function hideSpinner() {
    const spinnerOverlay = document.getElementById('spinnerOverlay');
    if (spinnerOverlay) {
        setTimeout(() => {
            spinnerOverlay.classList.remove('show');
        }, 500);
    }
}

function showConfirmation(title, message) {
    document.getElementById('confirmationTitle').textContent = title;
    document.getElementById('confirmationMessage').textContent = message;
    const successModal = new bootstrap.Modal(document.getElementById('successModal'));
    successModal.show();
}

function showErrorMessage(formId, message) {
    let errorContainer = document.getElementById(`${formId}-error-container`);

    if (!errorContainer) {
        errorContainer = document.createElement('div');
        errorContainer.id = `${formId}-error-container`;
        errorContainer.className = 'alert alert-danger mt-3';
        errorContainer.role = 'alert';

        const form = document.getElementById(formId);
        if (form) {
            form.parentNode.insertBefore(errorContainer, form.nextSibling);
        }
    }

    errorContainer.textContent = message;
    errorContainer.style.display = 'block';
}

function clearErrorMessage(formId) {
    const errorContainer = document.getElementById(`${formId}-error-container`);
    if (errorContainer) {
        errorContainer.style.display = 'none';
    }
}

function loadServiceAdvisors() {
    showSpinner();

    fetch('/admin/service-advisors/api/advisors', {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + getToken()
        }
    })
        .then(response => {
            if (!response.ok) {
                if (response.status === 401) {
                    window.location.href = '/admin/login?error=session_expired';
                    throw new Error('Session expired');
                }
                throw new Error(`Server responded with status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            hideSpinner();
            serviceAdvisors = data;
            renderServiceAdvisors();
            setupPagination();
        })
        .catch(error => {
            hideSpinner();
            alert('Failed to load advisor details. Please try again.');
        });
}

function getFilteredAdvisors() {
    const searchTerm = document.getElementById('advisorSearch').value.toLowerCase();

    let filtered = serviceAdvisors.filter(advisor => {
        return (
            (advisor.firstName + ' ' + advisor.lastName).toLowerCase().includes(searchTerm) ||
            advisor.email.toLowerCase().includes(searchTerm) ||
            (advisor.formattedId || '').toLowerCase().includes(searchTerm) ||
            (advisor.department || '').toLowerCase().includes(searchTerm)
        );
    });

    if (currentFilter !== 'all-advisors') {
        if (currentFilter === 'high-workload') {
            filtered = filtered.filter(advisor => advisor.workloadPercentage >= 75);
        } else if (currentFilter === 'available') {
            filtered = filtered.filter(advisor => advisor.workloadPercentage < 50);
        }
    }

    return filtered;
}

function renderServiceAdvisors() {
    const filteredAdvisors = getFilteredAdvisors();
    const startIndex = (currentPage - 1) * itemsPerPage;
    const endIndex = startIndex + itemsPerPage;
    const paginatedAdvisors = filteredAdvisors.slice(startIndex, endIndex);
    const gridContainer = document.getElementById('advisorsGrid');

    gridContainer.innerHTML = '';

    if (paginatedAdvisors.length === 0) {
        gridContainer.innerHTML = `
      <div class="no-advisors-message">
        <i class="fas fa-user-tie fa-3x mb-3 text-muted"></i>
        <h4>No service advisors found</h4>
        <p class="text-muted">Try adjusting your filters or add a new service advisor</p>
      </div>
    `;
        return;
    }

    paginatedAdvisors.forEach(advisor => {
        const workloadClass = getWorkloadClass(advisor.workloadPercentage);
        const card = document.createElement('div');
        card.className = 'advisor-card';
        card.addEventListener('click', function() {
            showAdvisorDetails(advisor);
        });

        card.innerHTML = `
      <div class="advisor-card-header">
        <div class="advisor-avatar">${getInitials(advisor.firstName, advisor.lastName)}</div>
        <div class="advisor-header-info">
          <div class="advisor-name">${advisor.firstName} ${advisor.lastName}</div>
          <div class="advisor-id">${advisor.formattedId || ('SA-' + advisor.advisorId)}</div>
        </div>
      </div>
      <div class="advisor-card-body">
        <div class="advisor-detail">
          <div class="advisor-detail-icon">
            <i class="fas fa-envelope"></i>
          </div>
          <div class="advisor-detail-content">
            <div class="advisor-detail-label">Email</div>
            <div class="advisor-detail-value">${advisor.email}</div>
          </div>
        </div>
        <div class="advisor-detail">
          <div class="advisor-detail-icon">
            <i class="fas fa-phone"></i>
          </div>
          <div class="advisor-detail-content">
            <div class="advisor-detail-label">Phone</div>
            <div class="advisor-detail-value">${advisor.phoneNumber}</div>
          </div>
        </div>
        <div class="advisor-detail">
          <div class="advisor-detail-icon">
            <i class="fas fa-building"></i>
          </div>
          <div class="advisor-detail-content">
            <div class="advisor-detail-label">Department</div>
            <div class="advisor-detail-value">${advisor.department || 'Not assigned'}</div>
          </div>
        </div>
      </div>
      <div class="advisor-card-footer">
        <div class="workload-container">
          <div class="workload-progress ${workloadClass}" style="width: ${advisor.workloadPercentage}%;"></div>
        </div>
        <div class="workload-text">
          <span>Workload</span>
          <span class="workload-label">${advisor.workloadPercentage}% (${advisor.activeServices} active)</span>
        </div>
      </div>
    `;

        gridContainer.appendChild(card);
    });

    updatePaginationUI();
}

function updatePaginationUI() {
    const filteredAdvisors = getFilteredAdvisors();
    const totalPages = Math.ceil(filteredAdvisors.length / itemsPerPage);
    const pagination = document.getElementById('pagination');

    pagination.innerHTML = '';

    const prevBtn = document.createElement('li');
    prevBtn.className = 'page-item ' + (currentPage === 1 ? 'disabled' : '');
    prevBtn.innerHTML = `
  <a class="page-link" href="#" aria-label="Previous">
      <i class="fas fa-chevron-left"></i>
  </a>
  `;
    prevBtn.addEventListener('click', function(e) {
        e.preventDefault();
        if (currentPage > 1) {
            currentPage--;
            renderServiceAdvisors();
        }
    });
    pagination.appendChild(prevBtn);

    for (let i = 1; i <= totalPages; i++) {
        const pageItem = document.createElement('li');
        pageItem.className = `page-item ${i === currentPage ? 'active' : ''}`;
        pageItem.innerHTML = `<a class="page-link" href="#">${i}</a>`;
        pageItem.addEventListener('click', function(e) {
            e.preventDefault();
            currentPage = i;
            renderServiceAdvisors();
        });
        pagination.appendChild(pageItem);
    }

    const nextBtn = document.createElement('li');
    nextBtn.className = 'page-item ' + (currentPage === totalPages || totalPages === 0 ? 'disabled' : '');
    nextBtn.innerHTML = `
  <a class="page-link" href="#" aria-label="Next">
      <i class="fas fa-chevron-right"></i>
  </a>
  `;
    nextBtn.addEventListener('click', function(e) {
        e.preventDefault();
        if (currentPage < totalPages) {
            currentPage++;
            renderServiceAdvisors();
        }
    });
    pagination.appendChild(nextBtn);
}

function setupPagination() {
    const filteredAdvisors = getFilteredAdvisors();
    const totalPages = Math.ceil(filteredAdvisors.length / itemsPerPage);
    updatePaginationUI();
}

function getInitials(firstName, lastName) {
    const firstInitial = firstName ? firstName.charAt(0).toUpperCase() : '';
    const lastInitial = lastName ? lastName.charAt(0).toUpperCase() : '';
    return firstInitial + lastInitial;
}

function getWorkloadClass(percentage) {
    if (percentage >= 75) {
        return 'high';
    } else if (percentage >= 50) {
        return 'medium';
    } else {
        return 'low';
    }
}

function getStatusIcon(status) {
    switch (status.toLowerCase()) {
        case 'received':
            return 'clipboard-check';
        case 'diagnosis':
            return 'stethoscope';
        case 'repair':
            return 'tools';
        case 'completed':
            return 'check-circle';
        default:
            return 'circle';
    }
}

function getWorkloadText(percentage) {
    if (percentage >= 75) {
        return 'High workload';
    } else if (percentage >= 50) {
        return 'Moderate workload';
    } else {
        return 'Available for new services';
    }
}

function getActiveServicesClass(count) {
    if (count >= 6) {
        return 'danger';
    } else if (count >= 3) {
        return 'warning';
    } else {
        return 'success';
    }
}

function showAdvisorDetails(advisor) {
    document.getElementById('editAdvisorFromDetailsBtn').setAttribute('data-advisor-id', advisor.advisorId);
    document.getElementById('viewAdvisorInitials').textContent = getInitials(advisor.firstName, advisor.lastName);
    document.getElementById('viewAdvisorName').textContent = advisor.firstName + ' ' + advisor.lastName;
    document.getElementById('viewAdvisorEmail').textContent = advisor.email;
    document.getElementById('viewAdvisorPhone').textContent = advisor.phoneNumber;
    document.getElementById('viewAdvisorId').textContent = advisor.formattedId || ('SA-' + advisor.advisorId);
    document.getElementById('viewAdvisorDepartment').textContent = advisor.department || 'Not assigned';
    document.getElementById('viewAdvisorHireDate').textContent = advisor.hireDate ? new Date(advisor.hireDate).toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' }) : 'Not available';
    document.getElementById('viewAdvisorWorkloadValue').textContent = advisor.workloadPercentage + '%';

    const workloadBar = document.getElementById('viewAdvisorWorkloadBar');
    workloadBar.style.width = advisor.workloadPercentage + '%';
    workloadBar.className = 'workload-progress ' + getWorkloadClass(advisor.workloadPercentage);

    document.getElementById('viewAdvisorWorkloadText').textContent = getWorkloadText(advisor.workloadPercentage);

    const detailsModal = new bootstrap.Modal(document.getElementById('advisorDetailsModal'));
    detailsModal.show();
}

function validateField(fieldId, errorId, validationFn, errorMessage) {
    const field = document.getElementById(fieldId);
    const errorElement = document.getElementById(errorId);

    if (!field || !errorElement) return true;

    const isValid = validationFn(field.value);

    if (!isValid) {
        field.classList.add('is-invalid');
        errorElement.textContent = errorMessage;
        errorElement.classList.add('show');
        return false;
    } else {
        field.classList.remove('is-invalid');
        errorElement.textContent = '';
        errorElement.classList.remove('show');
        return true;
    }
}

function validateName(value) {
    return value.trim().length >= 1;
}

function validateEmail(value) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(value);
}

function validatePhone(value) {
    const phoneRegex = /^\d{10}$/;
    return phoneRegex.test(value.replace(/\D/g, ''));
}

function validateDepartment(value) {
    return value.trim() !== '';
}

document.addEventListener('DOMContentLoaded', function() {
    const addAdvisorValidations = {
        'firstName': {
            errorId: 'firstName-error',
            validationFn: validateName,
            errorMessage: 'First name must be at least 2 characters'
        },
        'lastName': {
            errorId: 'lastName-error',
            validationFn: validateName,
            errorMessage: 'Last name must be at least 1 characters'
        },
        'email': {
            errorId: 'email-error',
            validationFn: validateEmail,
            errorMessage: 'Please enter a valid email address'
        },
        'phone': {
            errorId: 'phone-error',
            validationFn: validatePhone,
            errorMessage: 'Please enter a valid 10-digit phone number'
        },
        'department': {
            errorId: 'department-error',
            validationFn: validateDepartment,
            errorMessage: 'Please select a department'
        }
    };

    const editAdvisorValidations = {
        'editFirstName': {
            errorId: 'editFirstName-error',
            validationFn: validateName,
            errorMessage: 'First name must be at least 2 characters'
        },
        'editLastName': {
            errorId: 'editLastName-error',
            validationFn: validateName,
            errorMessage: 'Last name must be at least 2 characters'
        },
        'editEmail': {
            errorId: 'editEmail-error',
            validationFn: validateEmail,
            errorMessage: 'Please enter a valid email address'
        },
        'editPhone': {
            errorId: 'editPhone-error',
            validationFn: validatePhone,
            errorMessage: 'Please enter a valid 10-digit phone number'
        },
        'editDepartment': {
            errorId: 'editDepartment-error',
            validationFn: validateDepartment,
            errorMessage: 'Please select a department'
        }
    };

    setupFormValidation('addAdvisorForm', addAdvisorValidations);
    setupFormValidation('editAdvisorForm', editAdvisorValidations);
});

function setupFormValidation(formId, fieldValidations) {
    const form = document.getElementById(formId);
    if (!form) return;

    Object.keys(fieldValidations).forEach(fieldId => {
        const field = document.getElementById(fieldId);
        if (field) {
            field.addEventListener('blur', () => {
                const validation = fieldValidations[fieldId];
                validateField(fieldId, validation.errorId, validation.validationFn, validation.errorMessage);
            });
        }
    });
}

function validateForm(formId, fieldValidations) {
    let isValid = true;

    Object.keys(fieldValidations).forEach(fieldId => {
        const validation = fieldValidations[fieldId];
        const fieldValid = validateField(fieldId, validation.errorId, validation.validationFn, validation.errorMessage);
        isValid = isValid && fieldValid;
    });

    return isValid;
}

function generateRandomPassword() {
    const letters = 'ABCDEFGHJKLMNPQRSTUVWXYZ';
    const numbers = '123456789';

    let password = 'SA2025-';

    for (let i = 0; i < 3; i++) {
        password += letters.charAt(Math.floor(Math.random() * letters.length));
    }

    for (let i = 0; i < 3; i++) {
        password += numbers.charAt(Math.floor(Math.random() * numbers.length));
    }

    return password;
}

function saveServiceAdvisor() {
    const form = document.getElementById('addAdvisorForm');

    const addAdvisorValidations = {
        'firstName': {
            errorId: 'firstName-error',
            validationFn: validateName,
            errorMessage: 'First name must be at least 2 characters'
        },
        'lastName': {
            errorId: 'lastName-error',
            validationFn: validateName,
            errorMessage: 'Last name must be at least 2 characters'
        },
        'email': {
            errorId: 'email-error',
            validationFn: validateEmail,
            errorMessage: 'Please enter a valid email address'
        },
        'phone': {
            errorId: 'phone-error',
            validationFn: validatePhone,
            errorMessage: 'Please enter a valid 10-digit phone number'
        },
        'department': {
            errorId: 'department-error',
            validationFn: validateDepartment,
            errorMessage: 'Please select a department'
        }
    };

    if (!validateForm('addAdvisorForm', addAdvisorValidations)) {
        return;
    }

    const generatedPassword = generateRandomPassword();

    const formData = {
        firstName: document.getElementById('firstName').value,
        lastName: document.getElementById('lastName').value,
        email: document.getElementById('email').value,
        phoneNumber: document.getElementById('phone').value,
        department: document.getElementById('department').value,
        password: generatedPassword
    };

    showSpinner();

    fetch('/admin/service-advisors', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + getToken()
        },
        body: JSON.stringify(formData)
    })
        .then(response => {
            if (!response.ok) {
                return response.text().then(text => {
                    throw new Error(`Server responded with status: ${response.status}` +
                        (text ? `, message: ${text}` : ''));
                });
            }
            return response.json();
        })
        .then(data => {
            hideSpinner();

            const modal = bootstrap.Modal.getInstance(document.getElementById('addAdvisorModal'));
            modal.hide();

            showConfirmation(
                "Service Advisor Added Successfully",
                "The service advisor has been created and login credentials have been sent to their email address."
            );

            form.reset();
            loadServiceAdvisors();
        })
        .catch(error => {
            hideSpinner();
            showErrorMessage('addAdvisorForm', 'Failed to create service advisor. ' +
                (error.message || 'Please check your connection and try again.'));
        });
}

function showEditModal(advisorId) {
    const detailsModal = bootstrap.Modal.getInstance(document.getElementById('advisorDetailsModal'));
    if (detailsModal) {
        detailsModal.hide();
    }

    const form = document.getElementById('editAdvisorForm');
    if (form) {
        clearErrorMessage('editAdvisorForm');

        form.querySelectorAll('.is-invalid').forEach(input => {
            input.classList.remove('is-invalid');
        });

        form.querySelectorAll('.error-message').forEach(error => {
            error.classList.remove('show');
            error.textContent = '';
        });
    }

    showSpinner();

    fetch(`/admin/service-advisors/${advisorId}`, {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + getToken()
        }
    })
        .then(response => {
            if (!response.ok) {
                throw new Error(`Server responded with status: ${response.status}`);
            }
            return response.json();
        })
        .then(advisor => {
            hideSpinner();

            document.getElementById('editAdvisorId').value = advisor.advisorId;
            document.getElementById('editUserId').value = advisor.userId;
            document.getElementById('editFirstName').value = advisor.firstName;
            document.getElementById('editLastName').value = advisor.lastName;
            document.getElementById('editEmail').value = advisor.email;
            document.getElementById('editPhone').value = advisor.phoneNumber;
            document.getElementById('editDepartment').value = advisor.department;
            document.getElementById('editPassword').value = '';

            const editModal = new bootstrap.Modal(document.getElementById('editAdvisorModal'));
            editModal.show();
        })
        .catch(error => {
            hideSpinner();
            showErrorMessage('editAdvisorForm', 'Failed to load advisor details for editing. Please try again.');
        });
}

function updateServiceAdvisor() {
    const form = document.getElementById('editAdvisorForm');

    const editAdvisorValidations = {
        'editFirstName': {
            errorId: 'editFirstName-error',
            validationFn: validateName,
            errorMessage: 'First name must be at least 2 characters'
        },
        'editLastName': {
            errorId: 'editLastName-error',
            validationFn: validateName,
            errorMessage: 'Last name must be at least 2 characters'
        },
        'editEmail': {
            errorId: 'editEmail-error',
            validationFn: validateEmail,
            errorMessage: 'Please enter a valid email address'
        },
        'editPhone': {
            errorId: 'editPhone-error',
            validationFn: validatePhone,
            errorMessage: 'Please enter a valid 10-digit phone number'
        },
        'editDepartment': {
            errorId: 'editDepartment-error',
            validationFn: validateDepartment,
            errorMessage: 'Please select a department'
        }
    };

    if (!validateForm('editAdvisorForm', editAdvisorValidations)) {
        return;
    }

    const advisorId = document.getElementById('editAdvisorId').value;

    const formData = {
        advisorId: advisorId,
        userId: document.getElementById('editUserId').value,
        firstName: document.getElementById('editFirstName').value,
        lastName: document.getElementById('editLastName').value,
        email: document.getElementById('editEmail').value,
        phoneNumber: document.getElementById('editPhone').value,
        department: document.getElementById('editDepartment').value
    };

    const password = document.getElementById('editPassword').value;
    if (password) {
        formData.password = password;
    }

    showSpinner();

    fetch(`/admin/service-advisors/${advisorId}`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + getToken()
        },
        body: JSON.stringify(formData)
    })
        .then(response => {
            if (!response.ok) {
                throw new Error(`Server responded with status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            hideSpinner();

            const modal = bootstrap.Modal.getInstance(document.getElementById('editAdvisorModal'));
            modal.hide();

            showConfirmation(
                'Service Advisor Updated',
                'The service advisor information has been updated successfully.' +
                (password ? ' A new password has been sent to their email.' : '')
            );

            form.reset();
            loadServiceAdvisors();
        })
        .catch(error => {
            hideSpinner();
            showErrorMessage('editAdvisorForm', 'Failed to update service advisor. Please try again.');
        });
}

function filterServiceAdvisors(searchTerm) {
    currentPage = 1;
    renderServiceAdvisors();
}