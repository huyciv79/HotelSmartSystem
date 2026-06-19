import { useLanguage } from "../../context/LanguageContext";

const HeroBanner = ({ name, tier, nextStayDate }) => {
  const { t } = useLanguage();
  return (
    <div className="h-72 rounded-none overflow-hidden relative font-['Montserrat'] border border-outline-variant shadow-lg">
      <img
        src="https://images.unsplash.com/photo-1566073771259-6a8506099945?w=1000&h=400&fit=crop"
        alt="Luxury hotel"
        className="absolute inset-0 w-full h-full object-cover"
      />
      <div className="absolute inset-0 bg-gradient-to-r from-black/85 via-black/30 to-transparent" />
      <div className="relative h-full p-10 flex flex-col sm:flex-row justify-between items-start sm:items-end gap-4 text-left">
        <div>
          <span className="px-3 py-1 bg-primary text-white text-[9px] font-black uppercase tracking-widest rounded-none">
            {tier}
          </span>
          <h1 className="mt-3 text-white text-3xl font-black uppercase tracking-wider italic leading-tight">
            {t('db_hero_welcome_back', 'Chào mừng trở lại')}, {name}
          </h1>
          <p className="mt-1.5 text-white/80 text-xs font-bold uppercase tracking-wider">
            {t('db_hero_next_stay', 'Kỳ nghỉ tiếp theo của bạn bắt đầu từ ngày')} {nextStayDate}
          </p>
        </div>
        {/* Removed Quản lý kỳ nghỉ button */}
      </div>
    </div>
  );
};

export default HeroBanner;
