import {apiClient} from './client';

export interface Car {
    id?: number;
    brand: string;
    model: string;
    year: number;
    mileage: number;
    price: number;
    description?: string;
    originalBrand?: string;
    originalModel?: string;
    exteriorColor?: string;
    interiorColor?: string;
    engineCapacity?: string;
    transmission?: string;
    driveType?: string;
    photoUrls?: string[];
    mainPhotoUrl?: string;
    sourceUrl?: string;
    parsedAt?: string;
    createdAt?: string;
    updatedAt?: string;
}

export interface CarFilters {
    brand?: string;
    model?: string;
    yearFrom?: number;
    yearTo?: number;
    mileageFrom?: number;
    mileageTo?: number;
    priceFrom?: number;
    priceTo?: number;
    transmission?: string;
    driveType?: string;
    search?: string;
    page?: number;
    size?: number;
    sort?: string[];
}

export interface PaginationInfo {
    total_elements: number;
    total_pages: number;
    current_page: number;
    page_size: number;
    first: boolean;
    last: boolean;
    empty: boolean;
}

export interface PageResponse {
    content: Car[];
    pagination: PaginationInfo;
}

export interface FilterOptions {
    brands: string[];
    yearMin: number;
    yearMax: number;
    priceMin: number;
    priceMax: number;
    transmissions: string[];
    driveTypes: string[];
}

class CarApi {
    async getCars(filters: CarFilters = {}): Promise<PageResponse> {
        const params = new URLSearchParams();

        Object.entries(filters).forEach(([key, value]) => {
            if (value !== undefined && value !== null && value !== '') {
                if (Array.isArray(value)) {
                    params.append(key, value.join(','));
                } else {
                    params.append(key, String(value));
                }
            }
        });

        const response = await apiClient.get<PageResponse>('/api/v1/cars', {params});
        return response.data;
    }

    async getCarById(id: number): Promise<Car> {
        const response = await apiClient.get<Car>(`/api/v1/cars/${id}`);
        return response.data;
    }

    async getFilterOptions(): Promise<FilterOptions> {
        const response = await apiClient.get<FilterOptions>('/api/v1/cars/filters');
        return response.data;
    }
}

export const carApi = new CarApi();