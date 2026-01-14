// --- JWT helpers ---
const TOKEN_KEY = "jwt_token";
let authMode = "login"; // "login" | "register"

function setToken(token) { localStorage.setItem(TOKEN_KEY, token); }
function getToken() { return localStorage.getItem(TOKEN_KEY); }
function clearToken() { localStorage.removeItem(TOKEN_KEY); }

function authHeaders() {
  const token = getToken();
  return token ? { "Authorization": `Bearer ${token}` } : {};
}

async function apiFetch(url, options = {}) {
  const headers = { ...(options.headers || {}), ...authHeaders() };
  const res = await fetch(url, { ...options, headers });

  if (res.status === 401) {
    clearToken();
    setLoggedOutUI("Du må logge inn.");
  }
  return res;
}

function parseJwtEmail(token) {
  // JWT payload er base64url. Dette er kun for å vise e-post i UI (ikke for sikkerhet).
  try {
    const payload = token.split(".")[1];
    const base64 = payload.replace(/-/g, "+").replace(/_/g, "/");
    const json = decodeURIComponent(atob(base64).split("").map(c => `%${("00"+c.charCodeAt(0).toString(16)).slice(-2)}`).join(""));
    return JSON.parse(json).sub || "Innlogget";
  } catch {
    return "Innlogget";
  }
}

// --- DOM ---
const whoamiEl = document.getElementById("whoami");
const logoutBtn = document.getElementById("logoutBtn");

const authCard = document.getElementById("authCard");
const authForm = document.getElementById("authForm");
const authEmail = document.getElementById("authEmail");
const authPassword = document.getElementById("authPassword");
const authMsg = document.getElementById("authMsg");
const authSubmitBtn = document.getElementById("authSubmitBtn");

const appCard = document.getElementById("appCard");

let currentFilter = "ALL";
const listEl = document.getElementById("list");
const statsEl = document.getElementById("stats");
const form = document.getElementById("createForm");
const formMsg = document.getElementById("formMsg");

// --- Tabs (login/register) ---
document.querySelectorAll(".tab").forEach(btn => {
  btn.addEventListener("click", () => {
    document.querySelectorAll(".tab").forEach(b => b.classList.remove("active"));
    btn.classList.add("active");
    authMode = btn.dataset.tab;
    authMsg.textContent = "";

    if (authMode === "login") {
      authSubmitBtn.textContent = "Logg inn";
      authPassword.autocomplete = "current-password";
      document.querySelector("#authCard h2").textContent = "Logg inn";
    } else {
      authSubmitBtn.textContent = "Registrer";
      authPassword.autocomplete = "new-password";
      document.querySelector("#authCard h2").textContent = "Registrer";
    }
  });
});

// --- Auth submit ---
authForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  authMsg.textContent = "";

  const email = (authEmail.value || "").trim().toLowerCase();
  const password = authPassword.value || "";

  if (!email || !password || password.length < 6) {
    authMsg.textContent = "Skriv inn gyldig e-post og passord (minst 6 tegn).";
    return;
  }

  const endpoint = authMode === "login" ? "/api/auth/login" : "/api/auth/register";

  const res = await fetch(endpoint, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password })
  });

  if (!res.ok) {
    const text = await res.text();
    authMsg.textContent = text || "Kunne ikke autentisere.";
    return;
  }

  const data = await res.json(); // { token: "..." }
  setToken(data.token);
  setLoggedInUI();
  await load();
});

logoutBtn.addEventListener("click", () => {
  clearToken();
  setLoggedOutUI("Du er logget ut.");
});

// --- App filters ---
document.querySelectorAll(".filter").forEach(btn => {
  btn.addEventListener("click", () => {
    document.querySelectorAll(".filter").forEach(b => b.classList.remove("active"));
    btn.classList.add("active");
    currentFilter = btn.dataset.filter;
    load();
  });
});

// --- Create form ---
form.addEventListener("submit", async (e) => {
  e.preventDefault();
  formMsg.textContent = "";

  const data = new FormData(form);
  const payload = {
    company: data.get("company"),
    role: data.get("role"),
    link: data.get("link"),
    deadline: data.get("deadline")
  };

  const res = await apiFetch("/api/apps", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload)
  });

  if (!res.ok) {
    const text = await res.text();
    formMsg.textContent = text || "Noe gikk galt";
    return;
  }

  form.reset();
  await load();
});

