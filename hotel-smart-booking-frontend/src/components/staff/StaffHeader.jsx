import { useState } from "react";
import { Search } from "lucide-react";
import NotificationDropdown from "../NotificationDropdown";

const getInitials = (name) => {
  if (!name) return 'EH';
  const cleanName = name.trim().replace(/\s+/g, ' ');
  const parts = cleanName.split(' ');
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
};

const StaffHeader = ({ currentUser, isManager, searchQuery, setSearchQuery }) => {
  const [imgError, setImgError] = useState(false);
  const avatar = currentUser.avatar || currentUser.avatarUrl;
  const hasAvatar = avatar && avatar !== 'null' && avatar !== 'undefined' && !imgError;

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
