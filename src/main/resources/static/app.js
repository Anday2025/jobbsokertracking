// app.js (ferdig løsning)
// ------------------------------------------------------------
// LAGRING
const LS_TOKEN = "jwt";
const LS_LANG = "lang"; // "no" | "en"

// UI state
let currentFilter = "ALL";
let authMode = "login"; // "login" | "register"
let lang = (localStorage.getItem(LS_LANG) || "no").toLowerCase();

// ELEMENTER (må finnes i HTML)
const listEl = document.getElementById("list");
const statsEl = document.getElementById("stats");
const form = document.getElementById("createForm");
const formMsg = document.getElementById("formMsg");

const loginBtn = document.getElementById("loginBtn");
const logoutBtn = document.getElementById("logoutBtn");
const whoamiEl = document.getElementById("whoami");
const langBtn = document.getElementById("langBtn");

// Modal / auth
const authModal = document.getElementById("authModal");
const authClose = document.getElementById("authClose");
const authForm = document.getElementById("authForm");
const authEmail = document.getElementById("authEmail");
const authPassword = document.getElementById("authPassword");
const authSubmitBtn = document.getElementById("authSubmitBtn");
const toggleAuthModeBtn = document.getElementById("toggleAuthMode");
const authMsg = document.getElementById("authMsg");

// Oversettelser (tilpass gjerne)
const I18N = {
  no: {
    title: "Jobbsøker-tracker",
    subtitle: "Hold oversikt over søknadene dine.",
    login: "Logg inn",
    logout: "Logg ut",
    notLogged: "Ikke innlogget",
    email: "E-post",
    password: "Passord",
    createUser: "Opprett bruker",
    haveUser: "Har du bruker? Logg inn",
    ok: "OK",
    errorGeneric: "Noe gikk galt",
    invalidCreds: "Ugyldig e-post eller passord",
    mustLogin: "Logg inn for å se søknadene dine.",
    add: "Legg til",
    delete: "Slett",
    noHits: "Ingen treff.",
    statuses: {
      PLANLAGT: "Planlagt",
      SOKT: "Søkt",
      INTERVJU: "Intervju",
      AVSLATT: "Avslått",
      TILBUD: "Tilbud"
    },
    statsFmt: (c) =>
      `Planlagt: ${c.PLANLAGT || 0} • Søkt: ${c.SOKT || 0} • Intervju: ${c.INTERVJU || 0} • Tilbud: ${c.TILBUD || 0} • Avslått: ${c.AVSLATT || 0}`
  },
  en: {
    title: "Job tracker",
    subtitle: "Keep track of your applications.",
    login: "Sign in",
    logout: "Sign out",
    notLogged: "Not signed in",
    email: "Email",
    password: "Password",
    createUser: "Create account",
    haveUser: "Already have an account? Sign in",
    ok: "OK",
    errorGeneric: "Something went wrong",
    invalidCreds: "Invalid email or password",
    mustLogin: "Sign in to see your applications.",
    add: "Add",
    delete: "Delete",
    noHits: "No results.",
    statuses: {
      PLANLAGT: "Planned",
      SOKT: "Applied",
      INTERVJU: "Interview",
      AVSLATT: "Rejected",
      TILBUD: "Offer"
    },
    statsFmt: (c) =>
      `Planned: ${c.PLANLAGT || 0} • Applied: ${c.SOKT || 0} • Interview: ${c.INTERVJU || 0} • Offer: ${c.TILBUD || 0} • Rejected: ${c.AVSLATT || 0}`
  }
};

function T() {
  return I18N[lang] || I18N.no;
}

// ------------------------------------------------------------
// API helper som alltid sender token
async function apiFetch(url, options = {}) {
  const token = localStorage.getItem(LS_TOKEN);
  const headers = new Headers(options.headers || {});

  if (options.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
    // ekstra fallback (skader ikke)
    headers.set("X-Auth-Token", token);
  }

  const res = await fetch(url, { ...options, headers });

  if (res.status === 401) {
    // token ugyldig/utløpt → logg ut lokalt
    localStorage.removeItem(LS_TOKEN);
  }
  return res;
}

