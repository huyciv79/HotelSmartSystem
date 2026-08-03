import axios from 'axios';

const getBaseUrl = () => {
  if (import.meta.env.VITE_API_BASE_URL) {
    return import.meta.env.VITE_API_BASE_URL;
  }
  if (typeof window !== 'undefined') {
    const hostname = window.location.hostname;
    const userAgent = window.navigator?.userAgent || '';
    const isAndroid = /android/i.test(userAgent);

    if (hostname !== 'localhost' && hostname !== '127.0.0.1') {
      return `http://${hostname}:8080/api`;
    }

    if (isAndroid) {
      return 'http://10.0.2.2:8080/api';
    }
  }
  return 'http://localhost:8080/api';
};

const axiosInstance = axios.create({
  baseURL: getBaseUrl(),
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000,
});

// Request interceptor to automatically attach authorization header
axiosInstance.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor to handle errors & automatic fallback retries
axiosInstance.interceptors.response.use(
  (response) => {
    return response;
  },
  async (error) => {
    // Automatic fallback retry for Android Emulator if localhost connection fails
    if (error.code === 'ERR_NETWORK' && error.config && !error.config._retry) {
      error.config._retry = true;
      const currentBaseUrl = error.config.baseURL || '';
      if (currentBaseUrl.includes('localhost:8080') || currentBaseUrl.includes('127.0.0.1:8080')) {
        error.config.baseURL = currentBaseUrl.replace(/localhost:8080|127\.0\.0\.1:8080/, '10.0.2.2:8080');
        return axiosInstance(error.config);
      }
    }

    if (error.response && error.response.status === 401) {
      // Prevent redirecting if the 401 is from the login attempt itself
      if (error.config.url && error.config.url.includes('/auth/login')) {
        return Promise.reject(error);
      }
      
      // Clear local storage authentication info
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('user');
      
      // Redirect to home/login and refresh page state
      window.location.href = '/';
    }
    return Promise.reject(error);
  }
);

export default axiosInstance;
