import {apiClient} from './client';

export interface LoginCredentials {
    username: string;
    password: string;
}

export interface AuthResponse {
    access_token: string;
    refresh_token: string;
    expires_in: number;
    username: string;
    roles: string[];
    token_type: string;
}

export interface User {
    username: string;
    roles: string[];
}

class AuthApi {
    async login(credentials: LoginCredentials): Promise<AuthResponse> {
        const response = await apiClient.post<AuthResponse>('/api/v1/auth/login', credentials);
        return response.data;
    }

    async refreshToken(refreshToken: string): Promise<AuthResponse> {
        const response = await apiClient.post<AuthResponse>(
            '/api/v1/auth/refresh',
            {},
            {
                headers: {
                    Authorization: `Bearer ${refreshToken}`,
                },
            }
        );
        return response.data;
    }

    async logout(): Promise<void> {
        const token = localStorage.getItem('access_token');
        if (token) {
            await apiClient.post('/api/v1/auth/logout', null, {
                headers: {
                    Authorization: `Bearer ${token}`,
                },
            });
        }
    }
}

export const authApi = new AuthApi();
