let currentFilter = "ALL";

// ---------- ELEMENTS ----------
const listEl = document.getElementById("list");
const statsEl = document.getElementById("stats");
const form = document.getElementById("createForm");
const formMsg = document.getElementById("formMsg");

const loginBtn = document.getElementById("loginBtn");
const logoutBtn = document.getElementById("logoutBtn");
const whoamiEl = document.getElementById("whoami");
const langBtn = document.getElementById("langBtn");

const authModal = document.getElementById("authModal");
const closeAuth = document.getElementById("closeAuth");
const authForm = document.getElementById("authForm");
const authEmail = document.getElementById("authEmail");
const authPassword = document.getElementById("authPassword");
const authTitle = document.getElementById("authTitle");
const authSubmitBtn = document.getElementById("authSubmitBtn");
const toggleAuthMode = document.getElementById("toggleAuthMode");
const authMsg = document.getElementById("authMsg");

// ---------- I18N (NO/EN) ----------
const I18N = {
  no: {
    title: "Jobbsøker-tracker",
    subtitle: "Hold oversikt over søknadene dine.",
    login: "Logg inn",
    logout: "Logg ut",
    createUser: "Opprett bruker",
    email: "E-post",
    password: "Passord",
    newApp: "Ny søknad",
    apps: "Søknader",
    add: "Legg til",
    emptyLogin: "Ingen treff. Logg inn for å se dine søknader.",
    hint: "Logg inn for å lagre og se dine søknader.",
    filters: { ALL:"Alle", PLANLAGT:"Planlagt", SOKT:"Søkt", INTERVJU:"Intervju", AVSLATT:"Avslått", TILBUD:"Tilbud" },
    labels: { company:"Firma *", role:"Stilling *", link:"Link", deadline:"Frist" },
    ph: { company:"F.eks. NAV / Telenor", role:"F.eks. Junior utvikler", link:"https://..." }
  },
  en: {
    title: "Job Tracker",
    subtitle: "Keep track of your job applications.",
    login: "Sign in",
    logout: "Sign out",
    createUser: "Create account",
    email: "Email",
    password: "Password",
    newApp: "New application",
    apps: "Applications",
    add: "Add",
    emptyLogin: "No results. Sign in to see your applications.",
    hint: "Sign in to save and view your applications.",
    filters: { ALL:"All", PLANLAGT:"Planned", SOKT:"Applied", INTERVJU:"Interview", AVSLATT:"Rejected", TILBUD:"Offer" },
    labels: { company:"Company *", role:"Role *", link:"Link", deadline:"Deadline" },
    ph: { company:"e.g. DNB / Telenor", role:"e.g. Junior developer", link:"https://..." }
  }
};

const LANG_KEY = "jt_lang";
let lang = localStorage.getItem(LANG_KEY) || "no";

function t() { return I18N[lang]; }

function applyLang() {
  document.documentElement.lang = lang;

  document.getElementById("t_title").textContent = t().title;
  document.getElementById("t_subtitle").textContent = t().subtitle;

  loginBtn.textContent = t().login;
  logoutBtn.textContent = t().logout;
  langBtn.textContent = (lang === "no") ? "NO/EN" : "EN/NO";

  document.getElementById("t_newAppTitle").textContent = t().newApp;
  document.getElementById("t_appsTitle").textContent = t().apps;
  document.getElementById("t_addBtn").textContent = t().add;
  document.getElementById("t_emptyLogin").textContent = t().emptyLogin;
  document.getElementById("t_loginHint").textContent = t().hint;

  document.getElementById("t_companyLabel").textContent = t().labels.company;
  document.getElementById("t_roleLabel").textContent = t().labels.role;
  document.getElementById("t_linkLabel").textContent = t().labels.link;
  document.getElementById("t_deadlineLabel").textContent = t().labels.deadline;

  document.getElementById("t_companyPh").placeholder = t().ph.company;
  document.getElementById("t_rolePh").placeholder = t().ph.role;
  document.getElementById("t_linkPh").placeholder = t().ph.link;

  document.getElementById("t_f_all").textContent = t().filters.ALL;
  document.getElementById("t_f_planned").textContent = t().filters.PLANLAGT;
  document.getElementById("t_f_applied").textContent = t().filters.SOKT;
  document.getElementById("t_f_interview").textContent = t().filters.INTERVJU;
  document.getElementById("t_f_rejected").textContent = t().filters.AVSLATT;
  document.getElementById("t_f_offer").textContent = t().filters.TILBUD;

  authEmail.placeholder = t().email;
  authPassword.placeholder = t().password;

  // auth modal texts depend on mode:
  updateAuthTexts();
}

