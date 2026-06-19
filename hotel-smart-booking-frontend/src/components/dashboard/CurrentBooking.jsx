import { useLanguage } from "../../context/LanguageContext";

const CurrentBooking = ({ booking, onViewDetail }) => {
  const { t } = useLanguage();
  const { suiteName, refCode, status, checkIn, checkOut, guests } = booking;

  return (
    <div className="bg-white rounded-none border border-outline-variant shadow-lg overflow-hidden font-['Montserrat'] text-left">
      <div className="p-6 bg-[#111] border-b border-slate-900 flex justify-between items-center">
        <div>
          <h2 className="text-white text-base font-black uppercase tracking-wider m-0">
            {suiteName === "Grand Deluxe Suite" ? t("room_grand_deluxe_suite", "Phòng Grand Deluxe Suite") : suiteName}
          </h2>
          <p className="text-slate-400 text-xs font-bold uppercase tracking-widest mt-1">
            {t("db_current_ref_code", "Mã đặt phòng:")} {refCode}
          </p>
        </div>
        <span className="px-4 py-2 bg-green-100 text-green-700 text-xs font-black uppercase tracking-widest rounded-none">
          {status}
        </span>
      </div>

      <div className="p-8 grid grid-cols-1 sm:grid-cols-3 gap-6">
        <div>
          <p className="text-slate-400 text-[10px] font-black uppercase tracking-widest">
            {t("db_check_in_header", "NHẬN PHÒNG (CHECK-IN)")}
          </p>
          <p className="text-slate-900 text-sm font-black mt-1 uppercase tracking-wide">{checkIn.date}</p>
          <p className="text-slate-500 text-[10px] font-bold uppercase tracking-wider">{checkIn.dayTime}</p>
        </div>
        <div>
          <p className="text-slate-400 text-[10px] font-black uppercase tracking-widest">
            {t("db_check_out_header", "TRẢ PHÒNG (CHECK-OUT)")}
          </p>
          <p className="text-slate-900 text-sm font-black mt-1 uppercase tracking-wide">{checkOut.date}</p>
          <p className="text-slate-500 text-[10px] font-bold uppercase tracking-wider">{checkOut.dayTime}</p>
        </div>
        <div>
          <p className="text-slate-400 text-[10px] font-black uppercase tracking-widest">
            {t("db_guests_header", "SỐ KHÁCH (GUESTS)")}
          </p>
          <p className="text-slate-900 text-sm font-black mt-1 uppercase tracking-wide">{guests.count}</p>
          <p className="text-slate-500 text-[10px] font-bold uppercase tracking-wider">{guests.bedInfo}</p>
        </div>
      </div>

      <div className="p-6 bg-slate-50 border-t border-slate-150 flex flex-wrap gap-4 items-center">
        <button className="px-6 py-2.5 bg-slate-900 hover:bg-slate-850 text-white font-bold uppercase text-[10px] tracking-widest transition-all cursor-pointer border-none rounded-none h-10">
          {t("db_btn_request", "Gửi yêu cầu")}
        </button>
        <button 
          onClick={onViewDetail}
          className="px-6 py-2.5 bg-primary hover:bg-slate-950 text-white font-bold uppercase text-[10px] tracking-widest transition-all cursor-pointer border-none parallelogram-btn h-10"
        >
          {t("db_btn_view_detail", "Xem Chi Tiết")}
        </button>
      </div>
    </div>
  );
};

export default CurrentBooking;
