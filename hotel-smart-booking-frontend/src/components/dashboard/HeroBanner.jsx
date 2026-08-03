import { useLanguage } from "../../context/LanguageContext";

const HeroBanner = ({ name, tier, nextStayDate }) => {
  const { t } = useLanguage();
  return (
    <div className="h-auto min-h-[200px] sm:h-72 rounded-2xl overflow-hidden relative font-['Montserrat'] border border-slate-200/60 shadow-md hover:shadow-lg transition-all duration-300">
      <img
        src="https://images.unsplash.com/photo-1566073771259-6a8506099945?w=1000&h=400&fit=crop"
        alt="Luxury hotel"
        className="absolute inset-0 w-full h-full object-cover"
      />
      <div className="absolute inset-0 bg-gradient-to-r from-black/85 via-black/50 to-black/10" />
      <div className="relative h-full p-6 sm:p-10 flex flex-col sm:flex-row justify-between items-start sm:items-end gap-4 text-left">
        <div>
          <span className="px-3 py-1 bg-primary text-white text-[9px] font-black uppercase tracking-widest rounded-lg shadow-sm">
            {tier}
          </span>
          <h1 className="mt-3 text-white text-xl sm:text-3xl font-black uppercase tracking-wider leading-tight m-0">
            {t('db_hero_welcome_back', 'Chào mừng trở lại')}, {name}
          </h1>
          <p className="mt-2 text-white/80 text-[11px] sm:text-xs font-semibold uppercase tracking-wider m-0">
            {t('db_hero_next_stay', 'Kỳ nghỉ tiếp theo của bạn bắt đầu từ ngày')} <span className="font-bold text-amber-300">{nextStayDate}</span>
          </p>
        </div>
      </div>
    </div>
  );
};

export default HeroBanner;
