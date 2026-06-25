const API_BASE = "http://localhost:8080/api";

function addMessage(text, type) {
    const chatBox = document.getElementById("chatBox");

    const div = document.createElement("div");
    div.classList.add("msg", type);
    div.innerText = text;

    chatBox.appendChild(div);
    chatBox.scrollTop = chatBox.scrollHeight;
}

function setStatus(msg) {
    document.getElementById("uploadStatus").innerText = msg;
}

async function uploadPDF() {
    const file = document.getElementById("fileInput").files[0];

    if (!file) {
        alert("Select a file first");
        return;
    }

    const formData = new FormData();
    formData.append("file", file);

    try {
        setStatus("Uploading...");

        const res = await fetch(`${API_BASE}/api/files/upload`, {
            method: "POST",
            body: formData
        });

        if (!res.ok) throw new Error("Upload failed");

        setStatus("Upload successful ✅");
    } catch (err) {
        console.error(err);
        setStatus("Upload failed ❌");
    }
}

async function askQuestion() {
    const input = document.getElementById("questionInput");
    const question = input.value;

    if (!question) return;

    addMessage(question, "user");
    input.value = "";

    try {
        const res = await fetch(`${API_BASE}/api/query`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ question })
        });

        if (!res.ok) throw new Error("Query failed");

        const data = await res.text(); // or res.json()

        addMessage(data, "bot");

    } catch (err) {
        console.error(err);
        addMessage("Error connecting to backend ❌", "bot");
    }
}