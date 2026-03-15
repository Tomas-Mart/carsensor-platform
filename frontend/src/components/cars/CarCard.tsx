'use client';

import Link from 'next/link';
import Image from 'next/image';
import {Car} from '@/lib/api/cars';
import {Card, CardContent} from '@/components/ui/card';
import {Car as CarIcon} from 'lucide-react';

interface CarCardProps {
    car: Car;
}

export function CarCard({car}: CarCardProps) {
    const formatPrice = (price: number) => {
        return new Intl.NumberFormat('ru-RU', {
            style: 'currency',
            currency: 'RUB',
            minimumFractionDigits: 0,
        }).format(price);
    };

    const formatMileage = (mileage: number) => {
        return new Intl.NumberFormat('ru-RU').format(mileage);
    };

    return (
        <Link href={`/cars/${car.id}`}>
            <Card className="overflow-hidden transition-all hover:shadow-lg">
                <div className="relative h-48 w-full bg-muted">
                    {car.mainPhotoUrl ? (
                        <Image
                            src={car.mainPhotoUrl}
                            alt={`${car.brand} ${car.model}`}
                            fill
                            className="object-cover"
                            sizes="(max-width: 768px) 100vw, (max-width: 1200px) 50vw, 33vw"
                        />
                    ) : (
                        <div className="flex h-full items-center justify-center">
                            <CarIcon className="h-12 w-12 text-muted-foreground"/>
                        </div>
                    )}
                </div>
                <CardContent className="p-4">
                    <h3 className="text-lg font-semibold">
                        {car.brand} {car.model}
                    </h3>
                    <p className="text-sm text-muted-foreground">
                        {car.year} год · {formatMileage(car.mileage)} км
                    </p>
                    <p className="mt-2 text-xl font-bold text-primary">
                        {formatPrice(car.price)}
                    </p>
                </CardContent>
            </Card>
        </Link>
    );
}