// ------------------------------------------------------------
// AUTH
function setAuthUI(emailOrNull) {
  const signedIn = !!emailOrNull;

  // whoami
  whoamiEl.textContent = signedIn ? emailOrNull : T().notLogged;

  // knapper
  if (loginBtn) loginBtn.classList.toggle("hidden", signedIn);
  logoutBtn.classList.toggle("hidden", !signedIn);

  // Form (disable når ikke innlogget)
  const disable = !signedIn;
  form.querySelectorAll("input, button").forEach((el) => (el.disabled = disable));

  // Hint i liste når ikke innlogget
  if (disable) {
    statsEl.textContent = "—";
    listEl.innerHTML = `<div class="muted">${T().mustLogin}</div>`;
  }
}

async function refreshAuthState() {
  const token = localStorage.getItem(LS_TOKEN);
  if (!token) {
    setAuthUI(null);
    return;
  }

  const res = await apiFetch("/api/auth/me");
  if (!res.ok) {
    setAuthUI(null);
    return;
  }

  const me = await res.json();
  const email =
    me?.email ||
    me?.username ||
    me?.user?.email ||
    me?.user?.username ||
    null;

  setAuthUI(email);
}

async function login(email, password) {
  const res = await fetch("/api/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password })
  });

  const text = await res.text();
  if (!res.ok) {
    // ofte 401/403 her
    throw new Error(text || T().invalidCreds);
  }

  let data = {};
  try {
    data = JSON.parse(text || "{}");
  } catch {
    // ignore
  }

  const token =
    data.token ||
    data.jwt ||
    data.accessToken ||
    data?.data?.token ||
    data?.data?.jwt;

  // Noen backend returnerer token som ren tekst
  const raw = (text || "").trim();
  const fallbackRawToken = raw.startsWith("eyJ") ? raw : null;

  const finalToken = token || fallbackRawToken;
  if (!finalToken) {
    throw new Error("Fant ikke token i responsen fra /api/auth/login");
  }

  localStorage.setItem(LS_TOKEN, finalToken);
  return finalToken;
}

async function register(email, password) {
  const res = await fetch("/api/auth/register", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password })
  });

  const text = await res.text();
  if (!res.ok) throw new Error(text || T().errorGeneric);

  // Noen register-endepunkt returnerer token, noen ikke.
  let data = {};
  try { data = JSON.parse(text || "{}"); } catch {}

  const token =
    data.token ||
    data.jwt ||
    data.accessToken ||
    data?.data?.token ||
    data?.data?.jwt;

  if (token) {
    localStorage.setItem(LS_TOKEN, token);
    return token;
  }
  return null;
}

function logout() {
  localStorage.removeItem(LS_TOKEN);
  setAuthUI(null);
}

// ------------------------------------------------------------
// MODAL
function openModal() {
  if (!authModal) return;
  authModal.classList.remove("hidden");
  authMsg.textContent = "";
  setTimeout(() => authEmail?.focus(), 0);
}

function closeModal() {
  if (!authModal) return;
  authModal.classList.add("hidden");
  authMsg.textContent = "";
}

// ------------------------------------------------------------
// APP (Job applications)
function badge(status) {
  return T().statuses[status] ?? status;
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
    body: JSON.stringify({ status })
  });

  if (res.status === 401) {
    setAuthUI(null);
    openModal();
    return;
  }
  await load();
}

