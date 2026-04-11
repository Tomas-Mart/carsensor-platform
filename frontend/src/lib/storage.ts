export const storage = {
    getToken: () => {
        const token = localStorage.getItem('access_token');
        // Проверяем, что токен не null, не undefined и не строка 'undefined'
        if (token && token !== 'undefined') {
            return token;
        }
        return null;
    },
    getRefreshToken: () => {
        const token = localStorage.getItem('refresh_token');
        if (token && token !== 'undefined') {
            return token;
        }
        return null;
    },
    getUser: () => {
        const user = localStorage.getItem('user');
        if (user && user !== 'undefined') {
            try {
                return JSON.parse(user);
            } catch {
                return null;
            }
        }
        return null;
    },
    setAuth: (accessToken: string, refreshToken: string, user: any) => {
        if (accessToken && accessToken !== 'undefined') {
            localStorage.setItem('access_token', accessToken);
        }
        if (refreshToken && refreshToken !== 'undefined') {
            localStorage.setItem('refresh_token', refreshToken);
        }
        if (user) {
            localStorage.setItem('user', JSON.stringify(user));
        }
    },
    clearAuth: () => {
        localStorage.removeItem('access_token');
        localStorage.removeItem('refresh_token');
        localStorage.removeItem('user');
    }
};