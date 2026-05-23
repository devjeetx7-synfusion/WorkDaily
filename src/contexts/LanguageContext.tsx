import { createContext, useContext, ReactNode } from "react";
import { useTranslation } from "react-i18next";

const LANGUAGE_NAMES: Record<string, string> = {
  en: "English", hi: "हिन्दी", bn: "বাংলা", pa: "ਪੰਜਾਬੀ",
  mr: "मराठी", ta: "தமிழ்", te: "తెలుగు", gu: "ગુજરાતી",
  kn: "ಕನ್ನಡ", ml: "മലയാളം",
};

interface LanguageContextType {
  lang: string;
  setLang: (l: string) => void;
  t: (key: string) => string;
  languages: typeof LANGUAGE_NAMES;
}

const LanguageContext = createContext<LanguageContextType | null>(null);

export const useLanguage = () => {
  const ctx = useContext(LanguageContext);
  if (!ctx) throw new Error("useLanguage must be used within LanguageProvider");
  return ctx;
};

export const LanguageProvider = ({ children }: { children: ReactNode }) => {
  const { i18n, t } = useTranslation();

  const setLang = (l: string) => {
    i18n.changeLanguage(l);
  };

  return (
    <LanguageContext.Provider value={{ lang: i18n.language, setLang, t: t as unknown as (key: string) => string, languages: LANGUAGE_NAMES }}>
      {children}
    </LanguageContext.Provider>
  );
};
