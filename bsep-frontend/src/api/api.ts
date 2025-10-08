import axios from 'axios';

const api = axios.create({
    baseURL: 'https://localhost:8443',
    timeout: 10000,
});

api.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token');
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

api.interceptors.response.use(
    (response) => {
        return response;
    },
    (error) => {
        // Handle JWT and authentication errors
        if (error.response?.status === 401) {
            // Clear any invalid tokens
            localStorage.removeItem('token');
            localStorage.removeItem('user');

            // Check if this is a JWT signature error
            const errorMessage = error.response?.data?.message || error.message || '';
            if (errorMessage.includes('JWT signature') || errorMessage.includes('JWT validity')) {
                console.warn('Invalid JWT token detected, clearing auth data');
            }

            // Only redirect if not already on login/register pages
            const currentPath = window.location.pathname;
            if (currentPath !== '/login' && currentPath !== '/register') {
                window.location.href = '/login';
            }
        }

        // Handle other common errors
        if (error.response?.status === 403) {
            console.warn('Access forbidden - insufficient permissions');
        }

        return Promise.reject(error);
    }
);

export default api;