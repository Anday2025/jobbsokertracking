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

  // modal views:
  // login | register | forgot | resend | reset
  authView: "login",

  // reset token from URL (?token=...)
  resetToken: null,
};

// =========================
// i18n
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

    newPassword: "Nytt passord",
    confirmPassword: "Bekreft passord",
    resetPassword: "Reset passord",
    reset: "Reset",
    back: "Tilbake",

    resendVerification: "Resend verification",
    forgotPassword: "Forgot password?",
    send: "Send",

    passMismatch: "passord ikke er samme",
    weakPass: "Passordkrav: minst 8 tegn, stor/liten bokstav og tall",

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

    regOk: "Bruker opprettet. Sjekk e-posten din for bekreftelseslenke.",
    forgotOk: "Hvis e-post finnes, er reset-link sendt.",
    resendOk: "Hvis e-post finnes, er ny verifikasjon sendt.",
    resetOk: "Passord er oppdatert. Du kan logge inn nå ✅",
  },
  en: {
    title: "Job tracker",
    subtitle: "Keep track of your applications.",

    login: "Sign in",
    logout: "Sign out",
    register: "Create account",
    email: "Email",
    password: "Password",

    newPassword: "New password",
    confirmPassword: "Confirm password",
    resetPassword: "Reset password",
    reset: "Reset",
    back: "Back",

    resendVerification: "Resend verification",
    forgotPassword: "Forgot password?",
    send: "Send",

    passMismatch: "Passwords do not match",
    weakPass: "Password must be 8+ chars, upper/lowercase + number",

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

    regOk: "Account created. Check your email to verify your account.",
    forgotOk: "If the email exists, a reset link has been sent.",
    resendOk: "If the email exists, a new verification email has been sent.",
    resetOk: "Password updated. You can sign in now ✅",
  }
};

function t(key) {
  return (T[state.lang] && T[state.lang][key]) || key;
}

// =========================
// DOM helper
// =========================
const $ = (sel) => document.querySelector(sel);

function setMsg(el, text, ok = false) {
  if (!el) return;
  el.textContent = text || "";
  el.classList.toggle("ok", !!ok);
}

function show(el) { el?.classList.remove("hidden"); }
function hide(el) { el?.classList.add("hidden"); }

// =========================
// DOM refs
// =========================
const loginBtn = $("#loginBtn");
const logoutBtn = $("#logoutBtn");
const whoami = $("#whoami");
const langBtn = $("#langBtn");

const authModal = $("#authModal");
const closeAuth = $("#closeAuth");
const authForm = $("#authForm");
const authMsg = $("#authMsg");

const createForm = $("#createForm");
const formMsg = $("#formMsg");

const stats = $("#stats");
const listEl = $("#list");

const filterBtns = Array.from(document.querySelectorAll(".filter"));

// =========================
// Modal open/close
// =========================
function openModal() {
  if (!authModal) return;
  authModal.classList.remove("hidden");
  document.body.classList.add("modalOpen");
  setMsg(authMsg, "");
  setTimeout(() => {
    const firstInput = authForm?.querySelector("input");
    firstInput?.focus();
  }, 30);
}

function closeModal() {
  if (!authModal) return;
  authModal.classList.add("hidden");
  document.body.classList.remove("modalOpen");
  setMsg(authMsg, "");
}

// close modal by clicking backdrop
authModal?.addEventListener("click", (e) => {
  if (e.target === authModal) closeModal();
});

closeAuth?.addEventListener("click", (e) => {
  e.preventDefault();
  closeModal();
});

loginBtn?.addEventListener("click", (e) => {
  e.preventDefault();
  state.authView = "login";
  renderAuthView();
  openModal();
});

// =========================
// Helpers
// =========================
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

function getQueryParam(name) {
  const url = new URL(window.location.href);
  return url.searchParams.get(name);
}

function removeQueryParam(name) {
  const url = new URL(window.location.href);
  url.searchParams.delete(name);
  window.history.replaceState({}, "", url.toString());
}

function isStrongPassword(pw) {
  if (!pw) return false;
  // minst 8 tegn + stor + liten + tall
  return /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$/.test(pw);
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
  const txt = await res.text().catch(() => "");
  try {
    const json = JSON.parse(txt);
    if (json?.error) return json.error;
  } catch {}
  return txt || `HTTP ${res.status}`;
}

