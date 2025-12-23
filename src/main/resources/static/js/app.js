document.addEventListener("DOMContentLoaded", function () {
  if (
    typeof tasksData !== "undefined" &&
    tasksData &&
    document.getElementById("taskCalendar")
  ) {
    const calendarEl = document.getElementById("taskCalendar");

    function getPriorityColor(priority) {
      const p = (
        typeof priority === "string" ? priority : priority?.label || ""
      ).toUpperCase();
      switch (p) {
        case "HIGH":
          return "#E53935";
        case "MEDIUM":
          return "#FF9800";
        case "LOW":
          return "#4CAF50";
        default:
          return "#5A77F5";
      }
    }

    const events = tasksData
      .map((task) => {
        const dueDate = task.dueDate;
        if (!dueDate) return null;

        let dateString =
          typeof dueDate === "string"
            ? dueDate.split("T")[0]
            : `${dueDate.year}-${String(dueDate.monthValue).padStart(
                2,
                "0"
              )}-${String(dueDate.dayOfMonth).padStart(2, "0")}`;

        const pLabel = task.priority?.label || task.priority || "Low";
        const priorityClass = pLabel.toLowerCase().trim();

        return {
          title: task.title,
          start: dateString,
          className: `cal-event-pill cal-priority-${priorityClass}`,
          extendedProps: { taskId: task.id },
        };
      })
      .filter(Boolean);

    const calendar = new FullCalendar.Calendar(calendarEl, {
      initialView: "dayGridMonth",
      height: "auto",
      headerToolbar: {
        left: "prev,next today",
        center: "title",
        right: "dayGridMonth,timeGridWeek,timeGridDay",
      },
      events: events,
      editable: false,
      eventDisplay: "block",
      eventClick: function (info) {
        const taskId = info.event.extendedProps.taskId;
        const task = tasksData.find((t) => t.id == taskId);
        if (!task) return;

        openTaskModal({
          id: task.id,
          title: task.title,
          desc: task.description,
          duedate: task.dueDate,
          created: task.createdDate || task.createdAt,
          status: task.status,
          priority: task.priority,
        });
      },
    });

    calendar.render();
  }
});

function parseDate(dateVal) {
  if (!dateVal) return "—";
  if (typeof dateVal === "string") return dateVal.split("T")[0];
  if (dateVal.year && dateVal.monthValue && dateVal.dayOfMonth) {
    return `${dateVal.year}-${String(dateVal.monthValue).padStart(
      2,
      "0"
    )}-${String(dateVal.dayOfMonth).padStart(2, "0")}`;
  }
  return "—";
}

function openTaskModal(el) {
  const modal = document.getElementById("taskModal");
  const card = modal.querySelector(".task-modal-card");
  modal.style.display = "flex";

  const data = el.dataset
    ? {
        id: el.dataset.id,
        title: el.dataset.title,
        desc: el.dataset.desc,
        duedate: el.dataset.duedate,
        created: el.dataset.created,
        status: el.dataset.status,
        priority: el.dataset.priority,
      }
    : el;

  let priorityVal = data.priority?.label || data.priority || "Low";
  const priority =
    priorityVal.charAt(0).toUpperCase() + priorityVal.slice(1).toLowerCase();

  let statusVal = data.status?.label || data.status || "Pending";
  const statusRaw = statusVal.toString().trim();

  card.classList.remove("priority-High", "priority-Medium", "priority-Low");
  card.classList.add("priority-" + priority);

  document.getElementById("m-id").innerText = "#" + (data.id || "—");
  document.getElementById("m-title").innerText = data.title || "Untitled Task";
  document.getElementById("m-desc").innerText =
    data.desc || "No additional details provided.";
  document.getElementById("m-due").innerText = parseDate(data.duedate);
  document.getElementById("m-created").innerText = parseDate(data.created);

  const statusEl = document.getElementById("m-status");
  const statusClass = statusRaw.toLowerCase().replace(/ /g, "_");
  statusEl.className = "status-badge status-" + statusClass;
  statusEl.innerText = statusRaw;

  const priorityEl = document.getElementById("m-priority");
  priorityEl.className = "priority-badge priority-" + priority;
  priorityEl.innerText = priority + " Priority";
}

function closeTaskModal() {
  document.getElementById("taskModal").style.display = "none";
}

document.addEventListener("DOMContentLoaded", function () {
  const modal = document.getElementById("taskModal");
  if (modal) {
    modal.addEventListener("click", function (e) {
      if (e.target === modal) {
        window.closeTaskModal();
      }
    });
  }
});
