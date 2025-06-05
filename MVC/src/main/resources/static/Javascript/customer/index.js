/**
 * Customer Authentication Script for Index Page
 * Handles UI changes based on authentication status
 */
document.addEventListener('DOMContentLoaded', function() {
    // Initialize AOS animation library if it exists
    if (typeof AOS !== 'undefined') {
        AOS.init({
            duration: 800,
            easing: 'ease-in-out',
            once: true
        });
    }

    // Check if user is logged in - check on each page load
    checkAuthAndUpdateUI();

    /**
     * Check authentication status and update UI accordingly
     */
    function checkAuthAndUpdateUI() {
        const token = sessionStorage.getItem('authToken');
        const userInfoStr = sessionStorage.getItem('userInfo');

        if (token && userInfoStr) {
            try {
                // Parse user info
                const userInfo = JSON.parse(userInfoStr);

                // Handle UI changes for logged-in user
                handleLoggedInUser(userInfo);

                // Verify token validity in background
                validateTokenInBackground(token);
            } catch (error) {
                console.error('Error parsing user info:', error);
                // Clear invalid session data
                sessionStorage.removeItem('authToken');
                sessionStorage.removeItem('userInfo');
                handleNonLoggedInUser();
            }
        } else {
            // Handle UI for non-logged-in user
            handleNonLoggedInUser();
        }
    }

    /**
     * Validate token with backend
     */
    function validateTokenInBackground(token) {
        fetch('/api/customer/auth/validate-token', {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        })
            .then(response => {
                if (!response.ok) {
                    throw new Error('Token validation failed');
                }
                return response.json();
            })
            .then(data => {
                if (!data.valid) {
                    console.warn('Token invalid, logging out');
                    logout(false); // Silent logout (no message)
                } else {
                    // Update user info from server in case anything changed
                    if (data.user) {
                        const currentInfo = JSON.parse(sessionStorage.getItem('userInfo') || '{}');
                        const updatedInfo = {
                            ...currentInfo,
                            ...data.user
                        };
                        sessionStorage.setItem('userInfo', JSON.stringify(updatedInfo));
                    }
                }
            })
            .catch(error => {
                console.error('Token validation error:', error);
                // Don't logout on network errors to prevent poor user experience
            });
    }

    /**
     * Handle UI changes for logged-in user
     */
    function handleLoggedInUser(userInfo) {
        // Get navigation elements
        const loginBtn = document.querySelector('a.btn-login');
        const signupBtn = document.querySelector('a.btn-signup');

        if (!loginBtn || !signupBtn) {
            console.warn('Login/signup buttons not found');
            return;
        }

        // Hide signup button
        signupBtn.style.display = 'none';

        // Check if user dropdown already exists
        const existingDropdown = document.querySelector('.dropdown #userDropdown');
        if (!existingDropdown) {
            // Replace login button with user dropdown
            const dropdownHtml = `
                <div class="dropdown">
                    <button class="btn btn-user dropdown-toggle" type="button" id="userDropdown" data-bs-toggle="dropdown" aria-expanded="false">
                        <i class="bi bi-person-circle me-1"></i> ${userInfo.firstName}
                    </button>
                    <ul class="dropdown-menu dropdown-menu-end" aria-labelledby="userDropdown">
                        <li><a class="dropdown-item" href="/customer/profile">My Profile</a></li>
                        <li><a class="dropdown-item" href="/customer/myVehicles">My Vehicles</a></li>
                        <li><a class="dropdown-item" href="/customer/serviceHistory">Service History</a></li>
                        <li><hr class="dropdown-divider"></li>
                        <li><a class="dropdown-item" href="#" id="logoutBtn">Logout</a></li>
                    </ul>
                </div>
            `;

            // Replace login button with dropdown
            const btnContainer = loginBtn.parentNode;
            btnContainer.innerHTML = dropdownHtml;

            // Add logout functionality
            document.getElementById('logoutBtn').addEventListener('click', function(e) {
                e.preventDefault();
                logout(true); // Show logout message
            });
        }

        // Update Book Service buttons
        updateBookServiceButtons(true);

        // Show premium badge if user has premium membership
        if (userInfo.membershipType === 'PREMIUM') {
            addPremiumBadge();
        }
    }

    /**
     * Handle UI for non-logged-in user
     */
    function handleNonLoggedInUser() {
        // Make sure login and signup buttons are visible and correctly linked
        const loginBtn = document.querySelector('a.btn-login');
        const signupBtn = document.querySelector('a.btn-signup');
        const userDropdown = document.querySelector('.dropdown');

        if (loginBtn && signupBtn) {
            loginBtn.style.display = '';
            signupBtn.style.display = '';
            loginBtn.href = '/authentication/login';
            signupBtn.href = '/authentication/login';
        } else if (userDropdown) {
            // Replace user dropdown with login/signup buttons
            const btnContainer = userDropdown.parentNode;
            btnContainer.innerHTML = `
                <a href="/authentication/login" class="btn btn-login me-2">Login</a>
                <a href="/authentication/login" class="btn btn-signup">Signup</a>
            `;
        }

        // Update Book Service buttons to redirect to login
        updateBookServiceButtons(false);
    }

    /**
     * Update all Book Service buttons based on login status
     */
    function updateBookServiceButtons(isLoggedIn) {
        // Book Service button functionality (hero section and CTA section)
        const bookServiceBtns = document.querySelectorAll('.btn-hero, .btn-cta');

        bookServiceBtns.forEach(btn => {
            // Remove existing event listeners by cloning the node
            const newBtn = btn.cloneNode(true);
            if (btn.parentNode) {
                btn.parentNode.replaceChild(newBtn, btn);
            }

            newBtn.addEventListener('click', function(e) {
                e.preventDefault();

                if (isLoggedIn) {
                    // Redirect to book service page
                    window.location.href = '/customer/bookService';
                } else {
                    // Redirect to login page with a message
                    window.location.href = '/authentication/login?message=' +
                        encodeURIComponent('Please login to book a service') +
                        '&type=info';
                }
            });
        });
    }

    /**
     * Add premium badge for premium members
     */
    function addPremiumBadge() {
        const userDropdown = document.getElementById('userDropdown');
        if (userDropdown && !userDropdown.querySelector('.badge')) {
            // Add premium badge next to username
            const premiumBadge = document.createElement('span');
            premiumBadge.className = 'badge bg-warning text-dark ms-1';
            premiumBadge.textContent = 'PREMIUM';
            userDropdown.appendChild(premiumBadge);

            // Also add to dropdown menu
            const dropdownMenu = userDropdown.nextElementSibling;
            if (dropdownMenu) {
                // Check if premium item already exists
                if (!dropdownMenu.querySelector('.premium-item')) {
                    const firstItem = dropdownMenu.querySelector('.dropdown-item');
                    if (firstItem) {
                        // Add premium item in dropdown
                        const premiumItem = document.createElement('li');
                        premiumItem.className = 'premium-item';
                        premiumItem.innerHTML = `
                            <a class="dropdown-item d-flex align-items-center" href="/customer/membership">
                                <span class="badge bg-warning text-dark me-2">PREMIUM</span>
                                Membership Benefits
                            </a>
                        `;
                        dropdownMenu.insertBefore(premiumItem, firstItem);
                    }
                }
            }
        }
    }

    /**
     * Logout function
     * @param {boolean} showMessage Whether to show a logout message
     */
    function logout(showMessage = true) {
        // Clear session storage
        sessionStorage.removeItem('authToken');
        sessionStorage.removeItem('userInfo');

        // Show toast notification if requested
        if (showMessage) {
            showToast('You have been logged out successfully', 'success');
        }

        // Update UI immediately
        handleNonLoggedInUser();

        // Redirect to home page after short delay if showing message
        if (showMessage) {
            setTimeout(() => {
                window.location.href = '/';
            }, 1000);
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

    // Navbar scroll effect
    function handleNavbarScroll() {
        const navbar = document.querySelector('.navbar');
        if (navbar) {
            if (window.scrollY > 50) {
                navbar.classList.add('scrolled');
            } else {
                navbar.classList.remove('scrolled');
            }
        }
    }

    // Add scroll event listener
    window.addEventListener('scroll', handleNavbarScroll);
    // Initial call to set correct state
    handleNavbarScroll();

    // Check if redirected with message parameter
    const urlParams = new URLSearchParams(window.location.search);
    const messageParam = urlParams.get('message');
    const messageType = urlParams.get('type') || 'info';

    if (messageParam) {
        showToast(decodeURIComponent(messageParam), messageType);
        // Remove the parameters from URL
        window.history.replaceState({}, document.title, window.location.pathname);
    }
});