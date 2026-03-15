'use client';

import {useEffect, useState} from 'react';
import {CarFilters as Filters} from '@/lib/api/cars';
import {Button} from '@/components/ui/button';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {Card, CardContent} from '@/components/ui/card';
import {X} from 'lucide-react';

interface CarFiltersProps {
    filters: Filters;
    onFiltersChange: (filters: Filters) => void;
    onReset: () => void;
}

export function CarFilters({filters, onFiltersChange, onReset}: CarFiltersProps) {
    const [localFilters, setLocalFilters] = useState<Filters>(filters);

    useEffect(() => {
        setLocalFilters(filters);
    }, [filters]);

    const handleChange = (key: keyof Filters, value: string | undefined) => {
        const newFilters = {...localFilters, [key]: value || undefined};
        setLocalFilters(newFilters);
    };

    const handleApply = () => {
        onFiltersChange(localFilters);
    };

    const hasFilters = Object.values(filters).some(v => v !== undefined && v !== '');

    return (
        <Card>
            <CardContent className="p-6">
                <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
                    <div className="space-y-2">
                        <Label>Марка</Label>
                        <Input
                            placeholder="Например: Toyota"
                            value={localFilters.brand || ''}
                            onChange={(e) => handleChange('brand', e.target.value)}
                        />
                    </div>
                    <div className="space-y-2">
                        <Label>Модель</Label>
                        <Input
                            placeholder="Например: Camry"
                            value={localFilters.model || ''}
                            onChange={(e) => handleChange('model', e.target.value)}
                        />
                    </div>
                    <div className="space-y-2">
                        <Label>Год от</Label>
                        <Input
                            type="number"
                            placeholder="2020"
                            value={localFilters.yearFrom || ''}
                            onChange={(e) => handleChange('yearFrom', e.target.value)}
                        />
                    </div>
                    <div className="space-y-2">
                        <Label>Год до</Label>
                        <Input
                            type="number"
                            placeholder="2025"
                            value={localFilters.yearTo || ''}
                            onChange={(e) => handleChange('yearTo', e.target.value)}
                        />
                    </div>
                    <div className="space-y-2">
                        <Label>Цена от (₽)</Label>
                        <Input
                            type="number"
                            placeholder="500000"
                            value={localFilters.priceFrom || ''}
                            onChange={(e) => handleChange('priceFrom', e.target.value)}
                        />
                    </div>
                    <div className="space-y-2">
                        <Label>Цена до (₽)</Label>
                        <Input
                            type="number"
                            placeholder="5000000"
                            value={localFilters.priceTo || ''}
                            onChange={(e) => handleChange('priceTo', e.target.value)}
                        />
                    </div>
                </div>
                <div className="mt-4 flex justify-end gap-2">
                    {hasFilters && (
                        <Button variant="ghost" onClick={onReset}>
                            <X className="mr-2 h-4 w-4"/>
                            Сбросить
                        </Button>
                    )}
                    <Button onClick={handleApply}>
                        Применить фильтры
                    </Button>
                </div>
            </CardContent>
        </Card>
    );
}