langBtn.addEventListener("click", () => {
  lang = (lang === "no") ? "en" : "no";
  localStorage.setItem(LANG_KEY, lang);
  applyLang();
});

// ---------- MODAL OPEN/CLOSE ----------
function openModal() {
  authMsg.textContent = "";
  authMsg.classList.remove("ok");
  authModal.classList.remove("hidden");
  document.body.classList.add("modalOpen");
  authEmail.focus();
}
function closeModal() {
  authModal.classList.add("hidden");
  document.body.classList.remove("modalOpen");
}

loginBtn.addEventListener("click", openModal);
closeAuth.addEventListener("click", closeModal);
authModal.addEventListener("click", (e) => {
  if (e.target === authModal) closeModal();
});
document.addEventListener("keydown", (e) => {
  if (e.key === "Escape" && !authModal.classList.contains("hidden")) closeModal();
});

// ---------- AUTH MODE (login/register) ----------
let authMode = "login"; // "login" | "register"

function updateAuthTexts() {
  if (authMode === "login") {
    authTitle.textContent = t().login;
    authSubmitBtn.textContent = t().login;
    toggleAuthMode.textContent = t().createUser;
  } else {
    authTitle.textContent = t().createUser;
    authSubmitBtn.textContent = t().createUser;
    toggleAuthMode.textContent = t().login;
  }
}

toggleAuthMode.addEventListener("click", () => {
  authMode = (authMode === "login") ? "register" : "login";
  authMsg.textContent = "";
  authMsg.classList.remove("ok");
  updateAuthTexts();
});

// ---------- FETCH HELPERS (COOKIE!) ----------
async function api(path, options = {}) {
  const res = await fetch(path, {
    ...options,
    credentials: "include", // 👈 cookie sendes alltid
    headers: {
      ...(options.headers || {}),
      "Content-Type": options.body ? "application/json" : (options.headers?.["Content-Type"] || undefined)
    }
  });
  return res;
}

// ---------- AUTH REQUESTS ----------
async function fetchMe() {
  const res = await api("/api/auth/me");
  if (!res.ok) return null;
  return await res.json();
}

async function setLoggedInUI(email) {
  whoamiEl.textContent = email;
  whoamiEl.classList.remove("hidden");
  logoutBtn.classList.remove("hidden");
  loginBtn.classList.add("hidden");
}

function setLoggedOutUI() {
  whoamiEl.textContent = "—";
  whoamiEl.classList.add("hidden");
  logoutBtn.classList.add("hidden");
  loginBtn.classList.remove("hidden");

  statsEl.textContent = "—";
  listEl.innerHTML = `<div class="muted" id="t_emptyLogin">${t().emptyLogin}</div>`;
}

logoutBtn.addEventListener("click", async () => {
  await api("/api/auth/logout", { method: "POST" });
  setLoggedOutUI();
});

// submit login/register
authForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  authMsg.textContent = "";
  authMsg.classList.remove("ok");

  const payload = {
    email: authEmail.value.trim(),
    password: authPassword.value
  };

  const endpoint = (authMode === "login") ? "/api/auth/login" : "/api/auth/register";

  const res = await api(endpoint, {
    method: "POST",
    body: JSON.stringify(payload)
  });

  if (!res.ok) {
    const text = await res.text();
    authMsg.textContent = text || (authMode === "login" ? "Innlogging feilet." : "Registrering feilet.");
    return;
  }

  if (authMode === "register") {
    authMsg.textContent = (lang === "no")
      ? "Bruker opprettet. Du kan nå logge inn."
      : "Account created. You can sign in now.";
    authMsg.classList.add("ok");
    authMode = "login";
    updateAuthTexts();
    return;
  }

  // login ok -> sjekk /me (cookie må fungere)
  const me = await fetchMe();
  if (!me || !me.email) {
    authMsg.textContent = "Innlogging feilet. Token ble ikke akseptert (/api/auth/me).";
    return;
  }

  authMsg.textContent = "OK";
  authMsg.classList.add("ok");

  await setLoggedInUI(me.email);
  closeModal();
  await load(); // load apps
});

