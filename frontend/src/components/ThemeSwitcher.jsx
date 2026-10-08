import { useTheme } from '../context/ThemeContext'

const SWATCH = {
    light: '#ffffff',
    orange: '#ff6b00',
    navy: '#0b1120',
    purple: '#9333ea',
    green: '#16a34a',
}

function ThemeSwitcher() {
    const { theme, setTheme, THEMES } = useTheme()

    return (
        <div className="flex gap-1.5">
            {THEMES.map((t) => (
                <button
                    key={t}
                    onClick={() => setTheme(t)}
                    title={t}
                    className={`w-5 h-5 rounded-full border-2 btn-press transition-all ${
                        theme === t ? 'border-white scale-110' : 'border-transparent opacity-50 hover:opacity-100'
                    }`}
                    style={{
                        backgroundColor: SWATCH[t],
                        boxShadow: theme === t
                            ? `0 0 8px ${SWATCH[t]}, 0 0 14px ${SWATCH[t]}`
                            : 'none',
                    }}
                />
            ))}
        </div>
    )
}

export default ThemeSwitcher