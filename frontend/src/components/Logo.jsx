function Logo({ className = 'w-10 h-10' }) {
    return (
        <svg viewBox="0 0 48 48" className={className} fill="none" xmlns="http://www.w3.org/2000/svg">
            <defs>
                <linearGradient id="csdtGrad" x1="0" y1="0" x2="48" y2="48" gradientUnits="userSpaceOnUse">
                    <stop stopColor="var(--accent)" />
                    <stop offset="1" stopColor="var(--accent-2)" />
                </linearGradient>
            </defs>
            <rect width="48" height="48" rx="14" fill="url(#csdtGrad)" />
            <path d="M14 32V16h6.5c3.6 0 5.8 1.9 5.8 5v.1c0 2.2-1.1 3.6-2.9 4.3 2.2.6 3.6 2.2 3.6 4.7v.1c0 3.3-2.4 5.2-6.2 5.2H14v-3.4zm3.6-9.5h2.5c1.6 0 2.5-.7 2.5-2v-.1c0-1.3-.9-1.9-2.5-1.9h-2.5v4zm0 6.9h2.9c1.8 0 2.7-.8 2.7-2.1v-.1c0-1.3-1-2-2.8-2h-2.8v4.2z" fill="white" />
            <path d="M30 32V16h3.6v12.6H36V32h-6z" fill="white" opacity="0.85" />
        </svg>
    )
}
export default Logo