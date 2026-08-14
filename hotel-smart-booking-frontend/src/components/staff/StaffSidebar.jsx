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
    <aside className="fixed left-0 top-0 bottom-0 w-72 bg-white border-r border-slate-100 flex flex-col justify-between z-30 font-['Montserrat'] shadow-[4px_0_24px_rgba(0,0,0,0.015)]">
      <div className="flex-1 flex flex-col overflow-y-auto">
        {/* Header info */}
        <div className="p-6 border-b border-slate-100 flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-primary to-rose-600 flex items-center justify-center text-white shadow-[0_4px_16px_rgba(162,5,19,0.2)] shrink-0">
            <span className="material-symbols-outlined text-lg">hotel</span>
          </div>
          <div>
            <h2 className="text-base font-extrabold tracking-wide text-slate-800 m-0 leading-tight">The Iris Hub</h2>
            <span className="text-[9px] font-bold text-slate-400 uppercase tracking-widest block mt-0.5">Smart Booking</span>
          </div>
        </div>


        {/* Nav Menu */}
        <nav className="px-4 space-y-1">
          <button
            onClick={() => setActiveTab('overview')}
            className={`w-full py-3 px-4 text-xs font-bold uppercase tracking-widest border-none flex items-center gap-3 cursor-pointer transition-all duration-200 rounded-xl ${
              activeTab === 'overview'
                ? 'bg-primary/10 text-primary font-extrabold shadow-[0_4px_12px_rgba(162,5,19,0.03)]'
                : 'bg-transparent text-slate-500 hover:text-slate-900 hover:bg-slate-50'
            }`}
          >
            <span className="material-symbols-outlined text-base">dashboard</span>
            Tổng quan
          </button>

          <button
            onClick={() => setActiveTab('bookings')}
            className={`w-full py-3 px-4 text-xs font-bold uppercase tracking-widest border-none flex items-center gap-3 cursor-pointer transition-all duration-200 rounded-xl ${
              activeTab === 'bookings'
                ? 'bg-primary/10 text-primary font-extrabold shadow-[0_4px_12px_rgba(162,5,19,0.03)]'
                : 'bg-transparent text-slate-500 hover:text-slate-900 hover:bg-slate-50'
            }`}
          >
            <span className="material-symbols-outlined text-base">receipt_long</span>
            Quản lý đặt phòng
          </button>

          <button
            onClick={() => setActiveTab('qr-check-in')}
            className={`w-full py-3 px-4 text-xs font-bold uppercase tracking-widest border-none flex items-center gap-3 cursor-pointer transition-all duration-200 rounded-xl ${
              activeTab === 'qr-check-in'
                ? 'bg-primary/10 text-primary font-extrabold shadow-[0_4px_12px_rgba(162,5,19,0.03)]'
                : 'bg-transparent text-slate-500 hover:text-slate-900 hover:bg-slate-50'
            }`}
          >
            <span className="material-symbols-outlined text-base">qr_code_scanner</span>
            QR Check-in
          </button>

          <button
            onClick={() => setActiveTab('face-check-in')}
            className={`w-full py-3 px-4 text-xs font-bold uppercase tracking-widest border-none flex items-center gap-3 cursor-pointer transition-all duration-200 rounded-xl ${
              activeTab === 'face-check-in'
                ? 'bg-primary/10 text-primary font-extrabold shadow-[0_4px_12px_rgba(162,5,19,0.03)]'
                : 'bg-transparent text-slate-500 hover:text-slate-900 hover:bg-slate-50'
            }`}
          >
            <span className="material-symbols-outlined text-base">face</span>
            Check-in khuôn mặt
          </button>

          {isManager && (
            <>
              <button
                onClick={() => setActiveTab('rooms')}
                className={`w-full py-3 px-4 text-xs font-bold uppercase tracking-widest border-none flex items-center gap-3 cursor-pointer transition-all duration-200 rounded-xl ${
                  activeTab === 'rooms'
                    ? 'bg-primary/10 text-primary font-extrabold shadow-[0_4px_12px_rgba(162,5,19,0.03)]'
                    : 'bg-transparent text-slate-500 hover:text-slate-900 hover:bg-slate-50'
                }`}
              >
                <span className="material-symbols-outlined text-base">meeting_room</span>
                Quản lý loại phòng
              </button>

              <button
                onClick={() => setActiveTab('rooms-list')}
                className={`w-full py-3 px-4 text-xs font-bold uppercase tracking-widest border-none flex items-center gap-3 cursor-pointer transition-all duration-200 rounded-xl ${
                  activeTab === 'rooms-list'
                    ? 'bg-primary/10 text-primary font-extrabold shadow-[0_4px_12px_rgba(162,5,19,0.03)]'
                    : 'bg-transparent text-slate-500 hover:text-slate-900 hover:bg-slate-50'
                }`}
              >
                <span className="material-symbols-outlined text-base">bedroom_child</span>
                Quản lý phòng
              </button>

              <button
                onClick={() => setActiveTab('reports')}
                className={`w-full py-3 px-4 text-xs font-bold uppercase tracking-widest border-none flex items-center gap-3 cursor-pointer transition-all duration-200 rounded-xl ${
                  activeTab === 'reports'
                    ? 'bg-primary/10 text-primary font-extrabold shadow-[0_4px_12px_rgba(162,5,19,0.03)]'
                    : 'bg-transparent text-slate-500 hover:text-slate-900 hover:bg-slate-50'
                }`}
              >
                <span className="material-symbols-outlined text-base">query_stats</span>
                Báo cáo doanh thu
              </button>
            </>
          )}

          <button
            onClick={() => setActiveTab('services')}
            className={`w-full py-3 px-4 text-xs font-bold uppercase tracking-widest border-none flex items-center gap-3 cursor-pointer transition-all duration-200 rounded-xl ${
              activeTab === 'services'
                ? 'bg-primary/10 text-primary font-extrabold shadow-[0_4px_12px_rgba(162,5,19,0.03)]'
                : 'bg-transparent text-slate-500 hover:text-slate-900 hover:bg-slate-50'
            }`}
          >
            <span className="material-symbols-outlined text-base">room_service</span>
            Quản lý dịch vụ
          </button>


        </nav>
      </div>

      {/* Bottom widgets */}
      <div className="flex flex-col shrink-0">


        {/* Logout section */}
        <div className="p-4 border-t border-slate-100/50">
          <button
            onClick={handleLogout}
            className="w-full py-3 bg-transparent hover:bg-rose-50/50 text-slate-500 hover:text-rose-600 font-extrabold uppercase text-[10px] tracking-widest transition-all duration-300 cursor-pointer flex items-center justify-center gap-2 border border-dashed border-slate-200 hover:border-solid hover:border-rose-200 rounded-xl"
          >
            <span className="material-symbols-outlined text-base">logout</span>
            Đăng xuất
          </button>
        </div>
      </div>
    </aside>
  );
};

export default StaffSidebar;
