// ===================================================================
// CampusEase shared frontend API helper
// Used by every page (login, dashboard, menu, cart, orders, admin).
// Change API_BASE_URL to wherever your Spring Boot backend is running.
// ===================================================================

const API_BASE_URL = "https://campusease-backend-3xag.onrender.com/api";

const Auth = {
  saveSession(authResponse) {
    localStorage.setItem("ce_token", authResponse.token);
    localStorage.setItem("ce_userId", authResponse.userId);
    localStorage.setItem("ce_username", authResponse.username);
    localStorage.setItem("ce_email", authResponse.email);
    localStorage.setItem("ce_role", authResponse.role);
  },
  getToken() {
    return localStorage.getItem("ce_token");
  },
  getRole() {
    return localStorage.getItem("ce_role");
  },
  getUsername() {
    return localStorage.getItem("ce_username");
  },
  isLoggedIn() {
    return !!this.getToken();
  },
  isAdmin() {
    return this.getRole() === "ROLE_ADMIN";
  },
  isVendor() {
    return this.getRole() === "ROLE_VENDOR";
  },
  logout() {
    localStorage.removeItem("ce_token");
    localStorage.removeItem("ce_userId");
    localStorage.removeItem("ce_username");
    localStorage.removeItem("ce_email");
    localStorage.removeItem("ce_role");
    window.location.href = "index.html";
  },
  requireLogin() {
    if (!this.isLoggedIn()) {
      window.location.href = "index.html";
    }
  },
  requireAdmin() {
    this.requireLogin();
    if (!this.isAdmin()) {
      alert("Admin access only.");
      window.location.href = "try.html";
    }
  },
  requireVendor() {
    this.requireLogin();
    if (!this.isVendor()) {
      alert("Vendor access only.");
      window.location.href = "try.html";
    }
  }
};

async function apiFetch(path, { method = "GET", body = null, auth = true } = {}) {
  const headers = { "Content-Type": "application/json" };
  if (auth && Auth.getToken()) {
    headers["Authorization"] = "Bearer " + Auth.getToken();
  }

  let response;
  try {
    response = await fetch(API_BASE_URL + path, {
      method,
      headers,
      body: body ? JSON.stringify(body) : null
    });
  } catch (networkErr) {
    throw new Error("Could not reach the server. Is the backend running at " + API_BASE_URL + "?");
  }

  if (response.status === 401) {
    Auth.logout();
    throw new Error("Session expired. Please log in again.");
  }

  const text = await response.text();
  const data = text ? JSON.parse(text) : null;

  if (!response.ok) {
    const message = (data && data.message) ? data.message : "Request failed (" + response.status + ")";
    throw new Error(message);
  }
  return data;
}

function showError(message) {
  const box = document.getElementById("errorBox");
  if (box) {
    box.textContent = message;
    box.style.display = "block";
  } else {
    alert(message);
  }
}