import { ChevronRight, MoreVertical } from "lucide-react";
import { useLanguage } from "../../context/LanguageContext";

const BookingHistory = ({ bookings, onViewDetail, onViewAll }) => {
  const { t } = useLanguage();

  return (
    <div className="bg-white rounded-2xl border border-slate-200/80 shadow-sm hover:shadow-md transition-all duration-300 overflow-hidden font-['Montserrat'] text-left">
      <div className="p-4 sm:p-5 border-b border-slate-100 flex justify-between items-center bg-slate-50/70">
        <h2 className="text-slate-900 font-black uppercase text-xs sm:text-sm tracking-wider m-0">
          {t("db_history_title", "Lịch sử đặt phòng")}
        </h2>
        <button 
          onClick={onViewAll}
          className="flex items-center gap-1 text-primary text-[10px] font-black uppercase tracking-wider cursor-pointer border-none bg-transparent hover:text-slate-900 transition-colors"
        >
          {t("db_history_view_all", "Xem tất cả")}
          <ChevronRight size={14} />
        </button>
      </div>

      <div className="flex flex-col divide-y divide-slate-100">
        {bookings.length === 0 ? (
          <div className="p-8 text-center text-slate-400 font-bold text-[10px] uppercase tracking-wider">
            {t("db_history_empty", "Không có lịch sử đặt phòng")}
          </div>
        ) : (
          bookings.map((booking, index) => {
            let statusText = booking.status || 'Confirmed';
            let statusClass = 'bg-emerald-100 text-emerald-700 border-emerald-200';

            const normalizedStatus = (booking.status || '').toLowerCase().replace(/[^a-z]/g, '');
            if (normalizedStatus === 'cancelled') {
              statusText = t("status_cancelled", "Đã hủy");
              statusClass = 'bg-rose-100 text-rose-700 border-rose-200';
            } else if (normalizedStatus === 'checkedout' || normalizedStatus === 'completed') {
              statusText = t("status_checked_out", "Đã trả phòng");
              statusClass = 'bg-slate-200 text-slate-700 border-slate-300';
            } else if (normalizedStatus === 'checkedin') {
              statusText = t("status_checked_in", "Đã nhận phòng");
              statusClass = 'bg-sky-100 text-sky-700 border-sky-200';
            } else if (normalizedStatus === 'paid') {
              statusText = 'Đã thanh toán';
              statusClass = 'bg-emerald-100 text-emerald-700 border-emerald-200';
            } else if (normalizedStatus === 'partiallypaid' || normalizedStatus === 'depositpaid') {
              statusText = 'Đã cọc 30%';
              statusClass = 'bg-indigo-100 text-indigo-700 border-indigo-200';
            } else {
              statusText = 'Chờ thanh toán';
              statusClass = 'bg-amber-100 text-amber-700 border-amber-200';
            }

            return (
              <div
                key={index}
                onClick={() => onViewDetail && onViewDetail(booking.id)}
                className="p-4 sm:p-5 flex flex-col gap-3 hover:bg-slate-50/80 transition-all duration-200 cursor-pointer"
              >
                {/* Top Row: Room name & Status */}
                <div className="flex justify-between items-start gap-3">
                  <div className="flex-1 min-w-0">
                    <p className="text-slate-900 font-black text-xs sm:text-sm uppercase tracking-wide truncate m-0">{booking.name}</p>
                    <p className="text-slate-400 text-[10px] font-semibold uppercase tracking-wider mt-0.5 m-0">
                      <span className="font-bold text-slate-600">{booking.roomType}</span>
                    </p>
                  </div>
                  <span className={`px-2.5 py-1 text-[8.5px] font-extrabold uppercase tracking-wider shrink-0 rounded-full border ${statusClass}`}>
                    {statusText}
                  </span>
                </div>

                {/* Bottom Row: Period & Amount */}
                <div className="flex justify-between items-end border-t border-slate-100 pt-2.5">
                  <div className="text-[10px] text-slate-500 font-medium">
                    {booking.period}
                  </div>
                  <div className="text-right">
                    <span className="text-[8.5px] text-slate-400 font-bold uppercase tracking-wider block leading-none mb-1">
                      {t("db_history_total_cost", "TỔNG TIỀN")}
                    </span>
                    <span className="text-primary text-xs sm:text-sm font-black tracking-tight">{booking.amount}</span>
                  </div>
                </div>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
};

export default BookingHistory;
