import { ChevronRight, MoreVertical } from "lucide-react";

const BookingHistory = ({ bookings, onViewDetail }) => {
  return (
    <div className="bg-white rounded-none border border-outline-variant shadow-lg overflow-hidden font-['Montserrat'] text-left">
      <div className="p-6 border-b border-neutral-300/30 flex justify-between items-center bg-slate-50">
        <h2 className="text-black font-black uppercase text-sm tracking-wider m-0">Lịch sử đặt phòng</h2>
        <button className="flex items-center gap-1 text-primary text-[10px] font-black uppercase tracking-wider cursor-pointer border-none bg-transparent hover:text-slate-900 transition-colors">
          Xem tất cả
          <ChevronRight size={14} />
        </button>
      </div>

      <div className="bg-slate-100 grid grid-cols-[1fr_1fr_0.8fr_0.8fr_0.3fr] px-6 py-4 border-b border-slate-200">
        <span className="text-slate-500 text-[9px] font-black uppercase tracking-wider">
          ĐIỂM ĐẾN
        </span>
        <span className="text-slate-500 text-[9px] font-black uppercase tracking-wider">
          THỜI GIAN LƯU TRÚ
        </span>
        <span className="text-slate-500 text-[9px] font-black uppercase tracking-wider">
          TỔNG TIỀN
        </span>
        <span className="text-slate-500 text-[9px] font-black uppercase tracking-wider">
          TRẠNG THÁI
        </span>
        <span />
      </div>

      <div>
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
                className="grid grid-cols-[1fr_1fr_0.8fr_0.8fr_0.3fr] px-6 py-5 border-t border-neutral-300/20 items-center cursor-pointer hover:bg-slate-50 transition-colors"
              >
                <div>
                  <p className="text-zinc-900 font-black text-sm uppercase tracking-wide">{booking.name}</p>
                  <p className="text-zinc-500 text-[10px] font-bold uppercase tracking-wider">{booking.roomType}</p>
                </div>
                <p className="text-zinc-800 text-xs font-bold uppercase tracking-wider">{booking.period}</p>
                <p className="text-primary text-xs font-black">{booking.amount}</p>
                <div>
                  <span className={`px-2.5 py-1 text-[9px] font-black uppercase tracking-widest rounded-none ${statusClass}`}>
                    {statusText}
                  </span>
                </div>
                <div className="flex justify-end">
                  <MoreVertical size={16} className="text-zinc-400 cursor-pointer" />
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
