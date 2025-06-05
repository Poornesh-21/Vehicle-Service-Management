document.addEventListener('DOMContentLoaded', function() {
    // Initialize AOS animation library if available
    if (typeof AOS !== 'undefined') {
        AOS.init({
            duration: 800,
            easing: 'ease-in-out',
            once: true
        });
    }

    // Set active navbar item
    setActiveNavItem();

    // Navbar scroll effect
    window.addEventListener('scroll', handleNavbarScroll);
    handleNavbarScroll();

    // Check authentication status and update UI
    checkAuthStatus();
});

/**
 * Set active state for navbar items
 */
function setActiveNavItem() {
    // Get all nav links
    const navLinks = document.querySelectorAll('.navbar-nav .nav-link');

    // Remove active class from all links
    navLinks.forEach(link => {
        link.classList.remove('active');
    });

    // Set active class on the About Us link
    const aboutLink = document.querySelector('.navbar-nav .nav-link[href="/customer/aboutUs"]');
    if (aboutLink) {
        aboutLink.classList.add('active');
    } else {
        // Fallback: look for link with text "About Us"
        navLinks.forEach(link => {
            if (link.textContent.trim() === 'About Us') {
                link.classList.add('active');
            }
        });
    }
}

/**
 * Handle navbar scroll effect
 */
function handleNavbarScroll() {
    const navbar = document.querySelector('.navbar');
    if (navbar) {
        navbar.classList.toggle('scrolled', window.scrollY > 50);
    }
}

/**
 * Check authentication status and update UI
 */
function checkAuthStatus() {
    const token = sessionStorage.getItem('authToken');
    const userInfoStr = sessionStorage.getItem('userInfo');

    if (token && userInfoStr) {
        try {
            const userInfo = JSON.parse(userInfoStr);
            updateNavbarForLoggedInUser(userInfo);
        } catch (error) {
            console.error("Error processing auth data:", error);
            sessionStorage.removeItem('authToken');
            sessionStorage.removeItem('userInfo');
        }
    }
}

/**
 * Update navbar for logged in user
 */
function updateNavbarForLoggedInUser(userInfo) {
    // Get navigation elements
    const authContainer = document.querySelector('.ms-lg-3');
    if (!authContainer) return;

    // Create user dropdown HTML
    const dropdownHtml = `
        <div class="dropdown">
            <button class="btn btn-primary dropdown-toggle" type="button" id="userDropdown" data-bs-toggle="dropdown" aria-expanded="false">
                <i class="bi bi-person-circle me-1"></i> ${userInfo.firstName}
                ${userInfo.membershipType === 'PREMIUM' ? '<span class="badge bg-warning text-dark ms-1">PREMIUM</span>' : ''}
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

    // Replace auth container content
    authContainer.innerHTML = dropdownHtml;

    // Add logout handler
    document.getElementById('logoutBtn')?.addEventListener('click', function(e) {
        e.preventDefault();
        logout();
    });
}

/**
 * Handle user logout
 */
function logout() {
    sessionStorage.removeItem('authToken');
    sessionStorage.removeItem('userInfo');
    window.location.href = '/';
}