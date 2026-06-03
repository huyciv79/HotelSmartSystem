import { Search, Bell, Cog } from "lucide-react";

const DashboardHeader = ({ avatarUrl }) => {
  return (
    <header className="sticky top-0 left-72 w-[calc(100%-288px)] h-16 bg-white/70 backdrop-blur-sm border-b border-amber-400/20 shadow-sm z-10 flex items-center justify-between px-8">
      <div>
        <h2 className="text-black font-bold text-lg font-['Playfair_Display']">
          Dashboard
        </h2>
        <p className="text-zinc-700/70 text-xs uppercase tracking-wider font-['Geist']">
          Welcome back to your luxury sanctuary
        </p>
      </div>

      <div className="flex items-center gap-8">
        <div className="relative">
          <Search
            size={16}
            className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400"
          />
          <input
            type="text"
            placeholder="Search concierge..."
            className="w-64 pl-10 pr-4 py-2 bg-gray-100 rounded-xl text-sm font-['Geist'] outline-none placeholder:text-gray-400"
          />
        </div>

        <div className="flex items-center gap-4">
          <button className="relative p-2 text-gray-600 hover:text-gray-800 transition-colors cursor-pointer">
            <Bell size={20} />
            <span className="absolute top-1.5 right-1.5 size-2 bg-amber-200 rounded-full" />
          </button>

          <button className="p-2 text-gray-600 hover:text-gray-800 transition-colors cursor-pointer">
            <Cog size={20} />
          </button>

          <img
            src={avatarUrl}
            alt="User avatar"
            className="size-10 rounded-full border-2 border-amber-200 object-cover"
          />
        </div>
      </div>
    </header>
  );
};

export default DashboardHeader;
