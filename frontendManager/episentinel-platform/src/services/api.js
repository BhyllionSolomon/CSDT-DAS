const API_BASE_URL = "http://localhost:8081";

const api = {

    async get(url) {

        const response = await fetch(`${API_BASE_URL}${url}`);

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }

        return {
            data: await response.json()
        };
    },

    async post(url, body) {

        const response = await fetch(`${API_BASE_URL}${url}`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(body)
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }

        return {
            data: await response.json()
        };
    }

};

export default api;