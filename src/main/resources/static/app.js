let currentFilter = "ALL";

// ====== AUTH STATE ======
const TOKEN_KEY = "jt_token";
const LANG_KEY = "jt_lang";

function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}
function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token);
}
function clearToken() {
  localStorage.removeItem(TOKEN_KEY);
}

async function apiFetch(path, options = {}) {
  const token = getToken();
  const headers = new Headers(options.headers || {});
  if (!headers.has("Content-Type") && options.body) {
    headers.set("Content-Type", "application/json");
  }
  if (token) {
    // ⚠️ VIKTIG: Bearer prefix!
    headers.set("Authorization", `Bearer ${token}`);
  }

  const res = await fetch(path, { ...options, headers });

  // Hvis token er ugyldig/utløpt -> logg ut i UI
  if (res.status === 401) {
    // Ikke slett token hvis det var login/register som feilet
    if (!path.startsWith("/api/auth/")) {
      clearToken();
      setLoggedOutUI();
    }
  }
  return res;
}

// ====== DOM ======
const listEl = document.getElementById("list");
const statsEl = document.getElementById("stats");
const form = document.getElementById("createForm");
const formMsg = document.getElementById("formMsg");

const loginBtn = document.getElementById("loginBtn");
const logoutBtn = document.getElementById("logoutBtn");
const whoamiEl = document.getElementById("whoami");

const authModal = document.getElementById("authModal");
const closeAuth = document.getElementById("closeAuth");
const authForm = document.getElementById("authForm");
const authEmail = document.getElementById("authEmail");
const authPassword = document.getElementById("authPassword");
const authMsg = document.getElementById("authMsg");
const toggleAuthMode = document.getElementById("toggleAuthMode");
const authTitle = document.getElementById("authTitle");
const authSubmitBtn = document.getElementById("authSubmitBtn");

let authMode = "login"; // "login" | "register"

// ====== MODAL OPEN/CLOSE ======
function openModal() {
  authMsg.textContent = "";
  authMsg.classList.remove("ok");
  authModal.classList.remove("hidden");
  document.body.classList.add("modalOpen");
  setTimeout(() => authEmail.focus(), 0);
}
function closeModal() {
  authModal.classList.add("hidden");
  document.body.classList.remove("modalOpen");
}

loginBtn?.addEventListener("click", openModal);
closeAuth?.addEventListener("click", closeModal);
authModal?.addEventListener("click", (e) => {
  if (e.target === authModal) closeModal();
});
document.addEventListener("keydown", (e) => {
  if (e.key === "Escape" && !authModal.classList.contains("hidden")) closeModal();
});

// ====== AUTH UI ======
function setLoggedInUI(email) {
  whoamiEl.textContent = email;
  whoamiEl.classList.remove("hidden");
  logoutBtn.classList.remove("hidden");
  loginBtn.classList.add("hidden");

  // enable app usage
  form.querySelectorAll("input,button").forEach(el => el.disabled = false);
}

function setLoggedOutUI() {
  whoamiEl.textContent = "—";
  whoamiEl.classList.add("hidden");
  logoutBtn.classList.add("hidden");
  loginBtn.classList.remove("hidden");

  // disable creating while logged out (valgfritt)
  form.querySelectorAll("input,button").forEach(el => el.disabled = true);

  // reset list placeholder
  listEl.innerHTML = `<div class="muted" id="t_emptyLogin">Ingen treff. Logg inn for å se dine søknader.</div>`;
  statsEl.textContent = "—";
}

logoutBtn?.addEventListener("click", () => {
  clearToken();
  setLoggedOutUI();
});

// ====== AUTH ACTIONS ======
toggleAuthMode?.addEventListener("click", () => {
  authMode = authMode === "login" ? "register" : "login";
  authTitle.textContent = authMode === "login" ? "Logg inn" : "Opprett bruker";
  authSubmitBtn.textContent = authMode === "login" ? "Logg inn" : "Opprett bruker";
  toggleAuthMode.textContent = authMode === "login" ? "Opprett bruker" : "Tilbake til logg inn";
  authMsg.textContent = "";
});

