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

// Event Listeners
document.addEventListener("DOMContentLoaded", () => {
    // Load existing files
    loadFiles();

    // Drag and Drop Event Listeners
    ["dragenter", "dragover"].forEach(eventName => {
        dropZone.addEventListener(eventName, (e) => {
            e.preventDefault();
            dropZone.classList.add("dragover");
        }, false);
    });

    ["dragleave", "drop"].forEach(eventName => {
        dropZone.addEventListener(eventName, (e) => {
            e.preventDefault();
            dropZone.classList.remove("dragover");
        }, false);
    });

    dropZone.addEventListener("drop", (e) => {
        const dt = e.dataTransfer;
        const files = dt.files;
        if (files.length > 0) {
            handleFilesSelect(files);
        }
    });

    fileInput.addEventListener("change", (e) => {
        if (e.target.files.length > 0) {
            handleFilesSelect(e.target.files);
        }
    });

    // Enter Key in Chat Input
    questionInput.addEventListener("keypress", (e) => {
        if (e.key === "Enter" && !e.shiftKey) {
            e.preventDefault();
            askQuestion();
        }
    });
});

// Set Upload Status Helper
function setStatus(text, type = "info") {
    uploadStatus.innerText = text;
    uploadStatus.className = "status-message " + type;
    
    // Clear status after 5 seconds if not uploading
    if (type !== "info") {
        setTimeout(() => {
            if (uploadStatus.innerText === text) {
                uploadStatus.innerText = "";
                uploadStatus.className = "status-message";
            }
        }, 5000);
    }
}

// Handle File Selection
function handleFilesSelect(files) {
    uploadFiles(files);
}

// Upload Files to Backend
async function uploadFiles(files) {
    const formData = new FormData();
    for (let i = 0; i < files.length; i++) {
        formData.append("files", files[i]);
    }

    try {
        setStatus("Uploading and processing...", "info");

        const res = await fetch(`${API_BASE}/files/upload`, {
            method: "POST",
            body: formData
        });

        if (!res.ok) {
            const errorText = await res.text();
            throw new Error(errorText || "Upload failed");
        }

        const data = await res.text();
        setStatus(data || "Upload successful ✅", "success");
        
        // Refresh the file list
        loadFiles();
    } catch (err) {
        console.error(err);
        setStatus("Upload failed: " + err.message + " ❌", "error");
    }
}

// Load Files List
async function loadFiles() {
    try {
        const res = await fetch(`${API_BASE}/files`);
        if (!res.ok) throw new Error("Failed to load documents");
        
        const files = await res.json();
        renderFileList(files);
    } catch (err) {
        console.error("Error loading files:", err);
    }
}

// Render File List in Sidebar
function renderFileList(files) {
    fileList.innerHTML = "";
    docCount.innerText = files.length;

    if (files.length === 0) {
        fileList.innerHTML = `<li style="padding: 10px; color: #64748b; font-size: 13px; text-align: center; font-style: italic;">No documents uploaded yet</li>`;
        return;
    }

    files.forEach(file => {
        const li = document.createElement("li");
        li.className = "file-item";
        li.innerHTML = `
            <div class="file-info" title="${file.fileName}">
                <span class="file-icon">📄</span>
                <span class="file-name">${file.fileName}</span>
            </div>
            <button class="delete-btn" onclick="deleteFile('${file.documentId}', '${file.fileName}')" title="Delete document">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/><line x1="10" y1="11" x2="10" y2="17"/><line x1="14" y1="11" x2="14" y2="17"/></svg>
            </button>
        `;
        fileList.appendChild(li);
    });
}

// Delete File
async function deleteFile(documentId, fileName) {
    if (!confirm(`Are you sure you want to delete "${fileName}"? This will remove all associated vector data.`)) {
        return;
    }

    try {
        setStatus(`Deleting ${fileName}...`, "info");
        const res = await fetch(`${API_BASE}/files/${documentId}`, {
            method: "DELETE"
        });

        if (!res.ok) throw new Error("Delete failed");

        setStatus("Deleted successfully ✅", "success");
        loadFiles();
    } catch (err) {
        console.error(err);
        setStatus("Delete failed ❌", "error");
    }
}

// Add Message to Chat Box
function addMessage(text, type) {
    // Hide empty state on first message
    if (emptyState) {
        emptyState.style.display = "none";
    }

    const div = document.createElement("div");
    div.classList.add("msg", type);
    div.innerText = text;

    chatBox.appendChild(div);
    chatBox.scrollTop = chatBox.scrollHeight;
}

// Show Bot Typing Indicator
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

// Remove Bot Typing Indicator
function removeTypingIndicator() {
    if (typingIndicator && typingIndicator.parentNode) {
        typingIndicator.parentNode.removeChild(typingIndicator);
    }
    typingIndicator = null;
}

// Ask Question to Chatbot
async function askQuestion() {
    const question = questionInput.value.trim();
    if (!question) return;

    // Add user message
    addMessage(question, "user");
    questionInput.value = "";

    // Show bot thinking indicator
    showTypingIndicator();

    try {
        const res = await fetch(`${API_BASE}/query`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ question })
        });

        if (!res.ok) throw new Error("Query failed");

        const data = await res.text();
        
        // Remove typing indicator and add response
        removeTypingIndicator();
        addMessage(data, "bot");

    } catch (err) {
        console.error(err);
        removeTypingIndicator();
        addMessage("Error connecting to AI backend. Make sure the server and Ollama are running. ❌", "bot");
    }
}