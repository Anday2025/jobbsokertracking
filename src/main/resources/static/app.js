// =========================
// Config
// =========================
const API = {
  register: "/api/auth/register",
  login: "/api/auth/login",
  me: "/api/auth/me",
  logout: "/api/auth/logout",
  apps: "/api/apps",
};

const STORAGE_LANG = "lang"; // "no" | "en"

// =========================
// State
// =========================
let state = {
  me: null,              // { email }
  apps: [],              // array of JobApplicationDto
  filter: "ALL",         // ALL | PLANLAGT | SOKT | INTERVJU | AVSLATT | TILBUD
  lang: localStorage.getItem(STORAGE_LANG) || "no",
  authMode: "login",     // "login" | "register"
};

// =========================
// i18n (minimal)
// =========================
const T = {
  no: {
    title: "Jobbsøker-tracker",
    subtitle: "Hold oversikt over søknadene dine.",
    login: "Logg inn",
    logout: "Logg ut",
    register: "Opprett bruker",
    email: "E-post",
    password: "Passord",
    newApp: "Ny søknad",
    apps: "Søknader",
    add: "Legg til",
    companyReq: "company og role er påkrevd",
    loginHint: "Logg inn for å lagre og se dine søknader.",
    emptyLogin: "Ingen treff. Logg inn for å se dine søknader.",
    empty: "Ingen treff.",
    link: "Link",
    deadline: "Frist",
    status: "Status",
    del: "Slett",
    planned: "Planlagt",
    applied: "Søkt",
    interview: "Intervju",
    rejected: "Avslått",
    offer: "Tilbud",
    all: "Alle",
    ok: "OK",
  },
  en: {
    title: "Job tracker",
    subtitle: "Keep track of your applications.",
    login: "Sign in",
    logout: "Sign out",
    register: "Create account",
    email: "Email",
    password: "Password",
    newApp: "New application",
    apps: "Applications",
    add: "Add",
    companyReq: "company and role are required",
    loginHint: "Sign in to save and view your applications.",
    emptyLogin: "No results. Sign in to view your applications.",
    empty: "No results.",
    link: "Link",
    deadline: "Deadline",
    status: "Status",
    del: "Delete",
    planned: "Planned",
    applied: "Applied",
    interview: "Interview",
    rejected: "Rejected",
    offer: "Offer",
    all: "All",
    ok: "OK",
  }
};

function t(key) {
  return (T[state.lang] && T[state.lang][key]) || key;
}

// =========================
// DOM
// =========================
const $ = (sel) => document.querySelector(sel);

const loginBtn = $("#loginBtn");
const logoutBtn = $("#logoutBtn");
const whoami = $("#whoami");
const langBtn = $("#langBtn");

const authModal = $("#authModal");
const closeAuth = $("#closeAuth");
const authForm = $("#authForm");
const authEmail = $("#authEmail");
const authPassword = $("#authPassword");
const authSubmitBtn = $("#authSubmitBtn");
const toggleAuthMode = $("#toggleAuthMode");
const authMsg = $("#authMsg");

const createForm = $("#createForm");
const formMsg = $("#formMsg");

const stats = $("#stats");
const listEl = $("#list");

const filterBtns = Array.from(document.querySelectorAll(".filter"));

// =========================
// Helpers
// =========================
function setMsg(el, text, ok = false) {
  if (!el) return;
  el.textContent = text || "";
  el.classList.toggle("ok", !!ok);
}

function show(el) { el?.classList.remove("hidden"); }
function hide(el) { el?.classList.add("hidden"); }

function openModal() {
  authModal.classList.remove("hidden");
  document.body.classList.add("modalOpen");
  setMsg(authMsg, "");
  // fokus:
  setTimeout(() => authEmail?.focus(), 30);
}
function closeModal() {
  authModal.classList.add("hidden");
  document.body.classList.remove("modalOpen");
  setMsg(authMsg, "");
}

