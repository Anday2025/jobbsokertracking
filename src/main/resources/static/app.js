// -----------------------
// Keys / state
// -----------------------
const TOKEN_KEY = "jwt_token";
const LANG_KEY = "lang"; // "no" | "en"
let currentFilter = "ALL";
let authMode = "login"; // login | register
let lastApps = [];

// -----------------------
// i18n
// -----------------------
const I18N = {
  no: {
    title: "Jobbsøker-tracker",
    subtitle: "Hold oversikt over søknadene dine.",
    login: "Logg inn",
    logout: "Logg ut",
    email: "E-post",
    password: "Passord",
    createUser: "Opprett bruker",
    haveUser: "Har du bruker? Logg inn",
    lockedHint: "Logg inn for å lagre og se dine søknader.",
    newTitle: "Ny søknad",
    companyLabel: "Firma *",
    companyPh: "F.eks. NAV / Telenor",
    roleLabel: "Stilling *",
    rolePh: "F.eks. Junior utvikler",
    linkLabel: "Link",
    linkPh: "https://...",
    deadlineLabel: "Frist",
    addBtn: "Legg til",
    appsTitle: "Søknader",
    filters: { ALL:"Alle", PLANLAGT:"Planlagt", SOKT:"Søkt", INTERVJU:"Intervju", AVSLATT:"Avslått", TILBUD:"Tilbud" },
    badge:   { PLANLAGT:"Planlagt", SOKT:"Søkt", INTERVJU:"Intervju", AVSLATT:"Avslått", TILBUD:"Tilbud" },
    stats: (c) => `Planlagt: ${c.PLANLAGT||0} • Søkt: ${c.SOKT||0} • Intervju: ${c.INTERVJU||0} • Tilbud: ${c.TILBUD||0} • Avslått: ${c.AVSLATT||0}`,
    noResults: "Ingen treff. Logg inn for å se dine søknader.",
    noLink: "Ingen link",
    delete: "Slett",
    deadlineTxt: "Frist",
    statusTxt: "Status",
    invalidAuth: "Skriv inn gyldig e-post og passord (minst 6 tegn).",
    authFailed: "Kunne ikke logge inn.",
    sessionExpired: "Sesjonen er utløpt. Logg inn på nytt.",
    footer: "Bygget med Java (Spring Boot) + vanilla JS + JWT."
  },
  en: {
    title: "Job Tracker",
    subtitle: "Track your job applications.",
    login: "Sign in",
    logout: "Sign out",
    email: "Email",
    password: "Password",
    createUser: "Create account",
    haveUser: "Already have an account? Sign in",
    lockedHint: "Sign in to save and view your applications.",
    newTitle: "New application",
    companyLabel: "Company *",
    companyPh: "e.g. NAV / Telenor",
    roleLabel: "Role *",
    rolePh: "e.g. Junior developer",
    linkLabel: "Link",
    linkPh: "https://...",
    deadlineLabel: "Deadline",
    addBtn: "Add",
    appsTitle: "Applications",
    filters: { ALL:"All", PLANLAGT:"Planned", SOKT:"Applied", INTERVJU:"Interview", AVSLATT:"Rejected", TILBUD:"Offer" },
    badge:   { PLANLAGT:"Planned", SOKT:"Applied", INTERVJU:"Interview", AVSLATT:"Rejected", TILBUD:"Offer" },
    stats: (c) => `Planned: ${c.PLANLAGT||0} • Applied: ${c.SOKT||0} • Interview: ${c.INTERVJU||0} • Offer: ${c.TILBUD||0} • Rejected: ${c.AVSLATT||0}`,
    noResults: "No results. Sign in to view your applications.",
    noLink: "No link",
    delete: "Delete",
    deadlineTxt: "Deadline",
    statusTxt: "Status",
    invalidAuth: "Enter a valid email and password (min 6 chars).",
    authFailed: "Could not sign in.",
    sessionExpired: "Session expired. Please sign in again.",
    footer: "Built with Java (Spring Boot) + vanilla JS + JWT."
  }
};

