/*  app.js  (B1: HttpOnly SESSION-cookie)
    - Backend setter cookie "SESSION" på /api/auth/login (HttpOnly)
    - Frontend bruker fetch(..., { credentials: "include" }) for ALLE API-kall
    - INGEN localStorage token
*/

(() => {
  // -----------------------------
  // Config
  // -----------------------------
  const API = {
    login: "/api/auth/login",
    register: "/api/auth/register",
    me: "/api/auth/me",
    logout: "/api/auth/logout",
    apps: "/api/apps",
  };

  // -----------------------------
  // DOM helpers
  // -----------------------------
  const $ = (id) => document.getElementById(id);
  const show = (el) => el && el.classList.remove("hidden");
  const hide = (el) => el && el.classList.add("hidden");

  // Header
  const loginBtn = $("loginBtn");
  const logoutBtn = $("logoutBtn");
  const whoami = $("whoami");
  const langBtn = $("langBtn");

  // Modal
  const authModal = $("authModal");
  const closeAuth = $("closeAuth");
  const authTitle = $("authTitle");
  const authForm = $("authForm");
  const authEmail = $("authEmail");
  const authPassword = $("authPassword");
  const authSubmitBtn = $("authSubmitBtn");
  const toggleAuthMode = $("toggleAuthMode");
  const authMsg = $("authMsg");

  // App UI
  const createForm = $("createForm");
  const formMsg = $("formMsg");
  const listEl = $("list");
  const statsEl = $("stats");
  const emptyLogin = $("t_emptyLogin");
  const loginHint = $("t_loginHint");

  // Filters
  const filterBtns = Array.from(document.querySelectorAll(".filter"));

  // -----------------------------
  // State
  // -----------------------------
  let authedEmail = null;
  let mode = "login"; // "login" | "register"
  let currentFilter = "ALL";
  let appsCache = [];

  // -----------------------------
  // i18n (NO/EN) with localStorage
  // -----------------------------
  const I18N_KEY = "jobtracker_lang";
  const dict = {
    no: {
      title: "Jobbsøker-tracker",
      subtitle: "Hold oversikt over søknadene dine.",
      login: "Logg inn",
      logout: "Logg ut",
      notLoggedIn: "Ikke innlogget",
      newApp: "Ny søknad",
      apps: "Søknader",
      add: "Legg til",
      company: "Firma *",
      role: "Stilling *",
      link: "Link",
      deadline: "Frist",
      all: "Alle",
      planned: "Planlagt",
      applied: "Søkt",
      interview: "Intervju",
      rejected: "Avslått",
      offer: "Tilbud",
      emptyLogin: "Ingen treff. Logg inn for å se dine søknader.",
      loginHint: "Logg inn for å lagre og se dine søknader.",
      createUser: "Opprett bruker",
      password: "Passord",
      email: "E-post",
      registerTitle: "Opprett bruker",
      loginFailed: "Innlogging feilet.",
      registerOk: "Bruker opprettet. Du kan logge inn nå.",
      registerFailed: "Registrering feilet.",
      addFailed: "Kunne ikke lagre søknaden.",
      deleteFailed: "Kunne ikke slette.",
      updateFailed: "Kunne ikke oppdatere.",
    },
    en: {
      title: "Job search tracker",
      subtitle: "Keep track of your applications.",
      login: "Sign in",
      logout: "Sign out",
      notLoggedIn: "Not signed in",
      newApp: "New application",
      apps: "Applications",
      add: "Add",
      company: "Company *",
      role: "Role *",
      link: "Link",
      deadline: "Deadline",
      all: "All",
      planned: "Planned",
      applied: "Applied",
      interview: "Interview",
      rejected: "Rejected",
      offer: "Offer",
      emptyLogin: "No results. Sign in to see your applications.",
      loginHint: "Sign in to save and view your applications.",
      createUser: "Create account",
      password: "Password",
      email: "Email",
      registerTitle: "Create account",
      loginFailed: "Sign in failed.",
      registerOk: "Account created. You can sign in now.",
      registerFailed: "Registration failed.",
      addFailed: "Could not save application.",
      deleteFailed: "Could not delete.",
      updateFailed: "Could not update.",
    },
  };

  function getLang() {
    const saved = localStorage.getItem(I18N_KEY);
    return saved === "en" ? "en" : "no";
  }

  function setLang(next) {
    localStorage.setItem(I18N_KEY, next);
    applyLang();
  }

  function t(key) {
    const lang = getLang();
    return dict[lang][key] ?? key;
  }

  function applyLang() {
    const lang = getLang();

    // Toggle button label
    langBtn.textContent = lang === "no" ? "NO/EN" : "EN/NO";

    // Static text elements that exist in HTML via ids
    const map = [
      ["t_title", "title"],
      ["t_subtitle", "subtitle"],
      ["t_newAppTitle", "newApp"],
      ["t_appsTitle", "apps"],
      ["t_companyLabel", "company"],
      ["t_roleLabel", "role"],
      ["t_linkLabel", "link"],
      ["t_deadlineLabel", "deadline"],
      ["t_addBtn", "add"],
      ["t_f_all", "all"],
      ["t_f_planned", "planned"],
      ["t_f_applied", "applied"],
      ["t_f_interview", "interview"],
      ["t_f_rejected", "rejected"],
      ["t_f_offer", "offer"],
      ["t_emptyLogin", "emptyLogin"],
      ["t_loginHint", "loginHint"],
    ];
    map.forEach(([id, key]) => {
      const el = $(id);
      if (el) el.textContent = t(key);
    });

    // Header buttons
    loginBtn.textContent = t("login");
    logoutBtn.textContent = t("logout");

    // Modal
    authEmail.placeholder = t("email");
    authPassword.placeholder = t("password");
    if (mode === "login") {
      authTitle.textContent = t("login");
      authSubmitBtn.textContent = t("login");
      toggleAuthMode.textContent = t("createUser");
    } else {
      authTitle.textContent = t("registerTitle");
      authSubmitBtn.textContent = t("createUser");
      toggleAuthMode.textContent = t("login");
    }

    // Whoami
    if (!authedEmail) {
      whoami.textContent = t("notLoggedIn");
    }
  }

  // -----------------------------
  // Modal open/close
  // -----------------------------
  function openAuthModal() {
    authMsg.textContent = "";
    authMsg.classList.remove("ok");
    authModal.classList.remove("hidden");
    document.body.classList.add("modalOpen");
    setTimeout(() => authEmail.focus(), 50);
  }

  function closeAuthModal() {
    authModal.classList.add("hidden");
    document.body.classList.remove("modalOpen");
    authMsg.textContent = "";
    authMsg.classList.remove("ok");
  }

  // -----------------------------
  // API helper (IMPORTANT for cookies)
  // -----------------------------
  async function apiFetch(path, options = {}) {
    const opts = {
      ...options,
      headers: {
        "Content-Type": "application/json",
        ...(options.headers || {}),
      },
      credentials: "include", // 👈 send/receive SESSION cookie
    };

    const res = await fetch(path, opts);

    // Try parse JSON if any
    const contentType = res.headers.get("content-type") || "";
    let data = null;
    if (contentType.includes("application/json")) {
      try {
        data = await res.json();
      } catch (_) {
        data = null;
      }
    } else {
      // read text to show in errors
      try {
        data = await res.text();
      } catch (_) {
        data = null;
      }
    }

    if (!res.ok) {
      const err = new Error(typeof data === "string" ? data : "Request failed");
      err.status = res.status;
      err.data = data;
      throw err;
    }

    return data;
  }

  // -----------------------------
  // Auth flow (B1)
  // -----------------------------
  async function checkMe() {
    try {
      const me = await apiFetch(API.me, { method: "GET" });
      authedEmail = me?.email || null;
      setAuthedUI(true);
      await loadApps(); // fetch list on login
    } catch (e) {
      authedEmail = null;
      setAuthedUI(false);
      // not logged in is normal: /me returns 401
    }
  }

  function setAuthedUI(isAuthed) {
    if (isAuthed && authedEmail) {
      hide(loginBtn);
      show(whoami);
      show(logoutBtn);
      whoami.textContent = authedEmail;
      hide(emptyLogin);
      hide(loginHint);
    } else {
      show(loginBtn);
      hide(whoami);
      hide(logoutBtn);
      whoami.textContent = t("notLoggedIn");
      show(emptyLogin);
      show(loginHint);
      // clear list
      appsCache = [];
      renderList([]);
      statsEl.textContent = "—";
    }
  }

  async function doLogin(email, password) {
    // Backend returns {email: "..."} and sets cookie
    await apiFetch(API.login, {
      method: "POST",
      body: JSON.stringify({ email, password }),
    });

    // Confirm cookie works by calling /me
    const me = await apiFetch(API.me, { method: "GET" });
    authedEmail = me.email;
    setAuthedUI(true);
    closeAuthModal();
    await loadApps();
  }

  async function doRegister(email, password) {
    await apiFetch(API.register, {
      method: "POST",
      body: JSON.stringify({ email, password }),
    });
  }

  async function doLogout() {
    try {
      await apiFetch(API.logout, { method: "POST" });
    } catch (_) {
      // ignore
    }
    authedEmail = null;
    setAuthedUI(false);
  }

  // -----------------------------
  // Applications
  // Assumptions about your backend API:
  // - GET    /api/apps             -> array
  // - POST   /api/apps             -> create (body: {company, role, link, deadline})
  // - PUT    /api/apps/{id}/status  -> update status (body: {status})
  // - DELETE /api/apps/{id}         -> delete
  // If your endpoints differ, tell me, and I’ll adjust.
  // -----------------------------
  async function loadApps() {
    if (!authedEmail) return;
    const list = await apiFetch(API.apps, { method: "GET" });
    appsCache = Array.isArray(list) ? list : [];
    render();
  }

  async function createApp(payload) {
    await apiFetch(API.apps, {
      method: "POST",
      body: JSON.stringify(payload),
    });
    await loadApps();
  }

  async function updateStatus(id, status) {
    await apiFetch(`${API.apps}/${id}/status`, {
      method: "PUT",
      body: JSON.stringify({ status }),
    });
    await loadApps();
  }

  async function deleteApp(id) {
    await apiFetch(`${API.apps}/${id}`, { method: "DELETE" });
    await loadApps();
  }

  // -----------------------------
  // Rendering
  // -----------------------------
  function normStatus(s) {
    return String(s || "").toUpperCase();
  }

  function filterApps(apps, filter) {
    if (filter === "ALL") return apps;
    return apps.filter((a) => normStatus(a.status) === filter);
  }

  function computeStats(apps) {
    const counts = { PLANLAGT: 0, SOKT: 0, INTERVJU: 0, TILBUD: 0, AVSLATT: 0 };
    apps.forEach((a) => {
      const st = normStatus(a.status);
      if (counts[st] != null) counts[st]++;
    });
    return counts;
  }

  function setStats(counts) {
    // Keep your existing style like: "Planlagt: 2 • Søkt: 1 • ..."
    const lang = getLang();
    const label = (k) => {
      const mapNo = { PLANLAGT: "Planlagt", SOKT: "Søkt", INTERVJU: "Intervju", TILBUD: "Tilbud", AVSLATT: "Avslått" };
      const mapEn = { PLANLAGT: "Planned", SOKT: "Applied", INTERVJU: "Interview", TILBUD: "Offer", AVSLATT: "Rejected" };
      return (lang === "no" ? mapNo : mapEn)[k];
    };
    statsEl.textContent =
      `${label("PLANLAGT")}: ${counts.PLANLAGT} • ` +
      `${label("SOKT")}: ${counts.SOKT} • ` +
      `${label("INTERVJU")}: ${counts.INTERVJU} • ` +
      `${label("TILBUD")}: ${counts.TILBUD} • ` +
      `${label("AVSLATT")}: ${counts.AVSLATT}`;
  }

  function isOverdue(deadline) {
    if (!deadline) return false;
    const d = new Date(deadline);
    if (Number.isNaN(d.getTime())) return false;
    const now = new Date();
    // overdue if before today (ignore time)
    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    const dd = new Date(d.getFullYear(), d.getMonth(), d.getDate());
    return dd < today;
  }

  function render() {
    const filtered = filterApps(appsCache, currentFilter);
    renderList(filtered);
    setStats(computeStats(appsCache));
  }

  function renderList(items) {
    // If not logged in, keep the "emptyLogin" message and don't show items
    if (!authedEmail) {
      listEl.innerHTML = `<div class="muted" id="t_emptyLogin">${t("emptyLogin")}</div>`;
      return;
    }

    if (!items.length) {
      listEl.innerHTML = `<div class="muted">${getLang() === "no" ? "Ingen treff." : "No results."}</div>`;
      return;
    }

    listEl.innerHTML = "";
    items.forEach((app) => {
      const el = document.createElement("div");
      el.className = "item" + (isOverdue(app.deadline) ? " overdue" : "");

      const title = `${app.company ?? ""} — ${app.role ?? ""}`.trim();

      const deadline = app.deadline ? String(app.deadline) : "";
      const status = normStatus(app.status) || "PLANLAGT";
      const link = app.link ? String(app.link) : "";

      el.innerHTML = `
        <div class="top">
          <div>
            <div class="title">${escapeHtml(title)}</div>
            <div class="meta">
              ${deadline ? `<span>${getLang() === "no" ? "Frist" : "Deadline"}: <span class="badge">${escapeHtml(deadline)}</span></span>` : ""}
              <span>${getLang() === "no" ? "Status" : "Status"}: <span class="badge">${escapeHtml(statusLabel(status))}</span></span>
              ${link ? `<a href="${escapeAttr(link)}" target="_blank" rel="noopener">Link</a>` : ""}
            </div>
          </div>

          <div class="actions">
            <select data-id="${app.id}" class="statusSelect" aria-label="Status">
              ${statusOption("PLANLAGT", status)}
              ${statusOption("SOKT", status)}
              ${statusOption("INTERVJU", status)}
              ${statusOption("AVSLATT", status)}
              ${statusOption("TILBUD", status)}
            </select>
            <button class="btn ghost deleteBtn" data-id="${app.id}" type="button">
              ${getLang() === "no" ? "Slett" : "Delete"}
            </button>
          </div>
        </div>
      `;

      listEl.appendChild(el);
    });

    // Bind events after render
    listEl.querySelectorAll(".statusSelect").forEach((sel) => {
      sel.addEventListener("change", async (e) => {
        const id = e.target.getAttribute("data-id");
        const next = e.target.value;
        try {
          await updateStatus(id, next);
        } catch (err) {
          toastError(t("updateFailed"), err);
        }
      });
    });

    listEl.querySelectorAll(".deleteBtn").forEach((btn) => {
      btn.addEventListener("click", async (e) => {
        const id = e.target.getAttribute("data-id");
        try {
          await deleteApp(id);
        } catch (err) {
          toastError(t("deleteFailed"), err);
        }
      });
    });
  }

  function statusLabel(s) {
    const lang = getLang();
    const no = { PLANLAGT: "Planlagt", SOKT: "Søkt", INTERVJU: "Intervju", AVSLATT: "Avslått", TILBUD: "Tilbud" };
    const en = { PLANLAGT: "Planned", SOKT: "Applied", INTERVJU: "Interview", AVSLATT: "Rejected", TILBUD: "Offer" };
    return (lang === "no" ? no : en)[s] || s;
  }

  function statusOption(value, current) {
    const sel = value === current ? "selected" : "";
    return `<option value="${value}" ${sel}>${escapeHtml(statusLabel(value))}</option>`;
  }

  function escapeHtml(str) {
    return String(str)
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll('"', "&quot;")
      .replaceAll("'", "&#039;");
  }

  function escapeAttr(str) {
    // minimal for href
    return String(str).replaceAll('"', "%22").replaceAll("'", "%27");
  }

  function toastOk(msg) {
    authMsg.textContent = msg;
    authMsg.classList.add("ok");
    setTimeout(() => {
      authMsg.textContent = "";
      authMsg.classList.remove("ok");
    }, 2500);
  }

  function toastError(prefix, err) {
    const detail = err?.data
      ? typeof err.data === "string"
        ? err.data
        : JSON.stringify(err.data)
      : err?.message || "";
    formMsg.textContent = `${prefix}${detail ? " (" + detail + ")" : ""}`;
    setTimeout(() => (formMsg.textContent = ""), 3000);
  }

  // -----------------------------
  // Events
  // -----------------------------
  loginBtn.addEventListener("click", () => {
    mode = "login";
    applyLang();
    openAuthModal();
  });

  closeAuth.addEventListener("click", closeAuthModal);

  authModal.addEventListener("click", (e) => {
    if (e.target === authModal) closeAuthModal();
  });

  document.addEventListener("keydown", (e) => {
    if (e.key === "Escape" && !authModal.classList.contains("hidden")) closeAuthModal();
  });

  toggleAuthMode.addEventListener("click", () => {
    mode = mode === "login" ? "register" : "login";
    authMsg.textContent = "";
    authMsg.classList.remove("ok");
    applyLang();
  });

  authForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    authMsg.textContent = "";
    authMsg.classList.remove("ok");

    const email = authEmail.value.trim();
    const password = authPassword.value;

    try {
      if (mode === "login") {
        await doLogin(email, password);
      } else {
        await doRegister(email, password);
        authMsg.textContent = t("registerOk");
        authMsg.classList.add("ok");
        mode = "login";
        applyLang();
      }
    } catch (err) {
      const msg =
        (mode === "login" ? t("loginFailed") : t("registerFailed")) +
        ` (${err?.status ?? "?"})` +
        (err?.data ? `: ${typeof err.data === "string" ? err.data : JSON.stringify(err.data)}` : "");
      authMsg.textContent = msg;
      authMsg.classList.remove("ok");
    }
  });

  logoutBtn.addEventListener("click", async () => {
    await doLogout();
  });

  langBtn.addEventListener("click", () => {
    const next = getLang() === "no" ? "en" : "no";
    setLang(next);
    render();
  });

  filterBtns.forEach((btn) => {
    btn.addEventListener("click", () => {
      filterBtns.forEach((b) => b.classList.remove("active"));
      btn.classList.add("active");
      currentFilter = btn.getAttribute("data-filter") || "ALL";
      render();
    });
  });

  createForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    formMsg.textContent = "";

    if (!authedEmail) {
      openAuthModal();
      return;
    }

    const fd = new FormData(createForm);
    const payload = {
      company: (fd.get("company") || "").toString().trim(),
      role: (fd.get("role") || "").toString().trim(),
      link: (fd.get("link") || "").toString().trim(),
      deadline: (fd.get("deadline") || "").toString().trim() || null,
    };

    try {
      await createApp(payload);
      createForm.reset();
    } catch (err) {
      formMsg.textContent =
        `${t("addFailed")} (${err?.status ?? "?"})` +
        (err?.data ? `: ${typeof err.data === "string" ? err.data : JSON.stringify(err.data)}` : "");
    }
  });

  // -----------------------------
  // Init
  // -----------------------------
  applyLang();
  whoami.textContent = t("notLoggedIn");
  checkMe(); // checks cookie-based session and loads apps if logged in
})();
