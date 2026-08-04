/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  darkMode: 'class',
  theme: {
    extend: {
      fontFamily: {
        display: ['"Space Grotesk"', 'sans-serif'],
        body: ['"Inter"', 'sans-serif'],
        mono: ['"IBM Plex Mono"', 'monospace'],
      },
      colors: {
        bg: 'rgb(var(--c-bg) / <alpha-value>)',
        bgSunken: 'rgb(var(--c-bg-sunken) / <alpha-value>)',
        surface: 'rgb(var(--c-surface) / <alpha-value>)',
        surfaceHover: 'rgb(var(--c-surface-hover) / <alpha-value>)',
        borderc: 'rgb(var(--c-border) / <alpha-value>)',
        textPrimary: 'rgb(var(--c-text-primary) / <alpha-value>)',
        textSecondary: 'rgb(var(--c-text-secondary) / <alpha-value>)',
        textMuted: 'rgb(var(--c-text-muted) / <alpha-value>)',
        accent: 'rgb(var(--c-accent) / <alpha-value>)',
        accent2: 'rgb(var(--c-accent-2) / <alpha-value>)',
        success: 'rgb(var(--c-success) / <alpha-value>)',
        warning: 'rgb(var(--c-warning) / <alpha-value>)',
        danger: 'rgb(var(--c-danger) / <alpha-value>)',
        info: 'rgb(var(--c-info) / <alpha-value>)',
      },
      boxShadow: {
        card: '0 1px 2px rgb(0 0 0 / 0.06), 0 1px 8px rgb(0 0 0 / 0.04)',
        elevated: '0 8px 30px rgb(0 0 0 / 0.16)',
        glow: '0 0 24px rgb(var(--c-accent-2) / 0.35)',
      },
      keyframes: {
        curve: {
          '0%': { strokeDashoffset: '240' },
          '100%': { strokeDashoffset: '0' },
        },
        riseIn: {
          '0%': { opacity: '0', transform: 'translateY(8px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        pulseSoft: {
          '0%, 100%': { opacity: '1' },
          '50%': { opacity: '0.45' },
        },
        shimmer: {
          '0%': { backgroundPosition: '-400px 0' },
          '100%': { backgroundPosition: '400px 0' },
        },
      },
      animation: {
        curve: 'curve 1.8s ease-out forwards',
        riseIn: 'riseIn 0.5s cubic-bezier(0.16,1,0.3,1) both',
        pulseSoft: 'pulseSoft 2.4s ease-in-out infinite',
        shimmer: 'shimmer 1.6s linear infinite',
      },
    },
  },
  plugins: [],
}
