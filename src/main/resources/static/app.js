/* =========================
   B1 (Session-cookie) app.js
   - No JWT
   - Uses HttpOnly JSESSIONID cookie
   - fetch(..., { credentials: "include" })
   - NO/EN toggle
   ========================= */

let currentFilter = "ALL";
let authMode = "login"; // "login" | "register"

const LS_LANG = "jt_lang";

/* ---------- DOM (matches your HTML) ---------- */
const langBtn   = document.getElementById("langBtn");
const whoamiEl  = document.getElementById("whoami");
const logoutBtn = document.getElementById("logoutBtn");
const loginBtn  = document.getElementById("loginBtn");

const listEl  = document.getElementById("list");
const statsEl = document.getElementById("stats");
const form    = document.getElementById("createForm");
const formMsg = document.getElementById("formMsg");

const loginHint = document.getElementById("t_loginHint");
const emptyLogin = document.getElementById("t_emptyLogin");

/* ---------- Modal ---------- */
const authModal         = document.getElementById("authModal");
const authTitle         = document.getElementById("authTitle");
const authForm          = document.getElementById("authForm");
const authEmail         = document.getElementById("authEmail");
const authPassword      = document.getElementById("authPassword");
const authMsg           = document.getElementById("authMsg");
const authSubmitBtn     = document.getElementById("authSubmitBtn");
const toggleAuthModeBtn = document.getElementById("toggleAuthMode");
const closeAuthBtn      = document.getElementById("closeAuth");

/* ---------- Language ---------- */
let lang = (localStorage.getItem(LS_LANG) || "no").toLowerCase();

const T = {
  no: {
    title: "Jobbsøker-tracker",
    subtitle: "Hold oversikt over søknadene dine.",
    login: "Logg inn",
    logout: "Logg ut",
    notLoggedIn: "Ikke innlogget",
    email: "E-post",
    password: "Passord",
    loginTitle: "Logg inn",
    registerTitle: "Opprett bruker",
    toggleToRegister: "Opprett bruker",
    toggleToLogin: "Tilbake til logg inn",
    hintLogin: "Logg inn for å lagre og se dine søknader.",
    emptyLogin: "Ingen treff. Logg inn for å se dine søknader.",
    newApp: "Ny søknad",
    apps: "Søknader",
    add: "Legg til",
    all: "Alle",
    planned: "Planlagt",
    applied: "Søkt",
    interview: "Intervju",
    rejected: "Avslått",
    offer: "Tilbud",
    noHits: "Ingen treff.",
    delete: "Slett",
    linkNone: "Ingen link",
    deadline: "Frist",
    status: "Status",
    errors: {
      invalid: "Skriv inn e-post og passord.",
      loginFailed: "Innlogging feilet.",
      registerFailed: "Registrering feilet.",
      unauthorized: "Du må logge inn.",
      generic: "Noe gikk galt."
    },
    footer: "Bygget med Java (Spring Boot) + vanilla JS + Session-cookie."
  },
  en: {
    title: "Job Tracker",
    subtitle: "Keep track of your job applications.",
    login: "Sign in",
    logout: "Sign out",
    notLoggedIn: "Not signed in",
    email: "Email",
    password: "Password",
    loginTitle: "Sign in",
    registerTitle: "Create account",
    toggleToRegister: "Create account",
    toggleToLogin: "Back to sign in",
    hintLogin: "Sign in to save and view your applications.",
    emptyLogin: "No results. Sign in to view your applications.",
    newApp: "New application",
    apps: "Applications",
    add: "Add",
    all: "All",
    planned: "Planned",
    applied: "Applied",
    interview: "Interview",
    rejected: "Rejected",
    offer: "Offer",
    noHits: "No results.",
    delete: "Delete",
    linkNone: "No link",
    deadline: "Deadline",
    status: "Status",
    errors: {
      invalid: "Enter email and password.",
      loginFailed: "Login failed.",
      registerFailed: "Registration failed.",
      unauthorized: "You must sign in.",
      generic: "Something went wrong."
    },
    footer: "Built with Java (Spring Boot) + vanilla JS + Session cookie."
  }
};