async function removeItem(id) {
  const res = await apiFetch(`/api/apps/${id}`, { method: "DELETE" });

  if (res.status === 401) {
    setAuthUI(null);
    openModal();
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
  statsEl.textContent = T().statsFmt(counts);

  if (filtered.length === 0) {
    listEl.innerHTML = `<div class="muted">${T().noHits}</div>`;
    return;
  }

  listEl.innerHTML = "";
  for (const a of filtered) {
    const el = document.createElement("div");
    el.className = "item" + (isOverdue(a.deadline, a.status) ? " overdue" : "");

    const link = a.link
      ? `<a href="${a.link}" target="_blank" rel="noreferrer">Link</a>`
      : `<span class="muted">—</span>`;

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
          <button data-del="${a.id}">${T().delete}</button>
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
  const token = localStorage.getItem(LS_TOKEN);
  if (!token) {
    setAuthUI(null);
    return;
  }

  const res = await apiFetch("/api/apps");
  if (res.status === 401) {
    setAuthUI(null);
    return;
  }
  if (!res.ok) {
    listEl.innerHTML = `<div class="muted">${T().errorGeneric}</div>`;
    return;
  }

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

// ------------------------------------------------------------
// SPRÅK
function applyLanguage() {
  // knapp-tekst: NO/EN alltid synlig
  if (langBtn) langBtn.textContent = lang === "no" ? "EN" : "NO";

  // Hvis du bruker data-i18n i HTML kan vi utvide senere.
  // Foreløpig: oppdater auth UI label
  const email = whoamiEl?.textContent;
  const token = localStorage.getItem(LS_TOKEN);

  if (!token) setAuthUI(null);
  else setAuthUI(email && email.includes("@") ? email : email); // behold

  // Oppdater auth modal placeholders/knapper
  if (authEmail) authEmail.placeholder = T().email;
  if (authPassword) authPassword.placeholder = T().password;

  updateAuthModeTexts();
}

function toggleLanguage() {
  lang = (lang === "no") ? "en" : "no";
  localStorage.setItem(LS_LANG, lang);
  applyLanguage();
  // re-render liste for status-tekster
  load();
}

// ------------------------------------------------------------
// AUTH UI TEXTS
function updateAuthModeTexts() {
  if (!authSubmitBtn || !toggleAuthModeBtn) return;

  if (authMode === "login") {
    authSubmitBtn.textContent = T().login;
    toggleAuthModeBtn.textContent = T().createUser;
    authPassword.autocomplete = "current-password";
  } else {
    authSubmitBtn.textContent = T().createUser;
    toggleAuthModeBtn.textContent = T().haveUser;
    authPassword.autocomplete = "new-password";
  }
}

// ------------------------------------------------------------
// EVENTS
document.querySelectorAll(".filter").forEach(btn => {
  btn.addEventListener("click", () => {
    document.querySelectorAll(".filter").forEach(b => b.classList.remove("active"));
    btn.classList.add("active");
    currentFilter = btn.dataset.filter;
    load();
  });
});

form.addEventListener("submit", async (e) => {
  e.preventDefault();
  formMsg.textContent = "";

  const token = localStorage.getItem(LS_TOKEN);
  if (!token) {
    formMsg.textContent = T().mustLogin;
    openModal();
    return;
  }

  const data = new FormData(form);
  const payload = {
    company: data.get("company"),
    role: data.get("role"),
    link: data.get("link"),
    deadline: data.get("deadline")
  };

  const res = await apiFetch("/api/apps", {
    method: "POST",
    body: JSON.stringify(payload)
  });

  if (res.status === 401) {
    formMsg.textContent = T().mustLogin;
    setAuthUI(null);
    openModal();
    return;
  }

  if (!res.ok) {
    const text = await res.text();
    formMsg.textContent = text || T().errorGeneric;
    return;
  }

  form.reset();
  await load();
});

// Login/open modal
if (loginBtn) {
  loginBtn.addEventListener("click", () => {
    authMode = "login";
    updateAuthModeTexts();
    openModal();
  });
}

// Logout
logoutBtn.addEventListener("click", () => {
  logout();
  load();
});

// Modal close
if (authClose) authClose.addEventListener("click", closeModal);

// close when click on backdrop
if (authModal) {
  authModal.addEventListener("click", (e) => {
    if (e.target === authModal) closeModal();
  });
}

// esc closes
document.addEventListener("keydown", (e) => {
  if (e.key === "Escape" && authModal && !authModal.classList.contains("hidden")) {
    closeModal();
  }
});

// Toggle auth mode (login/register)
toggleAuthModeBtn.addEventListener("click", () => {
  authMode = (authMode === "login") ? "register" : "login";
  authMsg.textContent = "";
  updateAuthModeTexts();
});

// Auth submit
authForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  authMsg.textContent = "";

  const email = (authEmail.value || "").trim();
  const password = (authPassword.value || "").trim();

  if (!email || !password) {
    authMsg.textContent = T().errorGeneric;
    return;
  }

  try {
    if (authMode === "login") {
      await login(email, password);
    } else {
      await register(email, password);
      // hvis register ikke gir token, prøv login automatisk
      if (!localStorage.getItem(LS_TOKEN)) {
        await login(email, password);
      }
    }

    authMsg.textContent = T().ok;

    // Oppdater UI + last data
    await refreshAuthState();
    closeModal();
    await load();
  } catch (err) {
    authMsg.textContent = err?.message || T().errorGeneric;
  }
});

// Language button
if (langBtn) langBtn.addEventListener("click", toggleLanguage);

// ------------------------------------------------------------
// INIT
applyLanguage();
updateAuthModeTexts();

(async function init() {
  await refreshAuthState();
  await load();
})();
