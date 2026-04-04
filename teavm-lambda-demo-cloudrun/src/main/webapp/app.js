(function () {
    "use strict";

    var healthBadge = document.getElementById("health-badge");
    var form = document.getElementById("add-user-form");
    var nameInput = document.getElementById("input-name");
    var emailInput = document.getElementById("input-email");
    var formError = document.getElementById("form-error");
    var usersTable = document.getElementById("users-table");
    var usersBody = document.getElementById("users-body");
    var usersLoading = document.getElementById("users-loading");
    var usersEmpty = document.getElementById("users-empty");
    var userCount = document.getElementById("user-count");

    // --- Health check ---

    function checkHealth() {
        fetch("/health")
            .then(function (r) { return r.json(); })
            .then(function (data) {
                healthBadge.textContent = data.status === "ok" ? "healthy" : "unhealthy";
                healthBadge.className = "badge " + (data.status === "ok" ? "badge-ok" : "badge-error");
            })
            .catch(function () {
                healthBadge.textContent = "unreachable";
                healthBadge.className = "badge badge-error";
            });
    }

    // --- Users CRUD ---

    function loadUsers() {
        fetch("/users")
            .then(function (r) { return r.json(); })
            .then(function (users) {
                renderUsers(users);
            })
            .catch(function (err) {
                usersLoading.hidden = true;
                usersEmpty.textContent = "Failed to load users: " + err.message;
                usersEmpty.hidden = false;
            });
    }

    function renderUsers(users) {
        usersLoading.hidden = true;
        usersBody.innerHTML = "";

        if (users.length === 0) {
            usersTable.hidden = true;
            usersEmpty.hidden = false;
            userCount.textContent = "";
            return;
        }

        usersEmpty.hidden = true;
        usersTable.hidden = false;
        userCount.textContent = "(" + users.length + ")";

        users.forEach(function (user) {
            var tr = document.createElement("tr");
            tr.innerHTML =
                "<td>" + esc(user.id) + "</td>" +
                "<td>" + esc(user.name) + "</td>" +
                "<td>" + esc(user.email) + "</td>" +
                "<td></td>";

            var btn = document.createElement("button");
            btn.className = "btn-delete";
            btn.textContent = "Delete";
            btn.onclick = function () { deleteUser(user.id); };
            tr.lastElementChild.appendChild(btn);

            usersBody.appendChild(tr);
        });
    }

    function createUser(name, email) {
        formError.hidden = true;
        form.querySelector("button").disabled = true;

        fetch("/users", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ name: name, email: email })
        })
            .then(function (r) {
                if (!r.ok) return r.json().then(function (e) { throw new Error(e.error || r.statusText); });
                return r.json();
            })
            .then(function () {
                nameInput.value = "";
                emailInput.value = "";
                loadUsers();
            })
            .catch(function (err) {
                formError.textContent = err.message;
                formError.hidden = false;
            })
            .finally(function () {
                form.querySelector("button").disabled = false;
            });
    }

    function deleteUser(id) {
        fetch("/users/" + id, { method: "DELETE" })
            .then(function () {
                loadUsers();
            })
            .catch(function (err) {
                alert("Delete failed: " + err.message);
            });
    }

    // --- Helpers ---

    function esc(str) {
        var d = document.createElement("div");
        d.textContent = String(str);
        return d.innerHTML;
    }

    // --- Init ---

    form.addEventListener("submit", function (e) {
        e.preventDefault();
        createUser(nameInput.value.trim(), emailInput.value.trim());
    });

    checkHealth();
    loadUsers();
})();