function tr(key) {
  return T[lang]?.[key] ?? T.no[key] ?? key;
}
function trErr(key) {
  return T[lang]?.errors?.[key] ?? T.no.errors[key] ?? key;
}

function setText(id, value) {
  const el = document.getElementById(id);
  if (el) el.textContent = value;
}
function setPlaceholder(id, value) {
  const el = document.getElementById(id);
  if (el) el.setAttribute("placeholder", value);
}

/* ---------- API wrapper (B1) ---------- */
async function api(path, { method = "GET", body, auth = true } = {}) {
  const headers = {};
  if (body) headers["Content-Type"] = "application/json";

  const res = await fetch(path, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
    credentials: "include" // ✅ session cookie
  });

  const text = await res.text();
  let data = null;
  try { data = text ? JSON.parse(text) : null; } catch { data = text; }

  if (!res.ok) {
    const err = new Error(typeof data === "string" && data ? data : (data?.message || res.statusText));
    err.status = res.status;
    err.data = data;

    // Hvis du blir 401 på beskyttet endpoint => sett UI til utlogget
    if (err.status === 401 && auth) {
      setLoggedOutUI();
    }
    throw err;
  }

  return data;
}

/* ---------- Modal helpers ---------- */
function openModal() {
  authMsg.textContent = "";
  authMsg.classList.remove("ok");
  authModal.classList.remove("hidden");
  document.body.classList.add("modalOpen");
  setTimeout(() => authEmail?.focus(), 50);
}

function closeModal() {
  authModal.classList.add("hidden");
  document.body.classList.remove("modalOpen");
  authMsg.textContent = "";
  authMsg.classList.remove("ok");
}

/* ---------- UI state ---------- */
function show(el) { el?.classList?.remove("hidden"); }
function hide(el) { el?.classList?.add("hidden"); }

function setLoggedOutUI() {
  hide(whoamiEl);
  hide(logoutBtn);
  show(loginBtn);

  if (loginHint) {
    loginHint.textContent = tr("hintLogin");
    loginHint.classList.remove("hidden");
  }

  statsEl.textContent = "—";
  listEl.innerHTML = `<div class="muted" id="t_emptyLogin">${tr("emptyLogin")}</div>`;

  // ikke blokkér inputs; men gi hint. (valgfritt)
  // form.querySelectorAll("input,button").forEach(el => el.disabled = false);
}

function setLoggedInUI(email) {
  whoamiEl.textContent = email || "—";
  show(whoamiEl);
  show(logoutBtn);
  hide(loginBtn);

  if (loginHint) loginHint.classList.add("hidden");
}

/* ---------- Auth mode text ---------- */
function syncAuthMode() {
  if (authMode === "login") {
    authTitle.textContent = tr("loginTitle");
    authSubmitBtn.textContent = tr("login");
    toggleAuthModeBtn.textContent = tr("toggleToRegister");
    authPassword.autocomplete = "current-password";
  } else {
    authTitle.textContent = tr("registerTitle");
    authSubmitBtn.textContent = tr("toggleToRegister");
    toggleAuthModeBtn.textContent = tr("toggleToLogin");
    authPassword.autocomplete = "new-password";
  }
}

