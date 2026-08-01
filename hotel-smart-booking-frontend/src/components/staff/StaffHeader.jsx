import { useState } from "react";
import { Search } from "lucide-react";
import NotificationDropdown from "../NotificationDropdown";
import { useLanguage } from "../../context/LanguageContext";

const languagesList = [
  { code: 'VN', label: 'Tiếng Việt', flag: 'https://flagcdn.com/w40/vn.png' },
  { code: 'EN', label: 'English', flag: 'https://flagcdn.com/w40/us.png' },
  { code: 'JP', label: '日本語', flag: 'https://flagcdn.com/w40/jp.png' },
  { code: 'KR', label: '한국어', flag: 'https://flagcdn.com/w40/kr.png' },
  { code: 'CN', label: '简体中文', flag: 'https://flagcdn.com/w40/cn.png' }
];

const getInitials = (name) => {
  if (!name) return 'EH';
  const cleanName = name.trim().replace(/\s+/g, ' ');
  const parts = cleanName.split(' ');
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
};

const StaffHeader = ({ currentUser, isManager, searchQuery, setSearchQuery }) => {
  const { language, setLanguage, t } = useLanguage();
  const [imgError, setImgError] = useState(false);
  const [isLangOpen, setIsLangOpen] = useState(false);
  const avatar = currentUser.avatar || currentUser.avatarUrl;
  const hasAvatar = avatar && avatar !== 'null' && avatar !== 'undefined' && !imgError;
  const currentLangObj = languagesList.find(l => l.code === language) || languagesList[0];

  return (
    <header className="sticky top-0 left-72 w-full h-20 bg-white/95 backdrop-blur-md border-b border-slate-100 shadow-[0_1px_3px_rgba(0,0,0,0.01)] z-10 flex items-center justify-between px-10 text-left">
      <div>
        <h2 className="text-slate-800 font-extrabold text-base uppercase tracking-wider m-0 leading-tight">
          Elysian Hub
        </h2>
        <p className="text-slate-400 text-[9px] uppercase tracking-widest font-bold mt-1">
          {isManager ? "Hệ thống quản lý khách sạn thông minh" : "Bàn vận hành lễ tân Elysian"}
        </p>
      </div>

      <div className="flex items-center gap-8">
        <div className="relative flex items-center">
          <Search
            size={14}
            className="absolute left-4 text-slate-400"
          />
          <input
            type="text"
            placeholder="Tìm kiếm thông tin..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-72 pl-11 pr-12 py-2 bg-slate-50 border border-slate-200/50 rounded-full text-xs font-semibold text-slate-800 outline-none placeholder:text-slate-400 focus:border-primary/30 focus:bg-white transition-all shadow-inner"
          />
          <span className="absolute right-4 text-[9px] font-bold text-slate-400 bg-white border border-slate-200/60 px-1.5 py-0.5 rounded-md pointer-events-none select-none shadow-sm font-mono">
            ⌘K
          </span>
        </div>

        <div className="flex items-center gap-4">
          {/* Language Switcher Dropdown */}
          <div className="relative">
            <button
              type="button"
              onClick={() => setIsLangOpen(!isLangOpen)}
              className="flex items-center gap-1.5 px-2.5 py-1.5 bg-slate-100 hover:bg-slate-200/80 text-slate-700 text-[10px] font-bold uppercase tracking-wider transition-colors cursor-pointer border border-slate-200 rounded-md"
            >
              <img
                src={currentLangObj.flag}
                alt={currentLangObj.label}
                className="w-4 h-3 object-cover border border-slate-300"
              />
              <span>{currentLangObj.code}</span>
              <span className="material-symbols-outlined text-xs leading-none opacity-70">arrow_drop_down</span>
            </button>

            {isLangOpen && (
              <div className="absolute right-0 top-9 w-40 bg-white border border-slate-200 shadow-xl z-50 py-1 font-['Montserrat'] rounded-md">
                {languagesList.map((lang) => (
                  <button
                    key={lang.code}
                    onClick={() => {
                      setLanguage(lang.code);
                      setIsLangOpen(false);
                    }}
                    className={`w-full flex items-center gap-3 px-4 py-2 text-left text-[10px] font-bold uppercase tracking-wider transition-colors hover:bg-slate-50 border-none bg-transparent cursor-pointer ${
                      language === lang.code ? 'text-primary font-black bg-slate-50' : 'text-slate-700'
                    }`}
                  >
                    <img
                      src={lang.flag}
                      alt={lang.label}
                      className="w-4 h-3 object-cover border border-slate-200"
                    />
                    <span>{lang.label}</span>
                  </button>
                ))}
              </div>
            )}
          </div>

          <NotificationDropdown dark={false} />

          <div className="flex items-center gap-3 pl-4 border-l border-slate-150">
            <div className="text-right">
              <span className="text-slate-800 text-xs font-bold block uppercase tracking-wide">
                {currentUser.fullName}
              </span>
              <span className="text-primary text-[8px] font-extrabold uppercase tracking-widest block mt-0.5">
                {isManager ? "QUẢN LÝ" : "LỄ TÂN"}
              </span>
            </div>
            {hasAvatar ? (
              <img
                src={avatar}
                alt={currentUser.fullName}
                onError={() => setImgError(true)}
                className="w-10 h-10 rounded-full object-cover shadow-sm border border-slate-200/50"
              />
            ) : (
              <div className="w-10 h-10 rounded-full bg-gradient-to-tr from-primary via-primary/80 to-rose-700 text-white flex items-center justify-center font-black text-xs tracking-wider shadow-sm border border-slate-200/50">
                {getInitials(currentUser.fullName)}
              </div>
            )}
          </div>
        </div>
      </div>
    </header>
  );
};

export default StaffHeader;
