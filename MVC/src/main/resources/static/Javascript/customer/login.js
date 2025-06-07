document.addEventListener('DOMContentLoaded', function() {
    // Cache DOM elements
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
    let otpAction = '';
    let registrationData = null;

    // Initialize tab switching
    if (loginTab && registerTab) {
        loginTab.addEventListener('click', () => switchTab('login'));
        registerTab.addEventListener('click', () => switchTab('register'));
    }

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

    // Initialize OTP input behavior
    if (otpInputs.length > 0) {
        otpInputs.forEach((input, index) => {
            input.addEventListener('input', function(e) {
                this.value = this.value.replace(/[^0-9]/g, '');

                if (this.value.length === this.maxLength) {
                    if (index < otpInputs.length - 1) {
                        otpInputs[index + 1].focus();
                    } else {
                        this.blur();
                        const allFilled = Array.from(otpInputs).every(input => input.value.length === 1);
                        if (allFilled) {
                            document.getElementById('otpForm').dispatchEvent(new Event('submit'));
                        }
                    }
                }
            });

            input.addEventListener('keydown', function(e) {
                if (e.key === 'Backspace' && !this.value && index > 0) {
                    otpInputs[index - 1].focus();
                }
            });

            input.addEventListener('paste', function(e) {
                e.preventDefault();
                const pastedData = e.clipboardData.getData('text').trim();

                if (/^\d{4}$/.test(pastedData)) {
                    for (let i = 0; i < otpInputs.length; i++) {
                        otpInputs[i].value = pastedData[i] || '';
                    }

                    document.getElementById('otpForm').dispatchEvent(new Event('submit'));
                }
            });
        });
    }

    // Initialize form submissions
    if (document.getElementById('loginForm')) {
        document.getElementById('loginForm').addEventListener('submit', function(e) {
            e.preventDefault();
            const email = document.getElementById('login-email').value.trim();

            if (!isValidEmail(email)) {
                showError('login-email', 'Please enter a valid email address');
                return;
            }

            sendLoginOtp(email);
        });
    }

    if (document.getElementById('registerForm')) {
        document.getElementById('registerForm').addEventListener('submit', function(e) {
            e.preventDefault();

            const firstName = document.getElementById('register-firstName').value.trim();
            const lastName = document.getElementById('register-lastName').value.trim();
            const email = document.getElementById('register-email').value.trim();
            const phone = document.getElementById('register-phone').value.trim();
            const termsCheckbox = document.getElementById('terms-checkbox');

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
                registrationData = {
                    firstName: firstName,
                    lastName: lastName,
                    email: email,
                    phoneNumber: phone
                };

                sendRegistrationOtp(registrationData);
            }
        });
    }

    if (document.getElementById('otpForm')) {
        document.getElementById('otpForm').addEventListener('submit', function(e) {
            e.preventDefault();

            let otp = '';
            otpInputs.forEach(input => {
                otp += input.value;
            });

            if (otp.length !== 4 || !/^\d{4}$/.test(otp)) {
                showOtpError('Please enter a valid 4-digit OTP code');
                return;
            }

            if (otpAction === 'login') {
                verifyLoginOtp(displayEmail.textContent, otp);
            } else if (otpAction === 'register') {
                verifyRegistrationOtp(registrationData, otp);
            }
        });
    }

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

    if (resendOtpButton) {
        resendOtpButton.addEventListener('click', function() {
            if (this.disabled) return;

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

        clearErrors();
    }

    function showOtpForm(email) {
        loginForm.classList.add('hidden');
        registerForm.classList.add('hidden');
        otpForm.classList.remove('hidden');
        otpForm.classList.add('fade-in');

        displayEmail.textContent = email;
        startCountdown();

        otpInputs.forEach(input => input.value = '');
        otpInputs[0].focus();
    }

    function goBackToForm() {
        otpForm.classList.add('hidden');

        if (otpAction === 'login') {
            loginForm.classList.remove('hidden');
            loginForm.classList.add('fade-in');
        } else {
            registerForm.classList.remove('hidden');
            registerForm.classList.add('fade-in');
        }

        otpInputs.forEach(input => {
            input.value = '';
        });

        stopCountdown();
    }

    function startCountdown() {
        timerSeconds = 30;
        updateCountdownDisplay();

        resendOtpButton.disabled = true;

        if (countdownInterval) {
            clearInterval(countdownInterval);
        }

        countdownInterval = setInterval(function() {
            timerSeconds--;
            updateCountdownDisplay();

            if (timerSeconds <= 0) {
                stopCountdown();
                resendOtpButton.disabled = false;
            }
        }, 1000);
    }

    function stopCountdown() {
        if (countdownInterval) {
            clearInterval(countdownInterval);
            countdownInterval = null;
        }
    }

    function updateCountdownDisplay() {
        const minutes = Math.floor(timerSeconds / 60);
        const seconds = timerSeconds % 60;
        countdownElement.textContent = `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
    }

    function sendLoginOtp(email) {
        otpAction = 'login';

        const submitBtn = document.querySelector('#loginForm button[type="submit"]');
        const originalText = submitBtn.textContent;
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Sending...';

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
                    if (data.errorField) {
                        showError(data.errorField === 'email' ? 'login-email' : data.errorField, data.message);
                    } else {
                        showError('login-email', data.message);
                    }
                    throw new Error(data.message || 'Failed to send OTP');
                }

                showOtpForm(email);
            })
            .catch(error => {
                console.error('Error:', error);
                showError('login-email', 'Failed to send OTP. Please try again.');
            })
            .finally(() => {
                submitBtn.disabled = false;
                submitBtn.innerHTML = originalText;
            });
    }

    function sendRegistrationOtp(registerData) {
        otpAction = 'register';

        const submitBtn = document.querySelector('#registerForm button[type="submit"]');
        const originalText = submitBtn.textContent;
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Sending...';

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
                    if (data.errorField) {
                        showError('register-' + data.errorField, data.message);
                    } else {
                        showError('register-email', data.message);
                    }
                    throw new Error(data.message || 'Failed to send OTP');
                }

                showOtpForm(registerData.email);
            })
            .catch(error => {
                console.error('Error:', error);
                showError('register-email', 'Failed to send OTP. Please try again.');
            })
            .finally(() => {
                submitBtn.disabled = false;
                submitBtn.innerHTML = originalText;
            });
    }

    function verifyLoginOtp(email, otp) {
        const submitBtn = document.querySelector('#otpForm button[type="submit"]');
        const originalText = submitBtn.textContent;
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Verifying...';

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
                    showOtpError(data.message);
                    throw new Error(data.message || 'Invalid OTP');
                }

                sessionStorage.setItem('authToken', data.token);
                sessionStorage.setItem('userInfo', JSON.stringify({
                    firstName: data.firstName,
                    lastName: data.lastName,
                    email: data.email,
                    role: data.role,
                    membershipType: data.membershipType
                }));

                showToast('Login successful! Redirecting...', 'success');

                setTimeout(() => {
                    window.location.href = '/';
                }, 1000);
            })
            .catch(error => {
                console.error('Verification error:', error);
                otpInputs.forEach(input => input.value = '');
                otpInputs[0].focus();
            })
            .finally(() => {
                submitBtn.disabled = false;
                submitBtn.innerHTML = originalText;
            });
    }

    function verifyRegistrationOtp(registerData, otp) {
        const submitBtn = document.querySelector('#otpForm button[type="submit"]');
        const originalText = submitBtn.textContent;
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Verifying...';

        clearOtpErrors();

        const requestData = {
            email: registerData.email,
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
                    showOtpError(data.message);
                    throw new Error(data.message || 'Invalid OTP');
                }

                sessionStorage.setItem('authToken', data.token);
                sessionStorage.setItem('userInfo', JSON.stringify({
                    firstName: data.firstName,
                    lastName: data.lastName,
                    email: data.email,
                    role: data.role,
                    membershipType: data.membershipType
                }));

                showToast('Registration successful! Redirecting...', 'success');

                setTimeout(() => {
                    window.location.href = '/';
                }, 1000);
            })
            .catch(error => {
                console.error('Error:', error);
                otpInputs.forEach(input => input.value = '');
                otpInputs[0].focus();
            })
            .finally(() => {
                submitBtn.disabled = false;
                submitBtn.innerHTML = originalText;
            });
    }

    function showError(inputId, message) {
        const input = document.getElementById(inputId);
        if (!input) return;

        input.classList.add('is-invalid');

        let errorElement = input.parentElement.querySelector('.invalid-feedback');

        if (!errorElement) {
            errorElement = document.createElement('div');
            errorElement.className = 'invalid-feedback';
            input.parentElement.appendChild(errorElement);
        }

        errorElement.textContent = message;
        input.focus();

        input.addEventListener('input', function() {
            this.classList.remove('is-invalid');
        }, { once: true });
    }

    function showOtpError(message) {
        const otpForm = document.getElementById('otpForm');

        let errorElement = otpForm.querySelector('.otp-error');

        if (!errorElement) {
            errorElement = document.createElement('div');
            errorElement.className = 'otp-error alert alert-danger mt-3';
            otpForm.appendChild(errorElement);
        }

        errorElement.textContent = message;

        const otpInputsContainer = document.querySelector('.otp-inputs');
        if (otpInputsContainer) {
            otpInputsContainer.classList.add('shake');
            setTimeout(() => {
                otpInputsContainer.classList.remove('shake');
            }, 500);
        }
    }

    function clearErrors() {
        document.querySelectorAll('.form-control').forEach(input => {
            input.classList.remove('is-invalid');
        });

        document.querySelectorAll('.invalid-feedback').forEach(error => {
            error.remove();
        });
    }

    function clearOtpErrors() {
        const errorElement = document.querySelector('.otp-error');
        if (errorElement) {
            errorElement.remove();
        }
    }

    function showToast(message, type = 'info') {
        let toastContainer = document.querySelector('.toast-container');
        if (!toastContainer) {
            toastContainer = document.createElement('div');
            toastContainer.className = 'toast-container position-fixed top-0 end-0 p-3';
            document.body.appendChild(toastContainer);
        }

        const toastId = 'toast-' + Date.now();
        const toastHTML = `
            <div id="${toastId}" class="toast align-items-center text-white bg-${type === 'error' ? 'danger' : type} border-0" role="alert" aria-live="assertive" aria-atomic="true">
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

    function isValidEmail(email) {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailRegex.test(email);
    }

    function isValidPhone(phone) {
        const phoneRegex = /^[+]?[(]?[0-9]{3}[)]?[-\s.]?[0-9]{3}[-\s.]?[0-9]{4,6}$/;
        return phoneRegex.test(phone);
    }

    // Check for URL parameters
    const urlParams = new URLSearchParams(window.location.search);
    const messageParam = urlParams.get('message');
    const messageType = urlParams.get('type') || 'info';

    if (messageParam) {
        showToast(decodeURIComponent(messageParam), messageType);
        const newUrl = window.location.pathname;
        window.history.replaceState({}, document.title, newUrl);
    }
});