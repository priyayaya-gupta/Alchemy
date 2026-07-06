const API_BASE = "/api";

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

// JWT token localStorage se nikaalte hain.
// Agar token nahi hai, user ko login page pe bhej dete hain.
function getAuthHeaders() {
    const token = localStorage.getItem("alchemyToken");

    if (!token) {
        window.location.href = "/login.html";
        return {};
    }

    return {
        "Authorization": "Bearer " + token
    };
}

// Event Listeners
document.addEventListener("DOMContentLoaded", () => {

    // Existing uploaded files load karna
    loadFiles();

    // Drag enter/over par upload area highlight hoga
    ["dragenter", "dragover"].forEach(eventName => {
        dropZone.addEventListener(eventName, (e) => {
            e.preventDefault();
            dropZone.classList.add("dragover");
        }, false);
    });

    // Drag leave/drop par highlight remove hoga
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

// Upload status message set karne ke liye helper
function setStatus(text, type = "info") {
    uploadStatus.innerText = text;
    uploadStatus.className = "status-message " + type;

    // Success/error messages 5 sec ke baad clear ho jayenge
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

    // Multiple files ko FormData me add karna
    for (let i = 0; i < files.length; i++) {
        formData.append("files", files[i]);
    }

    try {
        setStatus("Uploading and processing...", "info");

        const res = await fetch(`${API_BASE}/files/upload`, {
            method: "POST",

            // FormData ke saath Content-Type manually mat dena.
            // Browser khud boundary set karta hai.
            headers: getAuthHeaders(),

            body: formData
        });

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
            headers: getAuthHeaders()
        });

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

            <button
                class="delete-btn"
                onclick="deleteFile('${file.documentId}','${file.fileName}')"
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
        `;

        fileList.appendChild(li);
    });
}

// File delete karna
async function deleteFile(documentId, fileName) {
    if (!confirm(`Are you sure you want to delete "${fileName}"? This will remove all associated vector data.`)) {
        return;
    }

    try {
        setStatus(`Deleting ${fileName}...`, "info");

        const res = await fetch(`${API_BASE}/files/${documentId}`, {
            method: "DELETE",
            headers: getAuthHeaders()
        });

        if (!res.ok) {
            throw new Error("Delete failed");
        }

        setStatus("Deleted successfully ✅", "success");

        // Delete ke baad file list refresh
        loadFiles();

    } catch (err) {
        console.error(err);
        setStatus("Delete failed ❌", "error");
    }
}

// Chat box me message add karna
function addMessage(text, type) {
    // First message par empty state hide
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

    // User ka message UI me add
    addMessage(question, "user");

    questionInput.value = "";

    // Bot typing indicator show
    showTypingIndicator();

    try {
        const res = await fetch(`${API_BASE}/query`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",

                // JWT token backend ko bhejna.
                // Backend token se userId extract karega.
                ...getAuthHeaders()
            },
            body: JSON.stringify({
                question: question,
                documentIds: selectedDocumentIds
            })
        });

        if (!res.ok) {
            throw new Error("Query failed");
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