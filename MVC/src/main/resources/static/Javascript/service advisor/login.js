document.addEventListener('DOMContentLoaded', function() {
    const togglePassword = document.getElementById('togglePassword');
    const password = document.getElementById('password');
    const eyeIcon = document.getElementById('eyeIcon');

    if (togglePassword && password && eyeIcon) {
        togglePassword.addEventListener('click', function() {
            const type = password.getAttribute('type') === 'password' ? 'text' : 'password';
            password.setAttribute('type', type);

            eyeIcon.classList.toggle('fa-eye');
            eyeIcon.classList.toggle('fa-eye-slash');
        });
    }

    const toggleNewPassword = document.getElementById('toggleNewPassword');
    const newPassword = document.getElementById('newPassword');
    const newEyeIcon = document.getElementById('newEyeIcon');

    if (toggleNewPassword && newPassword && newEyeIcon) {
        toggleNewPassword.addEventListener('click', function() {
            const type = newPassword.getAttribute('type') === 'password' ? 'text' : 'password';
            newPassword.setAttribute('type', type);

            newEyeIcon.classList.toggle('fa-eye');
            newEyeIcon.classList.toggle('fa-eye-slash');
        });
    }

    const toggleConfirmPassword = document.getElementById('toggleConfirmPassword');
    const confirmPassword = document.getElementById('confirmPassword');
    const confirmEyeIcon = document.getElementById('confirmEyeIcon');

    if (toggleConfirmPassword && confirmPassword && confirmEyeIcon) {
        toggleConfirmPassword.addEventListener('click', function() {
            const type = confirmPassword.getAttribute('type') === 'password' ? 'text' : 'password';
            confirmPassword.setAttribute('type', type);

            confirmEyeIcon.classList.toggle('fa-eye');
            confirmEyeIcon.classList.toggle('fa-eye-slash');
        });
    }

    if (newPassword) {
        newPassword.addEventListener('input', function() {
            const value = newPassword.value;
            let strength = 0;

            const lengthCheck = document.getElementById('length');
            if (value.length >= 8) {
                lengthCheck.innerHTML = '<i class="fas fa-check-circle"></i> At least 8 characters';
                lengthCheck.classList.add('requirement-met');
                strength += 20;
            } else {
                lengthCheck.innerHTML = '<i class="fas fa-times-circle"></i> At least 8 characters';
                lengthCheck.classList.remove('requirement-met');
            }

            const uppercaseCheck = document.getElementById('uppercase');
            if (/[A-Z]/.test(value)) {
                uppercaseCheck.innerHTML = '<i class="fas fa-check-circle"></i> At least 1 uppercase letter';
                uppercaseCheck.classList.add('requirement-met');
                strength += 20;
            } else {
                uppercaseCheck.innerHTML = '<i class="fas fa-times-circle"></i> At least 1 uppercase letter';
                uppercaseCheck.classList.remove('requirement-met');
            }

            const lowercaseCheck = document.getElementById('lowercase');
            if (/[a-z]/.test(value)) {
                lowercaseCheck.innerHTML = '<i class="fas fa-check-circle"></i> At least 1 lowercase letter';
                lowercaseCheck.classList.add('requirement-met');
                strength += 20;
            } else {
                lowercaseCheck.innerHTML = '<i class="fas fa-times-circle"></i> At least 1 lowercase letter';
                lowercaseCheck.classList.remove('requirement-met');
            }

            const numberCheck = document.getElementById('number');
            if (/[0-9]/.test(value)) {
                numberCheck.innerHTML = '<i class="fas fa-check-circle"></i> At least 1 number';
                numberCheck.classList.add('requirement-met');
                strength += 20;
            } else {
                numberCheck.innerHTML = '<i class="fas fa-times-circle"></i> At least 1 number';
                numberCheck.classList.remove('requirement-met');
            }

            const specialCheck = document.getElementById('special');
            if (/[^A-Za-z0-9]/.test(value)) {
                specialCheck.innerHTML = '<i class="fas fa-check-circle"></i> At least 1 special character';
                specialCheck.classList.add('requirement-met');
                strength += 20;
            } else {
                specialCheck.innerHTML = '<i class="fas fa-times-circle"></i> At least 1 special character';
                specialCheck.classList.remove('requirement-met');
            }

            const strengthProgress = document.getElementById('strengthProgress');
            const strengthText = document.getElementById('strengthText');

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
        });
    }

    if (confirmPassword) {
        confirmPassword.addEventListener('input', function() {
            const passwordMismatch = document.getElementById('passwordMismatch');

            if (newPassword.value !== confirmPassword.value) {
                passwordMismatch.classList.remove('d-none');
            } else {
                passwordMismatch.classList.add('d-none');
            }
        });
    }

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

            fetch('/serviceAdvisor/api/login', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(requestData)
            })
                .then(response => {
                    if (!response.ok) {
                        return response.json().then(errorData => {
                            throw new Error(errorData.message || 'Authentication failed');
                        });
                    }
                    return response.json();
                })
                .then(data => {
                    if (data.role && data.role.toLowerCase() !== 'serviceadvisor') {
                        throw new Error('You do not have permission to access the Service Advisor portal');
                    }

                    localStorage.setItem('jwtToken', data.token);
                    localStorage.setItem('userRole', data.role);
                    localStorage.setItem('userEmail', data.email);
                    localStorage.setItem('userName', data.firstName + ' ' + data.lastName);

                    const isTemporaryPassword = checkIfTemporaryPassword(passwordValue);

                    if (isTemporaryPassword) {
                        document.getElementById('currentPassword').value = passwordValue;

                        const changePasswordModal = new bootstrap.Modal(document.getElementById('changePasswordModal'));
                        changePasswordModal.show();

                        loginBtn.disabled = false;
                        loginBtn.innerHTML = originalText;
                    } else {
                        window.location.href = '/serviceAdvisor/dashboard?token=' + data.token;
                    }
                })
                .catch(error => {
                    showErrorMessage(error.message || 'Authentication failed. Please check your credentials.');

                    loginBtn.disabled = false;
                    loginBtn.innerHTML = originalText;
                });
        });
    }

    const changePasswordForm = document.getElementById('changePasswordForm');

    if (changePasswordForm) {
        changePasswordForm.addEventListener('submit', function(e) {
            e.preventDefault();

            const newPasswordValue = newPassword.value;
            const confirmPasswordValue = confirmPassword.value;
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

            fetch('/serviceAdvisor/api/change-password?token=' + token, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': 'Bearer ' + token
                },
                body: JSON.stringify(requestData)
            })
                .then(response => {
                    if (!response.ok) {
                        return response.json().then(errorData => {
                            throw new Error(errorData.error || 'Failed to change password');
                        });
                    }
                    return response.json();
                })
                .then(data => {
                    if (data.token) {
                        localStorage.setItem('jwtToken', data.token);
                    }

                    showSuccessMessage('Password changed successfully! Redirecting to dashboard...');

                    setTimeout(() => {
                        window.location.href = '/serviceAdvisor/dashboard?token=' + (data.token || token);
                    }, 1500);
                })
                .catch(error => {
                    submitBtn.disabled = false;
                    submitBtn.innerHTML = originalBtnText;

                    showErrorMessage(error.message || 'Failed to change password. Please try again.');
                });
        });
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
        const errorAlert = document.getElementById('loginError');
        if (errorAlert) {
            errorAlert.classList.add('d-none');
        }

        const passwordError = document.getElementById('passwordError');
        if (passwordError) {
            passwordError.classList.add('d-none');
        }

        const successAlert = document.getElementById('passwordSuccess');
        if (successAlert) {
            successAlert.classList.add('d-none');
        }
    }

    function checkIfTemporaryPassword(password) {
        return password.startsWith('SA2025-');
    }

    const existingToken = localStorage.getItem('jwtToken');
    const role = localStorage.getItem('userRole');

    if (existingToken && role && role.toLowerCase() === 'serviceadvisor') {
        fetch('/serviceAdvisor/api/validate-token', {
            method: 'GET',
            headers: {
                'Authorization': 'Bearer ' + existingToken
            }
        })
            .then(response => {
                if (response.ok) {
                    window.location.href = '/serviceAdvisor/dashboard?token=' + existingToken;
                } else {
                    localStorage.removeItem('jwtToken');
                    localStorage.removeItem('userRole');
                    localStorage.removeItem('userEmail');
                    localStorage.removeItem('userName');
                }
            })
            .catch(error => {
                localStorage.removeItem('jwtToken');
                localStorage.removeItem('userRole');
                localStorage.removeItem('userEmail');
                localStorage.removeItem('userName');
            });
    }
});

(function() {
    function patchFetch() {
        const originalFetch = window.fetch;

        window.fetch = function(url, options) {
            return originalFetch(url, options)
                .then(response => {
                    if (url === '/serviceAdvisor/api/login' && !response.ok) {
                        const clonedResponse = response.clone();

                        return clonedResponse.json().then(errorData => {
                            if (errorData && errorData.message &&
                                errorData.message.includes('Invalid email/password combination')) {

                                const modifiedData = {
                                    ...errorData,
                                    message: 'Invalid email or password. Please try again.'
                                };

                                const modifiedResponse = new Response(
                                    JSON.stringify(modifiedData),
                                    {
                                        status: response.status,
                                        statusText: response.statusText,
                                        headers: response.headers
                                    }
                                );

                                return modifiedResponse;
                            }

                            return response;
                        }).catch(() => {
                            return response;
                        });
                    }

                    return response;
                });
        };
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', patchFetch);
    } else {
        patchFetch();
    }
})();