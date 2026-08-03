import { useState } from "react";
import { useNavigate } from "react-router-dom";
import hotelLogo from "../../assets/hotel_logo.png";
import {
  LayoutDashboard,
  Building2,
  Plus,
  Settings,
  LogOut,
  ShieldCheck,
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

const DashboardSidebar = ({
  activeItem: controlledActive,
  onNavigate,
  setActivePage,
  isOpen = false,
  onClose,
}) => {
  const { t } = useLanguage();
  const navigate = useNavigate();
  const [internalActive, setInternalActive] = useState("overview");
  const activeItem = controlledActive ?? internalActive;

  const handleNavigate = (key) => {
    setInternalActive(key);
    onNavigate?.(key);
    onClose?.();
  };

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
    <>
      {/* Mobile Overlay Backdrop */}
      {isOpen && (
        <div 
          onClick={onClose}
          className="fixed inset-0 bg-black/65 backdrop-blur-xs z-40 md:hidden transition-opacity duration-300"
        />
      )}

      <aside className={`fixed left-0 top-0 bottom-0 w-72 bg-gradient-to-b from-[#141416] via-[#0d0d0f] to-[#070708] border-r border-neutral-900/50 flex flex-col z-50 font-['Montserrat'] shadow-[4px_0_24px_rgba(0,0,0,0.4)] transition-transform duration-300 ease-in-out ${
        isOpen ? 'translate-x-0' : '-translate-x-full md:translate-x-0'
      }`}>
        <div 
          onClick={() => {
            setActivePage?.("home");
            navigate("/");
            onClose?.();
          }}
          className="p-8 border-b border-neutral-900/40 cursor-pointer group flex justify-between items-center"
        >
          <div>
            <h1 className="text-white font-black text-2xl tracking-[0.2em] uppercase m-0 leading-none group-hover:text-primary transition-colors duration-300">
              ELYSIAN
            </h1>
            <span className="text-[7.5px] tracking-[0.35em] text-primary font-black uppercase leading-none mt-1.5 block">{t('nav_hotels_resorts_sub', 'HOTELS & RESORTS')}</span>
          </div>
          <button 
            onClick={(e) => {
              e.stopPropagation();
              onClose?.();
            }}
            className="md:hidden text-slate-400 hover:text-white p-1"
          >
            <span className="material-symbols-outlined text-2xl">close</span>
          </button>
        </div>

        <nav className="flex-1 pt-6 px-4 flex flex-col gap-2 overflow-y-auto">
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

        <div className="p-4 border-t border-neutral-900/30 flex flex-col gap-2.5">
          <button
            onClick={() => {
              setActivePage?.("booking");
              onClose?.();
            }}
            className="w-full py-3.5 bg-primary text-white font-black uppercase text-[10.5px] tracking-[0.15em] transition-all duration-300 cursor-pointer hover:bg-white hover:text-black hover:shadow-[0_0_20px_rgba(255,255,255,0.15)] flex items-center justify-center gap-2 parallelogram-btn border-none transform hover:-translate-y-0.5 active:translate-y-0 active:scale-98"
          >
            <Plus size={16} />
            <span>{t('db_sidebar_book_individual', 'ĐẶT CÁ NHÂN')}</span>
          </button>
          <button
            onClick={() => {
              setActivePage?.("group-booking");
              onClose?.();
            }}
            className="w-full py-3.5 bg-zinc-800 hover:bg-primary border border-zinc-700 hover:border-primary text-white font-black uppercase text-[10.5px] tracking-[0.15em] transition-all duration-300 cursor-pointer hover:shadow-[0_0_20px_rgba(162,5,19,0.15)] flex items-center justify-center gap-2 parallelogram-btn transform hover:-translate-y-0.5 active:translate-y-0 active:scale-98"
          >
            <Plus size={16} />
            <span>{t('db_sidebar_book_group', 'ĐẶT PHÒNG ĐOÀN')}</span>
          </button>
        </div>

        <div className="px-4 pb-6 flex flex-col gap-2 border-t border-neutral-900/20 pt-4">
          {bottomLinks.map(({ key, label, icon: Icon }) => {
            const isActive = activeItem === key;
            return (
              <button
                key={key}
                onClick={() => {
                  if (key === 'logout') {
                    onNavigate?.(key);
                    onClose?.();
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
    </>
  );
};

export default DashboardSidebar;

