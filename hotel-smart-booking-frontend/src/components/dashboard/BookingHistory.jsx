import { ChevronRight, MoreVertical } from "lucide-react";

const BookingHistory = ({ bookings }) => {
  return (
    <div className="bg-white rounded-xl shadow-sm outline outline-1 outline-neutral-300/30 overflow-hidden">
      <div className="p-6 border-b border-neutral-300/30 flex justify-between items-center">
        <h2 className="text-black font-['Playfair_Display']">Booking History</h2>
        <button className="flex items-center gap-1 text-yellow-800 text-sm font-bold font-['Geist'] cursor-pointer">
          View All
          <ChevronRight size={16} />
        </button>
      </div>

      <div className="bg-gray-100 grid grid-cols-[1fr_1fr_0.8fr_0.8fr_0.3fr] px-6 py-4">
        <span className="text-zinc-700 text-xs font-bold uppercase tracking-wider font-['Geist']">
          DESTINATION
        </span>
        <span className="text-zinc-700 text-xs font-bold uppercase tracking-wider font-['Geist']">
          STAY PERIOD
        </span>
        <span className="text-zinc-700 text-xs font-bold uppercase tracking-wider font-['Geist']">
          AMOUNT
        </span>
        <span className="text-zinc-700 text-xs font-bold uppercase tracking-wider font-['Geist']">
          STATUS
        </span>
        <span />
      </div>

      <div>
        {bookings.map((booking, index) => (
          <div
            key={index}
            className="grid grid-cols-[1fr_1fr_0.8fr_0.8fr_0.3fr] px-6 py-5 border-t border-neutral-300/20 items-center"
          >
            <div>
              <p className="text-zinc-900 font-bold font-['Geist']">{booking.name}</p>
              <p className="text-zinc-700 text-xs font-['Geist']">{booking.roomType}</p>
            </div>
            <p className="text-zinc-900 text-sm font-['Geist']">{booking.period}</p>
            <p className="text-zinc-900 font-bold font-['Geist']">{booking.amount}</p>
            <div>
              <span className="px-3 py-1 bg-green-100 rounded-full text-green-700 text-xs font-bold font-['Geist']">
                {booking.status}
              </span>
            </div>
            <div className="flex justify-end">
              <MoreVertical size={18} className="text-zinc-500 cursor-pointer" />
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default BookingHistory;
