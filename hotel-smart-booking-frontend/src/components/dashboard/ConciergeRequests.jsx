const ConciergeRequests = ({ requests }) => {
  return (
    <div className="p-8 bg-white rounded-xl shadow-sm outline outline-1 outline-neutral-300/30">
      <h2 className="text-black font-['Playfair_Display'] text-2xl">Concierge Requests</h2>

      <div className="pl-8 relative mt-8 flex flex-col gap-12">
        <div className="absolute left-[11px] top-[8px] w-0.5 h-[calc(100%-16px)] bg-neutral-300/30" />

        {requests.map((request) => (
          <div
            key={request.id}
            className={`relative flex justify-between items-start ${!request.isActive ? "opacity-60" : ""}`}
          >
            <div
              className={`absolute left-[-32px] top-[4px] size-6 rounded-full border-4 border-white ${request.dotColor}`}
            />

            <div>
              <h3 className="text-zinc-900 text-lg font-bold font-['Geist']">{request.title}</h3>
              <p className="text-zinc-700 text-sm font-['Geist']">{request.description}</p>
            </div>

            <span
              className={`text-xs font-bold uppercase tracking-wider font-['Geist'] shrink-0 ml-4 ${request.statusColor}`}
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
