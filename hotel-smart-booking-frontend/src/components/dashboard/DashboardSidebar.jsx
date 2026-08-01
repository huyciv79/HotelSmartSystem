import { useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  LayoutDashboard,
  Building2,
  Plus,
  Settings,
  LogOut,
  ShieldCheck,
  Globe,
} from "lucide-react";
import { useLanguage } from "../../context/LanguageContext";

const navItems = [
  { key: "overview", label: "Overview", icon: LayoutDashboard },
  { key: "stays", label: "My Stays", icon: Building2 },
  { key: "ekyc", label: "Identity Verification", icon: ShieldCheck },
];

const bottomLinks = [
  { key: "settings", label: "Account Settings", icon: Settings },
  { key: "logout", label: "Logout", icon: LogOut },
];

const languagesList = [
  { code: 'VN', label: 'Tiếng Việt', flag: 'https://flagcdn.com/w40/vn.png' },
  { code: 'EN', label: 'English', flag: 'https://flagcdn.com/w40/us.png' },
  { code: 'JP', label: '日本語', flag: 'https://flagcdn.com/w40/jp.png' },
  { code: 'KR', label: '한국어', flag: 'https://flagcdn.com/w40/kr.png' },
  { code: 'CN', label: '简体中文', flag: 'https://flagcdn.com/w40/cn.png' }
];

