import { Star } from "lucide-react";

const SuiteCard = ({ suite }) => {
  return (
    <div className="min-w-[320px] w-80 bg-white rounded-xl shadow-sm overflow-hidden inline-flex flex-col hover:-translate-y-1 hover:shadow-md transition-all duration-300 group">
      <div className="h-48 relative overflow-hidden">
        <img
          src={suite.imageUrl}
          alt={suite.name}
          className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
        />
        <div className="absolute top-4 right-4 px-3 py-1 bg-white/90 rounded-full backdrop-blur-sm flex items-center gap-1">
          <Star size={12} className="fill-yellow-800 text-yellow-800" />
          <span className="text-zinc-900 text-xs font-bold font-['Geist']">
            {suite.rating}
          </span>
        </div>
      </div>

      <div className="p-6 flex flex-col gap-3">
        <p className="text-zinc-900 text-lg font-bold font-['Geist']">
          {suite.name}
        </p>
        <div className="flex justify-between items-center">
          <span className="text-zinc-700 text-sm font-['Geist']">
            {suite.amenities}
          </span>
          <span className="text-black text-sm font-bold font-['Geist']">
            {suite.price}
            <span className="text-xs font-normal">/night</span>
          </span>
        </div>
      </div>
    </div>
  );
};

export default SuiteCard;
