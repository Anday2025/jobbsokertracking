// --- JWT helpers ---
const TOKEN_KEY = "jwt_token";

function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token);
}

function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

function clearToken() {
  localStorage.removeItem(TOKEN_KEY);
}

function authHeaders() {
  const token = getToken();
  return token ? { "Authorization": `Bearer ${token}` } : {};
}

// Wrapper rundt fetch som alltid legger på Authorization-header og håndterer 401
async function apiFetch(url, options = {}) {
  const headers = {
    ...(options.headers || {}),
    ...authHeaders()
  };

  const res = await fetch(url, { ...options, headers });

  if (res.status === 401) {
    clearToken();
    alert("Sesjonen din er utløpt eller du er ikke logget inn. Logg inn på nytt.");
    location.reload();
  }

  return res;
}

// Midlertidig login/register uten UI (kun for testing)
async function ensureLoggedIn() {
  if (getToken()) return;

  const email = prompt("Email:");
  const password = prompt("Passord (min 6 tegn):");
  if (!email || !password) return;

  // Prøv login først
  let res = await fetch("/api/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password })
  });

  // Hvis ikke ok, prøv register
  if (!res.ok) {
    res = await fetch("/api/auth/register", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email, password })
    });
  }

  if (!res.ok) {
    const msg = await res.text();
    alert(msg || "Kunne ikke logge inn / registrere.");
    return;
  }

  const data = await res.json(); // { token: "..." }
  setToken(data.token);
}

// --- din app-kode ---
let currentFilter = "ALL";

const listEl = document.getElementById("list");
const statsEl = document.getElementById("stats");
const form = document.getElementById("createForm");
const formMsg = document.getElementById("formMsg");

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
  if (!res.ok) {
    const text = await res.text();
    alert(text || "Kunne ikke laste søknader");
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

// Start: sørg for innlogging først, så last data
ensureLoggedIn().then(load);
