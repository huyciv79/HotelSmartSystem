import { useState } from "react";
import { Search, Bell } from "lucide-react";

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
    <header className="sticky top-0 left-72 w-full h-20 bg-[#0c0c0e]/90 backdrop-blur-md border-b border-neutral-900/40 shadow-sm z-10 flex items-center justify-between px-10 text-left">
      <div>
        <h2 className="text-white font-black text-base uppercase tracking-wider m-0">
          Elysian Hub
        </h2>
        <p className="text-slate-400 text-[9px] uppercase tracking-widest font-bold mt-1">
          {isManager ? "Hệ thống quản lý khách sạn thông minh" : "Bàn vận hành lễ tân Elysian"}
        </p>
      </div>

      <div className="flex items-center gap-8">
        <div className="relative">
          <Search
            size={15}
            className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-500"
          />
          <input
            type="text"
            placeholder="Tìm kiếm thông tin..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-64 pl-11 pr-4 py-2.5 bg-neutral-900/60 border border-neutral-800 rounded-none text-xs font-bold text-white outline-none placeholder:text-slate-500 focus:border-primary transition-all"
          />
        </div>

        <div className="flex items-center gap-4">
          <button className="relative p-2 text-slate-400 hover:text-white transition-colors cursor-pointer bg-transparent border-none">
            <Bell size={18} />
            <span className="absolute top-1.5 right-1.5 size-2 bg-primary rounded-full" />
          </button>

          <div className="flex items-center gap-3 pl-4 border-l border-neutral-800">
            <div className="text-right">
              <span className="text-white text-xs font-black block uppercase tracking-wide">
                {currentUser.fullName}
              </span>
              <span className="text-primary text-[8px] font-black uppercase tracking-widest block mt-0.5">
                {isManager ? "QUẢN LÝ" : "LỄ TÂN"}
              </span>
            </div>
            {hasAvatar ? (
              <img
                src={avatar}
                alt={currentUser.fullName}
                onError={() => setImgError(true)}
                className="w-10 h-10 rounded-full object-cover shadow-md border border-primary/20"
              />
            ) : (
              <div className="w-10 h-10 rounded-full bg-gradient-to-tr from-primary via-primary/80 to-rose-700 text-white flex items-center justify-center font-black text-xs tracking-wider shadow-md border border-primary/20">
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
