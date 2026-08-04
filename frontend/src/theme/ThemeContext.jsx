import { createContext, useContext, useEffect, useState } from "react";

const ThemeContext = createContext();

const themes = {
    light: "light",
    dark: "dark",
    green: "green",
    blue: "blue",
    neon: "neon"
};

export function ThemeProvider({ children }) {

    const [theme, setTheme] = useState(
        localStorage.getItem("theme") || "blue"
    );

    useEffect(() => {

        document.documentElement.setAttribute("data-theme", theme);

        localStorage.setItem("theme", theme);

    }, [theme]);

    return (

        <ThemeContext.Provider
            value={{
                theme,
                setTheme,
                themes
            }}
        >

            {children}

        </ThemeContext.Provider>

    );

}

export function useTheme() {

    return useContext(ThemeContext);

}