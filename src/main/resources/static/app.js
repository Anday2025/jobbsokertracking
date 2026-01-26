// =========================
// Config
// =========================
const API = {
  register: "/api/auth/register",
  login: "/api/auth/login",
  me: "/api/auth/me",
  logout: "/api/auth/logout",

  resendVerification: "/api/auth/resend-verification",
  forgotPassword: "/api/auth/forgot-password",
  resetPassword: "/api/auth/reset-password",

  apps: "/api/apps",
};

const STORAGE_LANG = "lang"; // "no" | "en"

// =========================
// State
// =========================
const state = {
  me: null,
  apps: [],
  filter: "ALL",
  lang: localStorage.getItem(STORAGE_LANG) || "no",

  // auth modal state
  authView: "login",        // login | register | forgot | resend | reset
  resetToken: null,         // token from URL (?token=...)
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
    confirmPassword: "Bekreft passord",
    newApp: "Ny søknad",
    apps: "Søknader",
    add: "Legg til",
    companyReq: "Firma og stilling er påkrevd",
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

    // auth extras
    forgotPassword: "Forgot password?",
    resendVerification: "Resend verification",
    send: "Send",
    back: "Tilbake",
    resetTitle: "Reset passord",
    resetBtn: "Reset",
    passwordMismatch: "Passord er ikke samme",
    fillEmail: "Fyll inn e-post",
    fillEmailAndPassword: "Fyll inn e-post og passord",
    regOk: "Bruker opprettet. Sjekk e-posten din for bekreftelseslenke.",
    resetLinkSent: "Hvis e-post finnes, er reset-link sendt.",
    verificationSent: "Hvis e-post finnes, er ny bekreftelse sendt.",
    resetOk: "Passord er oppdatert. Du kan logge inn nå.",
  },
  en: {
    title: "Job tracker",
    subtitle: "Keep track of your applications.",
    login: "Sign in",
    logout: "Sign out",
    register: "Create account",
    email: "Email",
    password: "Password",
    confirmPassword: "Confirm password",
    newApp: "New application",
    apps: "Applications",
    add: "Add",
    companyReq: "Company and role are required",
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

    forgotPassword: "Forgot password?",
    resendVerification: "Resend verification",
    send: "Send",
    back: "Back",
    resetTitle: "Reset password",
    resetBtn: "Reset",
    passwordMismatch: "Passwords do not match",
    fillEmail: "Enter email",
    fillEmailAndPassword: "Enter email and password",
    regOk: "Account created. Check your email to verify your account.",
    resetLinkSent: "If the email exists, a reset link was sent.",
    verificationSent: "If the email exists, a new verification email was sent.",
    resetOk: "Password updated. You can sign in now.",
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
const authTitle = $("#authTitle");

// main app
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
  if (!authModal) return;
  authModal.classList.remove("hidden");
  document.body.classList.add("modalOpen");
  renderAuthView();
  setTimeout(() => {
    authForm?.querySelector("input")?.focus();
  }, 40);
}

function closeModal() {
  if (!authModal) return;
  authModal.classList.add("hidden");
  document.body.classList.remove("modalOpen");
}

