document.addEventListener('DOMContentLoaded', function() {
    const API_BASE_URL = 'http://localhost:8080';
    const MVC_BASE_URL = '';

    setupPasswordToggles();
    setupPasswordStrengthMeter();
    setupLoginForm();
    setupPasswordChangeForm();
    checkExistingToken();
    setupFetchResponseHandler();

    function setupPasswordToggles() {
        const toggles = [
            { toggle: 'togglePassword', field: 'password', icon: 'eyeIcon' },
            { toggle: 'toggleNewPassword', field: 'newPassword', icon: 'newEyeIcon' },
            { toggle: 'toggleConfirmPassword', field: 'confirmPassword', icon: 'confirmEyeIcon' }
        ];

        toggles.forEach(item => {
            const toggleEl = document.getElementById(item.toggle);
            const fieldEl = document.getElementById(item.field);
            const iconEl = document.getElementById(item.icon);

            if (toggleEl && fieldEl && iconEl) {
                toggleEl.addEventListener('click', function() {
                    const type = fieldEl.getAttribute('type') === 'password' ? 'text' : 'password';
                    fieldEl.setAttribute('type', type);

                    iconEl.classList.toggle('fa-eye');
                    iconEl.classList.toggle('fa-eye-slash');
                });
            }
        });
    }

    function setupPasswordStrengthMeter() {
        const newPassword = document.getElementById('newPassword');

        if (newPassword) {
            newPassword.addEventListener('input', function() {
                const value = newPassword.value;
                let strength = 0;

                const lengthCheck = document.getElementById('length');
                if (value.length >= 8) {
                    updateRequirement(lengthCheck, true);
                    strength += 20;
                } else {
                    updateRequirement(lengthCheck, false);
                }

                const uppercaseCheck = document.getElementById('uppercase');
                if (/[A-Z]/.test(value)) {
                    updateRequirement(uppercaseCheck, true);
                    strength += 20;
                } else {
                    updateRequirement(uppercaseCheck, false);
                }

                const lowercaseCheck = document.getElementById('lowercase');
                if (/[a-z]/.test(value)) {
                    updateRequirement(lowercaseCheck, true);
                    strength += 20;
                } else {
                    updateRequirement(lowercaseCheck, false);
                }

                const numberCheck = document.getElementById('number');
                if (/[0-9]/.test(value)) {
                    updateRequirement(numberCheck, true);
                    strength += 20;
                } else {
                    updateRequirement(numberCheck, false);
                }

                const specialCheck = document.getElementById('special');
                if (/[^A-Za-z0-9]/.test(value)) {
                    updateRequirement(specialCheck, true);
                    strength += 20;
                } else {
                    updateRequirement(specialCheck, false);
                }

                updateStrengthMeter(strength);
            });
        }

        const confirmPassword = document.getElementById('confirmPassword');
        if (confirmPassword && newPassword) {
            confirmPassword.addEventListener('input', function() {
                const passwordMismatch = document.getElementById('passwordMismatch');

                if (newPassword.value !== confirmPassword.value) {
                    passwordMismatch.classList.remove('d-none');
                } else {
                    passwordMismatch.classList.add('d-none');
                }
            });
        }
    }

    function updateRequirement(element, isMet) {
        if (element) {
            if (isMet) {
                element.innerHTML = '<i class="fas fa-check-circle"></i> ' + element.innerText.substring(element.innerText.indexOf(' ') + 1);
                element.classList.add('requirement-met');
            } else {
                element.innerHTML = '<i class="fas fa-times-circle"></i> ' + element.innerText.substring(element.innerText.indexOf(' ') + 1);
                element.classList.remove('requirement-met');
            }
        }
    }

    function updateStrengthMeter(strength) {
        const strengthProgress = document.getElementById('strengthProgress');
        const strengthText = document.getElementById('strengthText');

        if (strengthProgress && strengthText) {
            strengthProgress.style.width = strength + '%';

            if (strength <= 20) {
                strengthText.textContent = 'Very Weak';
                strengthProgress.className = 'strength-progress very-weak';
            } else if (strength <= 40) {
                strengthText.textContent = 'Weak';
                strengthProgress.className = 'strength-progress weak';
            } else if (strength <= 60) {
                strengthText.textContent = 'Medium';
                strengthProgress.className = 'strength-progress medium';
            } else if (strength <= 80) {
                strengthText.textContent = 'Strong';
                strengthProgress.className = 'strength-progress strong';
            } else {
                strengthText.textContent = 'Very Strong';
                strengthProgress.className = 'strength-progress very-strong';
            }
        }
    }

    function setupLoginForm() {
        const loginForm = document.getElementById('loginForm');

        if (loginForm) {
            loginForm.addEventListener('submit', function(e) {
                e.preventDefault();

                const email = document.getElementById('email').value;
                const passwordValue = document.getElementById('password').value;

                hideErrorMessage();

                const loginBtn = document.querySelector('.btn-login');
                const originalText = loginBtn.innerHTML;
                loginBtn.disabled = true;
                loginBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span> Signing in...';

                const requestData = {
                    email: email,
                    password: passwordValue
                };

                fetch(`${API_BASE_URL}/serviceAdvisor/api/login`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(requestData)
                })
                    .then(handleApiResponse)
                    .then(data => {
                        console.log("Login successful", data);

                        if (data.role && data.role.toLowerCase() !== 'serviceadvisor') {
                            throw new Error('You do not have permission to access the Service Advisor portal');
                        }

                        storeSessionData(data);

                        const isTemporaryPassword = checkIfTemporaryPassword(passwordValue);

                        if (isTemporaryPassword) {
                            document.getElementById('currentPassword').value = passwordValue;
                            const changePasswordModal = new bootstrap.Modal(document.getElementById('changePasswordModal'));
                            changePasswordModal.show();

                            loginBtn.disabled = false;
                            loginBtn.innerHTML = originalText;
                        } else {
                            window.location.href = `${MVC_BASE_URL}/serviceAdvisor/dashboard`;
                        }
                    })
                    .catch(error => {
                        console.error("Login error:", error);
                        showErrorMessage(error.message || 'Authentication failed. Please check your credentials.');
                        loginBtn.disabled = false;
                        loginBtn.innerHTML = originalText;
                    });
            });
        }
    }

    function setupPasswordChangeForm() {
        const changePasswordForm = document.getElementById('changePasswordForm');

        if (changePasswordForm) {
            changePasswordForm.addEventListener('submit', function(e) {
                e.preventDefault();

                const newPasswordValue = document.getElementById('newPassword').value;
                const confirmPasswordValue = document.getElementById('confirmPassword').value;
                const currentPasswordValue = document.getElementById('currentPassword').value;
                const passwordMismatch = document.getElementById('passwordMismatch');

                hideErrorMessage();
                passwordMismatch.classList.add('d-none');

                if (newPasswordValue !== confirmPasswordValue) {
                    passwordMismatch.classList.remove('d-none');
                    return;
                }

                if (validatePasswordStrength(newPasswordValue) < 60) {
                    showErrorMessage('Password is not strong enough. Please follow the requirements.');
                    return;
                }

                const submitBtn = changePasswordForm.querySelector('button[type="submit"]');
                const originalBtnText = submitBtn.innerHTML;
                submitBtn.disabled = true;
                submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span> Setting password...';

                const token = localStorage.getItem('jwtToken');

                const requestData = {
                    currentPassword: currentPasswordValue,
                    newPassword: newPasswordValue,
                    confirmPassword: confirmPasswordValue,
                    isTemporaryPassword: true
                };

                fetch(`${API_BASE_URL}/serviceAdvisor/api/change-password`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${token}`
                    },
                    body: JSON.stringify(requestData)
                })
                    .then(handleApiResponse)
                    .then(data => {
                        console.log("Password changed successfully", data);

                        if (data.token) {
                            storeSessionData(data);
                        }

                        showSuccessMessage('Password changed successfully! Redirecting to dashboard...');

                        setTimeout(() => {
                            window.location.href = `${MVC_BASE_URL}/serviceAdvisor/dashboard`;
                        }, 1500);
                    })
                    .catch(error => {
                        console.error("Password change error:", error);
                        submitBtn.disabled = false;
                        submitBtn.innerHTML = originalBtnText;
                        showErrorMessage(error.message || 'Failed to change password. Please try again.');
                    });
            });
        }
    }

    function checkExistingToken() {
        const existingToken = localStorage.getItem('jwtToken');
        const role = localStorage.getItem('userRole');

        // Check if we're on the logout page
        const urlParams = new URLSearchParams(window.location.search);
        if (urlParams.has('logout')) {
            // Clear all storage on logout page load
            clearSessionData();
            return;
        }

        if (existingToken && role && role.toLowerCase() === 'serviceadvisor') {
            fetch(`${API_BASE_URL}/serviceAdvisor/api/validate-token`, {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${existingToken}`
                }
            })
                .then(response => {
                    if (response.ok) {
                        window.location.href = `${MVC_BASE_URL}/serviceAdvisor/dashboard`;
                    } else {
                        clearSessionData();
                    }
                })
                .catch(() => {
                    clearSessionData();
                });
        }
    }

    function setupFetchResponseHandler() {
        (function() {
            const originalFetch = window.fetch;

            window.fetch = function(url, options) {
                return originalFetch(url, options)
                    .then(response => {
                        if (url.includes('/serviceAdvisor/api/login') && !response.ok) {
                            console.log(`Response status: ${response.status} for URL: ${url}`);
                            const clonedResponse = response.clone();

                            return clonedResponse.json().then(errorData => {
                                console.log("Error data:", errorData);
                                if (errorData && errorData.message &&
                                    errorData.message.includes('Invalid email/password combination')) {

                                    const modifiedData = {
                                        ...errorData,
                                        message: 'Invalid email or password. Please try again.'
                                    };

                                    return new Response(
                                        JSON.stringify(modifiedData),
                                        {
                                            status: response.status,
                                            statusText: response.statusText,
                                            headers: response.headers
                                        }
                                    );
                                }

                                return response;
                            }).catch(error => {
                                console.error("Error parsing response:", error);
                                return response;
                            });
                        }

                        return response;
                    });
            };
        })();
    }



    function handleApiResponse(response) {
        console.log(`Response status: ${response.status}`);
        if (!response.ok) {
            return response.json().then(errorData => {
                console.error("API error:", errorData);
                throw new Error(errorData.message || errorData.error || 'Request failed');
            }).catch(error => {
                if (error instanceof SyntaxError) {
                    throw new Error(`Request failed with status ${response.status}`);
                }
                throw error;
            });
        }
        return response.json();
    }

    function storeSessionData(data) {
        localStorage.setItem('jwtToken', data.token);
        localStorage.setItem('userRole', data.role);
        localStorage.setItem('userEmail', data.email);
        localStorage.setItem('userName', data.firstName + ' ' + data.lastName);

        // Also store in sessionStorage for consistency with dashboard.js
        sessionStorage.setItem('jwt-token', data.token);
    }

    function clearSessionData() {
        localStorage.removeItem('jwtToken');
        localStorage.removeItem('userRole');
        localStorage.removeItem('userEmail');
        localStorage.removeItem('userName');
        sessionStorage.removeItem('jwt-token');
    }

    function validatePasswordStrength(password) {
        let strength = 0;

        if (password.length >= 8) strength += 20;
        if (/[A-Z]/.test(password)) strength += 20;
        if (/[a-z]/.test(password)) strength += 20;
        if (/[0-9]/.test(password)) strength += 20;
        if (/[^A-Za-z0-9]/.test(password)) strength += 20;

        return strength;
    }

    function checkIfTemporaryPassword(password) {
        return password.startsWith('SA2025-');
    }

    function showErrorMessage(message) {
        let errorAlert = document.getElementById('loginError');

        if (!errorAlert) {
            errorAlert = document.createElement('div');
            errorAlert.className = 'alert alert-danger mt-3';
            errorAlert.id = 'loginError';
            errorAlert.role = 'alert';

            const formEl = document.querySelector('.glass-card') || document.body;
            formEl.prepend(errorAlert);
        }

        errorAlert.textContent = message;
        errorAlert.classList.remove('d-none');
    }

    function showSuccessMessage(message) {
        let successAlert = document.getElementById('passwordSuccess');

        if (!successAlert) {
            successAlert = document.createElement('div');
            successAlert.className = 'alert alert-success mt-3';
            successAlert.id = 'passwordSuccess';
            successAlert.role = 'alert';

            const formEl = document.querySelector('#changePasswordForm') || document.body;
            formEl.prepend(successAlert);
        }

        successAlert.textContent = message;
        successAlert.classList.remove('d-none');
    }

    function hideErrorMessage() {
        const alerts = ['loginError', 'passwordError', 'passwordSuccess'];

        alerts.forEach(id => {
            const alert = document.getElementById(id);
            if (alert) {
                alert.classList.add('d-none');
            }
        });
    }
});