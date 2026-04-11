/** @type {import('next').NextConfig} */
const nextConfig = {
    reactStrictMode: true,
    output: 'standalone',
    images: {
        remotePatterns: [
            {
                protocol: 'https',
                hostname: '**.carsensor.net',
            },
            {
                protocol: 'https',
                hostname: 'via.placeholder.com',
            },
        ],
    },
    compiler: {
        removeConsole: process.env.NODE_ENV === 'production',
    },
    async rewrites() {
        return [
            {
                source: '/api/:path*',
                destination: 'http://carsensor-gateway:8080/api/:path*',
            },
        ];
    },
};

module.exports = nextConfig;