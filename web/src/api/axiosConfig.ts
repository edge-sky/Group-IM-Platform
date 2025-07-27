import axios from "axios";

let token = localStorage.getItem('token');
let uid = localStorage.getItem('uid');

const axiosInstance = axios.create({
  baseURL: 'http://localhost:8080',
});

axiosInstance.interceptors.request.use(
    // 请求前处理
    config => {
        // 允许重写 Content-Type
        if (!config.headers['Content-Type']) {
            config.headers['Content-Type'] = 'application/json';
        }
        if (token) {
            config.headers['Authorization'] = `${token}`;
        }
        return config;
    },
    error => {
        console.error('Request Error:', error);
        return Promise.reject(error);
    }
);

axiosInstance.interceptors.response.use(
    response => {
        if (response.data.respond === 'loginSuccess') {
            // 登录成功时，更新 token
            token = response.data.token;
            uid = response.data.uid;
            localStorage.setItem('token', token);
            localStorage.setItem('uid', uid);
            return response.status === 200;
        } else if (response.data.respond === 'tokenValid') {
            return response.status === 200;
        }
        return response.data;
    },
    error => {
        console.error('Response Error:', error);
        return Promise.reject(error);
    }
);

export default axiosInstance;