const DashboardSidebar = ({
  activeItem: controlledActive,
  onNavigate,
  setActivePage,
}) => {
  const { language, setLanguage, t } = useLanguage();
  const navigate = useNavigate();
  const [internalActive, setInternalActive] = useState("overview");
  const [isLangOpen, setIsLangOpen] = useState(false);
  const activeItem = controlledActive ?? internalActive;

  const handleNavigate = (key) => {
    setInternalActive(key);
    onNavigate?.(key);
  };

  const currentLangObj = languagesList.find(l => l.code === language) || languagesList[0];

  const getLabelTranslation = (key, defaultLabel) => {
    switch (key) {
      case "overview": return t("db_sidebar_overview", "Tổng quan");
      case "stays": return t("db_sidebar_stays", "Đặt phòng của tôi");
      case "ekyc": return t("db_sidebar_ekyc", "Xác minh danh tính (eKYC)");
      case "settings": return t("db_sidebar_settings", "Cài đặt tài khoản");
      case "logout": return t("db_sidebar_logout", "Đăng xuất");
      default: return defaultLabel;
    }
  };

  return (
    <aside className="fixed left-0 top-0 bottom-0 w-72 bg-gradient-to-b from-[#141416] via-[#0d0d0f] to-[#070708] border-r border-neutral-900/50 flex flex-col z-20 font-['Montserrat'] shadow-[4px_0_24px_rgba(0,0,0,0.4)]">
      <div 
        onClick={() => {
          setActivePage?.("home");
          navigate("/");
        }}
        className="p-8 border-b border-neutral-900/40 cursor-pointer group"
      >
        <h1 className="text-white font-black text-2xl tracking-[0.2em] uppercase m-0 leading-none group-hover:text-primary transition-colors duration-300">
          ELYSIAN
        </h1>
        <span className="text-[7.5px] tracking-[0.35em] text-primary font-black uppercase leading-none mt-1.5 block">{t('nav_hotels_resorts_sub', 'HOTELS & RESORTS')}</span>
      </div>

      <nav className="flex-1 pt-10 px-4 flex flex-col gap-3">
        {navItems.map(({ key, label, icon: Icon }) => {
          const isActive = activeItem === key;
          return (
            <button
              key={key}
              onClick={() => handleNavigate(key)}
              className={`flex items-center gap-4 transition-all duration-200 cursor-pointer rounded-sm ${
                isActive
                  ? "pl-5 py-3.5 bg-gradient-to-r from-primary/15 to-transparent border-l-4 border-primary text-primary font-black uppercase tracking-widest text-[10.5px] shadow-[inset_4px_0_8px_rgba(162,5,19,0.08)]"
                  : "pl-5 py-3.5 text-slate-400 hover:text-white hover:bg-white/5 font-bold uppercase tracking-widest text-[10.5px]"
              }`}
            >
              <Icon size={17} className={isActive ? "text-primary" : "text-slate-400"} />
              <span>{getLabelTranslation(key, label)}</span>
            </button>
          );
        })}
      </nav>

      <div className="p-5 border-t border-neutral-900/30 flex flex-col gap-3">
        <button
          onClick={() => setActivePage?.("booking")}
          className="w-full py-4 bg-primary text-white font-black uppercase text-[10.5px] tracking-[0.15em] transition-all duration-300 cursor-pointer hover:bg-white hover:text-black hover:shadow-[0_0_20px_rgba(255,255,255,0.15)] flex items-center justify-center gap-2 parallelogram-btn border-none transform hover:-translate-y-0.5 active:translate-y-0 active:scale-98"
        >
          <Plus size={16} />
          <span>{t('db_sidebar_book_individual', 'ĐẶT CÁ NHÂN')}</span>
        </button>
        <button
          onClick={() => setActivePage?.("group-booking")}
          className="w-full py-4 bg-zinc-800 hover:bg-primary border border-zinc-700 hover:border-primary text-white font-black uppercase text-[10.5px] tracking-[0.15em] transition-all duration-300 cursor-pointer hover:shadow-[0_0_20px_rgba(162,5,19,0.15)] flex items-center justify-center gap-2 parallelogram-btn transform hover:-translate-y-0.5 active:translate-y-0 active:scale-98"
        >
          <Plus size={16} />
          <span>{t('db_sidebar_book_group', 'ĐẶT PHÒNG ĐOÀN')}</span>
        </button>
      </div>

      {/* Language Switcher Dropdown */}
      <div className="px-4 pt-4 pb-2 border-t border-neutral-900/30 relative">
        <button
          type="button"
          onClick={() => setIsLangOpen(!isLangOpen)}
          className="w-full flex items-center justify-between px-4 py-2.5 bg-white/5 hover:bg-white/10 text-slate-300 border border-white/10 text-[10.5px] font-bold uppercase tracking-widest transition-all cursor-pointer rounded-sm"
        >
          <div className="flex items-center gap-2.5">
            <img
              src={currentLangObj.flag}
              alt={currentLangObj.label}
              className="w-4 h-3 object-cover border border-white/20"
            />
            <span>{currentLangObj.label}</span>
          </div>
          <span className="material-symbols-outlined text-sm leading-none opacity-80">
            {isLangOpen ? 'expand_less' : 'expand_more'}
          </span>
        </button>

        {isLangOpen && (
          <div className="absolute left-4 right-4 bottom-14 bg-[#141416] border border-neutral-800 shadow-2xl z-50 py-1.5 font-['Montserrat'] rounded-sm">
            {languagesList.map((lang) => (
              <button
                key={lang.code}
                onClick={() => {
                  setLanguage(lang.code);
                  setIsLangOpen(false);
                }}
                className={`w-full flex items-center gap-3 px-4 py-2 text-left text-[10px] font-bold uppercase tracking-wider transition-colors hover:bg-white/10 border-none bg-transparent cursor-pointer ${
                  language === lang.code ? 'text-primary' : 'text-slate-300'
                }`}
              >
                <img
                  src={lang.flag}
                  alt={lang.label}
                  className="w-4 h-3 object-cover border border-white/10"
                />
                <span>{lang.label}</span>
              </button>
            ))}
          </div>
        )}
      </div>

      <div className="px-4 pb-8 flex flex-col gap-3 pt-2">
        {bottomLinks.map(({ key, label, icon: Icon }) => {
          const isActive = activeItem === key;
          return (
            <button
              key={key}
              onClick={() => {
                if (key === 'logout') {
                  onNavigate?.(key);
                  return;
                }
                handleNavigate(key);
              }}
              className={`flex items-center gap-4 transition-all duration-200 cursor-pointer rounded-sm ${
                isActive
                  ? "pl-5 py-3.5 bg-gradient-to-r from-primary/15 to-transparent border-l-4 border-primary text-primary font-black uppercase tracking-widest text-[10.5px] shadow-[inset_4px_0_8px_rgba(162,5,19,0.08)]"
                  : "pl-5 py-3.5 text-slate-400 hover:text-white hover:bg-white/5 font-bold uppercase tracking-widest text-[10.5px]"
              }`}
            >
              <Icon size={17} className={isActive ? "text-primary" : "text-slate-400"} />
              <span>{getLabelTranslation(key, label)}</span>
            </button>
          );
        })}
      </div>
    </aside>
  );
};

export default DashboardSidebar;

