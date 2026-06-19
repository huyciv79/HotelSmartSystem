import { ChevronRight, MoreVertical } from "lucide-react";

const BookingHistory = ({ bookings, onViewDetail, onViewAll }) => {
  return (
    <div className="bg-white rounded-none border border-outline-variant shadow-lg overflow-hidden font-['Montserrat'] text-left">
      <div className="p-6 border-b border-neutral-300/30 flex justify-between items-center bg-slate-50">
        <h2 className="text-black font-black uppercase text-sm tracking-wider m-0">Lịch sử đặt phòng</h2>
        <button 
          onClick={onViewAll}
          className="flex items-center gap-1 text-primary text-[10px] font-black uppercase tracking-wider cursor-pointer border-none bg-transparent hover:text-slate-900 transition-colors"
        >
          Xem tất cả
          <ChevronRight size={14} />
        </button>
      </div>

      <div className="flex flex-col divide-y divide-neutral-100">
        {bookings.length === 0 ? (
          <div className="p-8 text-center text-slate-400 font-bold text-[10px] uppercase tracking-wider">
            Không có lịch sử đặt phòng
          </div>
        ) : (
          bookings.map((booking, index) => {
            let statusText = booking.status || 'Confirmed';
            let statusClass = 'bg-green-100 text-green-700';

            const normalizedStatus = (booking.status || '').toLowerCase().replace(/[^a-z]/g, '');
            if (normalizedStatus === 'cancelled') {
              statusText = 'Đã hủy';
              statusClass = 'bg-red-100 text-red-700';
            } else if (normalizedStatus === 'checkedout' || normalizedStatus === 'completed') {
              statusText = 'Đã trả phòng';
              statusClass = 'bg-slate-200 text-slate-700';
            } else if (normalizedStatus === 'checkedin') {
              statusText = 'Đã nhận phòng';
              statusClass = 'bg-blue-100 text-blue-700';
            } else if (normalizedStatus === 'paid') {
              statusText = 'Đã thanh toán';
              statusClass = 'bg-green-100 text-green-700';
            } else if (normalizedStatus === 'partiallypaid' || normalizedStatus === 'depositpaid') {
              statusText = 'Đã cọc 30%';
              statusClass = 'bg-indigo-100 text-indigo-700';
            } else {
              statusText = 'Chờ thanh toán';
              statusClass = 'bg-yellow-100 text-yellow-700';
            }

            return (
              <div
                key={index}
                onClick={() => onViewDetail && onViewDetail(booking.id)}
                className="p-5 flex flex-col gap-3.5 hover:bg-slate-50 transition-colors cursor-pointer"
              >
                {/* Top Row: Room name & Status */}
                <div className="flex justify-between items-start gap-4">
                  <div className="flex-1 min-w-0">
                    <p className="text-zinc-900 font-black text-xs uppercase tracking-wide truncate">{booking.name}</p>
                    <p className="text-zinc-400 text-[9px] font-bold uppercase tracking-wider mt-0.5">{booking.roomType}</p>
                  </div>
                  <span className={`px-2.5 py-1 text-[8px] font-black uppercase tracking-widest shrink-0 ${statusClass}`}>
                    {statusText}
                  </span>
                </div>

                {/* Bottom Row: Period & Amount */}
                <div className="flex justify-between items-end border-t border-dashed border-slate-100 pt-2.5">
                  <div className="text-[10px] text-zinc-500 font-bold uppercase tracking-wider">
                    {booking.period}
                  </div>
                  <div className="text-right">
                    <span className="text-[8px] text-slate-400 font-bold uppercase tracking-wider block leading-none mb-0.5">TỔNG TIỀN</span>
                    <span className="text-primary text-xs font-black">{booking.amount}</span>
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
