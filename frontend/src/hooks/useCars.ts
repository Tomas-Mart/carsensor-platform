'use client';

import {useCallback, useEffect, useState} from 'react';
import {Car, carApi, CarFilters, PageResponse} from '@/lib/api/cars';
import {useAuth} from './useAuth';

interface UseCarsReturn {
    cars: Car[];
    loading: boolean;
    error: string | null;
    pagination: PageResponse['pagination'] | null;
    filters: CarFilters;
    updateFilters: (newFilters: CarFilters) => void;
    resetFilters: () => void;
    currentPage: number;
    setCurrentPage: (page: number) => void;
}

const DEFAULT_PAGE_SIZE = 20;

export const useCars = (initialFilters: CarFilters = {}): UseCarsReturn => {
    const {isAuthenticated} = useAuth();
    const [cars, setCars] = useState<Car[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [pagination, setPagination] = useState<PageResponse['pagination'] | null>(null);
    const [filters, setFilters] = useState<CarFilters>(initialFilters);
    const [currentPage, setCurrentPage] = useState(0);

    const fetchCars = useCallback(async () => {
        if (!isAuthenticated) return;

        try {
            setLoading(true);
            setError(null);
            const response = await carApi.getCars({
                ...filters,
                page: currentPage,
                size: DEFAULT_PAGE_SIZE,
            });
            setCars(response.content);
            setPagination(response.pagination);
        } catch (err) {
            setError('Не удалось загрузить список автомобилей');
            console.error(err);
        } finally {
            setLoading(false);
        }
    }, [isAuthenticated, filters, currentPage]);

    useEffect(() => {
        fetchCars();
    }, [fetchCars]);

    const updateFilters = useCallback((newFilters: CarFilters) => {
        setFilters((prev) => ({...prev, ...newFilters}));
        setCurrentPage(0); // Сброс на первую страницу при изменении фильтров
    }, []);

    const resetFilters = useCallback(() => {
        setFilters({});
        setCurrentPage(0);
    }, []);

    return {
        cars,
        loading,
        error,
        pagination,
        filters,
        updateFilters,
        resetFilters,
        currentPage,
        setCurrentPage,
    };
};