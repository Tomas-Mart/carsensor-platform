'use client';

import {useEffect, useState} from 'react';
import {useParams, useRouter} from 'next/navigation';
import Image from 'next/image';
import {useAuth} from '@/hooks/useAuth';
import {Car, carApi} from '@/lib/api/cars';
import {Button} from '@/components/ui/button';
import {Card, CardContent} from '@/components/ui/card';
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import {Separator} from '@/components/ui/separator';
import {Skeleton} from '@/components/ui/skeleton';
import {Alert, AlertDescription} from '@/components/ui/alert';
import {ArrowLeft, Car as CarIcon, Cog, Fuel, Palette, Ruler,} from 'lucide-react';
import {format} from 'date-fns';
import {ru} from 'date-fns/locale';
import {motion} from 'framer-motion';

export default function CarDetailPage() {
    const params = useParams();
    const router = useRouter();
    const {isAuthenticated} = useAuth();
    const [car, setCar] = useState<Car | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [selectedImage, setSelectedImage] = useState<string | null>(null);

    const carId = params.id as string;

    useEffect(() => {
        if (!isAuthenticated) {
            router.push('/login');
            return;
        }

        const fetchCar = async () => {
            try {
                setLoading(true);
                const data = await carApi.getCarById(parseInt(carId));
                setCar(data);
                if (data.photoUrls && data.photoUrls.length > 0) {
                    setSelectedImage(data.photoUrls[0]);
                }
            } catch (err) {
                setError('Не удалось загрузить информацию об автомобиле');
                console.error(err);
            } finally {
                setLoading(false);
            }
        };

        fetchCar();
    }, [carId, isAuthenticated, router]);

    const formatPrice = (price: number) => {
        return new Intl.NumberFormat('ru-RU', {
            style: 'currency',
            currency: 'RUB',
            minimumFractionDigits: 0,
            maximumFractionDigits: 0,
        }).format(price);
    };

    const formatNumber = (num: number) => {
        return new Intl.NumberFormat('ru-RU').format(num);
    };

    if (loading) {
        return (
            <div className="container mx-auto px-4 py-8">
                <div className="mb-6">
                    <Skeleton className="h-10 w-32"/>
                </div>
                <div className="grid gap-8 lg:grid-cols-2">
                    <Skeleton className="h-[400px] rounded-lg"/>
                    <div className="space-y-4">
                        <Skeleton className="h-10 w-3/4"/>
                        <Skeleton className="h-8 w-1/2"/>
                        <Skeleton className="h-20 w-full"/>
                        <div className="grid grid-cols-2 gap-4">
                            {[...Array(6)].map((_, i) => (
                                <Skeleton key={i} className="h-16 rounded-lg"/>
                            ))}
                        </div>
                    </div>
                </div>
            </div>
        );
    }

    if (error || !car) {
        return (
            <div className="container mx-auto px-4 py-8">
                <Button
                    variant="ghost"
                    onClick={() => router.back()}
                    className="mb-6"
                >
                    <ArrowLeft className="mr-2 h-4 w-4"/>
                    Назад
                </Button>
                <Alert variant="destructive">
                    <AlertDescription>{error || 'Автомобиль не найден'}</AlertDescription>
                </Alert>
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-background">
            <div className="container mx-auto px-4 py-8">
                {/* Back Button */}
                <Button
                    variant="ghost"
                    onClick={() => router.back()}
                    className="mb-6"
                >
                    <ArrowLeft className="mr-2 h-4 w-4"/>
                    Назад к списку
                </Button>

                <div className="grid gap-8 lg:grid-cols-2">
                    {/* Image Gallery */}
                    <div className="space-y-4">
                        <motion.div
                            initial={{opacity: 0, y: 20}}
                            animate={{opacity: 1, y: 0}}
                            className="relative aspect-[4/3] overflow-hidden rounded-lg border bg-muted"
                        >
                            {selectedImage ? (
                                <Image
                                    src={selectedImage}
                                    alt={`${car.brand} ${car.model}`}
                                    fill
                                    className="object-cover"
                                    sizes="(max-width: 768px) 100vw, 50vw"
                                    priority
                                />
                            ) : (
                                <div className="flex h-full items-center justify-center">
                                    <CarIcon className="h-16 w-16 text-muted-foreground"/>
                                </div>
                            )}
                        </motion.div>

                        {car.photoUrls && car.photoUrls.length > 1 && (
                            <div className="flex gap-2 overflow-x-auto pb-2">
                                {car.photoUrls.map((url, index) => (
                                    <motion.button
                                        key={index}
                                        initial={{opacity: 0, scale: 0.9}}
                                        animate={{opacity: 1, scale: 1}}
                                        transition={{delay: index * 0.1}}
                                        onClick={() => setSelectedImage(url)}
                                        className={`relative h-20 w-20 flex-shrink-0 overflow-hidden rounded-md border ${
                                            selectedImage === url
                                                ? 'ring-2 ring-primary'
                                                : 'hover:ring-2 hover:ring-primary/50'
                                        }`}
                                    >
                                        <Image
                                            src={url}
                                            alt={`${car.brand} ${car.model} - фото ${index + 1}`}
                                            fill
                                            className="object-cover"
                                            sizes="80px"
                                        />
                                    </motion.button>
                                ))}
                            </div>
                        )}
                    </div>

                    {/* Car Details */}
                    <motion.div
                        initial={{opacity: 0, x: 20}}
                        animate={{opacity: 1, x: 0}}
                        className="space-y-6"
                    >
                        <div>
                            <div className="flex items-start justify-between">
                                <div>
                                    <h1 className="text-3xl font-bold">
                                        {car.brand} {car.model}
                                    </h1>
                                    <p className="mt-1 text-lg text-muted-foreground">
                                        {car.year} год · {formatNumber(car.mileage)} км
                                    </p>
                                </div>
                                <div className="text-right">
                                    <p className="text-3xl font-bold text-primary">
                                        {formatPrice(car.price)}
                                    </p>
                                    {car.originalBrand && (
                                        <p className="text-sm text-muted-foreground">
                                            Оригинал: {car.originalBrand}
                                        </p>
                                    )}
                                </div>
                            </div>
                        </div>

                        <Separator/>

                        {/* Key Specifications */}
                        <div className="grid grid-cols-2 gap-4 sm:grid-cols-3">
                            {car.transmission && (
                                <Card>
                                    <CardContent className="flex flex-col items-center p-4">
                                        <Cog className="h-6 w-6 text-primary"/>
                                        <span className="mt-2 text-sm font-medium">Трансмиссия</span>
                                        <span className="text-xs text-muted-foreground">{car.transmission}</span>
                                    </CardContent>
                                </Card>
                            )}
                            {car.driveType && (
                                <Card>
                                    <CardContent className="flex flex-col items-center p-4">
                                        <Fuel className="h-6 w-6 text-primary"/>
                                        <span className="mt-2 text-sm font-medium">Привод</span>
                                        <span className="text-xs text-muted-foreground">{car.driveType}</span>
                                    </CardContent>
                                </Card>
                            )}
                            {car.engineCapacity && (
                                <Card>
                                    <CardContent className="flex flex-col items-center p-4">
                                        <Ruler className="h-6 w-6 text-primary"/>
                                        <span className="mt-2 text-sm font-medium">Двигатель</span>
                                        <span className="text-xs text-muted-foreground">{car.engineCapacity}</span>
                                    </CardContent>
                                </Card>
                            )}
                            {car.exteriorColor && (
                                <Card>
                                    <CardContent className="flex flex-col items-center p-4">
                                        <Palette className="h-6 w-6 text-primary"/>
                                        <span className="mt-2 text-sm font-medium">Цвет</span>
                                        <span className="text-xs text-muted-foreground">{car.exteriorColor}</span>
                                    </CardContent>
                                </Card>
                            )}
                        </div>

                        {/* Detailed Specifications Tabs */}
                        <Tabs defaultValue="details" className="mt-8">
                            <TabsList className="grid w-full grid-cols-2">
                                <TabsTrigger value="details">Характеристики</TabsTrigger>
                                <TabsTrigger value="additional">Дополнительно</TabsTrigger>
                            </TabsList>
                            <TabsContent value="details" className="mt-4 space-y-4">
                                <div className="grid gap-4 sm:grid-cols-2">
                                    <div className="space-y-2">
                                        <div className="flex justify-between">
                                            <span className="text-muted-foreground">Марка</span>
                                            <span className="font-medium">{car.brand}</span>
                                        </div>
                                        <Separator/>
                                        <div className="flex justify-between">
                                            <span className="text-muted-foreground">Модель</span>
                                            <span className="font-medium">{car.model}</span>
                                        </div>
                                        <Separator/>
                                        <div className="flex justify-between">
                                            <span className="text-muted-foreground">Год выпуска</span>
                                            <span className="font-medium">{car.year}</span>
                                        </div>
                                        <Separator/>
                                        <div className="flex justify-between">
                                            <span className="text-muted-foreground">Пробег</span>
                                            <span className="font-medium">{formatNumber(car.mileage)} км</span>
                                        </div>
                                    </div>
                                    <div className="space-y-2">
                                        <div className="flex justify-between">
                                            <span className="text-muted-foreground">Цена</span>
                                            <span className="font-medium text-primary">{formatPrice(car.price)}</span>
                                        </div>
                                        <Separator/>
                                        {car.transmission && (
                                            <>
                                                <div className="flex justify-between">
                                                    <span className="text-muted-foreground">Трансмиссия</span>
                                                    <span className="font-medium">{car.transmission}</span>
                                                </div>
                                                <Separator/>
                                            </>
                                        )}
                                        {car.driveType && (
                                            <>
                                                <div className="flex justify-between">
                                                    <span className="text-muted-foreground">Привод</span>
                                                    <span className="font-medium">{car.driveType}</span>
                                                </div>
                                                <Separator/>
                                            </>
                                        )}
                                        {car.engineCapacity && (
                                            <div className="flex justify-between">
                                                <span className="text-muted-foreground">Объем двигателя</span>
                                                <span className="font-medium">{car.engineCapacity}</span>
                                            </div>
                                        )}
                                    </div>
                                </div>
                            </TabsContent>
                            <TabsContent value="additional" className="mt-4 space-y-4">
                                {car.description && (
                                    <div>
                                        <h3 className="mb-2 font-medium">Описание</h3>
                                        <p className="text-sm text-muted-foreground">{car.description}</p>
                                    </div>
                                )}
                                <div className="grid gap-4 sm:grid-cols-2">
                                    <div className="space-y-2">
                                        {car.exteriorColor && (
                                            <div className="flex justify-between">
                                                <span className="text-muted-foreground">Цвет кузова</span>
                                                <span className="font-medium">{car.exteriorColor}</span>
                                            </div>
                                        )}
                                        {car.interiorColor && (
                                            <div className="flex justify-between">
                                                <span className="text-muted-foreground">Цвет салона</span>
                                                <span className="font-medium">{car.interiorColor}</span>
                                            </div>
                                        )}
                                    </div>
                                    <div className="space-y-2">
                                        {car.parsedAt && (
                                            <div className="flex justify-between">
                                                <span className="text-muted-foreground">Дата парсинга</span>
                                                <span className="font-medium">
                          {format(new Date(car.parsedAt), 'dd MMM yyyy', {locale: ru})}
                        </span>
                                            </div>
                                        )}
                                        {car.sourceUrl && (
                                            <div className="flex justify-between">
                                                <span className="text-muted-foreground">Источник</span>
                                                <a
                                                    href={car.sourceUrl}
                                                    target="_blank"
                                                    rel="noopener noreferrer"
                                                    className="font-medium text-primary hover:underline"
                                                >
                                                    CarSensor
                                                </a>
                                            </div>
                                        )}
                                    </div>
                                </div>
                            </TabsContent>
                        </Tabs>
                    </motion.div>
                </div>
            </div>
        </div>
    );
}