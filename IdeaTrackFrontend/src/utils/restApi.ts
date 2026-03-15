import axios from "axios";

const BASE_URL = "http://localhost:8091";

const restApi = axios.create({
  baseURL: BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
});

// Request interceptor: attach JWT Bearer token from localStorage
restApi.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("jwt-token");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor: handle common errors (401, 403, etc.)
restApi.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      const status = error.response.status;
      if (status === 401) {
        console.error("Unauthorized – token may be expired or invalid.");
      } else if (status === 403) {
        console.error("Forbidden – you do not have permission for this action.");
      }
    }
    return Promise.reject(error);
  }
);

export default restApi;
export { BASE_URL };
