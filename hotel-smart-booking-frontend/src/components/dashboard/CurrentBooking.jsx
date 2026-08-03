import { useLanguage } from "../../context/LanguageContext";

const CurrentBooking = ({ booking, onViewDetail }) => {
  const { t } = useLanguage();
  const { suiteName, refCode, status, checkIn, checkOut, guests } = booking;

  return (
    <div className="bg-white rounded-2xl border border-slate-200/80 shadow-sm hover:shadow-md transition-all duration-300 overflow-hidden font-['Montserrat'] text-left">
      <div className="p-4 sm:p-6 bg-slate-900 border-b border-slate-800 flex flex-wrap justify-between items-center gap-3">
        <div>
          <h2 className="text-white text-base sm:text-lg font-black uppercase tracking-wider m-0">
            {suiteName === "Grand Deluxe Suite" ? t("room_grand_deluxe_suite", "Phòng Grand Deluxe Suite") : suiteName}
          </h2>
          <p className="text-slate-400 text-[11px] font-bold uppercase tracking-widest mt-1 m-0">
            {t("db_current_ref_code", "Mã đặt phòng:")} <span className="text-amber-400 ml-1">{refCode}</span>
          </p>
        </div>
        <span className="px-3 py-1 bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 text-[9.5px] font-extrabold uppercase tracking-widest rounded-full shadow-2xs">
          {status}
        </span>
      </div>

      <div className="p-4 sm:p-6 grid grid-cols-1 sm:grid-cols-3 gap-4 sm:gap-6 bg-white">
        <div className="p-3 bg-slate-50 rounded-xl border border-slate-100">
          <p className="text-slate-400 text-[9.5px] font-bold uppercase tracking-widest m-0">
            {t("db_check_in_header", "NHẬN PHÒNG (CHECK-IN)")}
          </p>
          <p className="text-slate-900 text-xs sm:text-sm font-black mt-1 uppercase tracking-wide m-0">{checkIn.date}</p>
          <p className="text-slate-500 text-[10px] font-semibold uppercase tracking-wider m-0 mt-0.5">{checkIn.dayTime}</p>
        </div>
        <div className="p-3 bg-slate-50 rounded-xl border border-slate-100">
          <p className="text-slate-400 text-[9.5px] font-bold uppercase tracking-widest m-0">
            {t("db_check_out_header", "TRẢ PHÒNG (CHECK-OUT)")}
          </p>
          <p className="text-slate-900 text-xs sm:text-sm font-black mt-1 uppercase tracking-wide m-0">{checkOut.date}</p>
          <p className="text-slate-500 text-[10px] font-semibold uppercase tracking-wider m-0 mt-0.5">{checkOut.dayTime}</p>
        </div>
        <div className="p-3 bg-slate-50 rounded-xl border border-slate-100">
          <p className="text-slate-400 text-[9.5px] font-bold uppercase tracking-widest m-0">
            {t("db_guests_header", "SỐ KHÁCH (GUESTS)")}
          </p>
          <p className="text-slate-900 text-xs sm:text-sm font-black mt-1 uppercase tracking-wide m-0">{guests.count}</p>
          <p className="text-slate-500 text-[10px] font-semibold uppercase tracking-wider m-0 mt-0.5">{guests.bedInfo}</p>
        </div>
      </div>

      <div className="p-4 sm:p-5 bg-slate-50/80 border-t border-slate-100 flex flex-wrap gap-3 items-center justify-end">
        <button 
          onClick={onViewDetail}
          className="w-full sm:w-auto px-5 py-2.5 bg-primary hover:bg-slate-900 active:scale-97 text-white font-bold uppercase text-[10.5px] tracking-wider rounded-xl transition-all duration-200 cursor-pointer border-none shadow-xs hover:shadow-md flex items-center justify-center gap-1.5"
        >
          <span>{t("db_btn_view_detail", "Xem Chi Tiết")}</span>
          <span className="material-symbols-outlined text-base">arrow_forward</span>
        </button>
      </div>
    </div>
  );
};

export default CurrentBooking;
