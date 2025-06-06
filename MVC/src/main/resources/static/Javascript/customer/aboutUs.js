/**
 * Book Service Modal JavaScript
 * Handles the booking form steps and submission
 */
document.addEventListener('DOMContentLoaded', function() {
    // Modal elements
    const bookServiceModal = document.getElementById('bookServiceModal');
    const bookingForm = document.getElementById('bookServiceForm');
    const vehiclesList = document.getElementById('vehiclesList');
    const newVehicleForm = document.getElementById('newVehicleForm');
    const showAddVehicleBtn = document.getElementById('showAddVehicleBtn');
    const cancelNewVehicleBtn = document.getElementById('cancelNewVehicleBtn');
    const saveNewVehicleBtn = document.getElementById('saveNewVehicleBtn');

    // Step navigation buttons
    const prevStepBtn = document.getElementById('prevStepBtn');
    const nextStepBtn = document.getElementById('nextStepBtn');
    const submitBookingBtn = document.getElementById('submitBookingBtn');

    // Current step tracker
    let currentStep = 1;
    let selectedVehicleId = null;
    let selectedServiceType = null;
    let isAddingNewVehicle = false;

    // Initialize modal
    if (bookServiceModal) {
        const modal = new bootstrap.Modal(bookServiceModal);

        // Initialize when modal is shown
        bookServiceModal.addEventListener('show.bs.modal', function() {
            // Reset form
            resetForm();
            // Load user's vehicles
            loadUserVehicles();
        });

        // Setup event listeners
        setupEventListeners();
    }

    /**
     * Setup all event listeners
     */
    function setupEventListeners() {
        // New vehicle form controls
        if (showAddVehicleBtn) {
            showAddVehicleBtn.addEventListener('click', function() {
                showNewVehicleForm();
            });
        }

        if (cancelNewVehicleBtn) {
            cancelNewVehicleBtn.addEventListener('click', function() {
                hideNewVehicleForm();
            });
        }

        if (saveNewVehicleBtn) {
            saveNewVehicleBtn.addEventListener('click', function() {
                saveNewVehicle();
            });
        }

        // Step navigation
        if (prevStepBtn) {
            prevStepBtn.addEventListener('click', function() {
                goToPreviousStep();
            });
        }

        if (nextStepBtn) {
            nextStepBtn.addEventListener('click', function() {
                goToNextStep();
            });
        }

        if (submitBookingBtn) {
            submitBookingBtn.addEventListener('click', function() {
                submitBooking();
            });
        }

        // Service selection
        document.querySelectorAll('.service-option').forEach(option => {
            option.addEventListener('click', function() {
                selectService(this);
            });
        });
    }

    /**
     * Reset the form to initial state
     */
    function resetForm() {
        // Reset step
        currentStep = 1;
        selectedVehicleId = null;
        selectedServiceType = null;
        isAddingNewVehicle = false;

        // Reset form inputs
        if (bookingForm) {
            bookingForm.reset();
        }

        // Reset vehicle form
        hideNewVehicleForm();

        // Reset service selection
        document.querySelectorAll('.service-option').forEach(option => {
            option.classList.remove('selected');
        });

        // Reset steps
        updateStepsUI();

        // Show step 1
        showStep(1);

        // Reset buttons
        if (prevStepBtn) prevStepBtn.style.display = 'none';
        if (nextStepBtn) nextStepBtn.style.display = '';
        if (submitBookingBtn) submitBookingBtn.style.display = 'none';
    }

    /**
     * Load user's vehicles
     */
    function loadUserVehicles() {
        // Check authentication
        const token = sessionStorage.getItem('authToken');

        if (!token) {
            showVehiclesError('Please log in to book a service');
            return;
        }

        // Show loading state
        if (vehiclesList) {
            vehiclesList.innerHTML = `
                <div class="loading-vehicles text-center py-4">
                    <div class="spinner-border text-wine mb-2" role="status"></div>
                    <p>Loading your vehicles...</p>
                </div>
            `;
        }

        // Fetch user's vehicles from API
        fetch('/customer/api/vehicles', {
            method: 'GET',
            headers: {
                'Authorization': 'Bearer ' + token
            }
        })
            .then(response => {
                if (!response.ok) {
                    throw new Error('Failed to load vehicles');
                }
                return response.json();
            })
            .then(data => {
                renderVehicles(data);
            })
            .catch(error => {
                console.error('Error loading vehicles:', error);
                showVehiclesError('Failed to load your vehicles. Please try again.');
            });
    }

    /**
     * Render vehicles in the grid
     */
    function renderVehicles(vehicles) {
        if (!vehiclesList) return;

        if (!vehicles || vehicles.length === 0) {
            vehiclesList.innerHTML = `
                <div class="no-vehicles text-center py-4">
                    <div class="empty-state-icon mb-3">
                        <i class="bi bi-car-front" style="font-size: 3rem; color: #d1d5db;"></i>
                    </div>
                    <h5>No Vehicles Found</h5>
                    <p class="text-muted">You don't have any vehicles registered yet. Add your first vehicle to book a service.</p>
                </div>
            `;
            return;
        }

        let html = '';

        vehicles.forEach(vehicle => {
            const vehicleIcon = vehicle.category === 'Bike' ? 'bi-bicycle' :
                (vehicle.category === 'Truck' ? 'bi-truck' : 'bi-car-front');

            html += `
                <div class="vehicle-card" data-vehicle-id="${vehicle.vehicleId}">
                    <div class="vehicle-icon">
                        <i class="bi ${vehicleIcon}"></i>
                    </div>
                    <h5 class="vehicle-name">${vehicle.brand} ${vehicle.model}</h5>
                    <div class="vehicle-details">
                        ${vehicle.registrationNumber}<br>
                        ${vehicle.year}
                    </div>
                </div>
            `;
        });

        vehiclesList.innerHTML = html;

        // Add click event listeners to vehicle cards
        document.querySelectorAll('.vehicle-card').forEach(card => {
            card.addEventListener('click', function() {
                selectVehicle(this);
            });
        });
    }

    /**
     * Show error when loading vehicles
     */
    function showVehiclesError(message) {
        if (!vehiclesList) return;

        vehiclesList.innerHTML = `
            <div class="alert alert-danger text-center" role="alert">
                <i class="bi bi-exclamation-triangle me-2"></i>
                ${message}
            </div>
        `;
    }

    /**
     * Select a vehicle
     */
    function selectVehicle(vehicleCard) {
        // Remove selection from all cards
        document.querySelectorAll('.vehicle-card').forEach(card => {
            card.classList.remove('selected');
        });

        // Add selection to clicked card
        vehicleCard.classList.add('selected');

        // Store selected vehicle ID
        selectedVehicleId = vehicleCard.dataset.vehicleId;
    }

    /**
     * Select a service
     */
    function selectService(serviceOption) {
        // Remove selection from all options
        document.querySelectorAll('.service-option').forEach(option => {
            option.classList.remove('selected');
        });

        // Add selection to clicked option
        serviceOption.classList.add('selected');

        // Store selected service type
        selectedServiceType = serviceOption.dataset.service;
        document.getElementById('selectedService').value = selectedServiceType;
    }

    /**
     * Show new vehicle form
     */
    function showNewVehicleForm() {
        if (newVehicleForm) {
            newVehicleForm.style.display = 'block';

            // Deselect any selected vehicle
            document.querySelectorAll('.vehicle-card').forEach(card => {
                card.classList.remove('selected');
            });

            selectedVehicleId = null;
            isAddingNewVehicle = true;
        }
    }

    /**
     * Hide new vehicle form
     */
    function hideNewVehicleForm() {
        if (newVehicleForm) {
            newVehicleForm.style.display = 'none';
            isAddingNewVehicle = false;

            // Reset form
            const inputs = newVehicleForm.querySelectorAll('input, select');
            inputs.forEach(input => {
                input.value = '';
            });
        }
    }

    /**
     * Save new vehicle
     */
    function saveNewVehicle() {
        // Get form data
        const brand = document.getElementById('vehicleBrand').value;
        const model = document.getElementById('vehicleModel').value;
        const regNumber = document.getElementById('vehicleRegNumber').value;
        const year = document.getElementById('vehicleYear').value;
        const category = document.getElementById('vehicleCategory').value;

        // Validate inputs
        if (!brand || !model || !regNumber || !year || !category) {
            showToast('Please fill in all required fields', 'error');
            return;
        }

        // Prepare vehicle data
        const vehicleData = {
            brand: brand,
            model: model,
            registrationNumber: regNumber,
            year: parseInt(year),
            category: category
        };

        // Get token
        const token = sessionStorage.getItem('authToken');

        if (!token) {
            showToast('Authentication error. Please log in again.', 'error');
            return;
        }

        // Show loading indicator
        saveNewVehicleBtn.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Saving...';
        saveNewVehicleBtn.disabled = true;

        // Call API to save vehicle
        fetch('/customer/api/vehicles', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer ' + token
            },
            body: JSON.stringify(vehicleData)
        })
            .then(response => {
                if (!response.ok) {
                    throw new Error('Failed to save vehicle');
                }
                return response.json();
            })
            .then(data => {
                // Show success message
                showToast('Vehicle added successfully', 'success');

                // Hide form
                hideNewVehicleForm();

                // Refresh vehicles list
                loadUserVehicles();

                // Reset button
                saveNewVehicleBtn.innerHTML = 'Save Vehicle';
                saveNewVehicleBtn.disabled = false;
            })
            .catch(error => {
                console.error('Error saving vehicle:', error);
                showToast('Failed to save vehicle. Please try again.', 'error');

                // Reset button
                saveNewVehicleBtn.innerHTML = 'Save Vehicle';
                saveNewVehicleBtn.disabled = false;
            });
    }

    /**
     * Go to next step
     */
    function goToNextStep() {
        // Validate current step
        if (!validateStep(currentStep)) {
            return;
        }

        // Move to next step
        currentStep++;

        // Update UI
        updateStepsUI();
        showStep(currentStep);

        // Update navigation buttons
        updateNavigationButtons();

        // If last step, update summary
        if (currentStep === 4) {
            updateSummary();
        }
    }

    /**
     * Go to previous step
     */
    function goToPreviousStep() {
        if (currentStep > 1) {
            currentStep--;

            // Update UI
            updateStepsUI();
            showStep(currentStep);

            // Update navigation buttons
            updateNavigationButtons();
        }
    }

    /**
     * Update steps UI
     */
    function updateStepsUI() {
        // Update step indicators
        document.querySelectorAll('.booking-step').forEach(step => {
            const stepNumber = parseInt(step.dataset.step);

            step.classList.remove('active', 'completed');

            if (stepNumber === currentStep) {
                step.classList.add('active');
            } else if (stepNumber < currentStep) {
                step.classList.add('completed');
            }
        });
    }

    /**
     * Show step content
     */
    function showStep(stepNumber) {
        // Hide all steps
        document.querySelectorAll('.booking-step-content').forEach(content => {
            content.style.display = 'none';
        });

        // Show current step
        const currentStepContent = document.getElementById(`step-${stepNumber}`);
        if (currentStepContent) {
            currentStepContent.style.display = 'block';
        }
    }

    /**
     * Update navigation buttons
     */
    function updateNavigationButtons() {
        if (!prevStepBtn || !nextStepBtn || !submitBookingBtn) return;

        // Previous button
        prevStepBtn.style.display = currentStep > 1 ? '' : 'none';

        // Next button
        nextStepBtn.style.display = currentStep < 4 ? '' : 'none';

        // Submit button
        submitBookingBtn.style.display = currentStep === 4 ? '' : 'none';
    }

    /**
     * Validate current step
     */
    function validateStep(step) {
        switch (step) {
            case 1:
                // Validate vehicle selection
                if (!selectedVehicleId && !isAddingNewVehicle) {
                    showToast('Please select a vehicle or add a new one', 'error');
                    return false;
                }

                if (isAddingNewVehicle) {
                    // Validate new vehicle form
                    const brand = document.getElementById('vehicleBrand').value;
                    const model = document.getElementById('vehicleModel').value;
                    const regNumber = document.getElementById('vehicleRegNumber').value;
                    const year = document.getElementById('vehicleYear').value;
                    const category = document.getElementById('vehicleCategory').value;

                    if (!brand || !model || !regNumber || !year || !category) {
                        showToast('Please complete the vehicle information', 'error');
                        return false;
                    }

                    // Save new vehicle first
                    saveNewVehicle();
                    return false; // Stop progression until vehicle is saved
                }

                return true;

            case 2:
                // Validate service selection
                if (!selectedServiceType) {
                    showToast('Please select a service type', 'error');
                    return false;
                }
                return true;

            case 3:
                // Validate service details
                const deliveryDate = document.getElementById('deliveryDate').value;

                if (!deliveryDate) {
                    showToast('Please select a preferred service date', 'error');
                    return false;
                }

                // Validate date is not in the past
                const today = new Date();
                today.setHours(0, 0, 0, 0);
                const selectedDate = new Date(deliveryDate);
                selectedDate.setHours(0, 0, 0, 0);

                if (selectedDate < today) {
                    showToast('Please select a future date', 'error');
                    return false;
                }

                return true;

            default:
                return true;
        }
    }

    /**
     * Update summary information
     */
    function updateSummary() {
        // Get vehicle info
        let vehicleInfo = '';
        if (selectedVehicleId) {
            const vehicleCard = document.querySelector(`.vehicle-card[data-vehicle-id="${selectedVehicleId}"]`);
            if (vehicleCard) {
                const vehicleName = vehicleCard.querySelector('.vehicle-name').textContent;
                const vehicleDetails = vehicleCard.querySelector('.vehicle-details').textContent.trim().split('\n')[0];
                vehicleInfo = `${vehicleName} (${vehicleDetails})`;
            }
        } else if (isAddingNewVehicle) {
            const brand = document.getElementById('vehicleBrand').value;
            const model = document.getElementById('vehicleModel').value;
            const regNumber = document.getElementById('vehicleRegNumber').value;
            vehicleInfo = `${brand} ${model} (${regNumber})`;
        }

        // Get service info
        const serviceInfo = selectedServiceType || '';

        // Get date and time
        const deliveryDate = document.getElementById('deliveryDate').value;
        const preferredTime = document.getElementById('preferredTime').value;
        const formattedDate = deliveryDate ? new Date(deliveryDate).toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'long',
            day: 'numeric'
        }) : '';

        // Get additional info
        const description = document.getElementById('serviceDescription').value || 'No additional information provided';

        // Update summary
        document.getElementById('summaryVehicle').textContent = vehicleInfo;
        document.getElementById('summaryService').textContent = serviceInfo;
        document.getElementById('summaryDate').textContent = formattedDate;
        document.getElementById('summaryTime').textContent = preferredTime || 'Any time';
        document.getElementById('summaryDescription').textContent = description;
    }

    /**
     * Submit booking
     */
    function submitBooking() {
        // Validate terms checkbox
        const termsCheck = document.getElementById('termsCheck');
        if (termsCheck && !termsCheck.checked) {
            showToast('Please agree to the terms and conditions', 'error');
            return;
        }

        // Get form data
        const deliveryDate = document.getElementById('deliveryDate').value;
        const preferredTime = document.getElementById('preferredTime').value || null;
        const description = document.getElementById('serviceDescription').value || '';

        // Prepare booking data
        const bookingData = {
            vehicleId: selectedVehicleId,
            serviceType: selectedServiceType,
            deliveryDate: deliveryDate,
            preferredTime: preferredTime,
            additionalDescription: description
        };

        // If adding new vehicle, include vehicle data
        if (isAddingNewVehicle) {
            bookingData.newVehicle = {
                brand: document.getElementById('vehicleBrand').value,
                model: document.getElementById('vehicleModel').value,
                registrationNumber: document.getElementById('vehicleRegNumber').value,
                year: parseInt(document.getElementById('vehicleYear').value),
                category: document.getElementById('vehicleCategory').value
            };
        }

        // Get token
        const token = sessionStorage.getItem('authToken');

        if (!token) {
            showToast('Authentication error. Please log in again.', 'error');
            return;
        }

        // Show loading indicator
        submitBookingBtn.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Processing...';
        submitBookingBtn.disabled = true;

        // Call API to save booking
        fetch('/customer/api/service-requests', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer ' + token
            },
            body: JSON.stringify(bookingData)
        })
            .then(response => {
                if (!response.ok) {
                    throw new Error('Failed to create booking');
                }
                return response.json();
            })
            .then(data => {
                // Hide current modal
                const bookServiceModalInstance = bootstrap.Modal.getInstance(bookServiceModal);
                if (bookServiceModalInstance) {
                    bookServiceModalInstance.hide();
                }

                // Show success modal
                showBookingSuccess(data);

                // Reset form for next time
                resetForm();

                // Reset button
                submitBookingBtn.innerHTML = '<i class="bi bi-check-circle me-1"></i> Confirm Booking';
                submitBookingBtn.disabled = false;
            })
            .catch(error => {
                console.error('Error creating booking:', error);
                showToast('Failed to create booking. Please try again.', 'error');

                // Reset button
                submitBookingBtn.innerHTML = '<i class="bi bi-check-circle me-1"></i> Confirm Booking';
                submitBookingBtn.disabled = false;
            });
    }

    /**
     * Show booking success modal
     */
    function showBookingSuccess(bookingData) {
        const successModal = document.getElementById('bookingSuccessModal');
        if (successModal) {
            // Set booking reference
            const bookingReference = document.getElementById('bookingReference');
            if (bookingReference) {
                // Generate a reference if not provided by API
                const reference = bookingData.reference ||
                    `ALB-${Math.floor(1000 + Math.random() * 9000)}-${new Date().getFullYear()}`;
                bookingReference.textContent = reference;
            }

            // Show modal
            const successModalInstance = new bootstrap.Modal(successModal);
            successModalInstance.show();
        }
    }

    /**
     * Show toast notification
     */
    function showToast(message, type = 'info') {
        // Check if toast container exists, if not create it
        let toastContainer = document.querySelector('.toast-container');
        if (!toastContainer) {
            toastContainer = document.createElement('div');
            toastContainer.className = 'toast-container position-fixed bottom-0 end-0 p-3';
            document.body.appendChild(toastContainer);
        }

        // Create toast element
        const toastId = 'toast-' + Date.now();
        const toastHtml = `
            <div id="${toastId}" class="toast align-items-center text-white bg-${type === 'error' ? 'danger' : type} border-0" role="alert" aria-live="assertive" aria-atomic="true">
                <div class="d-flex">
                    <div class="toast-body">
                        <i class="bi bi-${type === 'error' ? 'exclamation-triangle' : type === 'success' ? 'check-circle' : 'info-circle'} me-2"></i>
                        ${message}
                    </div>
                    <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
                </div>
            </div>
        `;

        // Add toast to container
        toastContainer.insertAdjacentHTML('beforeend', toastHtml);

        // Initialize and show toast
        const toastElement = document.getElementById(toastId);
        const toast = new bootstrap.Toast(toastElement, {
            autohide: true,
            delay: 5000
        });
        toast.show();

        // Remove toast after it's hidden
        toastElement.addEventListener('hidden.bs.toast', function() {
            toastElement.remove();
        });
    }
});