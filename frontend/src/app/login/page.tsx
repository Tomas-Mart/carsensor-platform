'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { Car, Eye, EyeOff, Loader2 } from 'lucide-react';

export default function LoginPage() {
  const router = useRouter();
  const [username, setUsername] = useState('admin');
  const [password, setPassword] = useState('admin123');
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      console.log('Sending login request...');
      const response = await fetch('http://localhost:8080/api/v1/auth/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ username, password }),
      });

      const data = await response.json();
      console.log('Response status:', response.status);
      console.log('Response data:', data);

      if (!response.ok) {
        throw new Error(data.message || data.detail || 'Ошибка входа');
      }

      if (data.access_token) {
        localStorage.setItem('token', data.access_token);
        localStorage.setItem('refresh_token', data.refresh_token);
        console.log('Token saved, redirecting to /cars...');
        
        // Используем window.location.href вместо router.push
        window.location.href = '/cars';
      } else {
        setError('Токен не получен');
      }
    } catch (err: any) {
      console.error('Login error:', err);
      setError(err.message || 'Ошибка входа');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-primary/5 via-background to-primary/5 p-4">
      <div className="w-full max-w-md">
        <div className="rounded-lg border bg-card text-card-foreground shadow-xl">
          <div className="flex flex-col p-6 space-y-1 text-center">
            <div className="flex justify-center mb-4">
              <div className="rounded-full bg-primary/10 p-3">
                <Car className="h-8 w-8 text-primary" />
              </div>
            </div>
            <h3 className="text-2xl font-bold">CarSensor</h3>
            <p className="text-sm text-muted-foreground">
              Войдите в систему для просмотра автомобилей
            </p>
          </div>
          <div className="p-6 pt-0">
            <form onSubmit={handleSubmit} className="space-y-4">
              {error && (
                <div className="p-3 bg-red-100 text-red-700 rounded-md text-sm text-center">
                  {error}
                </div>
              )}
              <div className="space-y-2">
                <label className="text-sm font-medium">Логин</label>
                <input
                  type="text"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                  placeholder="Введите логин"
                />
                <p className="text-xs text-muted-foreground">
                  Используйте <span className="font-mono">admin</span> для входа
                </p>
              </div>
              <div className="space-y-2">
                <label className="text-sm font-medium">Пароль</label>
                <div className="relative">
                  <input
                    type={showPassword ? 'text' : 'password'}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm pr-10"
                    placeholder="Введите пароль"
                  />
                  <button
                    type="button"
                    className="absolute right-0 top-0 h-full px-3"
                    onClick={() => setShowPassword(!showPassword)}
                  >
                    {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                  </button>
                </div>
                <p className="text-xs text-muted-foreground">
                  Пароль: <span className="font-mono">admin123</span>
                </p>
              </div>
              <button
                type="submit"
                disabled={loading}
                className="inline-flex items-center justify-center w-full h-10 px-4 py-2 bg-primary text-primary-foreground rounded-md hover:bg-primary/90 disabled:opacity-50"
              >
                {loading ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : null}
                {loading ? 'Вход...' : 'Войти'}
              </button>
            </form>
          </div>
          <div className="p-6 pt-0 text-center">
            <p className="text-xs text-muted-foreground">
              Демо-доступ: admin / admin123
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
