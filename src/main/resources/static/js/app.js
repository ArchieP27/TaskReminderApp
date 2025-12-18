document.addEventListener('DOMContentLoaded', function () {

    const tableView = document.getElementById('tableView');
    const cardView = document.getElementById('cardView');
    const calendarView = document.getElementById('calendarView');
    const calendarEl = document.getElementById('taskCalendar');

    const tableViewBtn = document.getElementById('tableViewBtn');
    const cardViewBtn = document.getElementById('cardViewBtn');
    const calendarViewBtn = document.getElementById('calendarViewBtn');

    const views = [tableView, cardView, calendarView];
    const buttons = [tableViewBtn, cardViewBtn, calendarViewBtn];

    let calendarInstance = null;

    function resetViews() {
        views.forEach(v => v.style.display = 'none');
        buttons.forEach(b => b.classList.remove('active'));
    }

    function setActiveView(view, button) {
        resetViews();
        if (view) {
            view.style.display = view === cardView ? 'grid' : 'block';
        }
        if (button) {
            button.classList.add('active');
        }
    }

    function getPriorityColor(priority) {
        switch (priority) {
            case 'HIGH': return '#E53935';
            case 'MEDIUM': return '#FF9800';
            case 'LOW': return '#4CAF50';
            default: return '#5A77F5';
        }
    }

    function initializeCalendar() {
        if (calendarInstance) {
            setActiveView(calendarView, calendarViewBtn);
            setTimeout(() => calendarInstance.updateSize(), 1);
            return;
        }

        const events = tasksData
            .map(task => {
                let dateString = null;
                const dueDate = task.dueDate || task.due_date;

                if (!dueDate) return null;

                if (
                    typeof dueDate === 'object' &&
                    dueDate.year &&
                    dueDate.monthValue &&
                    dueDate.dayOfMonth
                ) {
                    const y = dueDate.year;
                    const m = String(dueDate.monthValue).padStart(2, '0');
                    const d = String(dueDate.dayOfMonth).padStart(2, '0');
                    dateString = `${y}-${m}-${d}`;
                }

                else if (Array.isArray(dueDate)) {
                    const [y, m, d] = dueDate;
                    dateString = `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
                }

                else if (typeof dueDate === 'string' && dueDate.trim() !== '') {
                    dateString = dueDate.split('T')[0];
                }

                if (!dateString) return null;

                return {
                    title: task.title,
                    start: dateString,
                    color: getPriorityColor(task.priority),
                    taskId: task.id
                };
            })
            .filter(e => e !== null);

        calendarInstance = new FullCalendar.Calendar(calendarEl, {
            initialView: 'dayGridMonth',
            height: 'auto',
            headerToolbar: {
                left: 'prev,next today',
                center: 'title',
                right: 'dayGridMonth,timeGridWeek,timeGridDay'
            },
            events: events,
            editable: false,
            eventDisplay: 'block',
            eventClick: function (info) {
                alert(
                    `Task: ${info.event.title}\nDue: ${info.event.start.toLocaleDateString()}`
                );
            }
        });

        calendarInstance.render();
        setActiveView(calendarView, calendarViewBtn);
    }

    tableViewBtn.addEventListener('click', () => {
        setActiveView(tableView, tableViewBtn);
    });

    cardViewBtn.addEventListener('click', () => {
        setActiveView(cardView, cardViewBtn);
    });

    calendarViewBtn.addEventListener('click', () => {
        initializeCalendar();
    });

    setActiveView(tableView, tableViewBtn);
});

function openTaskModal(el) {

    document.getElementById("m-id").innerText = el.dataset.id;
    document.getElementById("m-title").innerText = el.dataset.title;
    document.getElementById("m-desc").innerText = el.dataset.desc || "—";
    document.getElementById("m-due").innerText = el.dataset.duedate;
    document.getElementById("m-status").innerText = el.dataset.status;
    document.getElementById("m-priority").innerText = el.dataset.priority;
    document.getElementById("m-created").innerText = el.dataset.created;
    document.getElementById("m-completed").innerText =
        el.dataset.completed ? el.dataset.completed : "Not completed";

    document.getElementById("taskModal").style.display = "flex";
}

function closeTaskModal() {
    document.getElementById("taskModal").style.display = "none";
}

document.addEventListener("DOMContentLoaded", function () {
    const modal = document.getElementById("taskModal");
    if (modal) {
        modal.addEventListener("click", function (e) {
            if (e.target === modal) {
                closeTaskModal();
            }
        });
    }
});