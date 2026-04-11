import type {Metadata} from 'next';
import type {ReactNode} from 'react';
import './globals.css';
import {Providers} from '@/providers/Providers';
import {Toaster} from '@/components/ui/sonner';

export const metadata: Metadata = {
    title: 'CarSensor - Автомобили из Японии',
    description: 'Просмотр автомобилей с японского аукциона CarSensor',
};

interface RootLayoutProps {
    children: ReactNode;
}

export default function RootLayout({children}: RootLayoutProps) {
    return (
        <html lang="ru" suppressHydrationWarning>
        <body>
        <Providers>
            {children}
            <Toaster richColors position="top-right"/>
        </Providers>
        </body>
        </html>
    );
}