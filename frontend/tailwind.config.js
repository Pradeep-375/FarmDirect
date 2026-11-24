/** Breakpoints follow the project spec: 481 large phones, 768 tablets, 1024 laptops, 1440 large desktops. */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    screens: { sm: '481px', md: '768px', lg: '1024px', xl: '1440px' },
    extend: {
      colors: {
        field: { 50: '#f1f8f2', 100: '#dcefdf', 200: '#b9dfc0', 300: '#8cc89a', 400: '#5aab72', 500: '#368d51', 600: '#277241', 700: '#1f5b36', 800: '#1a482c', 900: '#143a24' },
        wheat: { 100: '#fbf0cf', 300: '#f1d072', 500: '#e0aa2b', 700: '#a97a14' },
        soil: { 700: '#5a3e2b', 900: '#2e2118' },
      },
      fontFamily: { display: ['Fraunces', 'Georgia', 'serif'], sans: ['"Public Sans"', 'system-ui', 'sans-serif'] },
    },
  },
  plugins: [],
};
