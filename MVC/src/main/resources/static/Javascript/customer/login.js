/**
 * Customer Authentication Script for Login Page
 * Handles login and registration with OTP verification
 */
document.addEventListener('DOMContentLoaded', function() {
    // References to DOM elements
    const loginTab = document.getElementById('login-tab');
    const registerTab = document.getElementById('register-tab');
    const loginForm = document.getElementById('login-form');
    const registerForm = document.getElementById('register-form');
    const otpForm = document.getElementById('otp-form');
    const switchToRegister = document.getElementById('switch-to-register');
    const switchToLogin = document.getElementById('switch-to-login');
    const displayEmail = document.getElementById('display-email');
    const changeEmail = document.getElementById('change-email');
    const editEmail = document.getElementById('edit-email');
    const otpInputs = document.querySelectorAll('.otp-input');
    const countdownElement = document.getElementById('countdown');
    const resendOtpButton = document.getElementById('resend-otp');

    // State variables
    let currentForm = 'login';
    let countdownInterval;
    let timerSeconds = 30;
    let otpAction = ''; // 'login' or 'register'
    let registrationData = null;

    /**
     * Clear all form errors
     */
    function clearErrors() {
        // Remove invalid class from all inputs
        document.querySelectorAll('.form-control').forEach(input => {
            input.classList.remove('is-invalid');
        });

        // Clear all error messages
        document.querySelectorAll('.invalid-feedback').forEach(error => {
            error.textContent = '';
        });
    }

    /**
     * Clear OTP errors
     */
    function clearOtpErrors() {
        const errorContainer = document.querySelector('.otp-error');
        if (errorContainer) {
            errorContainer.style.display = 'none';
        }
    }

    /**
     * Show toast message (for success notifications only)
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

    // Tab switching functionality
    if (loginTab && registerTab) {
        loginTab.addEventListener('click', () => switchTab('login'));
        registerTab.addEventListener('click', () => switchTab('register'));
    }

    // Link switching functionality
    if (switchToRegister && switchToLogin) {
        switchToRegister.addEventListener('click', function(e) {
            e.preventDefault();
            switchTab('register');
        });

        switchToLogin.addEventListener('click', function(e) {
            e.preventDefault();
            switchTab('login');
        });
    }

    // OTP input behavior
    if (otpInputs.length > 0) {
        // Auto-focus next input when a digit is entered
        otpInputs.forEach((input, index) => {
            input.addEventListener('input', function(e) {
                // Only allow numbers
                this.value = this.value.replace(/[^0-9]/g, '');

                if (this.value.length === this.maxLength) {
                    if (index < otpInputs.length - 1) {
                        otpInputs[index + 1].focus();
                    } else {
                        this.blur();
                        // Auto-submit when all digits are filled
                        const allFilled = Array.from(otpInputs).every(input => input.value.length === 1);
                        if (allFilled) {
                            document.getElementById('otpForm').dispatchEvent(new Event('submit'));
                        }
                    }
                }
            });

            // Handle backspace to go to previous input
            input.addEventListener('keydown', function(e) {
                if (e.key === 'Backspace' && !this.value && index > 0) {
                    otpInputs[index - 1].focus();
                }
            });

            // Handle pasting OTP
            input.addEventListener('paste', function(e) {
                e.preventDefault();
                const pastedData = e.clipboardData.getData('text').trim();

                // Check if pasted content is a 4-digit number
                if (/^\d{4}$/.test(pastedData)) {
                    // Fill all inputs with respective digits
                    for (let i = 0; i < otpInputs.length; i++) {
                        otpInputs[i].value = pastedData[i] || '';
                    }

                    // Auto-submit if all filled
                    document.getElementById('otpForm').dispatchEvent(new Event('submit'));
                }
            });
        });
    }

    // Handle login form submission
    if (document.getElementById('loginForm')) {
        document.getElementById('loginForm').addEventListener('submit', function(e) {
            e.preventDefault();
            const email = document.getElementById('login-email').value.trim();

            if (!isValidEmail(email)) {
                showError('login-email', 'Please enter a valid email address');
                return;
            }

            // Send OTP for login
            sendLoginOtp(email);
        });
    }

    // Handle register form submission
    if (document.getElementById('registerForm')) {
        document.getElementById('registerForm').addEventListener('submit', function(e) {
            e.preventDefault();

            // Get form values
            const firstName = document.getElementById('register-firstName').value.trim();
            const lastName = document.getElementById('register-lastName').value.trim();
            const email = document.getElementById('register-email').value.trim();
            const phone = document.getElementById('register-phone').value.trim();
            const termsCheckbox = document.getElementById('terms-checkbox');

            // Validate form
            let isValid = true;

            if (!firstName) {
                showError('register-firstName', 'Please enter your first name');
                isValid = false;
            }

            if (!lastName) {
                showError('register-lastName', 'Please enter your last name');
                isValid = false;
            }

            if (!isValidEmail(email)) {
                showError('register-email', 'Please enter a valid email address');
                isValid = false;
            }

            if (!isValidPhone(phone)) {
                showError('register-phone', 'Please enter a valid phone number');
                isValid = false;
            }

            if (!termsCheckbox.checked) {
                alert('Please agree to the Terms of Service and Privacy Policy');
                isValid = false;
            }

            if (isValid) {
                // Store registration data
                registrationData = {
                    firstName: firstName,
                    lastName: lastName,
                    email: email,
                    phoneNumber: phone
                };

                // Send OTP for registration
                sendRegistrationOtp(registrationData);
            }
        });
    }

    // Handle OTP form submission
    if (document.getElementById('otpForm')) {
        document.getElementById('otpForm').addEventListener('submit', function(e) {
            e.preventDefault();

            // Get OTP value
            let otp = '';
            otpInputs.forEach(input => {
                otp += input.value;
            });

            if (otp.length !== 4 || !/^\d{4}$/.test(otp)) {
                alert('Please enter a valid 4-digit OTP code');
                return;
            }

            // Verify OTP
            if (otpAction === 'login') {
                verifyLoginOtp(displayEmail.textContent, otp);
            } else if (otpAction === 'register') {
                verifyRegistrationOtp(registrationData, otp);
            }
        });
    }

    // Handle change/edit email
    if (changeEmail && editEmail) {
        changeEmail.addEventListener('click', function(e) {
            e.preventDefault();
            goBackToForm();
        });

        editEmail.addEventListener('click', function(e) {
            e.preventDefault();
            goBackToForm();
        });
    }

    // Handle resend OTP
    if (resendOtpButton) {
        resendOtpButton.addEventListener('click', function() {
            if (otpAction === 'login') {
                const email = displayEmail.textContent.trim();
                sendLoginOtp(email);
            } else if (otpAction === 'register') {
                if (registrationData) {
                    sendRegistrationOtp(registrationData);
                }
            }
        });
    }

    /**
     * Switch between login and register tabs
     */
    function switchTab(tab) {
        if (tab === 'login') {
            loginTab.classList.add('active');
            registerTab.classList.remove('active');
            registerForm.classList.add('hidden');
            loginForm.classList.remove('hidden');
            loginForm.classList.add('fade-in');
            currentForm = 'login';
        } else {
            loginTab.classList.remove('active');
            registerTab.classList.add('active');
            loginForm.classList.add('hidden');
            registerForm.classList.remove('hidden');
            registerForm.classList.add('fade-in');
            currentForm = 'register';
        }
    }

    /**
     * Show OTP verification form
     */
    function showOtpForm(email) {
        loginForm.classList.add('hidden');
        registerForm.classList.add('hidden');
        otpForm.classList.remove('hidden');
        otpForm.classList.add('fade-in');

        // Display email and start countdown
        displayEmail.textContent = email;
        startCountdown();

        // Focus first OTP input and clear all inputs
        otpInputs.forEach(input => input.value = '');
        otpInputs[0].focus();
    }

    /**
     * Go back to login/register form
     */
    function goBackToForm() {
        otpForm.classList.add('hidden');

        if (otpAction === 'login') {
            loginForm.classList.remove('hidden');
            loginForm.classList.add('fade-in');
        } else {
            registerForm.classList.remove('hidden');
            registerForm.classList.add('fade-in');
        }

        // Reset OTP inputs
        otpInputs.forEach(input => {
            input.value = '';
        });

        // Stop countdown
        stopCountdown();
    }

    /**
     * Start OTP countdown timer
     */
    function startCountdown() {
        // Reset timer
        timerSeconds = 30;
        updateCountdownDisplay();

        // Disable resend button
        resendOtpButton.disabled = true;

        // Clear any existing interval
        if (countdownInterval) {
            clearInterval(countdownInterval);
        }

        // Start new countdown
        countdownInterval = setInterval(function() {
            timerSeconds--;
            updateCountdownDisplay();

            if (timerSeconds <= 0) {
                stopCountdown();
                resendOtpButton.disabled = false;
            }
        }, 1000);
    }

    /**
     * Stop countdown timer
     */
    function stopCountdown() {
        if (countdownInterval) {
            clearInterval(countdownInterval);
            countdownInterval = null;
        }
    }

    /**
     * Update countdown display
     */
    function updateCountdownDisplay() {
        const minutes = Math.floor(timerSeconds / 60);
        const seconds = timerSeconds % 60;
        countdownElement.textContent = `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
    }

    /**
     * Send OTP for login
     */
    function sendLoginOtp(email) {
        otpAction = 'login';

        // Show loading state
        const submitBtn = document.querySelector('#loginForm button[type="submit"]');
        const originalText = submitBtn.textContent;
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Sending...';

        // Clear previous errors
        clearErrors();

        const formData = new FormData();
        formData.append('email', email);

        fetch('/authentication/login/send-otp', {
            method: 'POST',
            body: formData
        })
            .then(response => response.json())
            .then(data => {
                if (data.success === false) {
                    // Show error below input field
                    if (data.errorField) {
                        showError(data.errorField === 'email' ? 'login-email' : data.errorField, data.message);
                    } else {
                        showError('login-email', data.message);
                    }
                    throw new Error(data.message || 'Failed to send OTP');
                }

                // Show OTP form
                showOtpForm(email);
            })
            .catch(error => {
                console.error('Error:', error);
            })
            .finally(() => {
                // Reset button state
                submitBtn.disabled = false;
                submitBtn.innerHTML = originalText;
            });
    }

    /**
     * Send OTP for registration
     */
    function sendRegistrationOtp(registerData) {
        otpAction = 'register';

        // Show loading state
        const submitBtn = document.querySelector('#registerForm button[type="submit"]');
        const originalText = submitBtn.textContent;
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Sending...';

        // Clear previous errors
        clearErrors();

        fetch('/authentication/register/send-otp', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(registerData)
        })
            .then(response => response.json())
            .then(data => {
                if (data.success === false) {
                    // Show error below input field
                    if (data.errorField) {
                        showError('register-' + data.errorField, data.message);
                    } else {
                        showError('register-email', data.message);
                    }
                    throw new Error(data.message || 'Failed to send OTP');
                }

                // Show OTP form
                showOtpForm(registerData.email);
            })
            .catch(error => {
                console.error('Error:', error);
            })
            .finally(() => {
                // Reset button state
                submitBtn.disabled = false;
                submitBtn.innerHTML = originalText;
            });
    }

    /**
     * Verify login OTP
     */
    function verifyLoginOtp(email, otp) {
        // Show loading state
        const submitBtn = document.querySelector('#otpForm button[type="submit"]');
        const originalText = submitBtn.textContent;
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Verifying...';

        // Clear previous errors
        clearOtpErrors();

        const formData = new FormData();
        formData.append('email', email);
        formData.append('otp', otp);

        fetch('/authentication/login/verify-otp', {
            method: 'POST',
            body: formData
        })
            .then(response => response.json())
            .then(data => {
                if (data.success === false) {
                    // Show error message
                    showOtpError(data.message);
                    throw new Error(data.message || 'Invalid OTP');
                }

                // Save auth token and user info to session storage
                sessionStorage.setItem('authToken', data.token);
                sessionStorage.setItem('userInfo', JSON.stringify({
                    firstName: data.firstName,
                    lastName: data.lastName,
                    email: data.email,
                    role: data.role,
                    membershipType: data.membershipType
                }));

                // Show success message
                showToast('Login successful! Redirecting...', 'success');

                // Redirect to the customer index page
                setTimeout(() => {
                    window.location.href = '/customer';
                }, 1000);
            })
            .catch(error => {
                console.error('Verification error:', error);

                // Clear OTP inputs and focus first input
                otpInputs.forEach(input => input.value = '');
                otpInputs[0].focus();
            })
            .finally(() => {
                // Reset button state
                submitBtn.disabled = false;
                submitBtn.innerHTML = originalText;
            });
    }

    /**
     * Verify registration OTP
     */
    function verifyRegistrationOtp(registerData, otp) {
        // Show loading state
        const submitBtn = document.querySelector('#otpForm button[type="submit"]');
        const originalText = submitBtn.textContent;
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Verifying...';

        // Clear previous errors
        clearOtpErrors();

        // Create a new object with registration data and OTP
        const requestData = {
            ...registerData,
            otp: otp
        };

        fetch('/authentication/register/verify-otp', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(requestData)
        })
            .then(response => response.json())
            .then(data => {
                if (data.success === false) {
                    // Show error message
                    showOtpError(data.message);
                    throw new Error(data.message || 'Invalid OTP');
                }

                // Save auth token and user info to session storage
                sessionStorage.setItem('authToken', data.token);
                sessionStorage.setItem('userInfo', JSON.stringify({
                    firstName: data.firstName,
                    lastName: data.lastName,
                    email: data.email,
                    role: data.role,
                    membershipType: data.membershipType
                }));

                // Show success message
                showToast('Registration successful! Redirecting...', 'success');

                // Redirect to the customer index page
                setTimeout(() => {
                    window.location.href = '/customer';
                }, 1000);
            })
            .catch(error => {
                console.error('Error:', error);

                // Clear OTP inputs and focus first input
                otpInputs.forEach(input => input.value = '');
                otpInputs[0].focus();
            })
            .finally(() => {
                // Reset button state
                submitBtn.disabled = false;
                submitBtn.innerHTML = originalText;
            });
    }

    /**
     * Show error message under input
     */
    function showError(inputId, message) {
        const input = document.getElementById(inputId);
        if (!input) return;

        // Add error class to input
        input.classList.add('is-invalid');

        // Check if error element already exists
        let errorElement = input.parentElement.querySelector('.invalid-feedback');

        // Create error element if it doesn't exist
        if (!errorElement) {
            errorElement = document.createElement('div');
            errorElement.className = 'invalid-feedback';
            input.parentElement.appendChild(errorElement);
        }

        // Set error message
        errorElement.textContent = message;

        // Remove error after 5 seconds
        setTimeout(() => {
            input.classList.remove('is-invalid');
        }, 5000);

        // Remove error when input changes
        input.addEventListener('input', function() {
            this.classList.remove('is-invalid');
        }, { once: true });
    }

    /**
     * Show toast message
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

    /**
     * Validate email format
     */
    function isValidEmail(email) {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailRegex.test(email);
    }

    /**
     * Validate phone number format
     */
    function isValidPhone(phone) {
        // Basic phone validation - allows various formats
        const phoneRegex = /^[+]?[(]?[0-9]{3}[)]?[-\s.]?[0-9]{3}[-\s.]?[0-9]{4,6}$/;
        return phoneRegex.test(phone);
    }

    // Check if redirected from another page with a message
    const urlParams = new URLSearchParams(window.location.search);
    const messageParam = urlParams.get('message');
    const messageType = urlParams.get('type') || 'info';

    if (messageParam) {
        showToast(decodeURIComponent(messageParam), messageType);

        // Remove the parameters from URL
        const newUrl = window.location.pathname;
        window.history.replaceState({}, document.title, newUrl);
    }
});