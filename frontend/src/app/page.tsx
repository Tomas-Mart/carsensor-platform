'use client';

import {useEffect} from 'react';
import {useRouter} from 'next/navigation';
import {useAuth} from '@/hooks/useAuth';
import {Car, Loader2} from 'lucide-react';

export default function HomePage() {
    const router = useRouter();
    const {isAuthenticated, isLoading} = useAuth();

    useEffect(() => {
        if (!isLoading) {
            if (isAuthenticated) {
                router.push('/cars');
            } else {
                router.push('/login');
            }
        }
    }, [isAuthenticated, isLoading, router]);

    return (
        <div className="flex min-h-screen items-center justify-center">
            <div className="text-center">
                <Car className="mx-auto h-12 w-12 animate-pulse text-primary"/>
                <Loader2 className="mx-auto mt-4 h-6 w-6 animate-spin text-muted-foreground"/>
                <p className="mt-2 text-sm text-muted-foreground">Загрузка...</p>
            </div>
        </div>
    );
}