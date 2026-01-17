function updateClock() {
  const now = new Date();
  document.getElementById("liveClock").textContent = now.toLocaleTimeString();
}
updateClock();
setInterval(updateClock, 1000);

async function loadStats() {
  const res = await fetch("/api/dashboard/stats");
  const stats = await res.json();

  document.querySelectorAll(".stat-number").forEach((el) => {
    const key = el.dataset.key;
    let value = stats[key];

    const finalValue = Array.isArray(value) ? value.length : value || 0;
    animateCount(el, finalValue);
  });

  renderProgress(stats.completionRate || 0);
  loadRecentTasks(stats.recentTasks || []);
}

function animateCount(el, target) {
  let current = 0;
  const duration = 1000;
  const increment = target / (duration / 20);

  const interval = setInterval(() => {
    current += increment;
    if (current >= target) {
      el.textContent = target;
      clearInterval(interval);
    } else {
      el.textContent = Math.floor(current);
    }
  }, 20);
}

function renderProgress(rate) {
  const fill = document.querySelector(".tech-fill");
  const text = document.getElementById("completionRate");

  console.log("tech-fill:", fill);
  console.log("completionRate:", text);

  if (!fill || !text) {
    console.warn("Progress elements not found in DOM");
    return;
  }

  text.textContent = `${rate}%`;
  fill.style.width = `${rate}%`;

  if (rate < 30) {
    fill.style.background = "linear-gradient(90deg, #8015fa, #f87171)";
  } else if (rate < 70) {
    fill.style.background = "linear-gradient(90deg, #8015fa, #fde047)";
  } else {
    fill.style.background = "linear-gradient(90deg, #8015fa, #4ade80)";
  }
}

function loadRecentTasks(tasks) {
  const tbody = document.getElementById("recentTasksTable");
  if (!tbody) return;

  tbody.innerHTML = "";

  if (!tasks.length) {
    tbody.innerHTML = `<tr><td colspan="4" text-align="center">No tasks found.</td></tr>`;
    return;
  }

  tasks.forEach((t) => {
    const statusClass = t.status.toLowerCase().replace(/\s+/g, "");

    const row = document.createElement("tr");
    row.innerHTML = `
      <td>${t.title}</td>
      <td><span class="badge badge-${statusClass}">${t.status}</span></td>
      <td><span class="badge-priority badge-${t.priority.toLowerCase()}">${
      t.priority === "HIGH" ? "▲ " : t.priority === "LOW" ? "▼ " : "● "
    }${t.priority}</span></td>
      <td>${t.dueDate ? new Date(t.dueDate).toLocaleDateString() : "-"}</td>
    `;
    tbody.appendChild(row);
  });
}

document.querySelectorAll(".stat-card").forEach((card) => {
  card.onclick = () =>
    openModal(card.dataset.type, card.querySelector("h4").textContent);
});

async function openModal(type, title) {
  const modal = document.getElementById("modalOverlay");
  const list = document.getElementById("tasks-list");
  const titleEl = document.getElementById("modalTitle");

  titleEl.textContent = `Loading ${title}...`;
  list.innerHTML = "";
  modal.style.display = "flex";

  try {
    const res = await fetch(`/api/dashboard/tasks?type=${type}`);
    const tasks = await res.json();

    titleEl.textContent = title + " Tasks";

    list.innerHTML = tasks.length
      ? tasks
          .map(
            (t) => `
                <li>
                    <span style="font-weight:700;">${t.title}</span>
                    <small>${t.status}</small>
                </li>
            `
          )
          .join("")
      : "<li style='justify-content:center; color:var(--color-text-muted);'>No tasks found in this category.</li>";
  } catch (err) {
    titleEl.textContent = "Error";
    list.innerHTML = "<li>Could not load tasks.</li>";
  }
}

function closeModal() {
  document.getElementById("modalOverlay").style.display = "none";
}

document.addEventListener("DOMContentLoaded", () => {
  loadStats();
  document.querySelectorAll(".stat-card").forEach((card) => {
    card.onclick = () => {
      const label = card.querySelector(".stat-label").textContent;
      openModal(card.dataset.type, label);
    };
  });
});
