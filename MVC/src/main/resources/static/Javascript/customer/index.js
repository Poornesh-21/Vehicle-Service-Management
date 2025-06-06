
    /**
    * Vehicle Service Booking System
    * Complete solution for Albany Vehicle Service Booking
    */
    class BookingSystem {
    constructor() {
    this.state = {
    currentStep: 1,
    totalSteps: 4,
    selectedVehicleId: null,
    vehicles: [],
    newVehicle: null,
    selectedService: null,
    formData: {}
};

    this.elements = {};
    this.apiEndpoints = {
    vehicles: '/customer/api/vehicles',
    newVehicle: '/customer/api/vehicles/add',
    bookService: '/customer/api/service-requests'
};

    this.init();
}

    /**
     * Initialize the booking system
     */
    init() {
    this.cacheElements();
    this.setupEventListeners();
    this.setupBookingSteps();
    this.initializeDateInputs();
    this.initUI();
}

    /**
     * Initialize UI components
     */
    initUI() {
    if (typeof AOS !== 'undefined') {
    AOS.init({
    duration: 800,
    easing: 'ease-in-out',
    once: true
});
}

    // Handle main navigation
    this.setupNavigation();

    // Set up book service buttons
    this.setupBookServiceButtons();
}

    /**
     * Setup navigation and scroll behavior
     */
    setupNavigation() {
    // Handle navbar scroll effect
    window.addEventListener('scroll', this.handleNavbarScroll);
    this.handleNavbarScroll();

    // Set active nav item based on current page
    this.setActiveNavItem();
}

    /**
     * Handle navbar scroll effect
     */
    handleNavbarScroll() {
    const navbar = document.querySelector('.navbar');
    if (navbar) {
    navbar.classList.toggle('scrolled', window.scrollY > 50);
}
}

    /**
     * Set active navigation item
     */
    setActiveNavItem() {
    const currentPath = window.location.pathname;
    const navLinks = document.querySelectorAll('.navbar-nav .nav-link');

    navLinks.forEach(link => {
    link.classList.remove('active');
});

    navLinks.forEach(link => {
    const href = link.getAttribute('href');

    if (href && href.startsWith('#') && currentPath === '/') {
    return;} else if (href && href.startsWith('/')) {
    if (currentPath === href || currentPath.startsWith(href) && href !== '/') {
    link.classList.add('active');
}
}
});

    if (currentPath === '/' || currentPath === '') {
    const homeLink = document.querySelector('.navbar-nav .nav-link[href="/"]');
    if (homeLink) {
    homeLink.classList.add('active');
}
}
}

    /**
     * Setup book service buttons
     */
    setupBookServiceButtons() {
    const bookButtons = document.querySelectorAll('#bookServiceBtn, #ctaBookServiceBtn');

    bookButtons.forEach(btn => {
    btn.addEventListener('click', this.showBookingModal.bind(this));
});
}

    /**
     * Show booking modal
     */
    showBookingModal() {
    const isLoggedIn = this.isUserLoggedIn();

    if (isLoggedIn) {
    const bookServiceModal = new bootstrap.Modal(document.getElementById('bookServiceModal'));
    bookServiceModal.show();
} else {
    window.location.href = '/authentication/login?message=' +
    encodeURIComponent('Please login to book a service') +
    '&type=info&redirect=/customer';
}
}

    /**
     * Check if user is logged in
     */
    isUserLoggedIn() {
    return sessionStorage.getItem('authToken') !== null;
}

    /**
     * Cache DOM elements for better performance
     */
    cacheElements() {
    this.elements = {
    modal: document.getElementById('bookServiceModal'),
    form: document.getElementById('bookServiceForm'),
    steps: document.querySelectorAll('.booking-steps .step'),
    stepContents: document.querySelectorAll('.booking-step-content'),
    nextBtn: document.getElementById('nextStepBtn'),
    prevBtn: document.getElementById('prevStepBtn'),
    submitBtn: document.getElementById('submitBookingBtn'),

    vehiclesList: document.getElementById('vehiclesList'),
    showAddVehicleBtn: document.getElementById('showAddVehicleBtn'),
    newVehicleForm: document.getElementById('newVehicleForm'),
    saveVehicleBtn: document.getElementById('saveNewVehicleBtn'),
    cancelVehicleBtn: document.getElementById('cancelNewVehicleBtn'),

    serviceOptions: document.querySelectorAll('.service-option'),
    selectedServiceInput: document.getElementById('selectedService'),

    deliveryDate: document.getElementById('deliveryDate'),
    preferredTime: document.getElementById('preferredTime'),
    serviceDescription: document.getElementById('serviceDescription'),

    summaryVehicle: document.getElementById('summaryVehicle'),
    summaryService: document.getElementById('summaryService'),
    summaryDate: document.getElementById('summaryDate'),
    summaryTime: document.getElementById('summaryTime'),
    summaryDescription: document.getElementById('summaryDescription'),
    termsCheck: document.getElementById('termsCheck')
};
}

    /**
     * Set up event listeners
     */
    setupEventListeners() {
    if (this.elements.modal) {
    this.elements.modal.addEventListener('show.bs.modal', this.onModalShow.bind(this));
}

    if (this.elements.nextBtn) {
    this.elements.nextBtn.addEventListener('click', this.goToNextStep.bind(this));
}

    if (this.elements.prevBtn) {
    this.elements.prevBtn.addEventListener('click', this.goToPreviousStep.bind(this));
}

    if (this.elements.submitBtn) {
    this.elements.submitBtn.addEventListener('click', this.submitBooking.bind(this));
}

    if (this.elements.showAddVehicleBtn) {
    this.elements.showAddVehicleBtn.addEventListener('click', this.showAddVehicleForm.bind(this));
}

    if (this.elements.saveVehicleBtn) {
    this.elements.saveVehicleBtn.addEventListener('click', this.saveNewVehicle.bind(this));
}

    if (this.elements.cancelVehicleBtn) {
    this.elements.cancelVehicleBtn.addEventListener('click', this.hideAddVehicleForm.bind(this));
}

    if (this.elements.serviceOptions) {
    this.elements.serviceOptions.forEach(option => {
    option.addEventListener('click', () => this.selectService(option));
});
}
}

    /**
     * Set up booking steps
     */
    setupBookingSteps() {
    const today = new Date();
    const formattedDate = today.toISOString().split('T')[0];
    if (this.elements.deliveryDate) {
    this.elements.deliveryDate.min = formattedDate;
}
}

    /**
     * Initialize date inputs
     */
    initializeDateInputs() {
    if (this.elements.deliveryDate) {
    const today = new Date();
    this.elements.deliveryDate.min = today.toISOString().split('T')[0];

    const tomorrow = new Date();
    tomorrow.setDate(today.getDate() + 1);
    this.elements.deliveryDate.value = tomorrow.toISOString().split('T')[0];
}
}

    /**
     * Handle modal show event
     */
    onModalShow() {
    this.resetForm();
    this.loadUserVehicles();
}

    /**
     * Reset form to initial state
     */
    resetForm() {
    this.state.currentStep = 1;
    this.state.selectedVehicleId = null;
    this.state.selectedService = null;
    this.state.formData = {};

    if (this.elements.form) {
    this.elements.form.reset();
}

    this.updateStepsUI();
    this.hideAddVehicleForm();

    if (this.elements.serviceOptions) {
    this.elements.serviceOptions.forEach(option => {
    option.classList.remove('selected');
});
}

    if (this.elements.selectedServiceInput) {
    this.elements.selectedServiceInput.value = '';
}

    if (this.elements.summaryVehicle) this.elements.summaryVehicle.textContent = '';
    if (this.elements.summaryService) this.elements.summaryService.textContent = '';
    if (this.elements.summaryDate) this.elements.summaryDate.textContent = '';
    if (this.elements.summaryTime) this.elements.summaryTime.textContent = '';
    if (this.elements.summaryDescription) this.elements.summaryDescription.textContent = '';
}

    /**
     * Load user vehicles from API
     */
    loadUserVehicles() {
    const token = sessionStorage.getItem('authToken');
    if (!token) {
    this.showVehiclesError('Authentication required');
    return;
}

    if (this.elements.vehiclesList) {
    this.elements.vehiclesList.innerHTML = `
                    <div class="loading-vehicles text-center py-4">
                        <div class="spinner-border text-primary mb-2" role="status"></div>
                        <p>Loading your vehicles...</p>
                    </div>
                `;
}

    fetch(this.apiEndpoints.vehicles, {
    headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
}
})
    .then(response => {
    if (!response.ok) {
    throw new Error('Failed to load vehicles');
}
    return response.json();
})
    .then(data => {
    this.state.vehicles = Array.isArray(data) ? data : [];
    this.renderVehiclesList();
})
    .catch(error => {
    console.error('Error loading vehicles:', error);
    this.showVehiclesError(error.message);
});
}

    /**
     * Render the list of vehicles
     */
    renderVehiclesList() {
    if (!this.elements.vehiclesList) return;

    if (!this.state.vehicles || this.state.vehicles.length === 0) {
    this.elements.vehiclesList.innerHTML = `
                    <div class="no-vehicles text-center py-4">
                        <div class="mb-3">
                            <i class="bi bi-car-front text-muted" style="font-size: 3rem;"></i>
                        </div>
                        <h5>No vehicles found</h5>
                        <p class="text-muted">Add a vehicle to continue</p>
                    </div>
                `;
    return;
}

    let html = '<div class="row g-3">';

    this.state.vehicles.forEach(vehicle => {
    const vehicleId = vehicle.vehicleId;
    const vehicleName = `${vehicle.brand} ${vehicle.model}`;
    const vehicleYear = vehicle.year || '';
    const vehicleReg = vehicle.registrationNumber || '';

    html += `
                    <div class="col-md-6">
                        <div class="vehicle-card ${this.state.selectedVehicleId === vehicleId ? 'selected' : ''}"
                             data-vehicle-id="${vehicleId}">
                            <div class="vehicle-icon">
                                <i class="bi bi-${vehicle.category === 'Bike' ? 'bicycle' : 'car-front'}"></i>
                            </div>
                            <div class="vehicle-info">
                                <h5>${this.escapeHtml(vehicleName)}</h5>
                                <p>${vehicleYear} · ${this.escapeHtml(vehicleReg)}</p>
                            </div>
                            <div class="vehicle-select">
                                <i class="bi bi-check-circle-fill"></i>
                            </div>
                        </div>
                    </div>
                `;
});

    html += '</div>';
    this.elements.vehiclesList.innerHTML = html;

    // Add event listeners to vehicle cards
    document.querySelectorAll('.vehicle-card').forEach(card => {
    card.addEventListener('click', () => {
    const vehicleId = parseInt(card.getAttribute('data-vehicle-id'), 10);
    this.selectVehicle(vehicleId);
});
});
}

    /**
     * Show error message when loading vehicles fails
     */
    showVehiclesError(message) {
    if (!this.elements.vehiclesList) return;

    this.elements.vehiclesList.innerHTML = `
                <div class="alert alert-danger">
                    <i class="bi bi-exclamation-triangle-fill me-2"></i>
                    ${message || 'Failed to load vehicles'}
                </div>
                <div class="text-center">
                    <p>Please try adding a new vehicle instead</p>
                </div>
            `;
}

    /**
     * Select a vehicle
     */
    selectVehicle(vehicleId) {
    this.state.selectedVehicleId = vehicleId;

    document.querySelectorAll('.vehicle-card').forEach(card => {
    card.classList.remove('selected');
});

    const selectedCard = document.querySelector(`.vehicle-card[data-vehicle-id="${vehicleId}"]`);
    if (selectedCard) {
    selectedCard.classList.add('selected');
}

    if (this.elements.nextBtn) {
    this.elements.nextBtn.disabled = false;
}
}

    /**
     * Show the add vehicle form
     */
    showAddVehicleForm() {
    if (this.elements.newVehicleForm) {
    this.elements.newVehicleForm.style.display = 'block';
}

    if (this.elements.showAddVehicleBtn) {
    this.elements.showAddVehicleBtn.style.display = 'none';
}
}

    /**
     * Hide the add vehicle form
     */
    hideAddVehicleForm() {
    if (this.elements.newVehicleForm) {
    this.elements.newVehicleForm.style.display = 'none';
}

    if (this.elements.showAddVehicleBtn) {
    this.elements.showAddVehicleBtn.style.display = 'block';
}
}

    /**
     * Save a new vehicle
     */
    saveNewVehicle() {
    const brand = document.getElementById('vehicleBrand').value.trim();
    const model = document.getElementById('vehicleModel').value.trim();
    const regNumber = document.getElementById('vehicleRegNumber').value.trim();
    const year = document.getElementById('vehicleYear').value;
    const category = document.getElementById('vehicleCategory').value;

    if (!brand || !model || !regNumber || !year || !category) {
    this.showToast('Please fill all required fields', 'error');
    return;
}

    const vehicleData = {
    brand,
    model,
    registrationNumber: regNumber,
    year: parseInt(year, 10),
    category
};

    const token = sessionStorage.getItem('authToken');
    if (!token) {
    this.showToast('Authentication required', 'error');
    return;
}

    if (this.elements.saveVehicleBtn) {
    this.elements.saveVehicleBtn.disabled = true;
    this.elements.saveVehicleBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>Saving...';
}

    fetch(this.apiEndpoints.newVehicle, {
    method: 'POST',
    headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
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
    this.state.vehicles.push(data);
    this.state.selectedVehicleId = data.vehicleId;
    this.renderVehiclesList();
    this.hideAddVehicleForm();

    // Reset form fields
    if (document.getElementById('vehicleBrand')) document.getElementById('vehicleBrand').value = '';
    if (document.getElementById('vehicleModel')) document.getElementById('vehicleModel').value = '';
    if (document.getElementById('vehicleRegNumber')) document.getElementById('vehicleRegNumber').value = '';
    if (document.getElementById('vehicleYear')) document.getElementById('vehicleYear').value = '';
    if (document.getElementById('vehicleCategory')) document.getElementById('vehicleCategory').value = '';

    this.showToast('Vehicle added successfully', 'success');
})
    .catch(error => {
    console.error('Error saving vehicle:', error);
    this.showToast(error.message || 'Failed to save vehicle', 'error');
})
    .finally(() => {
    if (this.elements.saveVehicleBtn) {
    this.elements.saveVehicleBtn.disabled = false;
    this.elements.saveVehicleBtn.innerHTML = 'Save Vehicle';
}
});
}

    /**
     * Select a service type
     */
    selectService(serviceOption) {
    const serviceType = serviceOption.getAttribute('data-service');
    this.state.selectedService = serviceType;

    if (this.elements.serviceOptions) {
    this.elements.serviceOptions.forEach(option => {
    option.classList.remove('selected');
});
}

    serviceOption.classList.add('selected');

    if (this.elements.selectedServiceInput) {
    this.elements.selectedServiceInput.value = serviceType;
}

    if (this.elements.nextBtn) {
    this.elements.nextBtn.disabled = false;
}
}

    /**
     * Go to the next step
     */
    goToNextStep() {
    if (!this.validateCurrentStep()) {
    return;
}

    if (this.state.currentStep < this.state.totalSteps) {
    this.state.currentStep++;
    this.updateStepsUI();

    if (this.state.currentStep === 4) {
    this.updateConfirmationSummary();
}
}
}

    /**
     * Go to the previous step
     */
    goToPreviousStep() {
    if (this.state.currentStep > 1) {
    this.state.currentStep--;
    this.updateStepsUI();
}
}

    /**
     * Update the UI to reflect the current step
     */
    updateStepsUI() {
    if (this.elements.steps) {
    this.elements.steps.forEach((step, index) => {
    const stepNum = index + 1;
    step.classList.remove('active', 'completed');

    if (stepNum < this.state.currentStep) {
    step.classList.add('completed');
} else if (stepNum === this.state.currentStep) {
    step.classList.add('active');
}
});
}

    if (this.elements.stepContents) {
    this.elements.stepContents.forEach((content, index) => {
    const stepNum = index + 1;
    content.style.display = stepNum === this.state.currentStep ? 'block' : 'none';
});
}

    if (this.elements.prevBtn) {
    this.elements.prevBtn.style.display = this.state.currentStep > 1 ? 'block' : 'none';
}

    if (this.elements.nextBtn) {
    this.elements.nextBtn.style.display = this.state.currentStep < this.state.totalSteps ? 'block' : 'none';
}

    if (this.elements.submitBtn) {
    this.elements.submitBtn.style.display = this.state.currentStep === this.state.totalSteps ? 'block' : 'none';
}
}

    /**
     * Validate the current step
     */
    validateCurrentStep() {
    switch (this.state.currentStep) {
    case 1:
    if (!this.state.selectedVehicleId &&
    this.elements.newVehicleForm &&
    this.elements.newVehicleForm.style.display === 'none') {
    this.showToast('Please select a vehicle or add a new one', 'error');
    return false;
}
    return true;

    case 2:
    if (!this.state.selectedService) {
    this.showToast('Please select a service type', 'error');
    return false;
}
    return true;

    case 3:
    if (this.elements.deliveryDate && !this.elements.deliveryDate.value) {
    this.showToast('Please select a preferred service date', 'error');
    return false;
}

    this.state.formData = {
    vehicleId: this.state.selectedVehicleId,
    serviceType: this.state.selectedService,
    deliveryDate: this.elements.deliveryDate ? this.elements.deliveryDate.value : null,
    preferredTime: this.elements.preferredTime ? this.elements.preferredTime.value : null,
    additionalDescription: this.elements.serviceDescription ? this.elements.serviceDescription.value : null
};

    return true;

    case 4:
    if (this.elements.termsCheck && !this.elements.termsCheck.checked) {
    this.showToast('Please agree to the terms and conditions', 'error');
    return false;
}
    return true;
}

    return true;
}

    /**
     * Update the confirmation summary
     */
    updateConfirmationSummary() {
    const selectedVehicle = this.state.vehicles.find(v => v.vehicleId === this.state.selectedVehicleId);

    if (selectedVehicle && this.elements.summaryVehicle) {
    this.elements.summaryVehicle.textContent = `${selectedVehicle.brand} ${selectedVehicle.model} (${selectedVehicle.registrationNumber})`;
}

    if (this.elements.summaryService) {
    this.elements.summaryService.textContent = this.state.selectedService || 'N/A';
}

    if (this.elements.deliveryDate && this.elements.summaryDate) {
    const deliveryDate = this.elements.deliveryDate.value;
    if (deliveryDate) {
    const formattedDate = new Date(deliveryDate).toLocaleDateString('en-US', {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric'
});
    this.elements.summaryDate.textContent = formattedDate;
} else {
    this.elements.summaryDate.textContent = 'N/A';
}
}

    if (this.elements.preferredTime && this.elements.summaryTime) {
    this.elements.summaryTime.textContent = this.elements.preferredTime.value || 'Any time';
}

    if (this.elements.serviceDescription && this.elements.summaryDescription) {
    this.elements.summaryDescription.textContent = this.elements.serviceDescription.value || 'No additional information provided';
}
}

    /**
     * Submit the booking
     */
    submitBooking() {
    if (!this.validateCurrentStep()) {
    return;
}

    const token = sessionStorage.getItem('authToken');
    if (!token) {
    this.showToast('Authentication required', 'error');
    return;
}

    if (this.elements.submitBtn) {
    this.elements.submitBtn.disabled = true;
    this.elements.submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>Processing...';
}

    const requestData = {
    vehicleId: this.state.selectedVehicleId,
    serviceType: this.state.selectedService,
    deliveryDate: this.elements.deliveryDate ? this.elements.deliveryDate.value : null,
    additionalDescription: this.elements.serviceDescription ? this.elements.serviceDescription.value : null
};

    fetch(this.apiEndpoints.bookService, {
    method: 'POST',
    headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
},
    body: JSON.stringify(requestData)
})
    .then(response => {
    if (!response.ok) {
    throw new Error('Failed to book service');
}
    return response.json();
})
    .then(data => {
    if (this.elements.modal) {
    const bookingModal = bootstrap.Modal.getInstance(this.elements.modal);
    if (bookingModal) {
    bookingModal.hide();
}
}

    this.showBookingSuccess(data);
})
    .catch(error => {
    console.error('Error booking service:', error);
    this.showToast(error.message || 'Failed to book service', 'error');
})
    .finally(() => {
    if (this.elements.submitBtn) {
    this.elements.submitBtn.disabled = false;
    this.elements.submitBtn.innerHTML = '<i class="bi bi-check-circle me-1"></i> Confirm Booking';
}
});
}

    /**
     * Show booking success modal
     */
    showBookingSuccess(bookingData) {
    const bookingReference = bookingData.requestId ?
    `ALB-${bookingData.requestId}-${new Date().getFullYear()}` :
    `ALB-${Math.floor(Math.random() * 10000)}-${new Date().getFullYear()}`;

    const successModal = new bootstrap.Modal(document.getElementById('bookingSuccessModal'));

    if (document.getElementById('bookingReference')) {
    document.getElementById('bookingReference').textContent = bookingReference;
}

    if (successModal) {
    successModal.show();
}
}

    /**
     * Show toast notification
     */
    showToast(message, type = 'info') {
    let toastContainer = document.querySelector('.toast-container');
    if (!toastContainer) {
    toastContainer = document.createElement('div');
    toastContainer.className = 'toast-container position-fixed top-0 end-0 p-3';
    document.body.appendChild(toastContainer);
}

    const toastId = 'toast-' + Date.now();
    const toastHTML = `
                <div id="${toastId}" class="toast align-items-center text-white bg-${type === 'error' ? 'danger' : type === 'success' ? 'success' : 'info'} border-0" role="alert" aria-live="assertive" aria-atomic="true">
                    <div class="d-flex">
                        <div class="toast-body">
                            ${message}
                        </div>
                        <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
                    </div>
                </div>
            `;

    toastContainer.insertAdjacentHTML('beforeend', toastHTML);

    const toastElement = document.getElementById(toastId);
    if (toastElement) {
    const toast = new bootstrap.Toast(toastElement, {
    autohide: true,
    delay: 5000
});
    toast.show();

    toastElement.addEventListener('hidden.bs.toast', function() {
    toastElement.remove();
});
}
}

    /**
     * Helper method to escape HTML special characters
     */
    escapeHtml(text) {
    if (!text) return '';
    return text
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}
}

    // Initialize the system when the page loads
    document.addEventListener('DOMContentLoaded', function() {
    window.bookingSystem = new BookingSystem();
});