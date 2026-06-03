const HeroBanner = ({ name, tier, nextStayDate }) => {
  return (
    <div className="h-72 rounded-2xl overflow-hidden relative">
      <img
        src="https://images.unsplash.com/photo-1566073771259-6a8506099945?w=1000&h=400&fit=crop"
        alt="Luxury hotel"
        className="absolute inset-0 w-full h-full object-cover"
      />
      <div className="absolute inset-0 bg-gradient-to-r from-black/90 via-black/40 to-transparent" />
      <div className="relative h-full p-10 flex justify-between items-end">
        <div>
          <span className="px-3 py-[3px] bg-amber-200 rounded-full text-lime-950 text-[10px] font-bold uppercase tracking-wide">
            {tier}
          </span>
          <h1 className="mt-3 text-white text-3xl font-bold font-['Playfair_Display']">
            Welcome back, {name}
          </h1>
          <p className="mt-1 text-white/70 text-sm font-['Geist']">
            Experience your next stay on {nextStayDate}
          </p>
        </div>
        <button className="px-8 py-3 bg-white rounded-xl text-black font-bold font-['Geist'] hover:bg-gray-50 transition">
          Manage Next Stay
        </button>
      </div>
    </div>
  );
};

export default HeroBanner;
