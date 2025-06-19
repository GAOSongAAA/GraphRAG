import axios from 'axios';
import { API_BASE_URL } from '../config';

const axiosClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 60000,
  headers: {
    'Content-Type': 'application/json',
  },
});

axiosClient.interceptors.response.use(
  (response) => {
    // Return the full response data, letting React Query handle parsing
    return response.data;
  },
  (error) => {
    const message =
      error.response?.data?.message ||
      error.message ||
      'A network error occurred';
    return Promise.reject(new Error(message));
  }
);

export default axiosClient;