function getLang() { return localStorage.getItem(LANG_KEY) || "no"; }
function setLang(lang) { localStorage.setItem(LANG_KEY, lang); }
function T() { return I18N[getLang()]; }

// -----------------------
// DOM
// -----------------------
const loginBtn = document.getElementById("loginBtn");
const logoutBtn = document.getElementById("logoutBtn");
const langBtn = document.getElementById("langBtn");
const whoamiEl = document.getElementById("whoami");

const authModal = document.getElementById("authModal");
const closeModalBtn = document.getElementById("closeModal");
const authTitleEl = document.getElementById("authTitle");
const authForm = document.getElementById("authForm");
const authEmail = document.getElementById("authEmail");
const authPassword = document.getElementById("authPassword");
const authMsg = document.getElementById("authMsg");
const authSubmitBtn = document.getElementById("authSubmitBtn");
const toggleAuthModeBtn = document.getElementById("toggleAuthMode");

const createForm = document.getElementById("createForm");
const formMsg = document.getElementById("formMsg");
const listEl = document.getElementById("list");
const statsEl = document.getElementById("stats");
const lockedHint = document.getElementById("lockedHint");

// text nodes
const t_title = document.getElementById("t_title");
const t_subtitle = document.getElementById("t_subtitle");
const t_newTitle = document.getElementById("t_newTitle");
const t_companyLabel = document.getElementById("t_companyLabel");
const t_roleLabel = document.getElementById("t_roleLabel");
const t_linkLabel = document.getElementById("t_linkLabel");
const t_deadlineLabel = document.getElementById("t_deadlineLabel");
const t_addBtn = document.getElementById("t_addBtn");
const t_appsTitle = document.getElementById("t_appsTitle");
const t_footer = document.getElementById("t_footer");
const t_lockedHint = document.getElementById("t_lockedHint");

// filters
document.querySelectorAll(".filter").forEach(btn => {
  btn.addEventListener("click", () => {
    document.querySelectorAll(".filter").forEach(b => b.classList.remove("active"));
    btn.classList.add("active");
    currentFilter = btn.dataset.filter;
    render(lastApps);
  });
});

// -----------------------
// JWT helpers
// -----------------------
function setToken(token) { localStorage.setItem(TOKEN_KEY, token); }
function getToken() { return localStorage.getItem(TOKEN_KEY); }
function clearToken() { localStorage.removeItem(TOKEN_KEY); }

function authHeaders() {
  const token = getToken();
  return token ? { "Authorization": "Bearer " + token } : {};
}

async function apiFetch(url, options = {}) {
  const headers = { ...(options.headers || {}), ...authHeaders() };
  const res = await fetch(url, { ...options, headers });

  if (res.status === 401) {
    // token invalid/expired → log out in UI
    clearToken();
    setLoggedOutUI(T().sessionExpired);
  }
  return res;
}

// parse email from JWT (for UI)
function parseJwtEmail(token) {
  try {
    const payload = token.split(".")[1];
    const base64 = payload.replace(/-/g, "+").replace(/_/g, "/");
    const json = decodeURIComponent(
      atob(base64).split("").map(c => `%${("00"+c.charCodeAt(0).toString(16)).slice(-2)}`).join("")
    );
    return JSON.parse(json).sub || "user";
  } catch {
    return "user";
  }
}

// -----------------------
// i18n apply
// -----------------------
function applyI18n() {
  const L = T();

  t_title.textContent = L.title;
  t_subtitle.textContent = L.subtitle;

  loginBtn.textContent = L.login;
  logoutBtn.textContent = L.logout;

  t_newTitle.textContent = L.newTitle;
  t_companyLabel.textContent = L.companyLabel;
  t_roleLabel.textContent = L.roleLabel;
  t_linkLabel.textContent = L.linkLabel;
  t_deadlineLabel.textContent = L.deadlineLabel;
  t_addBtn.textContent = L.addBtn;

  document.getElementById("companyInput").placeholder = L.companyPh;
  document.getElementById("roleInput").placeholder = L.rolePh;
  document.getElementById("linkInput").placeholder = L.linkPh;

  t_appsTitle.textContent = L.appsTitle;
  t_footer.textContent = L.footer;
  t_lockedHint.textContent = L.lockedHint;

  // modal
  authTitleEl.textContent = authMode === "login" ? L.login : L.createUser;
  authSubmitBtn.textContent = authMode === "login" ? L.login : L.createUser;
  toggleAuthModeBtn.textContent = authMode === "login" ? L.createUser : L.haveUser;
  authEmail.placeholder = L.email;
  authPassword.placeholder = L.password;

  render(lastApps);
}

