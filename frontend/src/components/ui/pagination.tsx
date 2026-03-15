'use client';

import * as React from 'react';
import {ChevronLeft, ChevronRight} from 'lucide-react';
import {buttonVariants} from '@/components/ui/button';
import {cn} from '@/lib/utils';

interface PaginationProps {
    currentPage: number;
    totalPages: number;
    onPageChange: (page: number) => void;
}

const Pagination = ({currentPage, totalPages, onPageChange}: PaginationProps) => {
    const pages = Array.from({length: totalPages}, (_, i) => i + 1);

    return (
        <nav role="navigation" aria-label="pagination" className="mx-auto flex w-full justify-center">
            <ul className="flex flex-row items-center gap-1">
                <li>
                    <button
                        onClick={() => onPageChange(currentPage - 1)}
                        disabled={currentPage === 1}
                        className={cn(
                            buttonVariants({variant: 'ghost', size: 'icon'}),
                            'gap-1 pl-2.5',
                            currentPage === 1 && 'pointer-events-none opacity-50'
                        )}
                    >
                        <ChevronLeft className="h-4 w-4"/>
                        <span className="sr-only">Назад</span>
                    </button>
                </li>
                {pages.map((page) => (
                    <li key={page}>
                        <button
                            onClick={() => onPageChange(page)}
                            className={cn(
                                buttonVariants({
                                    variant: currentPage === page ? 'outline' : 'ghost',
                                    size: 'icon',
                                })
                            )}
                        >
                            {page}
                        </button>
                    </li>
                ))}
                <li>
                    <button
                        onClick={() => onPageChange(currentPage + 1)}
                        disabled={currentPage === totalPages}
                        className={cn(
                            buttonVariants({variant: 'ghost', size: 'icon'}),
                            'gap-1 pr-2.5',
                            currentPage === totalPages && 'pointer-events-none opacity-50'
                        )}
                    >
                        <ChevronRight className="h-4 w-4"/>
                        <span className="sr-only">Вперед</span>
                    </button>
                </li>
            </ul>
        </nav>
    );
};

Pagination.displayName = 'Pagination';

export {Pagination};