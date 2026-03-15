'use client';

import {createContext, ReactNode, useEffect, useState} from 'react';
import {authApi, AuthResponse, User} from '@/lib/api/auth';
import {storage} from '@/lib/storage';

interface AuthContextType {
    user: User | null;
    loading: boolean;
    isAuthenticated: boolean;
    isLoading: boolean;
    login: (username: string, password: string) => Promise<void>;
    logout: () => Promise<void>;  // <-- Promise
}

export const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({children}: { children: ReactNode }) {
    const [user, setUser] = useState<User | null>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const savedUser = storage.getUser();
        const token = storage.getToken();

        if (token && savedUser) {
            setUser(savedUser);
        }
        setLoading(false);
    }, []);

    const login = async (username: string, password: string) => {
        const response: AuthResponse = await authApi.login({username, password});
        const userData: User = {
            username: response.username,
            roles: response.roles,
        };
        storage.setAuth(response.accessToken, response.refreshToken, userData);
        setUser(userData);
    };

    const logout = async (): Promise<void> => {  // <-- async + Promise
        try {
            await authApi.logout();
        } finally {
            storage.clearAuth();
            setUser(null);
        }
    };

    return (
        <AuthContext.Provider value={{
            user,
            loading,
            isAuthenticated: !!user,
            isLoading: loading,
            login,
            logout,
        }}>
            {children}
        </AuthContext.Provider>
    );
}