// ---------- EXISTING APP LOGIC ----------
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
  formMsg.classList.remove("ok");

  const data = new FormData(form);
  const payload = {
    company: data.get("company"),
    role: data.get("role"),
    link: data.get("link"),
    deadline: data.get("deadline")
  };

  const res = await api("/api/apps", {
    method: "POST",
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

function badge(status) {
  const mapNo = { PLANLAGT:"Planlagt", SOKT:"Søkt", INTERVJU:"Intervju", AVSLATT:"Avslått", TILBUD:"Tilbud" };
  const mapEn = { PLANLAGT:"Planned", SOKT:"Applied", INTERVJU:"Interview", AVSLATT:"Rejected", TILBUD:"Offer" };
  const m = (lang === "no") ? mapNo : mapEn;
  return m[status] ?? status;
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
  await api(`/api/apps/${id}/status`, {
    method: "PUT",
    body: JSON.stringify({ status })
  });
  await load();
}

async function removeItem(id) {
  await api(`/api/apps/${id}`, { method: "DELETE" });
  await load();
}

function render(apps) {
  const filtered = currentFilter === "ALL" ? apps : apps.filter(a => a.status === currentFilter);

  const counts = apps.reduce((acc, a) => {
    acc[a.status] = (acc[a.status] || 0) + 1;
    return acc;
  }, {});

  statsEl.textContent =
    `${badge("PLANLAGT")}: ${counts.PLANLAGT || 0} • ${badge("SOKT")}: ${counts.SOKT || 0} • ${badge("INTERVJU")}: ${counts.INTERVJU || 0} • ${badge("TILBUD")}: ${counts.TILBUD || 0} • ${badge("AVSLATT")}: ${counts.AVSLATT || 0}`;

  if (filtered.length === 0) {
    listEl.innerHTML = `<div class="muted">${(lang==="no") ? "Ingen treff." : "No results."}</div>`;
    return;
  }

  listEl.innerHTML = "";
  for (const a of filtered) {
    const el = document.createElement("div");
    el.className = "item" + (isOverdue(a.deadline, a.status) ? " overdue" : "");

    const link = a.link
      ? `<a href="${a.link}" target="_blank" rel="noreferrer">${(lang==="no") ? "Link" : "Link"}</a>`
      : `<span class="muted">${(lang==="no") ? "Ingen link" : "No link"}</span>`;

    el.innerHTML = `
      <div class="top">
        <div>
          <div class="title">${escapeHtml(a.company)} — ${escapeHtml(a.role)}</div>
          <div class="meta">
            <span>${(lang==="no") ? "Frist" : "Deadline"}: <span class="badge">${fmtDate(a.deadline)}</span></span>
            <span>${(lang==="no") ? "Status" : "Status"}: <span class="badge">${badge(a.status)}</span></span>
            <span>${link}</span>
          </div>
        </div>
        <div class="actions">
          <select data-id="${a.id}" class="statusSelect">
            ${["PLANLAGT","SOKT","INTERVJU","AVSLATT","TILBUD"].map(s =>
              `<option value="${s}" ${s===a.status ? "selected" : ""}>${badge(s)}</option>`
            ).join("")}
          </select>
          <button class="btn ghost" data-del="${a.id}">${(lang==="no") ? "Slett" : "Delete"}</button>
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
  // Hvis ikke innlogget -> ikke spam 401
  const me = await fetchMe();
  if (!me) {
    setLoggedOutUI();
    return;
  }
  await setLoggedInUI(me.email);

  const res = await api("/api/apps");
  if (!res.ok) {
    const text = await res.text();
    listEl.innerHTML = `<div class="muted">${escapeHtml(text || "Unauthorized")}</div>`;
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

// init
applyLang();
load();
