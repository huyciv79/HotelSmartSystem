const UpdatesFeed = ({ updates }) => {
  return (
    <div className="h-96 bg-white rounded-xl shadow-sm outline outline-1 outline-neutral-300/30 flex flex-col">
      <div className="p-6 border-b border-neutral-300/30">
        <h2 className="text-zinc-900 text-lg font-bold font-['Geist']">Updates</h2>
      </div>

      <div className="flex-1 px-6 pt-6 pb-1 flex flex-col gap-10 overflow-hidden">
        {updates.map((update) => {
          const Icon = update.icon;
          return (
            <div key={update.id} className="flex gap-4">
              <div
                className={`size-10 rounded-full ${update.iconBg} flex items-center justify-center shrink-0`}
              >
                <Icon className={`${update.iconColor} size-5`} />
              </div>

              <div className="min-w-0">
                <h3 className="text-zinc-900 text-sm font-bold font-['Geist']">{update.title}</h3>
                <p className="text-zinc-700 text-xs font-['Geist'] line-clamp-2">{update.preview}</p>
                <span className="pt-1 text-zinc-500 text-[10px] font-['Geist'] block">
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
