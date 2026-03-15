import type {Metadata} from 'next';
import {Inter} from 'next/font/google';
import './globals.css';
import {Providers} from '@/providers/Providers';
import {Toaster} from '@/components/ui/sonner';

const inter = Inter({subsets: ['latin', 'cyrillic']});

export const metadata: Metadata = {
    title: 'CarSensor - Автомобили из Японии',
    description: 'Просмотр автомобилей с японского аукциона CarSensor',
};

export default function RootLayout({
                                       children,
                                   }: {
    children: React.ReactNode;
}) {
    return (
        <html lang="ru" suppressHydrationWarning>
        <body className={inter.className}>
        <Providers>
            {children}
            <Toaster richColors position="top-right"/>
        </Providers>
        </body>
        </html>
    );
}