function fmtDate(iso) {
  if (!iso) return "";
  // iso = "2026-01-18"
  if (state.lang === "no") {
    const [y, m, d] = iso.split("-");
    return `${d}/${m}/${y}`;
  }
  return iso;
}

function statusLabel(s) {
  switch (s) {
    case "PLANLAGT": return t("planned");
    case "SOKT": return t("applied");
    case "INTERVJU": return t("interview");
    case "AVSLATT": return t("rejected");
    case "TILBUD": return t("offer");
    default: return s;
  }
}

// Cookie-auth: viktig at vi inkluderer cookies
async function apiFetch(url, options = {}) {
  const res = await fetch(url, {
    ...options,
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      ...(options.headers || {}),
    },
  });
  return res;
}

// =========================
// Rendering
// =========================
function applyI18n() {
  // Header
  $("#t_title").textContent = t("title");
  $("#t_subtitle").textContent = t("subtitle");

  loginBtn.textContent = t("login");
  logoutBtn.textContent = t("logout");
  langBtn.textContent = state.lang === "no" ? "NO/EN" : "EN/NO";

  // Cards
  $("#t_newAppTitle").textContent = t("newApp");
  $("#t_appsTitle").textContent = t("apps");
  $("#t_addBtn").textContent = t("add");

  // Filters
  $("#t_f_all").textContent = t("all");
  $("#t_f_planned").textContent = t("planned");
  $("#t_f_applied").textContent = t("applied");
  $("#t_f_interview").textContent = t("interview");
  $("#t_f_rejected").textContent = t("rejected");
  $("#t_f_offer").textContent = t("offer");

  // Form labels
  $("#t_companyLabel").textContent = state.lang === "no" ? "Firma *" : "Company *";
  $("#t_roleLabel").textContent = state.lang === "no" ? "Stilling *" : "Role *";
  $("#t_linkLabel").textContent = t("link");
  $("#t_deadlineLabel").textContent = t("deadline");

  // Placeholders
  $("#t_companyPh").setAttribute("placeholder", state.lang === "no" ? "F.eks. NAV / Telenor" : "e.g. NAV / Telenor");
  $("#t_rolePh").setAttribute("placeholder", state.lang === "no" ? "F.eks. Junior utvikler" : "e.g. Junior developer");
  $("#t_linkPh").setAttribute("placeholder", "https://...");

  // Auth modal
  $("#authTitle").textContent = state.authMode === "login" ? t("login") : t("register");
  authEmail.setAttribute("placeholder", t("email"));
  authPassword.setAttribute("placeholder", t("password"));
  authSubmitBtn.textContent = state.authMode === "login" ? t("login") : t("register");
  toggleAuthMode.textContent = state.authMode === "login" ? t("register") : t("login");

  // Hints
  $("#t_loginHint").textContent = t("loginHint");
  $("#t_emptyLogin").textContent = state.me ? t("empty") : t("emptyLogin");
}

function updateAuthUI() {
  if (state.me?.email) {
    whoami.textContent = state.me.email;
    show(whoami);
    show(logoutBtn);
    hide(loginBtn);
  } else {
    whoami.textContent = "—";
    hide(whoami);
    hide(logoutBtn);
    show(loginBtn);
  }
  applyI18n();
}

function filteredApps() {
  if (state.filter === "ALL") return state.apps;
  return state.apps.filter(a => a.status === state.filter);
}

function renderStats() {
  // Statistikk basert på ALLE apps (ikke filtrert)
  const counts = {
    PLANLAGT: 0, SOKT: 0, INTERVJU: 0, TILBUD: 0, AVSLATT: 0
  };
  for (const a of state.apps) {
    if (counts[a.status] !== undefined) counts[a.status]++;
  }
  const total = state.apps.length;

  // Format som i skjermbildet ditt
  stats.textContent =
    `${t("planned")}: ${counts.PLANLAGT} • ` +
    `${t("applied")}: ${counts.SOKT} • ` +
    `${t("interview")}: ${counts.INTERVJU} • ` +
    `${t("offer")}: ${counts.TILBUD} • ` +
    `${t("rejected")}: ${counts.AVSLATT} • ` +
    `Total: ${total}`;
}

