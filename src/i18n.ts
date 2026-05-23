import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import LanguageDetector from 'i18next-browser-languagedetector';

// Import JSON translation files
import en from './locales/en.json';
import hi from './locales/hi.json';
import bn from './locales/bn.json';
import te from './locales/te.json';
import mr from './locales/mr.json';
import ta from './locales/ta.json';
import pa from './locales/pa.json';
import gu from './locales/gu.json';
import kn from './locales/kn.json';
import ml from './locales/ml.json';

// Group resources
const resources = {
  en: { translation: en },
  hi: { translation: hi },
  bn: { translation: bn },
  te: { translation: te },
  mr: { translation: mr },
  ta: { translation: ta },
  pa: { translation: pa },
  gu: { translation: gu },
  kn: { translation: kn },
  ml: { translation: ml },
};

i18n
  // Detects user language dynamically (checks localStorage -> navigator.language)
  .use(LanguageDetector)
  // Passes i18n instance to react-i18next
  .use(initReactI18next)
  .init({
    resources,
    fallbackLng: 'en', // Fallback language
    supportedLngs: ['en', 'hi', 'bn', 'te', 'mr', 'ta', 'pa', 'gu', 'kn', 'ml'],
    interpolation: {
      escapeValue: false, // React already escapes values
    },
    detection: {
      order: ['localStorage', 'navigator'],
      caches: ['localStorage'], // Saves the user's choice to localStorage under 'i18nextLng'
      lookupLocalStorage: 'lang', // Override default to match existing legacy 'lang' key just in case
    }
  });

export default i18n;
