// Same Spring Boot application se frontend serve ho raha hai,
// isliye localhost hardcode karne ki zarurat nahi.
const API_BASE = "/alchemy/api";


// DOM Elements
const dropZone = document.getElementById("dropZone");
const fileInput = document.getElementById("fileInput");
const uploadStatus = document.getElementById("uploadStatus");
const fileList = document.getElementById("fileList");
const docCount = document.getElementById("docCount");
const chatBox = document.getElementById("chatBox");
const emptyState = document.getElementById("emptyState");
const questionInput = document.getElementById("questionInput");

let typingIndicator = null;

/*
 * Agar JWT cookie expire ya invalid ho jaaye,
 * backend 401 ya 403 return karega.
 *
 * Tab user ko login page par bhej denge.
 */
function handleUnauthorized(response) {
    if (response.status === 401 || response.status === 403) {
        window.location.href = "/alchemy/login.html";
        return true;
    }

    return false;
}

// Event Listeners
document.addEventListener("DOMContentLoaded", () => {

    // Existing uploaded files load karna
    loadFiles();

    // Drag enter/over par upload area highlight karna
    ["dragenter", "dragover"].forEach(eventName => {
        dropZone.addEventListener(eventName, (e) => {
            e.preventDefault();
            dropZone.classList.add("dragover");
        }, false);
    });

    // Drag leave/drop par highlight remove karna
    ["dragleave", "drop"].forEach(eventName => {
        dropZone.addEventListener(eventName, (e) => {
            e.preventDefault();
            dropZone.classList.remove("dragover");
        }, false);
    });

    // File drop hone par upload start
    dropZone.addEventListener("drop", (e) => {
        const files = e.dataTransfer.files;

        if (files.length > 0) {
            handleFilesSelect(files);
        }
    });

    // Browse se file select hone par upload start
    fileInput.addEventListener("change", (e) => {
        if (e.target.files.length > 0) {
            handleFilesSelect(e.target.files);
        }
    });

    // Enter press karne par question send
    questionInput.addEventListener("keypress", (e) => {
        if (e.key === "Enter" && !e.shiftKey) {
            e.preventDefault();
            askQuestion();
        }
    });
});

// Upload status message set karne ka helper
function setStatus(text, type = "info") {
    uploadStatus.innerText = text;
    uploadStatus.className = "status-message " + type;

    // Success/error message 5 seconds ke baad clear karna
    if (type !== "info") {
        setTimeout(() => {
            if (uploadStatus.innerText === text) {
                uploadStatus.innerText = "";
                uploadStatus.className = "status-message";
            }
        }, 5000);
    }
}

// Selected files ko upload function me bhejna
function handleFilesSelect(files) {
    uploadFiles(files);
}

// Files backend ko upload karna
async function uploadFiles(files) {
    const formData = new FormData();

    // Multiple files FormData me add karna
    for (let i = 0; i < files.length; i++) {
        formData.append("files", files[i]);
    }

    try {
        setStatus("Uploading and processing...", "info");

        const res = await fetch(`${API_BASE}/files/upload`, {
            method: "POST",

            /*
             * HttpOnly JWT cookie browser automatically bhejega.
             * Authorization header manually add nahi karna.
             */
            credentials: "include",

            /*
             * FormData ke saath Content-Type manually mat dena.
             * Browser multipart boundary khud set karta hai.
             */
            body: formData
        });

        if (handleUnauthorized(res)) {
            return;
        }

        if (!res.ok) {
            const errorText = await res.text();
            throw new Error(errorText || "Upload failed");
        }

        const data = await res.text();

        setStatus(data || "Upload successful ✅", "success");

        // Upload ke baad file list refresh
        loadFiles();

    } catch (err) {
        console.error(err);
        setStatus("Upload failed: " + err.message + " ❌", "error");
    }
}

// Uploaded files list backend se load karna
async function loadFiles() {
    try {
        const res = await fetch(`${API_BASE}/files`, {
            method: "GET",

            // Browser HttpOnly JWT cookie automatically send karega
            credentials: "include"
        });

        if (handleUnauthorized(res)) {
            return;
        }

        if (!res.ok) {
            throw new Error("Failed to load documents");
        }

        const files = await res.json();

        renderFileList(files);

    } catch (err) {
        console.error("Error loading files:", err);
    }
}

