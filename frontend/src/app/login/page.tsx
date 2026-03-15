'use client';

import {useState} from 'react';
import {useRouter} from 'next/navigation';
import {useForm} from 'react-hook-form';
import {zodResolver} from '@hookform/resolvers/zod';
import {z} from 'zod';
import {Car, Eye, EyeOff, Loader2} from 'lucide-react';
import {Button} from '@/components/ui/button';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle} from '@/components/ui/card';
import {Alert, AlertDescription} from '@/components/ui/alert';
import {useAuth} from '@/hooks/useAuth';
import {motion} from 'framer-motion';

const loginSchema = z.object({
    username: z.string().min(1, 'Логин обязателен'),
    password: z.string().min(1, 'Пароль обязателен'),
});

type LoginFormData = z.infer<typeof loginSchema>;

export default function LoginPage() {
    const router = useRouter();
    const {login} = useAuth();
    const [showPassword, setShowPassword] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [isLoading, setIsLoading] = useState(false);

    const {
        register,
        handleSubmit,
        formState: {errors},
    } = useForm<LoginFormData>({
        resolver: zodResolver(loginSchema),
    });

    const onSubmit = async (data: LoginFormData) => {
        setIsLoading(true);
        setError(null);

        try {
            await login(data.username, data.password);
            router.push('/cars');
        } catch (err) {
            setError('Неверный логин или пароль');
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div
            className="flex min-h-screen items-center justify-center bg-gradient-to-br from-primary/5 via-background to-primary/5 p-4">
            <motion.div
                initial={{opacity: 0, y: 20}}
                animate={{opacity: 1, y: 0}}
                transition={{duration: 0.5}}
                className="w-full max-w-md"
            >
                <Card className="border-none shadow-xl">
                    <CardHeader className="space-y-1 text-center">
                        <div className="flex justify-center mb-4">
                            <div className="rounded-full bg-primary/10 p-3">
                                <Car className="h-8 w-8 text-primary"/>
                            </div>
                        </div>
                        <CardTitle className="text-2xl font-bold">CarSensor</CardTitle>
                        <CardDescription>
                            Войдите в систему для просмотра автомобилей
                        </CardDescription>
                    </CardHeader>
                    <CardContent>
                        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
                            {error && (
                                <Alert variant="destructive">
                                    <AlertDescription>{error}</AlertDescription>
                                </Alert>
                            )}
                            <div className="space-y-2">
                                <Label htmlFor="username">Логин</Label>
                                <Input
                                    id="username"
                                    type="text"
                                    placeholder="Введите логин"
                                    {...register('username')}
                                    className={errors.username ? 'border-destructive' : ''}
                                />
                                {errors.username && (
                                    <p className="text-sm text-destructive">{errors.username.message}</p>
                                )}
                                <p className="text-xs text-muted-foreground">
                                    Используйте <span className="font-mono">admin</span> для входа
                                </p>
                            </div>
                            <div className="space-y-2">
                                <Label htmlFor="password">Пароль</Label>
                                <div className="relative">
                                    <Input
                                        id="password"
                                        type={showPassword ? 'text' : 'password'}
                                        placeholder="Введите пароль"
                                        {...register('password')}
                                        className={errors.password ? 'border-destructive pr-10' : 'pr-10'}
                                    />
                                    <Button
                                        type="button"
                                        variant="ghost"
                                        size="icon"
                                        className="absolute right-0 top-0 h-full px-3 py-2 hover:bg-transparent"
                                        onClick={() => setShowPassword(!showPassword)}
                                    >
                                        {showPassword ? (
                                            <EyeOff className="h-4 w-4 text-muted-foreground"/>
                                        ) : (
                                            <Eye className="h-4 w-4 text-muted-foreground"/>
                                        )}
                                    </Button>
                                </div>
                                {errors.password && (
                                    <p className="text-sm text-destructive">{errors.password.message}</p>
                                )}
                                <p className="text-xs text-muted-foreground">
                                    Пароль: <span className="font-mono">admin123</span>
                                </p>
                            </div>
                            <Button type="submit" className="w-full" disabled={isLoading}>
                                {isLoading ? (
                                    <>
                                        <Loader2 className="mr-2 h-4 w-4 animate-spin"/>
                                        Вход...
                                    </>
                                ) : (
                                    'Войти'
                                )}
                            </Button>
                        </form>
                    </CardContent>
                    <CardFooter className="flex flex-col space-y-2">
                        <p className="text-center text-xs text-muted-foreground">
                            Демо-доступ: admin / admin123
                        </p>
                    </CardFooter>
                </Card>
            </motion.div>
        </div>
    );
}