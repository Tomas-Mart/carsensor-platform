'use client';

import {useEffect, useState} from 'react';
import {useRouter} from 'next/navigation';
import {useAuth} from '@/hooks/useAuth';
import {useCars} from '@/hooks/useCars';
import {CarFilters} from '@/components/cars/CarFilters';
import {CarCard} from '@/components/cars/CarCard';
import {Pagination} from '@/components/ui/pagination';
import {Button} from '@/components/ui/button';
import {Input} from '@/components/ui/input';
import {Alert, AlertDescription} from '@/components/ui/alert';
import {Car, LogOut, Search, SlidersHorizontal, X} from 'lucide-react';
import {AnimatePresence, motion} from 'framer-motion';

export default function CarsPage() {
    const router = useRouter();
    const {user, logout} = useAuth();
    const [showFilters, setShowFilters] = useState(false);
    const [searchQuery, setSearchQuery] = useState('');
    const [debouncedQuery, setDebouncedQuery] = useState('');

    const {
        cars,
        loading,
        error,
        pagination,
        filters,
        updateFilters,
        resetFilters,
        currentPage,
        setCurrentPage,
    } = useCars();

    // Debounce поискового запроса
    useEffect(() => {
        const timer = setTimeout(() => {
            setDebouncedQuery(searchQuery);
        }, 500);

        return () => clearTimeout(timer);
    }, [searchQuery]);

    // Применяем поисковый запрос к фильтрам
    useEffect(() => {
        updateFilters({search: debouncedQuery || undefined});
    }, [debouncedQuery, updateFilters]);

    const handleLogout = async () => {
        await logout();
        router.push('/login');
    };

    const clearSearch = () => {
        setSearchQuery('');
    };

    if (error) {
        return (
            <div className="container mx-auto px-4 py-8">
                <Alert variant="destructive">
                    <AlertDescription>{error}</AlertDescription>
                </Alert>
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-background">
            {/* Header */}
            <header className="sticky top-0 z-50 border-b bg-background/80 backdrop-blur">
                <div className="container mx-auto flex h-16 items-center justify-between px-4">
                    <div className="flex items-center space-x-2">
                        <Car className="h-6 w-6 text-primary"/>
                        <span className="text-xl font-bold">CarSensor</span>
                    </div>

                    <div className="flex items-center space-x-4">
                        <span className="hidden text-sm text-muted-foreground sm:inline">
                            {user?.username || 'admin'}
                        </span>
                        <Button
                            variant="ghost"
                            size="icon"
                            onClick={handleLogout}
                            title="Выйти"
                        >
                            <LogOut className="h-5 w-5"/>
                        </Button>
                    </div>
                </div>
            </header>

            {/* Main Content */}
            <main className="container mx-auto px-4 py-8">
                {/* Search and Filters Bar */}
                <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                    <div className="relative flex-1">
                        <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground"/>
                        <Input
                            type="text"
                            placeholder="Поиск по марке, модели или описанию..."
                            value={searchQuery}
                            onChange={(e) => setSearchQuery(e.target.value)}
                            className="pl-10 pr-10"
                        />
                        {searchQuery && (
                            <Button
                                variant="ghost"
                                size="icon"
                                className="absolute right-1 top-1/2 h-7 w-7 -translate-y-1/2"
                                onClick={clearSearch}
                            >
                                <X className="h-4 w-4"/>
                            </Button>
                        )}
                    </div>

                    <Button
                        variant="outline"
                        onClick={() => setShowFilters(!showFilters)}
                        className="sm:w-auto"
                    >
                        <SlidersHorizontal className="mr-2 h-4 w-4"/>
                        Фильтры
                        {filters && Object.keys(filters).length > 0 && Object.keys(filters).some(key => filters[key as keyof typeof filters]) && (
                            <span className="ml-2 rounded-full bg-primary px-2 py-0.5 text-xs text-primary-foreground">
                                {Object.keys(filters).filter(key => filters[key as keyof typeof filters]).length}
                            </span>
                        )}
                    </Button>
                </div>

                {/* Filters Panel */}
                <AnimatePresence>
                    {showFilters && (
                        <motion.div
                            initial={{opacity: 0, height: 0}}
                            animate={{opacity: 1, height: 'auto'}}
                            exit={{opacity: 0, height: 0}}
                            transition={{duration: 0.2}}
                            className="mb-6 overflow-hidden"
                        >
                            <CarFilters
                                filters={filters}
                                onFiltersChange={updateFilters}
                                onReset={resetFilters}
                            />
                        </motion.div>
                    )}
                </AnimatePresence>

                {/* Results Count */}
                <div className="mb-4 flex items-center justify-between">
                    <p className="text-sm text-muted-foreground">
                        Найдено автомобилей: <span
                        className="font-medium text-foreground">{pagination?.total_elements || 0}</span>
                    </p>
                    {loading && <p className="text-sm text-muted-foreground">Загрузка...</p>}
                </div>

                {/* Cars Grid */}
                {loading && cars.length === 0 ? (
                    <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
                        {[...Array(8)].map((_, i) => (
                            <div key={i} className="h-[300px] rounded-lg bg-muted animate-pulse"/>
                        ))}
                    </div>
                ) : cars.length === 0 ? (
                    <div
                        className="flex min-h-[400px] flex-col items-center justify-center rounded-lg border-2 border-dashed">
                        <Car className="h-12 w-12 text-muted-foreground"/>
                        <h3 className="mt-4 text-lg font-semibold">Автомобили не найдены</h3>
                        <p className="text-sm text-muted-foreground">
                            Попробуйте изменить параметры фильтрации
                        </p>
                        <Button variant="outline" className="mt-4" onClick={resetFilters}>
                            Сбросить фильтры
                        </Button>
                    </div>
                ) : (
                    <>
                        <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
                            {cars.map((car) => (
                                <CarCard key={car.id} car={car}/>
                            ))}
                        </div>

                        {/* Pagination */}
                        {pagination && pagination.total_pages > 1 && (
                            <div className="mt-8 flex justify-center">
                                <Pagination
                                    currentPage={currentPage}
                                    totalPages={pagination.total_pages}
                                    onPageChange={setCurrentPage}
                                />
                            </div>
                        )}
                    </>
                )}
            </main>
        </div>
    );
}