// Sidebar me uploaded file list render karna
const currentRole = localStorage.getItem("alchemyRole");
function renderFileList(files) {
    fileList.innerHTML = "";
    docCount.innerText = files.length;

    if (files.length === 0) {
        fileList.innerHTML = `
            <li style="padding:10px; color:#64748b; font-size:13px; text-align:center; font-style:italic;">
                No documents uploaded yet
            </li>
        `;
        return;
    }

    files.forEach(file => {

        const li = document.createElement("li");
        li.className = "file-item";

        // Sirf ADMIN ko delete button dikhega
        const deleteButton = currentRole === "ADMIN"
            ? `
            <button
                class="delete-btn"
                data-id="${file.documentId}"
                data-name="${file.fileName}"
                onclick="deleteFile(this.dataset.id, this.dataset.name)"
                title="Delete document">

                <svg width="14"
                     height="14"
                     viewBox="0 0 24 24"
                     fill="none"
                     stroke="currentColor"
                     stroke-width="2"
                     stroke-linecap="round"
                     stroke-linejoin="round">

                    <polyline points="3 6 5 6 21 6"/>
                    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
                    <line x1="10" y1="11" x2="10" y2="17"/>
                    <line x1="14" y1="11" x2="14" y2="17"/>
                </svg>

            </button>
        `
            : "";

        li.innerHTML = `
        <label class="file-info" title="${file.fileName}">
            <input
                type="checkbox"
                class="file-checkbox"
                value="${file.documentId}"
                checked
            >

            <span class="file-icon">📄</span>
            <span class="file-name">${file.fileName}</span>
        </label>

        ${deleteButton}
    `;

        fileList.appendChild(li);
    });
}

// File delete karna
async function deleteFile(documentId, fileName) {

    // Frontend safety check.
    // Real security backend SecurityConfig enforce karega.
    if (currentRole !== "ADMIN") {
        setStatus("Only admin can delete documents.", "error");
        return;
    }

    const confirmed = confirm(
        `Are you sure you want to delete "${fileName}"? This will remove all associated vector data.`
    );

    if (!confirmed) {
        return;
    }

    try {
        setStatus(`Deleting ${fileName}...`, "info");

        const res = await fetch(`${API_BASE}/files/${documentId}`, {
            method: "DELETE",

            // Browser HttpOnly JWT cookie automatically send karega
            credentials: "include"
        });

        if (handleUnauthorized(res)) {
            return;
        }

        if (!res.ok) {
            const errorText = await res.text();
            throw new Error(errorText || "Delete failed");
        }

        setStatus("Deleted successfully ✅", "success");
        loadFiles();

    } catch (err) {
        console.error(err);
        setStatus("Delete failed: " + err.message + " ❌", "error");
    }
}

// Chat box me message add karna
function addMessage(text, type) {
    if (emptyState) {
        emptyState.style.display = "none";
    }

    const div = document.createElement("div");
    div.classList.add("msg", type);
    div.innerText = text;

    chatBox.appendChild(div);
    chatBox.scrollTop = chatBox.scrollHeight;
}

// Bot typing animation show karna
function showTypingIndicator() {
    if (emptyState) {
        emptyState.style.display = "none";
    }

    typingIndicator = document.createElement("div");
    typingIndicator.className = "typing-indicator";

    typingIndicator.innerHTML = `
        <div class="typing-dot"></div>
        <div class="typing-dot"></div>
        <div class="typing-dot"></div>
    `;

    chatBox.appendChild(typingIndicator);
    chatBox.scrollTop = chatBox.scrollHeight;
}

// Bot typing animation remove karna
function removeTypingIndicator() {
    if (typingIndicator && typingIndicator.parentNode) {
        typingIndicator.parentNode.removeChild(typingIndicator);
    }

    typingIndicator = null;
}

// Chatbot se question ask karna
async function askQuestion() {
    const question = questionInput.value.trim();

    if (!question) {
        return;
    }

    // Selected document IDs collect karna
    const selectedDocumentIds = [];

    document.querySelectorAll(".file-checkbox:checked")
        .forEach(cb => {
            selectedDocumentIds.push(cb.value);
        });

    // User message UI me add karna
    addMessage(question, "user");
    questionInput.value = "";

    showTypingIndicator();

    try {
        /*
         * QueryController:
         * @RequestMapping("/api/query")
         * @PostMapping("/ask")
         *
         * Final endpoint = /api/query/ask
         */
        const res = await fetch(`${API_BASE}/query/ask`, {
            method: "POST",

            // Browser HttpOnly JWT cookie automatically send karega
            credentials: "include",

            headers: {
                "Content-Type": "application/json"
            },

            body: JSON.stringify({
                /*
                 * Abhi ek chat thread hai, isliye fixed sessionId use kar rahe hain.
                 * ChatService isko logged-in userId ke saath combine karega.
                 */
                sessionId: "default",
                question: question,
                documentIds: selectedDocumentIds
            })
        });

        if (handleUnauthorized(res)) {
            removeTypingIndicator();
            return;
        }

        if (!res.ok) {
            const errorText = await res.text();
            throw new Error(errorText || "Query failed");
        }

        const data = await res.text();

        removeTypingIndicator();
        addMessage(data, "bot");

    } catch (err) {
        console.error(err);

        removeTypingIndicator();

        addMessage(
            "Error connecting to AI backend. Make sure the server, Qdrant, Redis, and Ollama are running. ❌",
            "bot"
        );
    }
}