import { useState } from "react";
import {
  LayoutDashboard,
  Building2,
  SlidersHorizontal,
  Award,
  Mail,
  LifeBuoy,
  Plus,
  Settings,
  LogOut,
} from "lucide-react";

const navItems = [
  { key: "overview", label: "Overview", icon: LayoutDashboard },
  { key: "stays", label: "My Stays", icon: Building2 },
  { key: "preferences", label: "Preferences", icon: SlidersHorizontal },
  { key: "rewards", label: "Rewards", icon: Award },
  { key: "messages", label: "Messages", icon: Mail },
  { key: "support", label: "Support", icon: LifeBuoy },
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
    <aside className="fixed left-0 top-0 bottom-0 w-72 bg-slate-900 flex flex-col z-20">
      <div className="p-8">
        <h1 className="text-amber-200 font-bold text-xl font-['Playfair_Display']">
          Elysian Kenther
        </h1>
        <p className="text-slate-500 uppercase tracking-widest text-xs font-['Geist'] mt-1">
          Luxury Concierge
        </p>
      </div>

      <nav className="flex-1 pt-8 px-4 flex flex-col gap-2">
        {navItems.map(({ key, label, icon: Icon }) => {
          const isActive = activeItem === key;
          return (
            <button
              key={key}
              onClick={() => handleNavigate(key)}
              className={`flex items-center gap-3 text-sm font-['Geist'] transition-colors cursor-pointer ${
                isActive
                  ? "pl-4 py-3 bg-gray-700/20 border-l-4 border-amber-200 text-amber-200"
                  : "pl-5 py-3 text-slate-500/70 hover:text-slate-400"
              }`}
            >
              <Icon size={18} />
              <span>{label}</span>
            </button>
          );
        })}
      </nav>

      <div className="p-4">
        <button
          onClick={() => setActivePage?.("home")}
          className="w-full py-4 bg-amber-200 rounded-xl flex items-center justify-center gap-2 cursor-pointer hover:bg-amber-300 transition-colors"
        >
          <Plus size={18} className="text-lime-950" />
          <span className="text-lime-950 font-bold font-['Geist']">
            Book New Stay
          </span>
        </button>
      </div>

      <div className="px-4 pb-8 flex flex-col gap-2">
        {bottomLinks.map(({ key, label, icon: Icon }) => (
          <button
            key={key}
            onClick={() => {
              if (key === 'logout') {
                onNavigate?.(key);
                return;
              }
              handleNavigate(key);
            }}
            className="flex items-center gap-3 pl-5 py-3 text-sm text-slate-500/70 hover:text-slate-400 font-['Geist'] transition-colors cursor-pointer"
          >
            <Icon size={18} />
            <span>{label}</span>
          </button>
        ))}
      </div>
    </aside>
  );
};

export default DashboardSidebar;