// -----------------------
// Modal open/close
// -----------------------
function openModal() {
  authMsg.textContent = "";
  authModal.classList.remove("hidden");
  authModal.setAttribute("aria-hidden","false");
  setTimeout(() => authEmail.focus(), 0);
}
function closeModal() {
  authModal.classList.add("hidden");
  authModal.setAttribute("aria-hidden","true");
}

loginBtn.addEventListener("click", openModal);
closeModalBtn.addEventListener("click", closeModal);
authModal.addEventListener("click", (e) => { if (e.target === authModal) closeModal(); });
document.addEventListener("keydown", (e) => { if (e.key === "Escape" && !authModal.classList.contains("hidden")) closeModal(); });

// switch login/register
toggleAuthModeBtn.addEventListener("click", () => {
  authMode = authMode === "login" ? "register" : "login";
  authMsg.textContent = "";
  applyI18n();
});

// -----------------------
// Auth submit (ROBUST token parsing)
// -----------------------
authForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  authMsg.textContent = "";
  const L = T();

  const email = (authEmail.value || "").trim().toLowerCase();
  const password = authPassword.value || "";

  if (!email || password.length < 6) {
    authMsg.textContent = L.invalidAuth;
    return;
  }

  const endpoint = authMode === "login" ? "/api/auth/login" : "/api/auth/register";

  const res = await fetch(endpoint, {
    method: "POST",
    headers: {"Content-Type":"application/json"},
    body: JSON.stringify({ email, password })
  });

  if (!res.ok) {
    const text = await res.text();
    authMsg.textContent = text || L.authFailed;
    return;
  }

  // robust token parsing (json or text)
  const contentType = res.headers.get("content-type") || "";
  let token = null;

  if (contentType.includes("application/json")) {
    const data = await res.json();
    token = data.token || data.jwt || data.accessToken || data.access_token || null;
  } else {
    const text = await res.text();
    token = text?.trim() || null;
  }

  if (!token) {
    authMsg.textContent = "Fant ikke token i svar fra server.";
    return;
  }

  setToken(token);

  closeModal();
  setLoggedInUI();
  await load();
});

// logout
logoutBtn.addEventListener("click", () => {
  clearToken();
  setLoggedOutUI("");
  lastApps = [];
  render(lastApps);
});

// language toggle
langBtn.addEventListener("click", () => {
  setLang(getLang() === "no" ? "en" : "no");
  applyI18n();
});

// -----------------------
// Create application
// -----------------------
createForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  formMsg.textContent = "";

  if (!getToken()) {
    formMsg.textContent = T().lockedHint;
    openModal();
    return;
  }

  const data = new FormData(createForm);
  const payload = {
    company: data.get("company"),
    role: data.get("role"),
    link: data.get("link"),
    deadline: data.get("deadline")
  };

  const res = await apiFetch("/api/apps", {
    method: "POST",
    headers: {"Content-Type":"application/json"},
    body: JSON.stringify(payload)
  });

  if (!res.ok) {
    const text = await res.text();
    formMsg.textContent = text || "Noe gikk galt";
    return;
  }

  createForm.reset();
  await load();
});

// -----------------------
// Render list
// -----------------------
function escapeHtml(str) {
  return (str ?? "")
    .replaceAll("&","&amp;")
    .replaceAll("<","&lt;")
    .replaceAll(">","&gt;")
    .replaceAll('"',"&quot;");
}

function badgeLabel(status) {
  return T().badge[status] ?? status;
}

