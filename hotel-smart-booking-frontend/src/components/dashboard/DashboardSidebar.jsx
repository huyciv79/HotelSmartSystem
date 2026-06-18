import { useState } from "react";
import {
  LayoutDashboard,
  Building2,
  Plus,
  Settings,
  LogOut,
  ShieldCheck,
} from "lucide-react";

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
}) => {
  const [internalActive, setInternalActive] = useState("overview");
  const activeItem = controlledActive ?? internalActive;

  const handleNavigate = (key) => {
    setInternalActive(key);
    onNavigate?.(key);
  };

  return (
    <aside className="fixed left-0 top-0 bottom-0 w-72 bg-gradient-to-b from-[#141416] via-[#0d0d0f] to-[#070708] border-r border-neutral-900/50 flex flex-col z-20 font-['Montserrat'] shadow-[4px_0_24px_rgba(0,0,0,0.4)]">
      <div 
        onClick={() => handleNavigate("overview")}
        className="p-8 border-b border-neutral-900/40 cursor-pointer group"
      >
        <h1 className="text-white font-black text-2xl tracking-[0.2em] uppercase m-0 leading-none group-hover:text-primary transition-colors duration-300">
          ELYSIAN
        </h1>
        <span className="text-[7.5px] tracking-[0.35em] text-primary font-black uppercase leading-none mt-1.5 block">HOTELS & RESORTS</span>
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
              <span>{label === "Overview" ? "Tổng quan" : label === "My Stays" ? "Đặt phòng của tôi" : label === "Identity Verification" ? "Xác minh danh tính (eKYC)" : label}</span>
            </button>
          );
        })}
      </nav>

      <div className="p-5 border-t border-neutral-900/30">
        <button
          onClick={() => setActivePage?.("booking")}
          className="w-full py-4 bg-primary text-white font-black uppercase text-[10.5px] tracking-[0.15em] transition-all duration-300 cursor-pointer hover:bg-white hover:text-black hover:shadow-[0_0_20px_rgba(255,255,255,0.15)] flex items-center justify-center gap-2 parallelogram-btn border-none transform hover:-translate-y-0.5 active:translate-y-0 active:scale-98"
        >
          <Plus size={16} />
          <span>ĐẶT PHÒNG MỚI</span>
        </button>
      </div>

      <div className="px-4 pb-8 flex flex-col gap-3 border-t border-neutral-900/20 pt-6">
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
              <span>{label === "Account Settings" ? "Cài đặt tài khoản" : label === "Logout" ? "Đăng xuất" : label}</span>
            </button>
          );
        })}
      </div>
    </aside>
  );
};

export default DashboardSidebar;