// --- Helpers ---
function badge(status) {
  const map = {
    PLANLAGT: "Planlagt",
    SOKT: "Søkt",
    INTERVJU: "Intervju",
    AVSLATT: "Avslått",
    TILBUD: "Tilbud"
  };
  return map[status] ?? status;
}

function fmtDate(d) {
  if (!d) return "—";
  return d;
}

function isOverdue(deadline, status) {
  if (!deadline) return false;
  if (status === "AVSLATT" || status === "TILBUD") return false;
  const today = new Date();
  const dd = new Date(deadline + "T00:00:00");
  return dd < new Date(today.getFullYear(), today.getMonth(), today.getDate());
}

async function updateStatus(id, status) {
  const res = await apiFetch(`/api/apps/${id}/status`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ status })
  });
  if (!res.ok) {
    const text = await res.text();
    alert(text || "Kunne ikke oppdatere status");
    return;
  }
  await load();
}

async function removeItem(id) {
  const res = await apiFetch(`/api/apps/${id}`, { method: "DELETE" });
  if (!res.ok && res.status !== 204) {
    const text = await res.text();
    alert(text || "Kunne ikke slette");
    return;
  }
  await load();
}

function render(apps) {
  const filtered = currentFilter === "ALL"
    ? apps
    : apps.filter(a => a.status === currentFilter);

  const counts = apps.reduce((acc, a) => {
    acc[a.status] = (acc[a.status] || 0) + 1;
    return acc;
  }, {});
  statsEl.textContent =
    `Planlagt: ${counts.PLANLAGT || 0} • Søkt: ${counts.SOKT || 0} • Intervju: ${counts.INTERVJU || 0} • Tilbud: ${counts.TILBUD || 0} • Avslått: ${counts.AVSLATT || 0}`;

  if (filtered.length === 0) {
    listEl.innerHTML = `<div class="muted">Ingen treff.</div>`;
    return;
  }

  listEl.innerHTML = "";
  for (const a of filtered) {
    const el = document.createElement("div");
    el.className = "item" + (isOverdue(a.deadline, a.status) ? " overdue" : "");

    const link = a.link
      ? `<a href="${a.link}" target="_blank" rel="noreferrer">Link</a>`
      : `<span class="muted">Ingen link</span>`;

    el.innerHTML = `
      <div class="top">
        <div>
          <div class="title">${escapeHtml(a.company)} — ${escapeHtml(a.role)}</div>
          <div class="meta">
            <span>Frist: <span class="badge">${fmtDate(a.deadline)}</span></span>
            <span>Status: <span class="badge">${badge(a.status)}</span></span>
            <span>${link}</span>
          </div>
        </div>
        <div class="actions">
          <select data-id="${a.id}" class="statusSelect">
            ${["PLANLAGT","SOKT","INTERVJU","AVSLATT","TILBUD"].map(s =>
              `<option value="${s}" ${s===a.status ? "selected" : ""}>${badge(s)}</option>`
            ).join("")}
          </select>
          <button data-del="${a.id}">Slett</button>
        </div>
      </div>
    `;
    listEl.appendChild(el);
  }

  document.querySelectorAll(".statusSelect").forEach(sel => {
    sel.addEventListener("change", (e) => updateStatus(e.target.dataset.id, e.target.value));
  });
  document.querySelectorAll("button[data-del]").forEach(btn => {
    btn.addEventListener("click", () => removeItem(btn.dataset.del));
  });
}

async function load() {
  const res = await apiFetch("/api/apps");
  if (!res.ok) return;
  const apps = await res.json();
  render(apps);
}

function escapeHtml(str) {
  return (str ?? "")
    .replaceAll("&","&amp;")
    .replaceAll("<","&lt;")
    .replaceAll(">","&gt;")
    .replaceAll('"',"&quot;");
}

// --- UI state ---
function setLoggedInUI() {
  const token = getToken();
  const email = token ? parseJwtEmail(token) : "Innlogget";
  whoamiEl.textContent = email;
  logoutBtn.classList.remove("hidden");

  authCard.classList.add("hidden");
  appCard.classList.remove("hidden");
}

function setLoggedOutUI(message) {
  whoamiEl.textContent = "Ikke innlogget";
  logoutBtn.classList.add("hidden");

  authCard.classList.remove("hidden");
  appCard.classList.add("hidden");

  if (message) authMsg.textContent = message;
}

// --- Init ---
(function init() {
  if (getToken()) {
    setLoggedInUI();
    load();
  } else {
    setLoggedOutUI("");
  }
})();