function renderList() {
  const apps = filteredApps();
  renderStats();

  listEl.innerHTML = "";

  if (!state.me) {
    const div = document.createElement("div");
    div.className = "muted";
    div.textContent = t("emptyLogin");
    listEl.appendChild(div);
    return;
  }

  if (apps.length === 0) {
    const div = document.createElement("div");
    div.className = "muted";
    div.textContent = t("empty");
    listEl.appendChild(div);
    return;
  }

  const today = new Date();
  const toISODate = (d) => d.toISOString().slice(0, 10);

  for (const a of apps) {
    const item = document.createElement("div");
    item.className = "item";

    // overdue styling
    if (a.deadline && a.status !== "AVSLATT" && a.status !== "TILBUD") {
      if (a.deadline < toISODate(today)) item.classList.add("overdue");
    }

    const top = document.createElement("div");
    top.className = "top";

    const left = document.createElement("div");
    const title = document.createElement("div");
    title.className = "title";
    title.textContent = `${a.company} — ${a.role}`;
    left.appendChild(title);

    const meta = document.createElement("div");
    meta.className = "meta";

    const d = document.createElement("span");
    d.className = "badge";
    d.textContent = `${t("deadline")}: ${a.deadline ? fmtDate(a.deadline) : "—"}`;
    meta.appendChild(d);

    const s = document.createElement("span");
    s.className = "badge";
    s.textContent = `${t("status")}: ${statusLabel(a.status)}`;
    meta.appendChild(s);

    if (a.link) {
      const link = document.createElement("a");
      link.href = a.link;
      link.target = "_blank";
      link.rel = "noreferrer";
      link.textContent = t("link");
      meta.appendChild(link);
    }

    left.appendChild(meta);

    const actions = document.createElement("div");
    actions.className = "actions";

    const select = document.createElement("select");
    ["PLANLAGT", "SOKT", "INTERVJU", "AVSLATT", "TILBUD"].forEach(st => {
      const opt = document.createElement("option");
      opt.value = st;
      opt.textContent = statusLabel(st);
      if (st === a.status) opt.selected = true;
      select.appendChild(opt);
    });
    select.addEventListener("change", async () => {
      await updateStatus(a.id, select.value);
    });

    const delBtn = document.createElement("button");
    delBtn.className = "btn ghost";
    delBtn.textContent = t("del");
    delBtn.addEventListener("click", async () => {
      await deleteApp(a.id);
    });

    actions.appendChild(select);
    actions.appendChild(delBtn);

    top.appendChild(left);
    top.appendChild(actions);

    item.appendChild(top);
    listEl.appendChild(item);
  }
}

// =========================
// API actions
// =========================
async function loadMe() {
  try {
    const res = await apiFetch(API.me, { method: "GET" });
    if (!res.ok) {
      state.me = null;
      updateAuthUI();
      return;
    }
    state.me = await res.json(); // { email }
    updateAuthUI();
  } catch {
    state.me = null;
    updateAuthUI();
  }
}

async function loadApps() {
  if (!state.me) {
    state.apps = [];
    renderList();
    return;
  }
  const res = await apiFetch(API.apps, { method: "GET" });
  if (!res.ok) {
    state.apps = [];
    renderList();
    return;
  }
  state.apps = await res.json();
  renderList();
}

async function doLogin(email, password) {
  const res = await apiFetch(API.login, {
    method: "POST",
    body: JSON.stringify({ email, password }),
  });

  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || "Login failed");
  }

  // backend returnerer { email }, cookie blir satt automatisk
  state.me = await res.json();
  updateAuthUI();
  await loadApps();
}

