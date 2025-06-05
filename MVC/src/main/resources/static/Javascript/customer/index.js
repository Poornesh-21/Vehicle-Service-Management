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

    // Check if user is logged in
    const token = sessionStorage.getItem('authToken');
    const userInfoStr = sessionStorage.getItem('userInfo');

    // Elements in the header
    const loginBtn = document.querySelector('a.btn-login');
    const signupBtn = document.querySelector('a.btn-signup');
    const navbarNav = document.querySelector('#navbarNav');

    if (token && userInfoStr) {
        try {
            // Parse user info
            const userInfo = JSON.parse(userInfoStr);

            // Handle UI changes for logged-in user
            handleLoggedInUser(userInfo);
        } catch (error) {
            console.error('Error parsing user info:', error);
            // Clear invalid session data
            sessionStorage.removeItem('authToken');
            sessionStorage.removeItem('userInfo');
        }
    } else {
        // Handle UI for non-logged-in user
        handleNonLoggedInUser();
    }

    /**
     * Handle UI changes for logged-in user
     */
    function handleLoggedInUser(userInfo) {
        if (loginBtn && signupBtn) {
            // Hide signup button
            signupBtn.style.display = 'none';

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
            loginBtn.parentNode.innerHTML = dropdownHtml;

            // Add logout functionality
            document.getElementById('logoutBtn').addEventListener('click', function(e) {
                e.preventDefault();
                logout();
            });
        }

        // Update Book Service buttons
        updateBookServiceButtons(true);

        // Show premium badge if user has premium membership
        if (userInfo.membershipType === 'PREMIUM') {
            addPremiumBadge();
        }

        // Validate token in background
        validateTokenInBackground();
    }

    /**
     * Handle UI for non-logged-in user
     */
    function handleNonLoggedInUser() {
        // Make sure login and signup buttons are visible and correctly linked
        if (loginBtn && signupBtn) {
            loginBtn.style.display = '';
            signupBtn.style.display = '';

            loginBtn.href = '/authentication/login';
            signupBtn.href = '/authentication/login';
        }

        // Update Book Service buttons
        updateBookServiceButtons(false);
    }

    /**
     * Update all Book Service buttons based on login status
     */
    function updateBookServiceButtons(isLoggedIn) {
        // Book Service button functionality (hero section and CTA section)
        const bookServiceBtns = document.querySelectorAll('.btn-hero, .btn-cta');

        bookServiceBtns.forEach(btn => {
            if (!btn.dataset.listenerAdded) {
                btn.dataset.listenerAdded = 'true';

                btn.addEventListener('click', function(e) {
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
            }
        });
    }

    /**
     * Add premium badge for premium members
     */
    function addPremiumBadge() {
        const userDropdown = document.getElementById('userDropdown');
        if (userDropdown) {
            // Add premium badge next to username
            const premiumBadge = document.createElement('span');
            premiumBadge.className = 'badge bg-warning text-dark ms-1';
            premiumBadge.textContent = 'PREMIUM';
            userDropdown.appendChild(premiumBadge);

            // Also add to dropdown menu
            const dropdownMenu = userDropdown.nextElementSibling;
            if (dropdownMenu) {
                const firstItem = dropdownMenu.querySelector('.dropdown-item');
                if (firstItem) {
                    // Add premium item in dropdown
                    const premiumItem = document.createElement('li');
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

    /**
     * Validate token in background to ensure it's still valid
     */
    function validateTokenInBackground() {
        const token = sessionStorage.getItem('authToken');
        if (!token) return;

        fetch('/api/customer/auth/validate-token', {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        })
            .then(response => response.json())
            .then(data => {
                if (!data.valid) {
                    console.warn('Token invalid, logging out');
                    logout();
                }
            })
            .catch(error => {
                console.error('Token validation error:', error);
                // Don't logout on network errors to prevent poor user experience
            });
    }

    /**
     * Logout function
     */
    function logout() {
        // Clear session storage
        sessionStorage.removeItem('authToken');
        sessionStorage.removeItem('userInfo');

        // Show toast notification
        showToast('You have been logged out successfully', 'success');

        // Redirect to home page after short delay
        setTimeout(() => {
            window.location.href = '/';
        }, 1000);
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
        const newUrl = window.location.pathname;
        window.history.replaceState({}, document.title, newUrl);
    }
});