// =========================
// Rendering (Main UI)
// =========================
function applyI18n() {
  $("#t_title") && ($("#t_title").textContent = t("title"));
  $("#t_subtitle") && ($("#t_subtitle").textContent = t("subtitle"));

  if (loginBtn) loginBtn.textContent = t("login");
  if (logoutBtn) logoutBtn.textContent = t("logout");
  if (langBtn) langBtn.textContent = state.lang === "no" ? "NO/EN" : "EN/NO";

  $("#t_newAppTitle") && ($("#t_newAppTitle").textContent = t("newApp"));
  $("#t_appsTitle") && ($("#t_appsTitle").textContent = t("apps"));
  $("#t_addBtn") && ($("#t_addBtn").textContent = t("add"));

  $("#t_f_all") && ($("#t_f_all").textContent = t("all"));
  $("#t_f_planned") && ($("#t_f_planned").textContent = t("planned"));
  $("#t_f_applied") && ($("#t_f_applied").textContent = t("applied"));
  $("#t_f_interview") && ($("#t_f_interview").textContent = t("interview"));
  $("#t_f_rejected") && ($("#t_f_rejected").textContent = t("rejected"));
  $("#t_f_offer") && ($("#t_f_offer").textContent = t("offer"));

  $("#t_companyLabel") && ($("#t_companyLabel").textContent = state.lang === "no" ? "Firma *" : "Company *");
  $("#t_roleLabel") && ($("#t_roleLabel").textContent = state.lang === "no" ? "Stilling *" : "Role *");
  $("#t_linkLabel") && ($("#t_linkLabel").textContent = t("link"));
  $("#t_deadlineLabel") && ($("#t_deadlineLabel").textContent = t("deadline"));

  $("#t_companyPh") && $("#t_companyPh").setAttribute("placeholder", state.lang === "no" ? "F.eks. NAV / Telenor" : "e.g. NAV / Telenor");
  $("#t_rolePh") && $("#t_rolePh").setAttribute("placeholder", state.lang === "no" ? "F.eks. Junior utvikler" : "e.g. Junior developer");
  $("#t_linkPh") && $("#t_linkPh").setAttribute("placeholder", "https://...");

  const loginHint = $("#t_loginHint");
  const emptyLogin = $("#t_emptyLogin");
  if (loginHint) loginHint.textContent = t("loginHint");
  if (emptyLogin) emptyLogin.textContent = state.me ? t("empty") : t("emptyLogin");

  // modal title
  const authTitle = $("#authTitle");
  if (authTitle) {
    authTitle.textContent =
        state.authView === "login" ? t("login")
            : state.authView === "register" ? t("register")
                : state.authView === "forgot" ? t("forgotPassword")
                    : state.authView === "resend" ? t("resendVerification")
                        : t("resetPassword");
  }
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
// Auth modal rendering
// =========================
function renderAuthView() {
  if (!authForm) return;

  setMsg(authMsg, "");
  applyI18n();

  // Build dynamic content inside authForm
  let html = "";

  if (state.authView === "login" || state.authView === "register") {
    html = `
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

      <div class="modalLinks">
        <button id="forgotBtn" class="linkBtn" type="button">${t("forgotPassword")}</button>
        <button id="resendBtn" class="linkBtn" type="button">${t("resendVerification")}</button>
      </div>

      <div id="authMsg" class="msg"></div>
    `;
  }

  if (state.authView === "forgot") {
    html = `
      <input id="authEmail" type="email" autocomplete="email" placeholder="${t("email")}" required />

      <div class="modalActions">
        <button class="btn primary" type="submit">${t("send")}</button>
        <button id="backBtn" class="btn ghost" type="button">${t("back")}</button>
      </div>

      <div id="authMsg" class="msg"></div>
    `;
  }

  if (state.authView === "resend") {
    html = `
      <input id="authEmail" type="email" autocomplete="email" placeholder="${t("email")}" required />

      <div class="modalActions">
        <button class="btn primary" type="submit">${t("send")}</button>
        <button id="backBtn" class="btn ghost" type="button">${t("back")}</button>
      </div>

      <div id="authMsg" class="msg"></div>
    `;
  }

  if (state.authView === "reset") {
    html = `
      <input id="newPassword" type="password" autocomplete="new-password" placeholder="${t("newPassword")}" required />
      <input id="confirmPassword" type="password" autocomplete="new-password" placeholder="${t("confirmPassword")}" required />

      <div class="modalActions">
        <button class="btn primary" type="submit">${t("reset")}</button>
        <button id="backBtn" class="btn ghost" type="button">${t("back")}</button>
      </div>

      <div id="authMsg" class="msg"></div>
    `;
  }

  authForm.innerHTML = html;

  // Update title
  const authTitle = $("#authTitle");
  if (authTitle) {
    authTitle.textContent =
        state.authView === "login" ? t("login")
            : state.authView === "register" ? t("register")
                : state.authView === "forgot" ? t("forgotPassword")
                    : state.authView === "resend" ? t("resendVerification")
                        : t("resetPassword");
  }

  // Wire buttons
  const toggleAuthMode = $("#toggleAuthMode");
  toggleAuthMode?.addEventListener("click", () => {
    state.authView = state.authView === "login" ? "register" : "login";
    renderAuthView();
  });

  $("#forgotBtn")?.addEventListener("click", () => {
    state.authView = "forgot";
    renderAuthView();
  });

  $("#resendBtn")?.addEventListener("click", () => {
    state.authView = "resend";
    renderAuthView();
  });

  $("#backBtn")?.addEventListener("click", () => {
    // if reset view came from URL token, we still go back to login
    state.authView = "login";
    renderAuthView();
  });
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
// Modal submit handler (dynamic)
// =========================
document.addEventListener("submit", async (e) => {
  if (e.target !== authForm) return;
  e.preventDefault();
  setMsg(authMsg, "");

  try {
    // LOGIN / REGISTER
    if (state.authView === "login" || state.authView === "register") {
      const email = ($("#authEmail")?.value || "").trim().toLowerCase();
      const password = $("#authPassword")?.value || "";

      if (!email || !password) {
        setMsg(authMsg, state.lang === "no" ? "Fyll inn e-post og passord" : "Enter email and password");
        return;
      }

      if (state.authView === "register") {
        if (!isStrongPassword(password)) {
          setMsg(authMsg, t("weakPass"));
          return;
        }
        await doRegister(email, password);

        setMsg(authMsg, t("regOk"), true);
        // go back to login automatically
        state.authView = "login";
        renderAuthView();
        return;
      }

      await doLogin(email, password);
      closeModal();
      return;
    }

    // FORGOT PASSWORD
    if (state.authView === "forgot") {
      const email = ($("#authEmail")?.value || "").trim().toLowerCase();
      if (!email) {
        setMsg(authMsg, state.lang === "no" ? "Skriv inn e-post" : "Enter email");
        return;
      }

      await doForgotPassword(email);

      setMsg(authMsg, t("forgotOk"), true);

      // back to login automatically
      state.authView = "login";
      renderAuthView();
      return;
    }

    // RESEND VERIFICATION
    if (state.authView === "resend") {
      const email = ($("#authEmail")?.value || "").trim().toLowerCase();
      if (!email) {
        setMsg(authMsg, state.lang === "no" ? "Skriv inn e-post" : "Enter email");
        return;
      }

      await doResendVerification(email);

      setMsg(authMsg, t("resendOk"), true);

      // back to login automatically
      state.authView = "login";
      renderAuthView();
      return;
    }

    // RESET PASSWORD
    if (state.authView === "reset") {
      const newPw = $("#newPassword")?.value || "";
      const confirmPw = $("#confirmPassword")?.value || "";

      if (!newPw || !confirmPw) {
        setMsg(authMsg, state.lang === "no" ? "Fyll inn begge feltene" : "Fill in both fields");
        return;
      }

      if (newPw !== confirmPw) {
        setMsg(authMsg, t("passMismatch"));
        return;
      }

      if (!isStrongPassword(newPw)) {
        setMsg(authMsg, t("weakPass"));
        return;
      }

      if (!state.resetToken) {
        setMsg(authMsg, state.lang === "no" ? "Token mangler" : "Missing token");
        return;
      }

      await doResetPassword(state.resetToken, newPw);

      setMsg(authMsg, t("resetOk"), true);

      // Remove token from URL after success
      removeQueryParam("token");
      state.resetToken = null;

      // back to login automatically
      state.authView = "login";
      renderAuthView();
      return;
    }

  } catch (err) {
    setMsg(authMsg, err?.message || "Noe gikk galt");
    console.error(err);
  }
}, true);

// =========================
// Events (main buttons)
// =========================
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
    state.authView = "login";
    renderAuthView();
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
  renderAuthView();
});

// =========================
// Init
// =========================
(async function init() {
  applyI18n();
  updateAuthUI();

  // ✅ Auto open reset view if URL has ?token=
  const token = getQueryParam("token");
  if (token) {
    state.resetToken = token;
    state.authView = "reset";
    renderAuthView();
    openModal();
  } else {
    state.authView = "login";
    renderAuthView();
  }

  await loadMe();
  await loadApps();
})();