function fmtDate(iso) {
  if (!iso) return "";
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

async function readError(res) {
  // backend kan returnere plain text eller json
  const txt = await res.text().catch(() => "");
  if (!txt) return `HTTP ${res.status}`;
  try {
    const obj = JSON.parse(txt);
    return obj?.error || obj?.message || txt;
  } catch {
    return txt;
  }
}

function getQueryToken() {
  const url = new URL(window.location.href);
  return url.searchParams.get("token");
}

function clearQueryToken() {
  const url = new URL(window.location.href);
  url.searchParams.delete("token");
  window.history.replaceState({}, "", url.toString());
}

// =========================
// AUTH MODAL VIEWS
// =========================
function renderAuthView(message = "", ok = false) {
  if (!authForm) return;

  // Title
  if (authTitle) {
    if (state.authView === "reset") authTitle.textContent = t("resetTitle");
    else if (state.authView === "register") authTitle.textContent = t("register");
    else if (state.authView === "forgot") authTitle.textContent = t("forgotPassword");
    else if (state.authView === "resend") authTitle.textContent = t("resendVerification");
    else authTitle.textContent = t("login");
  }

  const baseLinks = `
    <div class="modalLinks">
      <button type="button" class="linkBtn" data-action="forgot">${t("forgotPassword")}</button>
      <button type="button" class="linkBtn" data-action="resend">${t("resendVerification")}</button>
    </div>
  `;

  const msgBlock = `<div id="authMsg" class="msg ${ok ? "ok" : ""}">${message || ""}</div>`;

  if (state.authView === "login" || state.authView === "register") {
    authForm.innerHTML = `
      <input id="authEmail" type="email" autocomplete="email" placeholder="${t("email")}" required />
      <input id="authPassword" type="password" autocomplete="current-password" placeholder="${t("password")}" required />

      <div class="modalActions">
        <button id="authSubmitBtn" class="btn primary" type="submit">
          ${state.authView === "login" ? t("login") : t("register")}
        </button>
        <button id="toggleAuthMode" class="btn ghost" type="button">
          ${state.authView === "login" ? t("register") : t("login")}
        </button>
      </div>
      ${baseLinks}
      ${msgBlock}
    `;
  }

  if (state.authView === "forgot") {
    authForm.innerHTML = `
      <input id="fpEmail" type="email" autocomplete="email" placeholder="${t("email")}" required />
      <div class="modalActions">
        <button class="btn primary" type="submit">${t("send")}</button>
        <button class="btn ghost" type="button" data-action="back">${t("back")}</button>
      </div>
      ${msgBlock}
    `;
  }

  if (state.authView === "resend") {
    authForm.innerHTML = `
      <input id="rvEmail" type="email" autocomplete="email" placeholder="${t("email")}" required />
      <div class="modalActions">
        <button class="btn primary" type="submit">${t("send")}</button>
        <button class="btn ghost" type="button" data-action="back">${t("back")}</button>
      </div>
      ${msgBlock}
    `;
  }

  if (state.authView === "reset") {
    authForm.innerHTML = `
      <input id="rpNew" type="password" autocomplete="new-password" placeholder="${t("password")}" required />
      <input id="rpConfirm" type="password" autocomplete="new-password" placeholder="${t("confirmPassword")}" required />

      <div class="modalActions">
        <button class="btn primary" type="submit">${t("resetBtn")}</button>
        <button class="btn ghost" type="button" data-action="back">${t("back")}</button>
      </div>
      ${baseLinks}
      ${msgBlock}
    `;
  }
}

function setAuthView(view, message = "", ok = false) {
  state.authView = view;
  renderAuthView(message, ok);
}

// =========================
// Rendering (main app)
// =========================
function applyI18n() {
  const titleEl = $("#t_title");
  const subEl = $("#t_subtitle");
  if (titleEl) titleEl.textContent = t("title");
  if (subEl) subEl.textContent = t("subtitle");

  if (loginBtn) { }


  if (loginBtn) loginBtn.textContent = t("login");
  if (logoutBtn) logoutBtn.textContent = t("logout");
  if (langBtn) langBtn.textContent = state.lang === "no" ? "NO/EN" : "EN/NO";

  const newAppTitle = $("#t_newAppTitle");
  const appsTitle = $("#t_appsTitle");
  const addBtn = $("#t_addBtn");
  if (newAppTitle) newAppTitle.textContent = t("newApp");
  if (appsTitle) appsTitle.textContent = t("apps");
  if (addBtn) addBtn.textContent = t("add");

  const fAll = $("#t_f_all");
  const fPlanned = $("#t_f_planned");
  const fApplied = $("#t_f_applied");
  const fInterview = $("#t_f_interview");
  const fRejected = $("#t_f_rejected");
  const fOffer = $("#t_f_offer");

  if (fAll) fAll.textContent = t("all");
  if (fPlanned) fPlanned.textContent = t("planned");
  if (fApplied) fApplied.textContent = t("applied");
  if (fInterview) fInterview.textContent = t("interview");
  if (fRejected) fRejected.textContent = t("rejected");
  if (fOffer) fOffer.textContent = t("offer");

  const companyLabel = $("#t_companyLabel");
  const roleLabel = $("#t_roleLabel");
  const linkLabel = $("#t_linkLabel");
  const deadlineLabel = $("#t_deadlineLabel");

  if (companyLabel) companyLabel.textContent = state.lang === "no" ? "Firma *" : "Company *";
  if (roleLabel) roleLabel.textContent = state.lang === "no" ? "Stilling *" : "Role *";
  if (linkLabel) linkLabel.textContent = t("link");
  if (deadlineLabel) deadlineLabel.textContent = t("deadline");

  const companyPh = $("#t_companyPh");
  const rolePh = $("#t_rolePh");
  const linkPh = $("#t_linkPh");

  if (companyPh) companyPh.setAttribute("placeholder", state.lang === "no" ? "F.eks. NAV / Telenor" : "e.g. NAV / Telenor");
  if (rolePh) rolePh.setAttribute("placeholder", state.lang === "no" ? "F.eks. Junior utvikler" : "e.g. Junior developer");
  if (linkPh) linkPh.setAttribute("placeholder", "https://...");

  const loginHint = $("#t_loginHint");
  const emptyLogin = $("#t_emptyLogin");
  if (loginHint) loginHint.textContent = t("loginHint");
  if (emptyLogin) emptyLogin.textContent = state.me ? t("empty") : t("emptyLogin");
}

function updateAuthUI() {
  if (state.me?.email) {
    if (whoami) whoami.textContent = state.me.email;
    show(whoami);
    show(logoutBtn);
    hide(loginBtn);
  } else {
    if (whoami) whoami.textContent = "—";
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
  if (!stats) return;

  const counts = { PLANLAGT: 0, SOKT: 0, INTERVJU: 0, TILBUD: 0, AVSLATT: 0 };
  for (const a of state.apps) {
    if (counts[a.status] !== undefined) counts[a.status]++;
  }
  const total = state.apps.length;

  stats.textContent =
      `${t("planned")}: ${counts.PLANLAGT} • ` +
      `${t("applied")}: ${counts.SOKT} • ` +
      `${t("interview")}: ${counts.INTERVJU} • ` +
      `${t("offer")}: ${counts.TILBUD} • ` +
      `${t("rejected")}: ${counts.AVSLATT} • ` +
      `Total: ${total}`;
}

function renderList() {
  if (!listEl) return;

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
      try {
        await updateStatus(a.id, select.value);
      } catch (e) {
        select.value = a.status;
        console.error(e);
      }
    });

    const delBtn = document.createElement("button");
    delBtn.className = "btn ghost";
    delBtn.textContent = t("del");
    delBtn.addEventListener("click", async () => {
      try {
        await deleteApp(a.id);
      } catch (e) {
        console.error(e);
      }
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
    state.me = await res.json();
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
  if (!res.ok) throw new Error(await readError(res));

  state.me = await res.json();
  updateAuthUI();
  await loadApps();
}

async function doRegister(email, password) {
  const res = await apiFetch(API.register, {
    method: "POST",
    body: JSON.stringify({ email, password }),
  });
  if (!res.ok) throw new Error(await readError(res));
}

async function doResendVerification(email) {
  const res = await apiFetch(API.resendVerification, {
    method: "POST",
    body: JSON.stringify({ email }),
  });
  if (!res.ok) throw new Error(await readError(res));
}

async function doForgotPassword(email) {
  const res = await apiFetch(API.forgotPassword, {
    method: "POST",
    body: JSON.stringify({ email }),
  });
  if (!res.ok) throw new Error(await readError(res));
}

async function doResetPassword(token, password) {
  const res = await apiFetch(API.resetPassword, {
    method: "POST",
    body: JSON.stringify({ token, password }),
  });
  if (!res.ok) throw new Error(await readError(res));
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
  if (!res.ok) throw new Error(await readError(res));

  const created = await res.json();
  state.apps = [created, ...state.apps];
  renderList();
}

async function updateStatus(id, status) {
  const res = await apiFetch(`${API.apps}/${id}/status`, {
    method: "PUT",
    body: JSON.stringify({ status }),
  });
  if (!res.ok) throw new Error(await readError(res));

  const updated = await res.json();
  state.apps = state.apps.map(a => (a.id === id ? updated : a));
  renderList();
}

async function deleteApp(id) {
  const res = await apiFetch(`${API.apps}/${id}`, { method: "DELETE" });
  if (!(res.status === 204 || res.ok)) throw new Error(await readError(res));

  state.apps = state.apps.filter(a => a.id !== id);
  renderList();
}

// =========================
// Events
// =========================
loginBtn?.addEventListener("click", (e) => {
  e.preventDefault();
  setAuthView("login");
  openModal();
});

closeAuth?.addEventListener("click", (e) => {
  e.preventDefault();
  closeModal();
});

// close modal by clicking backdrop
authModal?.addEventListener("click", (e) => {
  if (e.target === authModal) closeModal();
});

// AUTH modal click actions (buttons inside views)
authForm?.addEventListener("click", async (e) => {
  const btn = e.target?.closest?.("button");
  if (!btn) return;

  const action = btn.getAttribute("data-action");
  if (!action) return;

  e.preventDefault();

  if (action === "forgot") {
    setAuthView("forgot");
    return;
  }
  if (action === "resend") {
    setAuthView("resend");
    return;
  }
  if (action === "back") {
    // if came from reset token url, go to login anyway
    setAuthView("login");
    return;
  }
});

// Toggle login/register
authForm?.addEventListener("click", (e) => {
  const toggle = e.target?.closest?.("#toggleAuthMode");
  if (!toggle) return;

  e.preventDefault();
  setAuthView(state.authView === "login" ? "register" : "login");
});

// Auth submit (all views)
authForm?.addEventListener("submit", async (e) => {
  e.preventDefault();

  try {
    // LOGIN / REGISTER
    if (state.authView === "login" || state.authView === "register") {
      const email = (authForm.querySelector("#authEmail")?.value || "").trim().toLowerCase();
      const password = authForm.querySelector("#authPassword")?.value || "";

      if (!email || !password) {
        renderAuthView(t("fillEmailAndPassword"), false);
        return;
      }

      if (state.authView === "register") {
        await doRegister(email, password);
        // show success and flip to login
        setAuthView("login", t("regOk"), true);
        return;
      }

      await doLogin(email, password);
      closeModal();
      return;
    }

    // FORGOT PASSWORD
    if (state.authView === "forgot") {
      const email = (authForm.querySelector("#fpEmail")?.value || "").trim().toLowerCase();
      if (!email) {
        renderAuthView(t("fillEmail"), false);
        return;
      }
      await doForgotPassword(email);
      setAuthView("login", t("resetLinkSent"), true);
      return;
    }

    // RESEND VERIFICATION
    if (state.authView === "resend") {
      const email = (authForm.querySelector("#rvEmail")?.value || "").trim().toLowerCase();
      if (!email) {
        renderAuthView(t("fillEmail"), false);
        return;
      }
      await doResendVerification(email);
      setAuthView("login", t("verificationSent"), true);
      return;
    }

    // RESET PASSWORD
    if (state.authView === "reset") {
      const newPass = authForm.querySelector("#rpNew")?.value || "";
      const confirm = authForm.querySelector("#rpConfirm")?.value || "";

      if (newPass !== confirm) {
        renderAuthView(t("passwordMismatch"), false);
        return;
      }

      if (!state.resetToken) {
        renderAuthView("Token mangler", false);
        return;
      }

      await doResetPassword(state.resetToken, newPass);

      // ✅ Clear url token, switch back to login automatically
      clearQueryToken();
      state.resetToken = null;

      // show ok message then go to login
      setAuthView("login", t("resetOk"), true);
      return;
    }

  } catch (err) {
    const msg = err?.message || "Noe gikk galt";
    renderAuthView(msg, false);
    console.error(err);
  }
});

// logout
logoutBtn?.addEventListener("click", async () => {
  try {
    await doLogout();
  } catch (e) {
    console.error(e);
  }
});

// create app
createForm?.addEventListener("submit", async (e) => {
  e.preventDefault();
  setMsg(formMsg, "");

  if (!state.me) {
    setMsg(formMsg, t("loginHint"));
    setAuthView("login");
    openModal();
    return;
  }

  const fd = new FormData(createForm);
  const company = (fd.get("company") || "").toString().trim();
  const role = (fd.get("role") || "").toString().trim();
  const link = (fd.get("link") || "").toString().trim();
  const deadline = (fd.get("deadline") || "").toString().trim();

  if (!company || !role) {
    setMsg(formMsg, t("companyReq"));
    return;
  }

  try {
    await createApp({ company, role, link, deadline });
    createForm.reset();
    setMsg(formMsg, "", true);
  } catch (err) {
    setMsg(formMsg, err?.message || "Error");
    console.error(err);
  }
});

// filters
filterBtns.forEach(btn => {
  btn.addEventListener("click", () => {
    filterBtns.forEach(b => b.classList.remove("active"));
    btn.classList.add("active");
    state.filter = btn.dataset.filter || "ALL";
    renderList();
  });
});

// language
langBtn?.addEventListener("click", () => {
  state.lang = state.lang === "no" ? "en" : "no";
  localStorage.setItem(STORAGE_LANG, state.lang);
  applyI18n();
  renderList();
  // if modal open, re-render
  if (authModal && !authModal.classList.contains("hidden")) {
    renderAuthView();
  }
});

// =========================
// Init
// =========================
(async function init() {
  applyI18n();
  updateAuthUI();

  // ✅ If url has ?token=... => open reset view automatically
  const token = getQueryToken();
  if (token) {
    state.resetToken = token;
    setAuthView("reset");
    openModal();
  }

  await loadMe();
  await loadApps();
})();
