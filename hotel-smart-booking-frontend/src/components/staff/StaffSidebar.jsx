import { useState } from 'react';

const getInitials = (name) => {
  if (!name) return 'EH';
  const cleanName = name.trim().replace(/\s+/g, ' ');
  const parts = cleanName.split(' ');
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
};

const StaffSidebar = ({ activeTab, setActiveTab, currentUser, isManager, handleLogout }) => {
  const [imgError, setImgError] = useState(false);
  const avatar = currentUser.avatar || currentUser.avatarUrl;
  const hasAvatar = avatar && avatar !== 'null' && avatar !== 'undefined' && !imgError;

  return (
    <aside className="fixed left-0 top-0 bottom-0 w-72 bg-gradient-to-b from-[#141416] via-[#0d0d0f] to-[#070708] border-r border-neutral-900/50 flex flex-col justify-between z-30 font-['Montserrat'] shadow-[4px_0_24px_rgba(0,0,0,0.4)]">
      <div>
        {/* Header info */}
        <div className="p-8 border-b border-neutral-900/40 bg-neutral-950/20">
          <span className="text-[9px] font-black tracking-[0.25em] text-primary uppercase block mb-1">TRANG QUẢN TRỊ</span>
          <h2 className="text-lg font-black uppercase tracking-wider text-white m-0">ELYSIAN HUB</h2>

          <div className="flex items-center gap-3 mt-6">
            {hasAvatar ? (
              <img
                src={avatar}
                alt={currentUser.fullName}
                onError={() => setImgError(true)}
                className="w-11 h-11 rounded-full object-cover shadow-lg border border-primary/20"
              />
            ) : (
              <div className="w-11 h-11 rounded-full bg-gradient-to-tr from-primary via-primary/80 to-rose-700 text-white flex items-center justify-center font-black text-sm tracking-wider shadow-lg border border-primary/20">
                {getInitials(currentUser.fullName)}
              </div>
            )}
            <div className="min-w-0">
              <p className="text-xs font-black text-white truncate m-0 uppercase tracking-wide">{currentUser.fullName}</p>
              <span className="inline-block mt-1 px-2.5 py-0.5 text-[8px] font-black tracking-widest text-primary bg-primary/10 uppercase border border-primary/20">
                {isManager ? 'QUẢN LÝ' : 'LỄ TÂN'}
              </span>
            </div>
          </div>
        </div>

        {/* Nav Menu */}
        <nav className="p-4 pt-8 space-y-2">
          <button
            onClick={() => setActiveTab('overview')}
            className={`w-full py-3.5 px-4 text-xs font-bold uppercase tracking-widest border-none flex items-center gap-3 cursor-pointer transition-all duration-200 rounded-sm ${
              activeTab === 'overview'
                ? 'bg-gradient-to-r from-primary/15 to-transparent border-l-4 border-primary text-primary font-black'
                : 'bg-transparent text-slate-400 hover:text-white hover:bg-white/5'
            }`}
          >
            <span className="material-symbols-outlined text-base">dashboard</span>
            Tổng quan
          </button>



          <button
            onClick={() => setActiveTab('bookings')}
            className={`w-full py-2 px-3 text-[11px] font-bold uppercase tracking-widest border-none flex items-center gap-3 cursor-pointer transition-all duration-200 rounded-sm ${activeTab === 'bookings'
                ? 'bg-gradient-to-r from-primary/15 to-transparent border-l-4 border-primary text-primary font-black'
                : 'bg-transparent text-slate-400 hover:text-white hover:bg-white/5'
              }`}
          >
            <span className="material-symbols-outlined text-base">receipt_long</span>
            Quản lý đặt phòng
          </button>


          <button
            onClick={() => setActiveTab('face-check-in')}
            className={`w-full py-3.5 px-4 text-xs font-bold uppercase tracking-widest border-none flex items-center gap-3 cursor-pointer transition-all duration-200 rounded-sm ${
              activeTab === 'face-check-in'
                ? 'bg-gradient-to-r from-primary/15 to-transparent border-l-4 border-primary text-primary font-black'
                : 'bg-transparent text-slate-400 hover:text-white hover:bg-white/5'
            }`}
          >
            <span className="material-symbols-outlined text-base">face</span>
            FaceID Check-in
          </button>

          {isManager && (
            <>
              <button
                onClick={() => setActiveTab('rooms')}
                className={`w-full py-3.5 px-4 text-xs font-bold uppercase tracking-widest border-none flex items-center gap-3 cursor-pointer transition-all duration-200 rounded-sm ${
                  activeTab === 'rooms'
                    ? 'bg-gradient-to-r from-primary/15 to-transparent border-l-4 border-primary text-primary font-black'
                    : 'bg-transparent text-slate-400 hover:text-white hover:bg-white/5'
                }`}
              >
                <span className="material-symbols-outlined text-base">meeting_room</span>
                Quản lý loại phòng
              </button>

              <button
                onClick={() => setActiveTab('rooms-list')}
                className={`w-full py-3.5 px-4 text-xs font-bold uppercase tracking-widest border-none flex items-center gap-3 cursor-pointer transition-all duration-200 rounded-sm ${
                  activeTab === 'rooms-list'
                    ? 'bg-gradient-to-r from-primary/15 to-transparent border-l-4 border-primary text-primary font-black'
                    : 'bg-transparent text-slate-400 hover:text-white hover:bg-white/5'
                }`}
              >
                <span className="material-symbols-outlined text-base">bedroom_child</span>
                Quản lý phòng
              </button>

              <button
                onClick={() => setActiveTab('reports')}
                className={`w-full py-3.5 px-4 text-xs font-bold uppercase tracking-widest border-none flex items-center gap-3 cursor-pointer transition-all duration-200 rounded-sm ${
                  activeTab === 'reports'
                    ? 'bg-gradient-to-r from-primary/15 to-transparent border-l-4 border-primary text-primary font-black'
                    : 'bg-transparent text-slate-400 hover:text-white hover:bg-white/5'
                }`}
              >
                <span className="material-symbols-outlined text-base">query_stats</span>
                Báo cáo doanh thu
              </button>
            </>
          )}

          <button
            onClick={() => setActiveTab('settings')}
            className={`w-full py-3.5 px-4 text-xs font-bold uppercase tracking-widest border-none flex items-center gap-3 cursor-pointer transition-all duration-200 rounded-sm ${
              activeTab === 'settings'
                ? 'bg-gradient-to-r from-primary/15 to-transparent border-l-4 border-primary text-primary font-black'
                : 'bg-transparent text-slate-400 hover:text-white hover:bg-white/5'
            }`}
          >
            <span className="material-symbols-outlined text-base">manage_accounts</span>
            Hồ sơ cá nhân
          </button>
        </nav>
      </div>

      {/* Bottom logout */}
      <div className="p-5 border-t border-neutral-900/30">
        <button
          onClick={handleLogout}
          className="w-full py-4 bg-transparent hover:bg-primary text-slate-400 hover:text-white font-black uppercase text-[10.5px] tracking-[0.15em] transition-all duration-300 cursor-pointer flex items-center justify-center gap-2 border border-dashed border-neutral-800 hover:border-solid hover:border-primary"
        >
          <span className="material-symbols-outlined text-base">logout</span>
          Đăng xuất
        </button>
      </div>
    </aside>
  );
};

export default StaffSidebar;
