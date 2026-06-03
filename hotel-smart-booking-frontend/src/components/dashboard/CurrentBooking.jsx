const CurrentBooking = ({ booking }) => {
  const { suiteName, refCode, status, checkIn, checkOut, guests } = booking;

  return (
    <div className="bg-white rounded-xl shadow-sm outline outline-1 outline-neutral-300/30 overflow-hidden">
      <div className="p-6 bg-slate-900 flex justify-between items-center">
        <div>
          <h2 className="text-white font-['Playfair_Display']">{suiteName}</h2>
          <p className="text-slate-500 text-sm font-['Geist']">Ref: {refCode}</p>
        </div>
        <span className="px-4 py-2 bg-green-500/20 rounded-full text-green-500 text-sm font-bold font-['Geist']">
          {status}
        </span>
      </div>

      <div className="p-8 grid grid-cols-3 gap-6">
        <div>
          <p className="text-zinc-700 text-xs font-bold uppercase tracking-wider font-['Geist']">
            CHECK IN
          </p>
          <p className="text-zinc-900 font-['Playfair_Display']">{checkIn.date}</p>
          <p className="text-zinc-700 font-['Geist']">{checkIn.dayTime}</p>
        </div>
        <div>
          <p className="text-zinc-700 text-xs font-bold uppercase tracking-wider font-['Geist']">
            CHECK OUT
          </p>
          <p className="text-zinc-900 font-['Playfair_Display']">{checkOut.date}</p>
          <p className="text-zinc-700 font-['Geist']">{checkOut.dayTime}</p>
        </div>
        <div>
          <p className="text-zinc-700 text-xs font-bold uppercase tracking-wider font-['Geist']">
            GUESTS
          </p>
          <p className="text-zinc-900 font-['Playfair_Display']">{guests.count}</p>
          <p className="text-zinc-700 font-['Geist']">{guests.bedInfo}</p>
        </div>
      </div>

      <div className="p-6 bg-gray-100 border-t border-neutral-300/30 flex items-center gap-4">
        <button className="px-6 py-2 bg-black rounded-lg text-white font-['Geist'] hover:bg-gray-800 transition cursor-pointer">
          Contact Concierge
        </button>
        <button className="px-6 py-2 rounded-lg outline outline-2 outline-amber-200 text-yellow-800 text-sm font-bold font-['Geist'] hover:bg-amber-50 transition cursor-pointer">
          Modify Booking
        </button>
        <button className="ml-auto px-6 py-2.5 text-red-500 text-sm font-bold font-['Geist'] hover:bg-red-50 hover:rounded-lg transition cursor-pointer">
          Cancel Stay
        </button>
      </div>
    </div>
  );
};

export default CurrentBooking;
