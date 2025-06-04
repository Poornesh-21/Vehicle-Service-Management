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

    let currentForm = 'login';
    let countdownInterval;
    let timerSeconds = 30;
    let otpAction = ''; // 'login' or 'register'
    let registrationData = null;

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

    // OTP-related functionality
    if (otpInputs.length > 0) {
        // Handle OTP input behavior
        otpInputs.forEach((input, index) => {
            // Auto-focus next input when a digit is entered
            input.addEventListener('input', function() {
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
        });
    }

    // Handle login form submission
    if (document.getElementById('loginForm')) {
        document.getElementById('loginForm').addEventListener('submit', function(e) {
            e.preventDefault();
            const email = document.getElementById('login-email').value;

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
            const firstName = document.getElementById('register-firstName').value;
            const lastName = document.getElementById('register-lastName').value;
            const email = document.getElementById('register-email').value;
            const phone = document.getElementById('register-phone').value;
            const termsCheckbox = document.getElementById('terms-checkbox');

            // Validate form
            let isValid = true;

            if (!firstName.trim()) {
                showError('register-firstName', 'Please enter your first name');
                isValid = false;
            }

            if (!lastName.trim()) {
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
            } else {
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
            const email = displayEmail.textContent.trim();
            if (otpAction === 'login') {
                sendLoginOtp(email);
            } else {
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

        // Focus first OTP input
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
        submitBtn.textContent = 'Sending...';

        const formData = new URLSearchParams();
        formData.append('email', email);

        fetch('/authentication/login/send-otp', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: formData
        })
            .then(response => {
                if (!response.ok) {
                    return response.json().then(data => {
                        throw new Error(data.message || 'Failed to send OTP');
                    });
                }
                return response.json();
            })
            .then(data => {
                if (data.success === false) {
                    throw new Error(data.message || 'Failed to send OTP');
                }
                showOtpForm(email);
            })
            .catch(error => {
                alert(error.message || 'Error sending OTP. Please try again.');
                console.error('Error:', error);
            })
            .finally(() => {
                // Reset button state
                submitBtn.disabled = false;
                submitBtn.textContent = originalText;
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
        submitBtn.textContent = 'Sending...';

        fetch('/authentication/register/send-otp', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(registerData)
        })
            .then(response => {
                if (!response.ok) {
                    return response.json().then(data => {
                        throw new Error(data.message || 'Failed to send OTP');
                    });
                }
                return response.json();
            })
            .then(data => {
                if (data.success === false) {
                    throw new Error(data.message || 'Failed to send OTP');
                }
                showOtpForm(registerData.email);
            })
            .catch(error => {
                alert(error.message || 'Error sending OTP. Please try again.');
                console.error('Error:', error);
            })
            .finally(() => {
                // Reset button state
                submitBtn.disabled = false;
                submitBtn.textContent = originalText;
            });
    }

    /**
     * Verify login OTP
     */
    function verifyLoginOtp(email, otp) {
        console.log('Verifying OTP for:', email, otp);

        // Show loading state
        const submitBtn = document.querySelector('#otpForm button[type="submit"]');
        const originalText = submitBtn.textContent;
        submitBtn.disabled = true;
        submitBtn.textContent = 'Verifying...';

        const formData = new URLSearchParams();
        formData.append('email', email);
        formData.append('otp', otp);

        fetch('/authentication/login/verify-otp', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            body: formData
        })
            .then(response => {
                if (!response.ok) {
                    return response.json().then(data => {
                        throw new Error(data.message || 'Invalid OTP');
                    });
                }
                return response.json();
            })
            .then(data => {
                console.log('API response:', data);
                if (data.success === false) {
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

                // Redirect to the book service page
                window.location.href = data.redirectUrl || '/customer/bookService';
            })
            .catch(error => {
                console.error('Verification error:', error);
                alert(error.message || 'OTP verification failed. Please try again.');
                otpInputs.forEach(input => input.value = ''); // Clear OTP inputs
                otpInputs[0].focus(); // Focus on the first input
            })
            .finally(() => {
                // Reset button state
                submitBtn.disabled = false;
                submitBtn.textContent = originalText;
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
        submitBtn.textContent = 'Verifying...';

        fetch(`/authentication/register/verify-otp?otp=${encodeURIComponent(otp)}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(registerData)
        })
            .then(response => {
                if (!response.ok) {
                    return response.json().then(data => {
                        throw new Error(data.message || 'Invalid OTP');
                    });
                }
                return response.json();
            })
            .then(data => {
                if (data.success === false) {
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

                // Use the redirectUrl from the server response
                window.location.href = data.redirectUrl || '/customer/bookService';
            })
            .catch(error => {
                alert(error.message || 'OTP verification failed. Please try again.');
                console.error('Error:', error);
                // Reset OTP inputs
                otpInputs.forEach(input => input.value = '');
                otpInputs[0].focus();
            })
            .finally(() => {
                // Reset button state
                submitBtn.disabled = false;
                submitBtn.textContent = originalText;
            });
    }

    /**
     * Show error message
     */
    function showError(inputId, message) {
        const input = document.getElementById(inputId);

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
});