/* ---------- Language apply ---------- */
function applyLanguage() {
  // button shows other language
  langBtn.textContent = (lang === "no") ? "EN" : "NO";
  document.documentElement.lang = lang;

  setText("t_title", tr("title"));
  setText("t_subtitle", tr("subtitle"));
  setText("t_newAppTitle", tr("newApp"));
  setText("t_appsTitle", tr("apps"));
  setText("t_addBtn", tr("add"));

  setText("t_f_all", tr("all"));
  setText("t_f_planned", tr("planned"));
  setText("t_f_applied", tr("applied"));
  setText("t_f_interview", tr("interview"));
  setText("t_f_rejected", tr("rejected"));
  setText("t_f_offer", tr("offer"));

  setText("t_loginHint", tr("hintLogin"));
  setText("t_emptyLogin", tr("emptyLogin"));

  // modal placeholders
  authEmail.placeholder = tr("email");
  authPassword.placeholder = tr("password");

  // header buttons
  loginBtn.textContent = tr("login");
  logoutBtn.textContent = tr("logout");

  syncAuthMode();
}

/* ---------- Apps rendering ---------- */
function badge(status) {
  const map = {
    PLANLAGT: tr("planned"),
    SOKT: tr("applied"),
    INTERVJU: tr("interview"),
    AVSLATT: tr("rejected"),
    TILBUD: tr("offer")
  };
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

function escapeHtml(str) {
  return (str ?? "")
    .replaceAll("&","&amp;")
    .replaceAll("<","&lt;")
    .replaceAll(">","&gt;")
    .replaceAll('"',"&quot;");
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
    `${tr("planned")}: ${counts.PLANLAGT || 0} • ${tr("applied")}: ${counts.SOKT || 0} • ` +
    `${tr("interview")}: ${counts.INTERVJU || 0} • ${tr("offer")}: ${counts.TILBUD || 0} • ${tr("rejected")}: ${counts.AVSLATT || 0}`;

  if (filtered.length === 0) {
    listEl.innerHTML = `<div class="muted">${tr("noHits")}</div>`;
    return;
  }

  listEl.innerHTML = "";
  for (const a of filtered) {
    const el = document.createElement("div");
    el.className = "item" + (isOverdue(a.deadline, a.status) ? " overdue" : "");

    const link = a.link
      ? `<a href="${a.link}" target="_blank" rel="noreferrer">Link</a>`
      : `<span class="muted">${tr("linkNone")}</span>`;

    el.innerHTML = `
      <div class="top">
        <div>
          <div class="title">${escapeHtml(a.company)} — ${escapeHtml(a.role)}</div>
          <div class="meta">
            <span>${tr("deadline")}: <span class="badge">${fmtDate(a.deadline)}</span></span>
            <span>${tr("status")}: <span class="badge">${badge(a.status)}</span></span>
            <span>${link}</span>
          </div>
        </div>
        <div class="actions">
          <select data-id="${a.id}" class="statusSelect">
            ${["PLANLAGT","SOKT","INTERVJU","AVSLATT","TILBUD"].map(s =>
              `<option value="${s}" ${s===a.status ? "selected" : ""}>${badge(s)}</option>`
            ).join("")}
          </select>
          <button class="btn ghost" data-del="${a.id}" type="button">${tr("delete")}</button>
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

/* ---------- API actions ---------- */
async function load() {
  try {
    const apps = await api("/api/apps", { auth: true });
    render(apps);
  } catch (e) {
    // 401 => setLoggedOutUI allerede
    if (e.status === 401) {
      // vis login-hint i liste
      return;
    }
    listEl.innerHTML = `<div class="muted">${escapeHtml(e.message || trErr("generic"))}</div>`;
  }
}

async function updateStatus(id, status) {
  try {
    await api(`/api/apps/${id}/status`, {
      method: "PUT",
      body: { status },
      auth: true
    });
    await load();
  } catch (e) {}
}

async function removeItem(id) {
  try {
    await api(`/api/apps/${id}`, { method: "DELETE", auth: true });
    await load();
  } catch (e) {}
}

/* ---------- Auth flow (B1) ---------- */
async function fetchMe() {
  try {
    return await api("/api/auth/me", { auth: false }); // /me is open? in config we allowed /api/auth/**
  } catch (e) {
    return null;
  }
}

async function doRegister(email, password) {
  await api("/api/auth/register", {
    method: "POST",
    body: { email, password },
    auth: false
  });

  // Etter register: bytt til login og gi beskjed
  authMode = "login";
  syncAuthMode();
  authMsg.textContent = (lang === "no")
    ? "Bruker opprettet! Logg inn nå."
    : "Account created! Please sign in.";
  authMsg.classList.add("ok");
}

async function doLogin(email, password) {
  // Login oppretter session cookie (JSESSIONID)
  await api("/api/auth/login", {
    method: "POST",
    body: { email, password },
    auth: false
  });

  // Bekreft med /me
  const me = await fetchMe();
  if (!me?.email) {
    throw new Error(trErr("loginFailed"));
  }

  setLoggedInUI(me.email);
  closeModal();
  await load();
}

async function doLogout() {
  try {
    await api("/api/auth/logout", { method: "POST", auth: false });
  } catch (e) {
    // selv om logout feiler, sett UI utlogget
  }
  setLoggedOutUI();
}

/* ---------- Event handlers ---------- */
document.querySelectorAll(".filter").forEach(btn => {
  btn.addEventListener("click", async () => {
    document.querySelectorAll(".filter").forEach(b => b.classList.remove("active"));
    btn.classList.add("active");
    currentFilter = btn.dataset.filter;
    await load();
  });
});

form?.addEventListener("submit", async (e) => {
  e.preventDefault();
  formMsg.textContent = "";
  formMsg.classList.remove("ok");

  const data = new FormData(form);
  const payload = {
    company: data.get("company"),
    role: data.get("role"),
    link: data.get("link"),
    deadline: data.get("deadline") || null
  };

  try {
    await api("/api/apps", { method: "POST", body: payload, auth: true });
    form.reset();
    await load();
  } catch (e2) {
    if (e2.status === 401) {
      formMsg.textContent = trErr("unauthorized");
      setAuthMode("login");
      openModal();
      return;
    }
    formMsg.textContent = e2.message || trErr("generic");
  }
});

function setAuthMode(mode) {
  authMode = mode;
  syncAuthMode();
  authMsg.textContent = "";
  authMsg.classList.remove("ok");
}

loginBtn?.addEventListener("click", () => {
  setAuthMode("login");
  openModal();
});

logoutBtn?.addEventListener("click", async () => {
  await doLogout();
});

closeAuthBtn?.addEventListener("click", closeModal);

authModal?.addEventListener("click", (e) => {
  if (e.target === authModal) closeModal();
});

document.addEventListener("keydown", (e) => {
  if (e.key === "Escape" && !authModal.classList.contains("hidden")) closeModal();
});

toggleAuthModeBtn?.addEventListener("click", () => {
  setAuthMode(authMode === "login" ? "register" : "login");
});

authForm?.addEventListener("submit", async (e) => {
  e.preventDefault();
  authMsg.textContent = "";
  authMsg.classList.remove("ok");

  const email = (authEmail.value || "").trim();
  const password = (authPassword.value || "").trim();

  if (!email || !password) {
    authMsg.textContent = trErr("invalid");
    return;
  }

  try {
    if (authMode === "login") {
      await doLogin(email, password);
    } else {
      await doRegister(email, password);
    }
  } catch (err) {
    authMsg.textContent =
      (authMode === "login" ? trErr("loginFailed") : trErr("registerFailed")) +
      (err?.message ? " " + err.message : "");
  }
});

langBtn?.addEventListener("click", () => {
  lang = (lang === "no") ? "en" : "no";
  localStorage.setItem(LS_LANG, lang);
  applyLanguage();
});

/* ---------- Boot ---------- */
(async function boot() {
  applyLanguage();
  setLoggedOutUI();

  // prøv å se om vi allerede er innlogget (session finnes)
  try {
    const me = await api("/api/auth/me", { auth: false });
    if (me?.email) {
      setLoggedInUI(me.email);
      await load();
    } else {
      setLoggedOutUI();
    }
  } catch (e) {
    setLoggedOutUI();
  }
})();
