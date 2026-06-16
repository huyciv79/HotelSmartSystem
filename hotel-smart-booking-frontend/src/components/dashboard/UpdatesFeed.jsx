const UpdatesFeed = ({ updates }) => {
  return (
    <div className="h-96 bg-white rounded-none border border-neutral-300/30 flex flex-col font-['Montserrat'] shadow-sm">
      <div className="p-6 border-b border-neutral-200">
        <h2 className="text-zinc-900 text-sm font-bold uppercase tracking-widest">Updates</h2>
      </div>

      <div className="flex-1 px-6 pt-6 pb-2 flex flex-col gap-6 overflow-y-auto">
        {updates.map((update) => {
          const Icon = update.icon;
          
          // Override background and color for a unified premium design
          let iconBgStyle = "bg-primary/5 text-primary border border-primary/20";
          if (update.title === "New Autumn Menu") {
            iconBgStyle = "bg-zinc-100 text-zinc-800 border border-zinc-200";
          } else if (update.title === "Payment Processed") {
            iconBgStyle = "bg-emerald-50 text-emerald-700 border border-emerald-200";
          }

          return (
            <div key={update.id} className="flex gap-4 items-start">
              <div
                className={`size-10 rounded-none flex items-center justify-center shrink-0 ${iconBgStyle}`}
              >
                <Icon className="size-4" />
              </div>

              <div className="min-w-0 flex-1">
                <h3 className="text-zinc-950 text-xs font-bold uppercase tracking-wide">{update.title}</h3>
                <p className="text-zinc-600 text-xs mt-1 leading-relaxed font-medium line-clamp-2">{update.preview}</p>
                <span className="pt-1.5 text-zinc-400 text-[9px] font-bold uppercase tracking-wider block">
                  {update.time}
                </span>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default UpdatesFeed;