async function doRegister(email, password) {
  const res = await apiFetch(API.register, {
    method: "POST",
    body: JSON.stringify({ email, password }),
  });

  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || "Register failed");
  }
}

async function doLogout() {
  await apiFetch(API.logout, { method: "POST" });
  state.me = null;
  state.apps = [];
  updateAuthUI();
  renderList();
}

async function createApp(payload) {
  const res = await apiFetch(API.apps, {
    method: "POST",
    body: JSON.stringify(payload),
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || "Unauthorized");
  }
  const created = await res.json();
  state.apps = [created, ...state.apps];
  renderList();
}

async function updateStatus(id, status) {
  const res = await apiFetch(`${API.apps}/${id}/status`, {
    method: "PUT",
    body: JSON.stringify({ status }),
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || "Failed to update");
  }
  const updated = await res.json();
  state.apps = state.apps.map(a => (a.id === id ? updated : a));
  renderList();
}

async function deleteApp(id) {
  const res = await apiFetch(`${API.apps}/${id}`, { method: "DELETE" });
  if (!res.ok && res.status !== 204) {
    const text = await res.text();
    throw new Error(text || "Failed to delete");
  }
  state.apps = state.apps.filter(a => a.id !== id);
  renderList();
}

// =========================
// Events
// =========================
loginBtn?.addEventListener("click", () => openModal());
closeAuth?.addEventListener("click", () => closeModal());

// lukk modal ved klikk utenfor
authModal?.addEventListener("click", (e) => {
  if (e.target === authModal) closeModal();
});

// bytte login/register
toggleAuthMode?.addEventListener("click", () => {
  state.authMode = state.authMode === "login" ? "register" : "login";
  applyI18n();
  setMsg(authMsg, "");
});

// submit auth
authForm?.addEventListener("submit", async (e) => {
  e.preventDefault();
  setMsg(authMsg, "");

  const email = authEmail.value.trim().toLowerCase();
  const password = authPassword.value;

  try {
    if (state.authMode === "register") {
      await doRegister(email, password);
      setMsg(authMsg, t("ok"), true);
      // auto-switch til login
      state.authMode = "login";
      applyI18n();
      return;
    }

    await doLogin(email, password);
    setMsg(authMsg, t("ok"), true);
    closeModal();
  } catch (err) {
    setMsg(authMsg, err.message || "Error");
  }
});

// logout
logoutBtn?.addEventListener("click", async () => {
  await doLogout();
});

// create form
createForm?.addEventListener("submit", async (e) => {
  e.preventDefault();
  setMsg(formMsg, "");

  if (!state.me) {
    setMsg(formMsg, t("loginHint"));
    openModal();
    return;
  }

  const fd = new FormData(createForm);
  const company = (fd.get("company") || "").toString().trim();
  const role = (fd.get("role") || "").toString().trim();
  const link = (fd.get("link") || "").toString().trim();
  const deadline = (fd.get("deadline") || "").toString().trim(); // "YYYY-MM-DD"

  if (!company || !role) {
    setMsg(formMsg, t("companyReq"));
    return;
  }

  try {
    await createApp({ company, role, link, deadline });
    createForm.reset();
    setMsg(formMsg, t("ok"), true);
  } catch (err) {
    setMsg(formMsg, err.message || "Error");
  }
});

// filters
filterBtns.forEach(btn => {
  btn.addEventListener("click", () => {
    filterBtns.forEach(b => b.classList.remove("active"));
    btn.classList.add("active");
    state.filter = btn.dataset.filter;
    renderList();
  });
});

// language
langBtn?.addEventListener("click", () => {
  state.lang = state.lang === "no" ? "en" : "no";
  localStorage.setItem(STORAGE_LANG, state.lang);
  applyI18n();
  renderList();
});

// =========================
// Init
// =========================
(async function init() {
  applyI18n();
  updateAuthUI();
  await loadMe();
  await loadApps();
})();
