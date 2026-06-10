import { Star } from "lucide-react";

const SuiteCard = ({ suite }) => {
  return (
    <div className="min-w-[320px] w-80 bg-white rounded-none border border-outline-variant shadow-lg overflow-hidden inline-flex flex-col hover:-translate-y-1 transition-all duration-300 group font-['Montserrat'] text-left">
      <div className="h-48 relative overflow-hidden">
        <img
          src={suite.imageUrl}
          alt={suite.name}
          className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
        />
        <div className="absolute top-4 right-4 px-3 py-1 bg-white/95 rounded-none shadow-sm flex items-center gap-1">
          <Star size={12} className="fill-amber-500 text-amber-500" />
          <span className="text-zinc-950 text-xs font-black">
            {suite.rating}
          </span>
        </div>
      </div>

      <div className="p-6 flex flex-col gap-3">
        <p className="text-zinc-950 text-base font-black uppercase tracking-wide">
          {suite.name}
        </p>
        <div className="flex justify-between items-center text-xs font-bold text-slate-700">
          <span className="text-slate-400 uppercase tracking-wider">
            {suite.amenities}
          </span>
          <span className="text-primary font-black">
            {suite.price}
            <span className="text-[10px] text-slate-500 font-bold uppercase"> / đêm</span>
          </span>
        </div>
      </div>
    </div>
  );
};

export default SuiteCard;
