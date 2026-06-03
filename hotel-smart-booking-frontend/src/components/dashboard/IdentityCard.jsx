import { ShieldCheck, CreditCard, Smartphone } from "lucide-react";

const IdentityCard = () => {
  return (
    <div className="p-8 bg-slate-900 rounded-xl shadow-xl relative overflow-hidden">
      <div className="absolute size-40 bg-amber-200 rounded-full blur-[32px] opacity-10 left-[170px] top-[169px]" />

      <div className="relative z-10 flex flex-col gap-6">
        <div className="flex justify-between items-center">
          <h2 className="text-white font-['Playfair_Display'] text-2xl">Identity Verified</h2>
          <ShieldCheck className="text-amber-200 size-6" />
        </div>

        <p className="text-slate-500 text-sm font-['Geist']">
          Your digital keys and seamless check-in are enabled.
        </p>

        <div className="flex gap-2">
          <div className="w-24 h-16 bg-white/10 rounded-lg outline outline-1 outline-white/20 flex items-center justify-center">
            <CreditCard className="text-white/40" />
          </div>
          <div className="w-24 h-16 bg-white/10 rounded-lg outline outline-1 outline-white/20 flex items-center justify-center">
            <Smartphone className="text-white/40" />
          </div>
        </div>

        <p className="text-white/50 text-xs uppercase tracking-wider font-['Geist']">
          VERIFIED ON SEPT 14, 2024
        </p>
      </div>
    </div>
  );
};

export default IdentityCard;