function fmtDate(d) { return d ? d : "—"; }

function isOverdue(deadline, status) {
  if (!deadline) return false;
  if (status === "AVSLATT" || status === "TILBUD") return false;
  const today = new Date();
  const dd = new Date(deadline + "T00:00:00");
  return dd < new Date(today.getFullYear(), today.getMonth(), today.getDate());
}

async function updateStatus(id, status) {
  if (!getToken()) return;
  const res = await apiFetch(`/api/apps/${id}/status`, {
    method: "PUT",
    headers: {"Content-Type":"application/json"},
    body: JSON.stringify({ status })
  });
  if (res.ok) await load();
}

async function removeItem(id) {
  if (!getToken()) return;
  const res = await apiFetch(`/api/apps/${id}`, { method: "DELETE" });
  if (res.ok || res.status === 204) await load();
}

function render(apps) {
  const L = T();

  const counts = (apps || []).reduce((acc, a) => {
    acc[a.status] = (acc[a.status] || 0) + 1;
    return acc;
  }, {});
  statsEl.textContent = L.stats(counts);

  const filtered = currentFilter === "ALL"
    ? (apps || [])
    : (apps || []).filter(a => a.status === currentFilter);

  if (!getToken() && filtered.length === 0) {
    listEl.innerHTML = `<div class="muted">${L.noResults}</div>`;
    return;
  }

  if (filtered.length === 0) {
    listEl.innerHTML = `<div class="muted">${getLang()==="no" ? "Ingen treff." : "No results."}</div>`;
    return;
  }

  listEl.innerHTML = "";
  for (const a of filtered) {
    const el = document.createElement("div");
    el.className = "item" + (isOverdue(a.deadline, a.status) ? " overdue" : "");

    const link = a.link
      ? `<a href="${a.link}" target="_blank" rel="noreferrer">Link</a>`
      : `<span class="muted">${L.noLink}</span>`;

    el.innerHTML = `
      <div class="top">
        <div>
          <div class="title">${escapeHtml(a.company)} — ${escapeHtml(a.role)}</div>
          <div class="meta">
            <span>${L.deadlineTxt}: <span class="badge">${fmtDate(a.deadline)}</span></span>
            <span>${L.statusTxt}: <span class="badge">${badgeLabel(a.status)}</span></span>
            <span>${link}</span>
          </div>
        </div>

        <div class="actions">
          <select data-id="${a.id}" class="statusSelect">
            ${["PLANLAGT","SOKT","INTERVJU","AVSLATT","TILBUD"].map(s =>
              `<option value="${s}" ${s===a.status ? "selected" : ""}>${badgeLabel(s)}</option>`
            ).join("")}
          </select>
          <button class="btn ghost" data-del="${a.id}">${L.delete}</button>
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

// -----------------------
// Load apps
// -----------------------
async function load() {
  if (!getToken()) {
    lastApps = [];
    render(lastApps);
    return;
  }

  const res = await apiFetch("/api/apps");
  if (!res.ok) return;

  lastApps = await res.json();
  render(lastApps);
}

// -----------------------
// Logged in/out UI
// -----------------------
function setLoggedInUI() {
  const email = parseJwtEmail(getToken());
  whoamiEl.textContent = email;
  whoamiEl.classList.remove("hidden");

  loginBtn.classList.add("hidden");
  logoutBtn.classList.remove("hidden");

  lockedHint.classList.add("hidden");
  document.body.classList.remove("locked");
}

function setLoggedOutUI(message) {
  whoamiEl.textContent = "";
  whoamiEl.classList.add("hidden");

  loginBtn.classList.remove("hidden");
  logoutBtn.classList.add("hidden");

  lockedHint.classList.remove("hidden");
  document.body.classList.add("locked");

  if (message) authMsg.textContent = message;
}

// -----------------------
// Init
// -----------------------
(function init(){
  if (!localStorage.getItem(LANG_KEY)) setLang("no");
  applyI18n();

  if (getToken()) {
    setLoggedInUI();
    load();
  } else {
    setLoggedOutUI("");
    render([]);
  }
})();
