const ConciergeRequests = ({ requests }) => {
  return (
    <div className="p-8 bg-white rounded-none border border-outline-variant shadow-lg font-['Montserrat'] text-left">
      <h2 className="text-black font-black uppercase text-sm tracking-wider border-b border-gray-150 pb-2 mb-6">Concierge Requests</h2>

      <div className="pl-8 relative mt-8 flex flex-col gap-12">
        <div className="absolute left-[11px] top-[8px] w-0.5 h-[calc(100%-16px)] bg-neutral-200" />

        {requests.map((request) => (
          <div
            key={request.id}
            className={`relative flex justify-between items-start ${!request.isActive ? "opacity-60" : ""}`}
          >
            <div
              className={`absolute left-[-32px] top-[4px] size-6 rounded-none border-4 border-white ${
                request.status === 'CONFIRMED' ? 'bg-primary' : 'bg-amber-500'
              }`}
            />

            <div>
              <h3 className="text-zinc-950 text-sm font-black uppercase tracking-wide">{request.title}</h3>
              <p className="text-zinc-500 text-xs font-bold mt-1">{request.description}</p>
            </div>

            <span
              className={`text-[9px] font-black uppercase tracking-wider shrink-0 ml-4 ${
                request.status === 'CONFIRMED' ? 'text-primary' : 'text-amber-600'
              }`}
            >
              {request.status}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
};

export default ConciergeRequests;
