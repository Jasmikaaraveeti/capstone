/**
 * IntelliRoute Explorer - Authentication Handler
 */

const Auth = {
  TOKEN_KEY: 'intelliroute_jwt_token',
  USER_KEY: 'intelliroute_user_data',

  getToken() {
    return localStorage.getItem(this.TOKEN_KEY);
  },

  getUser() {
    const raw = localStorage.getItem(this.USER_KEY);
    return raw ? JSON.parse(raw) : null;
  },

  setAuth(authResponse) {
    if (authResponse && authResponse.token) {
      localStorage.setItem(this.TOKEN_KEY, authResponse.token);
      localStorage.setItem(this.USER_KEY, JSON.stringify({
        id: authResponse.id,
        name: authResponse.name,
        email: authResponse.email
      }));
    }
  },

  logout() {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
    window.location.href = 'login.html';
  },

  isAuthenticated() {
    const token = this.getToken();
    if (!token) return false;
    // Basic JWT expiry check
    try {
      const payloadBase64 = token.split('.')[1];
      const payload = JSON.parse(atob(payloadBase64));
      return payload.exp * 1000 > Date.now();
    } catch (e) {
      return false;
    }
  },

  requireAuth() {
    if (!this.isAuthenticated()) {
      window.location.href = 'login.html';
    }
  },

  redirectIfAuthenticated() {
    if (this.isAuthenticated()) {
      window.location.href = 'dashboard.html';
    }
  }
};

// Form listeners for Login and Register pages
document.addEventListener('DOMContentLoaded', () => {
  // Login Page Handler
  const loginForm = document.getElementById('loginForm');
  if (loginForm) {
    Auth.redirectIfAuthenticated();

    loginForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const alertBox = document.getElementById('alertBox');
      const submitBtn = document.getElementById('submitBtn');

      alertBox.style.display = 'none';
      alertBox.className = 'alert';

      const email = document.getElementById('email').value.trim();
      const password = document.getElementById('password').value;

      if (!email || !password) {
        showAlert(alertBox, 'Please enter both email and password.', 'alert-danger');
        return;
      }

      submitBtn.disabled = true;
      submitBtn.innerText = 'LOGGING IN...';

      try {
        const response = await fetch('/api/auth/login', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ email, password })
        });

        const data = await response.json();

        if (response.ok && data.token) {
          Auth.setAuth(data);
          showAlert(alertBox, 'Login successful! Redirecting to dashboard...', 'alert-success');
          setTimeout(() => {
            window.location.href = 'dashboard.html';
          }, 800);
        } else {
          showAlert(alertBox, data.error || 'Invalid email or password.', 'alert-danger');
          submitBtn.disabled = false;
          submitBtn.innerText = 'LOGIN';
        }
      } catch (err) {
        showAlert(alertBox, 'Connection error. Please verify the backend is running.', 'alert-danger');
        submitBtn.disabled = false;
        submitBtn.innerText = 'LOGIN';
      }
    });
  }

  // Registration Page Handler
  const registerForm = document.getElementById('registerForm');
  if (registerForm) {
    Auth.redirectIfAuthenticated();

    registerForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const alertBox = document.getElementById('alertBox');
      const submitBtn = document.getElementById('submitBtn');

      alertBox.style.display = 'none';
      alertBox.className = 'alert';

      const name = document.getElementById('name').value.trim();
      const email = document.getElementById('email').value.trim();
      const password = document.getElementById('password').value;
      const confirmPassword = document.getElementById('confirmPassword').value;

      // Front-end validations
      if (!name) {
        showAlert(alertBox, 'Full Name is required.', 'alert-danger');
        return;
      }
      if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        showAlert(alertBox, 'Please enter a valid email address.', 'alert-danger');
        return;
      }
      if (!password || password.length < 6) {
        showAlert(alertBox, 'Password must be at least 6 characters long.', 'alert-danger');
        return;
      }
      if (password !== confirmPassword) {
        showAlert(alertBox, 'Passwords do not match.', 'alert-danger');
        return;
      }

      submitBtn.disabled = true;
      submitBtn.innerText = 'CREATING ACCOUNT...';

      try {
        const response = await fetch('/api/auth/register', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ name, email, password, confirmPassword })
        });

        const data = await response.json();

        if (response.ok && data.token) {
          Auth.setAuth(data);
          showAlert(alertBox, 'Account registered successfully! Redirecting to dashboard...', 'alert-success');
          setTimeout(() => {
            window.location.href = 'dashboard.html';
          }, 900);
        } else {
          showAlert(alertBox, data.error || 'Registration failed. Email may already be in use.', 'alert-danger');
          submitBtn.disabled = false;
          submitBtn.innerText = 'REGISTER';
        }
      } catch (err) {
        showAlert(alertBox, 'Connection error. Please verify the backend server is reachable.', 'alert-danger');
        submitBtn.disabled = false;
        submitBtn.innerText = 'REGISTER';
      }
    });
  }
});

function showAlert(box, msg, typeClass) {
  if (!box) return;
  box.className = 'alert ' + typeClass;
  box.innerText = msg;
  box.style.display = 'flex';
}