authForm?.addEventListener("submit", async (e) => {
  e.preventDefault();
  authMsg.textContent = "";
  authMsg.classList.remove("ok");

  const email = authEmail.value.trim();
  const password = authPassword.value;

  const endpoint = authMode === "login" ? "/api/auth/login" : "/api/auth/register";
  const res = await fetch(endpoint, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password })
  });

  if (!res.ok) {
    const text = await res.text().catch(() => "");
    authMsg.textContent = text || (authMode === "login" ? "Innlogging feilet." : "Registrering feilet.");
    return;
  }

  const data = await res.json().catch(() => ({}));
  const token = data.token;

  if (!token) {
    authMsg.textContent = "Innlogging feilet (token mangler).";
    return;
  }

  setToken(token);

  // test token med /me
  const meRes = await apiFetch("/api/auth/me");
  if (!meRes.ok) {
    authMsg.textContent = `Innlogging feilet. Token ble ikke akseptert (/api/auth/me ${meRes.status}).`;
    return;
  }

  const me = await meRes.json();
  authMsg.textContent = "OK";
  authMsg.classList.add("ok");

  setLoggedInUI(me.email || email);
  closeModal();
  await load();
});

// ====== FILTER BUTTONS ======
document.querySelectorAll(".filter").forEach(btn => {
  btn.addEventListener("click", () => {
    document.querySelectorAll(".filter").forEach(b => b.classList.remove("active"));
    btn.classList.add("active");
    currentFilter = btn.dataset.filter;
    load();
  });
});

// ====== CREATE APP ======
form.addEventListener("submit", async (e) => {
  e.preventDefault();
  formMsg.textContent = "";

  const data = new FormData(form);
  const payload = {
    company: data.get("company"),
    role: data.get("role"),
    link: data.get("link"),
    deadline: data.get("deadline") || null
  };

  const res = await apiFetch("/api/apps", {
    method: "POST",
    body: JSON.stringify(payload)
  });

  if (!res.ok) {
    const text = await res.text().catch(() => "");
    formMsg.textContent = text || "Noe gikk galt";
    return;
  }

  form.reset();
  await load();
});

function badge(status) {
  const map = { PLANLAGT:"Planlagt", SOKT:"Søkt", INTERVJU:"Intervju", AVSLATT:"Avslått", TILBUD:"Tilbud" };
  return map[status] ?? status;
}
function fmtDate(d) { return d || "—"; }

function isOverdue(deadline, status) {
  if (!deadline) return false;
  if (status === "AVSLATT" || status === "TILBUD") return false;
  const today = new Date();
  const dd = new Date(deadline + "T00:00:00");
  return dd < new Date(today.getFullYear(), today.getMonth(), today.getDate());
}

async function updateStatus(id, status) {
  await apiFetch(`/api/apps/${id}/status`, {
    method: "PUT",
    body: JSON.stringify({ status })
  });
  await load();
}

async function removeItem(id) {
  await apiFetch(`/api/apps/${id}`, { method: "DELETE" });
  await load();
}

function render(apps) {
  const filtered = currentFilter === "ALL" ? apps : apps.filter(a => a.status === currentFilter);

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
          <button class="btn ghost" data-del="${a.id}" type="button">Slett</button>
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
  const token = getToken();
  if (!token) return;

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

// ====== INIT ======
(async function init() {
  if (!getToken()) {
    setLoggedOutUI();
    return;
  }

  // Hvis token finnes: sjekk /me
  const meRes = await apiFetch("/api/auth/me");
  if (!meRes.ok) {
    clearToken();
    setLoggedOutUI();
    return;
  }
  const me = await meRes.json();
  setLoggedInUI(me.email || "Innlogget");
  await load();
})();
